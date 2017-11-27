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
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import ch.dissem.apps.abit.repository.AndroidLabelRepository.Companion.LABEL_ARCHIVE
import ch.dissem.apps.abit.util.UuidUtils
import ch.dissem.apps.abit.util.UuidUtils.asUuid
import ch.dissem.bitmessage.entity.BitmessageAddress
import ch.dissem.bitmessage.entity.Plaintext
import ch.dissem.bitmessage.entity.valueobject.InventoryVector
import ch.dissem.bitmessage.entity.valueobject.Label
import ch.dissem.bitmessage.ports.AbstractMessageRepository
import ch.dissem.bitmessage.ports.AlreadyStoredException
import ch.dissem.bitmessage.ports.MessageRepository
import ch.dissem.bitmessage.utils.Encode
import ch.dissem.bitmessage.utils.Strings.hex
import java.io.ByteArrayInputStream
import java.util.*

/**
 * [MessageRepository] implementation using the Android SQL API.
 */
class AndroidMessageRepository(private val sql: SqlHelper) : AbstractMessageRepository() {

    override fun findMessages(label: Label?, offset: Int, limit: Int) = if (label === LABEL_ARCHIVE) {
        super.findMessages(null as Label?, offset, limit)
    } else {
        super.findMessages(label, offset, limit)
    }

    override fun countUnread(label: Label?) = when {
        label === LABEL_ARCHIVE -> 0
        label == null -> DatabaseUtils.queryNumEntries(
            sql.readableDatabase,
            TABLE_NAME,
            "id IN (SELECT message_id FROM Message_Label WHERE label_id IN (SELECT id FROM Label WHERE type=?))",
            arrayOf(Label.Type.UNREAD.name)
        ).toInt()
        else -> DatabaseUtils.queryNumEntries(
            sql.readableDatabase,
            TABLE_NAME,
            "        id IN (SELECT message_id FROM Message_Label WHERE label_id=?) " +
                "AND id IN (SELECT message_id FROM Message_Label WHERE label_id IN (SELECT id FROM Label WHERE type=?))",
            arrayOf(label.id.toString(), Label.Type.UNREAD.name)
        ).toInt()
    }

