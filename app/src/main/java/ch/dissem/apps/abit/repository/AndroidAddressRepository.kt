/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.apps.abit.repository

import android.content.ContentValues
import android.database.Cursor
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.payload.V3Pubkey
import ch.dissem.bitmessage.entity.payload.V4Pubkey
import ch.dissem.bitmessage.entity.valueobject.PrivateKey
import ch.dissem.bitmessage.factory.Factory
import ch.dissem.bitmessage.ports.AddressRepository
import ch.dissem.bitmessage.utils.Encode
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * [AddressRepository] implementation using the Android SQL API.
 */
class AndroidAddressRepository(private val sql: SqlHelper) : AddressRepository {

    override fun findContact(ripeOrTag: ByteArray): BitmessageAddress? = findByRipeOrTag("public_key is null", ripeOrTag)

    override fun findIdentity(ripeOrTag: ByteArray): BitmessageAddress? = findByRipeOrTag("private_key is not null", ripeOrTag)

    private fun findByRipeOrTag(where: String, ripeOrTag: ByteArray): BitmessageAddress? {
        for (address in find(where)) {
            if (address.version > 3) {
                if (Arrays.equals(ripeOrTag, address.tag)) return address
            } else {
                if (Arrays.equals(ripeOrTag, address.ripe)) return address
            }
        }
        return null
    }

    override fun getIdentities() = find("private_key IS NOT NULL")

    override fun getChans() = find("chan = '1'")

    override fun getSubscriptions() = find("subscribed = '1'")

    override fun getSubscriptions(broadcastVersion: Long) = if (broadcastVersion > 4) {
        find("subscribed = '1' AND version > 3")
    } else {
        find("subscribed = '1' AND version <= 3")
    }

    override fun getContacts() = find("private_key IS NULL OR chan = '1'")

    /**
     * Returns the contacts in the following order:
     *
     *  * Subscribed addresses come first
     *  * Addresses with aliases (alphabetically)
     *  * Addresses without aliases are omitted
     *
     *
     * @return the ordered list of ids (address strings)
     */
    fun getContactIds(): List<String> = findIds(
        "($COLUMN_PRIVATE_KEY IS NULL OR $COLUMN_CHAN = '1') AND $COLUMN_ALIAS IS NOT NULL",
        "$COLUMN_SUBSCRIBED DESC, $COLUMN_ALIAS, $COLUMN_ADDRESS"
    )

    private fun findIds(where: String, orderBy: String): List<String> {
        val result = LinkedList<String>()

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_ADDRESS)

        sql.readableDatabase.query(
            TABLE_NAME, projection,
            where, null, null, null,
            orderBy
        ).use { c ->
            while (c.moveToNext()) {
                result.add(c.getString(c.getColumnIndex(COLUMN_ADDRESS)))
            }
        }
        return result
    }

    private fun find(where: String): List<BitmessageAddress> {
        val result = LinkedList<BitmessageAddress>()

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_ADDRESS, COLUMN_ALIAS, COLUMN_PUBLIC_KEY, COLUMN_PRIVATE_KEY, COLUMN_SUBSCRIBED, COLUMN_CHAN)

        sql.readableDatabase.query(
            TABLE_NAME, projection,
            where, null, null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                result.add(getAddress(c))
            }
        }
        return result
    }

    private fun getAddress(c: Cursor): BitmessageAddress {

        fun getIdentity(c: Cursor) = c.getBlob(c.getColumnIndex(COLUMN_PRIVATE_KEY))?.let {
            BitmessageAddress(PrivateKey.read(ByteArrayInputStream(it)))
        }

        fun getContact(c: Cursor) = BitmessageAddress(c.getString(c.getColumnIndex(COLUMN_ADDRESS))).also { address ->
            c.getBlob(c.getColumnIndex(COLUMN_PUBLIC_KEY))?.let { publicKeyBytes ->
                Factory.readPubkey(
                    version = address.version, stream = address.stream,
                    input = ByteArrayInputStream(publicKeyBytes), length = publicKeyBytes.size,
                    encrypted = false
                ).let {
                    address.pubkey = if (address.version == 4L && it is V3Pubkey) {
                        V4Pubkey(it)
                    } else {
                        it
                    }
                }
            }
        }

        return (getIdentity(c) ?: getContact(c)).apply {
            alias = c.getString(c.getColumnIndex(COLUMN_ALIAS))
            isChan = c.getInt(c.getColumnIndex(COLUMN_CHAN)) == 1
            isSubscribed = c.getInt(c.getColumnIndex(COLUMN_SUBSCRIBED)) == 1
        }
    }

    override fun save(address: BitmessageAddress) = if (exists(address)) {
        update(address)
    } else {
        insert(address)
    }

    private fun exists(address: BitmessageAddress): Boolean {
        sql.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM Address WHERE address=?",
            arrayOf(address.address)
        ).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0) > 0
        }
    }

    private fun update(address: BitmessageAddress) {
        // Create a new map of values, where column names are the keys
        val values = getContentValues(address)

        val update = sql.writableDatabase.update(TABLE_NAME, values, "address=?", arrayOf(address.address))
        if (update < 0) {
            LOG.error("Could not update address {}", address)
        }
    }

    private fun insert(address: BitmessageAddress) {
        // Create a new map of values, where column names are the keys
        val values = getContentValues(address).apply {
            put(COLUMN_ADDRESS, address.address)
            put(COLUMN_VERSION, address.version)
            put(COLUMN_CHAN, address.isChan)
        }

        val insert = sql.writableDatabase.insert(TABLE_NAME, null, values)
        if (insert < 0) {
            LOG.error("Could not insert address {}", address)
        }
    }

    private fun getContentValues(address: BitmessageAddress) = ContentValues().apply {
        address.alias?.let { put(COLUMN_ALIAS, it) }
        address.pubkey?.let { pubkey ->
            val out = ByteArrayOutputStream()
            pubkey.writer().writeUnencrypted(out)
            put(COLUMN_PUBLIC_KEY, out.toByteArray())
        }
        address.privateKey?.let { put(COLUMN_PRIVATE_KEY, Encode.bytes(it)) }
        if (address.isChan) {
            put(COLUMN_CHAN, true)
        }
        put(COLUMN_SUBSCRIBED, address.isSubscribed)
    }

    override fun remove(address: BitmessageAddress) {
        sql.writableDatabase.delete(TABLE_NAME, "address = ?", arrayOf(address.address))
    }

    override fun getAddress(address: String) = find("address = '$address'").firstOrNull()

    companion object {
        private val LOG = LoggerFactory.getLogger(AndroidAddressRepository::class.java)

        private const val TABLE_NAME = "Address"
        private const val COLUMN_ADDRESS = "address"
        private const val COLUMN_VERSION = "version"
        private const val COLUMN_ALIAS = "alias"
        private const val COLUMN_PUBLIC_KEY = "public_key"
        private const val COLUMN_PRIVATE_KEY = "private_key"
        private const val COLUMN_SUBSCRIBED = "subscribed"
        private const val COLUMN_CHAN = "chan"
    }
}
