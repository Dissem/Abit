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
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import ch.dissem.apps.abit.util.Labels
import ch.dissem.bitmessage.entity.valueobject.Label
import ch.dissem.bitmessage.ports.AbstractLabelRepository
import ch.dissem.bitmessage.ports.MessageRepository
import org.jetbrains.anko.db.transaction
import java.util.*

/**
 * [MessageRepository] implementation using the Android SQL API.
 */
class AndroidLabelRepository(private val sql: SqlHelper, private val context: Context) : AbstractLabelRepository() {

    override fun find(where: String): List<Label> {
        val result = LinkedList<Label>()

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        val projection = arrayOf(COLUMN_ID, COLUMN_LABEL, COLUMN_TYPE, COLUMN_COLOR)

        sql.readableDatabase.query(
            TABLE_NAME, projection,
            where, null, null, null,
            COLUMN_ORDER
        ).use { c ->
            while (c.moveToNext()) {
                result.add(getLabel(c, context))
            }
        }
        return result
    }

    override fun save(label: Label) {
        val db = sql.writableDatabase
        if (label.id != null) {
            val values = ContentValues()
            values.put(COLUMN_LABEL, label.toString())
            values.put(COLUMN_TYPE, label.type?.name)
            values.put(COLUMN_COLOR, label.color)
            values.put(COLUMN_ORDER, label.ord)
            db.update(TABLE_NAME, values, "id=?", arrayOf(label.id.toString()))
        } else {
            db.transaction {
                val exists = DatabaseUtils.queryNumEntries(db, TABLE_NAME, "label=?", arrayOf(label.toString())) > 0

                if (exists) {
                    val values = ContentValues()
                    values.put(COLUMN_TYPE, label.type?.name)
                    values.put(COLUMN_COLOR, label.color)
                    values.put(COLUMN_ORDER, label.ord)
                    db.update(TABLE_NAME, values, "label=?", arrayOf(label.toString()))
                } else {
                    val values = ContentValues()
                    values.put(COLUMN_LABEL, label.toString())
                    values.put(COLUMN_TYPE, label.type?.name)
                    values.put(COLUMN_COLOR, label.color)
                    values.put(COLUMN_ORDER, label.ord)
                    db.insertOrThrow(TABLE_NAME, null, values)
                }
            }
        }
    }

    internal fun findLabels(msgId: Any) = find("id IN (SELECT label_id FROM Message_Label WHERE message_id=$msgId)")

    companion object {
        val LABEL_ARCHIVE = Label("archive", null, 0).apply { id = Long.MAX_VALUE }

        private const val TABLE_NAME = "Label"
        private const val COLUMN_ID = "id"
        private const val COLUMN_LABEL = "label"
        private const val COLUMN_TYPE = "type"
        private const val COLUMN_COLOR = "color"
        private const val COLUMN_ORDER = "ord"

        internal fun getLabel(c: Cursor, context: Context): Label {
            val typeName = c.getString(c.getColumnIndex(COLUMN_TYPE))
            val type = if (typeName == null) null else Label.Type.valueOf(typeName)
            val text: String? = Labels.getText(type, null, context)
            val label = Label(
                text ?: c.getString(c.getColumnIndex(COLUMN_LABEL)),
                type,
                c.getInt(c.getColumnIndex(COLUMN_COLOR)))
            label.id = c.getLong(c.getColumnIndex(COLUMN_ID))
            return label
        }
    }
}
