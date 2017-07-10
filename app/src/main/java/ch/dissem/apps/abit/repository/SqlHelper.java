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

package ch.dissem.apps.abit.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.UUID;

import ch.dissem.apps.abit.util.Assets;
import ch.dissem.apps.abit.util.UuidUtils;

/**
 * Handles database migration and provides access.
 */
public class SqlHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 7;
    private static final String DATABASE_NAME = "jabit.db";

    private final Context ctx;

    public SqlHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        this.ctx = ctx;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 0:
                executeMigration(db, "V1.0__Create_table_inventory");
                executeMigration(db, "V1.1__Create_table_address");
                executeMigration(db, "V1.2__Create_table_message");
            case 1:
                // executeMigration(db, "V2.0__Update_table_message");
                executeMigration(db, "V2.1__Create_table_POW");
            case 2:
                executeMigration(db, "V3.0__Update_table_address");
            case 3:
                executeMigration(db, "V3.1__Update_table_POW");
                executeMigration(db, "V3.2__Update_table_message");
            case 4:
                executeMigration(db, "V3.3__Create_table_node");
            case 5:
                executeMigration(db, "V3.4__Add_label_outbox");
            case 6:
                executeMigration(db, "V4.0__Create_table_message_parent");
            case 7:
                setMissingConversationIds(db);
            default:
                // Nothing to do. Let's assume we won't upgrade from a version that's newer than
                // DATABASE_VERSION.
        }
    }

    /**
     * Set UUIDs for all messages that have no conversation ID
     */
    private void setMissingConversationIds(SQLiteDatabase db) {
        try (Cursor c = db.query(
            "Message", new String[]{"id"},
            "conversation IS NULL",
            null, null, null, null
        )) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                setMissingConversationId(id, db);
            }
        }

    }

    private void setMissingConversationId(long id, SQLiteDatabase db) {
        ContentValues values = new ContentValues(1);
        values.put("conversation", UuidUtils.asBytes(UUID.randomUUID()));
        db.update("Message", values, "id=?", new String[]{String.valueOf(id)});
    }

    private void executeMigration(SQLiteDatabase db, String name) {
        for (String statement : Assets.readSqlStatements(ctx, "db/migration/" + name + ".sql")) {
            db.execSQL(statement);
        }
    }

    static StringBuilder join(long... numbers) {
        StringBuilder streamList = new StringBuilder();
        for (int i = 0; i < numbers.length; i++) {
            if (i > 0) streamList.append(", ");
            streamList.append(numbers[i]);
        }
        return streamList;
    }

    static StringBuilder join(Enum<?>... types) {
        StringBuilder streamList = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) streamList.append(", ");
            streamList.append('\'').append(types[i].name()).append('\'');
        }
        return streamList;
    }
}
