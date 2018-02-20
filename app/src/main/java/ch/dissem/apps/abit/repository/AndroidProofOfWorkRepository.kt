/*
 * Copyright 2016 Christian Basler
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

import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.util.LinkedList

import ch.dissem.bitmessage.InternalContext
import ch.dissem.bitmessage.entity.ObjectMessage
import ch.dissem.bitmessage.factory.Factory
import ch.dissem.bitmessage.ports.ProofOfWorkRepository
import ch.dissem.bitmessage.utils.Encode

import ch.dissem.bitmessage.utils.Singleton.cryptography
import ch.dissem.bitmessage.utils.Strings.hex

/**
 * @author Christian Basler
 */
class AndroidProofOfWorkRepository(private val sql: SqlHelper) : ProofOfWorkRepository, InternalContext.ContextHolder {
    private lateinit var bmc: InternalContext

    override fun setContext(context: InternalContext) {
        this.bmc = context
    }

    override fun getItem(initialHash: ByteArray): ProofOfWorkRepository.Item {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_DATA, COLUMN_VERSION, COLUMN_NONCE_TRIALS_PER_BYTE, COLUMN_EXTRA_BYTES, COLUMN_EXPIRATION_TIME, COLUMN_MESSAGE_ID)

        sql.readableDatabase.query(
            TABLE_NAME, projection,
            "initial_hash=X'${hex(initialHash)}'",
            null, null, null, null
        ).use { c ->
            if (c.moveToFirst()) {
                val version = c.getInt(c.getColumnIndex(COLUMN_VERSION))
                val blob = c.getBlob(c.getColumnIndex(COLUMN_DATA))
                return if (c.isNull(c.getColumnIndex(COLUMN_MESSAGE_ID))) {
                    ProofOfWorkRepository.Item(
                        Factory.getObjectMessage(version, ByteArrayInputStream(blob), blob.size) ?: throw RuntimeException("Invalid object in repository"),
                        c.getLong(c.getColumnIndex(COLUMN_NONCE_TRIALS_PER_BYTE)),
                        c.getLong(c.getColumnIndex(COLUMN_EXTRA_BYTES))
                    )
                } else {
                    ProofOfWorkRepository.Item(
                        Factory.getObjectMessage(version, ByteArrayInputStream(blob), blob.size) ?: throw RuntimeException("Invalid object in repository"),
                        c.getLong(c.getColumnIndex(COLUMN_NONCE_TRIALS_PER_BYTE)),
                        c.getLong(c.getColumnIndex(COLUMN_EXTRA_BYTES)),
                        c.getLong(c.getColumnIndex(COLUMN_EXPIRATION_TIME)),
                        bmc.messageRepository.getMessage(c.getLong(c.getColumnIndex(COLUMN_MESSAGE_ID)))
                    )
                }
            }
        }
        throw RuntimeException("Object requested that we don't have. Initial hash: ${hex(initialHash)}")
    }

    override fun getItems(): List<ByteArray> {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_INITIAL_HASH)

        val result = LinkedList<ByteArray>()
        sql.readableDatabase.query(
            TABLE_NAME, projection, null, null, null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                val initialHash = c.getBlob(c.getColumnIndex(COLUMN_INITIAL_HASH))
                result.add(initialHash)
            }
        }
        return result
    }

    override fun putObject(item: ProofOfWorkRepository.Item) {
        try {
            // Create a new map of values, where column names are the keys
            val values = ContentValues().apply {
                put(COLUMN_INITIAL_HASH, cryptography().getInitialHash(item.objectMessage))
                put(COLUMN_DATA, Encode.bytes(item.objectMessage))
                put(COLUMN_VERSION, item.objectMessage.version)
                put(COLUMN_NONCE_TRIALS_PER_BYTE, item.nonceTrialsPerByte)
                put(COLUMN_EXTRA_BYTES, item.extraBytes)
                item.message?.let { message ->
                    put(COLUMN_EXPIRATION_TIME, item.expirationTime)
                    put(COLUMN_MESSAGE_ID, message.id as Long?)
                }
            }

            sql.writableDatabase.insertOrThrow(TABLE_NAME, null, values)
        } catch (e: SQLiteConstraintException) {
            LOG.trace(e.message, e)
        }

    }

    override fun putObject(objectMessage: ObjectMessage, nonceTrialsPerByte: Long, extraBytes: Long) =
        putObject(ProofOfWorkRepository.Item(objectMessage, nonceTrialsPerByte, extraBytes))

    override fun removeObject(initialHash: ByteArray) {
        sql.writableDatabase.delete(TABLE_NAME, "initial_hash=X'${hex(initialHash)}'", null)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AndroidProofOfWorkRepository::class.java)

        private const val TABLE_NAME = "POW"
        private const val COLUMN_INITIAL_HASH = "initial_hash"
        private const val COLUMN_DATA = "data"
        private const val COLUMN_VERSION = "version"
        private const val COLUMN_NONCE_TRIALS_PER_BYTE = "nonce_trials_per_byte"
        private const val COLUMN_EXTRA_BYTES = "extra_bytes"
        private const val COLUMN_EXPIRATION_TIME = "expiration_time"
        private const val COLUMN_MESSAGE_ID = "message_id"
    }
}
