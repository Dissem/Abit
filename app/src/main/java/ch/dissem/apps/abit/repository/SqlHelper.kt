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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ch.dissem.apps.abit.util.Assets
import ch.dissem.apps.abit.util.UuidUtils
import java.util.*

/**
 * Handles database migration and provides access.
 */
class SqlHelper(private val ctx: Context) : SQLiteOpenHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) = onUpgrade(db, 0, DATABASE_VERSION)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = mapOf(
        0 to {
            executeMigration(db, "V1.0__Create_table_inventory")
            executeMigration(db, "V1.1__Create_table_address")
            executeMigration(db, "V1.2__Create_table_message")
        },
        1 to {
            // executeMigration(db, "V2.0__Update_table_message");
            executeMigration(db, "V2.1__Create_table_POW")
        },
        2 to {
            executeMigration(db, "V3.0__Update_table_address")
        },
        3 to {
            executeMigration(db, "V3.1__Update_table_POW")
            executeMigration(db, "V3.2__Update_table_message")
        },
        4 to {
            executeMigration(db, "V3.3__Create_table_node")
        },
        5 to {
            executeMigration(db, "V3.4__Add_label_outbox")
        },
        6 to {
            executeMigration(db, "V4.0__Create_table_message_parent")
        },
        7 to {
            setMissingConversationIds(db)
        }
    ).filterKeys { it in oldVersion until newVersion }.forEach { (_, v) -> v.invoke() }

    /**
     * Set UUIDs for all messages that have no conversation ID
     */
    private fun setMissingConversationIds(db: SQLiteDatabase) = db.query(
        "Message", arrayOf("id"),
        "conversation IS NULL", null, null, null, null
    ).use { c ->
        while (c.moveToNext()) {
            val id = c.getLong(0)
            setMissingConversationId(id, db)
        }
    }

    private fun setMissingConversationId(id: Long, db: SQLiteDatabase) {
        val values = ContentValues(1).apply {
            put("conversation", UuidUtils.asBytes(UUID.randomUUID()))
        }
        db.update("Message", values, "id=?", arrayOf(id.toString()))
    }

    private fun executeMigration(db: SQLiteDatabase, name: String) {
        for (statement in Assets.readSqlStatements(ctx, "db/migration/$name.sql")) {
            db.execSQL(statement)
        }
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        private const val DATABASE_VERSION = 7
        const val DATABASE_NAME = "jabit.db"
    }
}
