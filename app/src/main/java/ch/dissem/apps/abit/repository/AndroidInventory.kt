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
import android.database.sqlite.SQLiteConstraintException
import ch.dissem.apps.abit.repository.SqlHelper.Companion.join
import ch.dissem.bitmessage.entity.ObjectMessage
import ch.dissem.bitmessage.entity.payload.ObjectType
import ch.dissem.bitmessage.entity.valueobject.InventoryVector
import ch.dissem.bitmessage.factory.Factory
import ch.dissem.bitmessage.ports.Inventory
import ch.dissem.bitmessage.utils.Encode
import ch.dissem.bitmessage.utils.UnixTime.MINUTE
import ch.dissem.bitmessage.utils.UnixTime.now
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * [Inventory] implementation using the Android SQL API.
 */
class AndroidInventory(private val sql: SqlHelper) : Inventory {

    private val cache = ConcurrentHashMap<Long, MutableMap<InventoryVector, Long>>()

    override fun getInventory(vararg streams: Long): List<InventoryVector> {
        val result = LinkedList<InventoryVector>()
        val now = now
        for (stream in streams) {
            for ((key, value) in getCache(stream)) {
                if (value > now) {
                    result.add(key)
                }
            }
        }
        return result
    }

    private fun getCache(stream: Long): MutableMap<InventoryVector, Long> {
        fun addToCache(stream: Long): MutableMap<InventoryVector, Long> {
            val result: MutableMap<InventoryVector, Long> = ConcurrentHashMap()
            cache.put(stream, result)

            val projection = arrayOf(COLUMN_HASH, COLUMN_EXPIRES)
            val db = sql.readableDatabase
            db.query(
                    TABLE_NAME, projection,
                    "stream = $stream", null, null, null, null
            ).use { c ->
                while (c.moveToNext()) {
                    val blob = c.getBlob(c.getColumnIndex(COLUMN_HASH))
                    val expires = c.getLong(c.getColumnIndex(COLUMN_EXPIRES))
                    InventoryVector.fromHash(blob)?.let { result.put(it, expires) }
                }
            }
            LOG.info("Stream #$stream inventory size: ${result.size}")
            return result
        }
        return cache[stream] ?: synchronized(cache) {
            return@synchronized cache[stream] ?: addToCache(stream)
        }
    }


    override fun getMissing(offer: List<InventoryVector>, vararg streams: Long) = offer - streams.flatMap { getCache(it).keys }

    override fun getObject(vector: InventoryVector): ObjectMessage? {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_VERSION, COLUMN_DATA)

        val db = sql.readableDatabase
        db.query(
                TABLE_NAME, projection,
                "hash = X'$vector'", null, null, null, null
        ).use { c ->
            if (!c.moveToFirst()) {
                LOG.info("Object requested that we don't have. IV: {}", vector)
                return null
            }

            val version = c.getInt(c.getColumnIndex(COLUMN_VERSION))
            val blob = c.getBlob(c.getColumnIndex(COLUMN_DATA))
            return Factory.getObjectMessage(version, ByteArrayInputStream(blob), blob.size)
        }
    }

    override fun getObjects(stream: Long, version: Long, vararg types: ObjectType): List<ObjectMessage> {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_VERSION, COLUMN_DATA)
        val where = StringBuilder("1=1")
        if (stream > 0) {
            where.append(" AND stream = ").append(stream)
        }
        if (version > 0) {
            where.append(" AND version = ").append(version)
        }
        if (types.isNotEmpty()) {
            where.append(" AND type IN (").append(join(*types)).append(")")
        }

        val db = sql.readableDatabase
        val result = LinkedList<ObjectMessage>()
        db.query(
                TABLE_NAME, projection,
                where.toString(), null, null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                val objectVersion = c.getInt(c.getColumnIndex(COLUMN_VERSION))
                val blob = c.getBlob(c.getColumnIndex(COLUMN_DATA))
                Factory.getObjectMessage(objectVersion, ByteArrayInputStream(blob), blob.size)?.let { result.add(it) }
            }
        }
        return result
    }

    override fun storeObject(objectMessage: ObjectMessage) {
        val iv = objectMessage.inventoryVector

        if (getCache(objectMessage.stream).containsKey(iv))
            return

        LOG.trace("Storing object {}", iv)

        try {
            val db = sql.writableDatabase
            // Create a new map of values, where column names are the keys
            val values = ContentValues()
            values.put(COLUMN_HASH, objectMessage.inventoryVector.hash)
            values.put(COLUMN_STREAM, objectMessage.stream)
            values.put(COLUMN_EXPIRES, objectMessage.expiresTime)
            values.put(COLUMN_DATA, Encode.bytes(objectMessage))
            values.put(COLUMN_TYPE, objectMessage.type)
            values.put(COLUMN_VERSION, objectMessage.version)

            db.insertOrThrow(TABLE_NAME, null, values)

            getCache(objectMessage.stream).put(iv, objectMessage.expiresTime)
        } catch (e: SQLiteConstraintException) {
            LOG.trace(e.message, e)
        }

    }

    override fun contains(objectMessage: ObjectMessage) = getCache(objectMessage.stream).keys.contains(objectMessage.inventoryVector)

    override fun cleanup() {
        val fiveMinutesAgo = now - 5 * MINUTE
        val db = sql.writableDatabase
        db.delete(TABLE_NAME, "expires < ?", arrayOf(fiveMinutesAgo.toString()))

        cache.values.map { it.entries }.forEach { entries -> entries.removeAll { it.value < fiveMinutesAgo } }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AndroidInventory::class.java)

        private const val TABLE_NAME = "Inventory"
        private const val COLUMN_HASH = "hash"
        private const val COLUMN_STREAM = "stream"
        private const val COLUMN_EXPIRES = "expires"
        private const val COLUMN_DATA = "data"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_VERSION = "version"
    }
}
