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
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.payload.V3Pubkey
import ch.dissem.bitmessage.entity.payload.V4Pubkey
import ch.dissem.bitmessage.entity.valueobject.PrivateKey
import ch.dissem.bitmessage.exception.ApplicationException
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

    override fun findContact(ripeOrTag: ByteArray): BitmessageAddress? {
        for (address in find("public_key is null")) {
            if (address.version > 3) {
                if (Arrays.equals(ripeOrTag, address.tag)) return address
            } else {
                if (Arrays.equals(ripeOrTag, address.ripe)) return address
            }
        }
        return null
    }

    override fun findIdentity(ripeOrTag: ByteArray): BitmessageAddress? {
        for (address in find("private_key is not null")) {
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
     *  * Addresses with Aliases (alphabetically)
     *  * Addresses (alphabetically)
     *
     *
     * @return the ordered list of ids (address strings)
     */
    fun getContactIds(): List<String> = findIds(
            "private_key IS NULL OR chan = '1'",
            "$COLUMN_SUBSCRIBED DESC, $COLUMN_ALIAS IS NULL, $COLUMN_ALIAS, $COLUMN_ADDRESS"
    )

    private fun findIds(where: String, orderBy: String): List<String> {
        val result = LinkedList<String>()

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_ADDRESS)

        val db = sql.readableDatabase
        db.query(
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

        val db = sql.readableDatabase
        db.query(
                TABLE_NAME, projection,
                where, null, null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                val address: BitmessageAddress

                val privateKeyBytes = c.getBlob(c.getColumnIndex(COLUMN_PRIVATE_KEY))
                if (privateKeyBytes != null) {
                    val privateKey = PrivateKey.read(ByteArrayInputStream(privateKeyBytes))
                    address = BitmessageAddress(privateKey)
                } else {
                    address = BitmessageAddress(c.getString(c.getColumnIndex(COLUMN_ADDRESS)))
                    val publicKeyBytes = c.getBlob(c.getColumnIndex(COLUMN_PUBLIC_KEY))
                    if (publicKeyBytes != null) {
                        var pubkey = Factory.readPubkey(address.version, address
                                .stream,
                                ByteArrayInputStream(publicKeyBytes), publicKeyBytes.size,
                                false)
                        if (address.version == 4L && pubkey is V3Pubkey) {
                            pubkey = V4Pubkey(pubkey)
                        }
                        address.pubkey = pubkey
                    }
                }
                address.alias = c.getString(c.getColumnIndex(COLUMN_ALIAS))
                address.isChan = c.getInt(c.getColumnIndex(COLUMN_CHAN)) == 1
                address.isSubscribed = c.getInt(c.getColumnIndex(COLUMN_SUBSCRIBED)) == 1

                result.add(address)
            }
        }
        return result
    }

    override fun save(address: BitmessageAddress) {
        if (exists(address)) {
            update(address)
        } else {
            insert(address)
        }
    }

    private fun exists(address: BitmessageAddress): Boolean {
        val db = sql.readableDatabase
        db.rawQuery(
                "SELECT COUNT(*) FROM Address WHERE address=?",
                arrayOf(address.address)
        ).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0) > 0
        }
    }

    private fun update(address: BitmessageAddress) {
        val db = sql.writableDatabase
        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        address.alias?.let { values.put(COLUMN_ALIAS, it) }
        address.pubkey?.let { pubkey ->
            val out = ByteArrayOutputStream()
            pubkey.writeUnencrypted(out)
            values.put(COLUMN_PUBLIC_KEY, out.toByteArray())
        }
        address.privateKey?.let { values.put(COLUMN_PRIVATE_KEY, Encode.bytes(it)) }
        if (address.isChan) {
            values.put(COLUMN_CHAN, true)
        }
        values.put(COLUMN_SUBSCRIBED, address.isSubscribed)

        val update = db.update(TABLE_NAME, values, "address=?", arrayOf(address.address))
        if (update < 0) {
            LOG.error("Could not update address {}", address)
        }
    }

    private fun insert(address: BitmessageAddress) {
        val db = sql.writableDatabase
        // Create a new map of values, where column names are the keys
        val values = ContentValues()
        values.put(COLUMN_ADDRESS, address.address)
        values.put(COLUMN_VERSION, address.version)
        values.put(COLUMN_ALIAS, address.alias)
        address.pubkey?.let { pubkey ->
            val out = ByteArrayOutputStream()
            pubkey.writeUnencrypted(out)
            values.put(COLUMN_PUBLIC_KEY, out.toByteArray())
        } ?: {
            values.put(COLUMN_PUBLIC_KEY, null as ByteArray?)
        }.invoke()
        address.privateKey?.let { values.put(COLUMN_PRIVATE_KEY, Encode.bytes(it)) }
        values.put(COLUMN_CHAN, address.isChan)
        values.put(COLUMN_SUBSCRIBED, address.isSubscribed)

        val insert = db.insert(TABLE_NAME, null, values)
        if (insert < 0) {
            LOG.error("Could not insert address {}", address)
        }
    }

    override fun remove(address: BitmessageAddress) {
        val db = sql.writableDatabase
        db.delete(TABLE_NAME, "address = ?", arrayOf(address.address))
    }

    fun getById(id: String): BitmessageAddress {
        val result = find("address = '$id'")
        return if (result.isNotEmpty()) {
            result[0]
        } else {
            throw ApplicationException("Address with id $id not found.")
        }
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