    override fun findConversations(label: Label?): List<UUID> {
        val projection = arrayOf(COLUMN_CONVERSATION)

        val where = when {
            label === LABEL_ARCHIVE -> "id NOT IN (SELECT message_id FROM Message_Label)"
            label == null -> null
            else -> "id IN (SELECT message_id FROM Message_Label WHERE label_id=${label.id})"
        }
        val result = LinkedList<UUID>()
        sql.readableDatabase.query(
            true,
            TABLE_NAME, projection, where,
            null, null, null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                val uuidBytes = c.getBlob(c.getColumnIndex(COLUMN_CONVERSATION))
                result.add(asUuid(uuidBytes))
            }
        }
        return result
    }


    private fun updateParents(db: SQLiteDatabase, message: Plaintext) {
        val inventoryVector = message.inventoryVector
        if (inventoryVector == null || message.parents.isEmpty()) {
            // There are no parents to save yet (they are saved in the extended data, that's enough for now)
            return
        }
        val childIV = inventoryVector.hash
        db.delete(PARENTS_TABLE_NAME, "child=?", arrayOf(hex(childIV)))

        // save new parents
        var order = 0
        val values = ContentValues()
        for (parentIV in message.parents) {
            getMessage(parentIV)?.let { parent ->
                mergeConversations(db, parent.conversationId, message.conversationId)
                order++
                values.put("parent", parentIV.hash)
                values.put("child", childIV)
                values.put("pos", order)
                values.put("conversation", UuidUtils.asBytes(message.conversationId))
                db.insertOrThrow(PARENTS_TABLE_NAME, null, values)
            }
        }
    }

    /**
     * Replaces every occurrence of the source conversation ID with the target ID

     * @param db     is used to keep everything within one transaction
     * *
     * @param source ID of the conversation to be merged
     * *
     * @param target ID of the merge target
     */
    private fun mergeConversations(db: SQLiteDatabase, source: UUID, target: UUID) {
        val values = ContentValues()
        values.put("conversation", UuidUtils.asBytes(target))
        val where = "conversation=X'${hex(UuidUtils.asBytes(source))}'"
        db.update(TABLE_NAME, values, where, null)
        db.update(PARENTS_TABLE_NAME, values, where, null)
    }

    override fun find(where: String, offset: Int, limit: Int): List<Plaintext> {
        val result = LinkedList<Plaintext>()

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_ID, COLUMN_IV, COLUMN_TYPE, COLUMN_SENDER, COLUMN_RECIPIENT, COLUMN_DATA, COLUMN_ACK_DATA, COLUMN_SENT, COLUMN_RECEIVED, COLUMN_STATUS, COLUMN_TTL, COLUMN_RETRIES, COLUMN_NEXT_TRY, COLUMN_CONVERSATION)

        val db = sql.readableDatabase
        db.query(
            TABLE_NAME, projection,
            where, null, null, null,
            "$COLUMN_RECEIVED DESC, $COLUMN_SENT DESC",
            if (limit == 0) null else "$offset, $limit"
        ).use { c ->
            while (c.moveToNext()) {
                result.add(getMessage(c))
            }
        }
        return result
    }

    private fun getMessage(c: Cursor): Plaintext = Plaintext.readWithoutSignature(
        Plaintext.Type.valueOf(c.getString(c.getColumnIndex(COLUMN_TYPE))),
        ByteArrayInputStream(c.getBlob(c.getColumnIndex(COLUMN_DATA)))
    ).build {
        id = c.getLong(c.getColumnIndex(COLUMN_ID))
        inventoryVector = InventoryVector.fromHash(c.getBlob(c.getColumnIndex(COLUMN_IV)))
        c.getString(c.getColumnIndex(COLUMN_SENDER))?.let {
            from = ctx.addressRepository.getAddress(it) ?: BitmessageAddress(it)
        }
        c.getString(c.getColumnIndex(COLUMN_RECIPIENT))?.let {
            to = ctx.addressRepository.getAddress(it) ?: BitmessageAddress(it)
        }
        ackData = c.getBlob(c.getColumnIndex(COLUMN_ACK_DATA))
        sent = c.getLong(c.getColumnIndex(COLUMN_SENT))
        received = c.getLong(c.getColumnIndex(COLUMN_RECEIVED))
        status = Plaintext.Status.valueOf(c.getString(c.getColumnIndex(COLUMN_STATUS)))
        ttl = c.getLong(c.getColumnIndex(COLUMN_TTL))
        retries = c.getInt(c.getColumnIndex(COLUMN_RETRIES))
        val nextTryColumn = c.getColumnIndex(COLUMN_NEXT_TRY)
        if (!c.isNull(nextTryColumn)) {
            nextTry = c.getLong(nextTryColumn)
        }
        conversation = asUuid(c.getBlob(c.getColumnIndex(COLUMN_CONVERSATION)))
        labels = findLabels(id!!)
    }

    private fun findLabels(msgId: Any) = (ctx.labelRepository as AndroidLabelRepository).findLabels(msgId)

    override fun save(message: Plaintext) {
        saveContactIfNecessary(message.from)
        saveContactIfNecessary(message.to)
        val db = sql.writableDatabase
        try {
            db.beginTransaction()

            // save message
            if (message.id == null) {
                insert(db, message)
            } else {
                update(db, message)
            }

            updateParents(db, message)

            // remove existing labels
            db.delete(JOIN_TABLE_NAME, "message_id=?", arrayOf(message.id.toString()))

            // save labels
            val values = ContentValues()
            for (label in message.labels) {
                values.put(JT_COLUMN_LABEL, label.id as Long?)
                values.put(JT_COLUMN_MESSAGE, message.id as Long?)
                db.insertOrThrow(JOIN_TABLE_NAME, null, values)
            }
            db.setTransactionSuccessful()
        } catch (e: SQLiteConstraintException) {
            throw AlreadyStoredException(cause = e)
        } finally {
            db.endTransaction()
        }
    }

    private fun getValues(message: Plaintext): ContentValues {
        val values = ContentValues()
        values.put(COLUMN_IV, message.inventoryVector?.hash)
        values.put(COLUMN_TYPE, message.type.name)
        values.put(COLUMN_SENDER, message.from.address)
        values.put(COLUMN_RECIPIENT, message.to?.address)
        values.put(COLUMN_DATA, Encode.bytes(message))
        values.put(COLUMN_ACK_DATA, message.ackData)
        values.put(COLUMN_SENT, message.sent)
        values.put(COLUMN_RECEIVED, message.received)
        values.put(COLUMN_STATUS, message.status.name)
        values.put(COLUMN_INITIAL_HASH, message.initialHash)
        values.put(COLUMN_TTL, message.ttl)
        values.put(COLUMN_RETRIES, message.retries)
        values.put(COLUMN_NEXT_TRY, message.nextTry)
        values.put(COLUMN_CONVERSATION, UuidUtils.asBytes(message.conversationId))
        return values
    }

    private fun insert(db: SQLiteDatabase, message: Plaintext) {
        val id = db.insertOrThrow(TABLE_NAME, null, getValues(message))
        message.id = id
    }

    private fun update(db: SQLiteDatabase, message: Plaintext) {
        db.update(TABLE_NAME, getValues(message), "id=?", arrayOf(message.id.toString()))
    }

    override fun remove(message: Plaintext) {
        val db = sql.writableDatabase
        db.delete(TABLE_NAME, "id = ?", arrayOf(message.id.toString()))
    }

    companion object {
        private const val TABLE_NAME = "Message"
        private const val COLUMN_ID = "id"
        private const val COLUMN_IV = "iv"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_SENDER = "sender"
        private const val COLUMN_RECIPIENT = "recipient"
        private const val COLUMN_DATA = "data"
        private const val COLUMN_ACK_DATA = "ack_data"
        private const val COLUMN_SENT = "sent"
        private const val COLUMN_RECEIVED = "received"
        private const val COLUMN_STATUS = "status"
        private const val COLUMN_TTL = "ttl"
        private const val COLUMN_RETRIES = "retries"
        private const val COLUMN_NEXT_TRY = "next_try"
        private const val COLUMN_INITIAL_HASH = "initial_hash"
        private const val COLUMN_CONVERSATION = "conversation"

        private const val PARENTS_TABLE_NAME = "Message_Parent"

        private const val JOIN_TABLE_NAME = "Message_Label"
        private const val JT_COLUMN_MESSAGE = "message_id"
        private const val JT_COLUMN_LABEL = "label_id"
    }
}
