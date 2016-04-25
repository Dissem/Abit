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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import ch.dissem.apps.abit.util.Assets;

/**
 * Handles database migration and provides access.
 */
public class SqlHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "jabit.db";

    protected final Context ctx;

    public SqlHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        this.ctx = ctx;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, 2);
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
            default:
                // Nothing to do. Let's assume we won't upgrade from a version that's newer than DATABASE_VERSION.
        }
    }

    protected void executeMigration(SQLiteDatabase db, String name) {
        for (String statement : Assets.readSqlStatements(ctx, "db/migration/" + name + ".sql")) {
            db.execSQL(statement);
        }
    }

    public static StringBuilder join(long... numbers) {
        StringBuilder streamList = new StringBuilder();
        for (int i = 0; i < numbers.length; i++) {
            if (i > 0) streamList.append(", ");
            streamList.append(numbers[i]);
        }
        return streamList;
    }

    public static StringBuilder join(Enum<?>... types) {
        StringBuilder streamList = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) streamList.append(", ");
            streamList.append('\'').append(types[i].name()).append('\'');
        }
        return streamList;
    }
}
