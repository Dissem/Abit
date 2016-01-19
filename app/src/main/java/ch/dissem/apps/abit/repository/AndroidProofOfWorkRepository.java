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

package ch.dissem.apps.abit.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import ch.dissem.bitmessage.entity.ObjectMessage;
import ch.dissem.bitmessage.factory.Factory;
import ch.dissem.bitmessage.ports.ProofOfWorkRepository;
import ch.dissem.bitmessage.utils.Encode;
import ch.dissem.bitmessage.utils.Strings;

import static ch.dissem.bitmessage.utils.Singleton.security;

/**
 * @author Christian Basler
 */
public class AndroidProofOfWorkRepository implements ProofOfWorkRepository {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidProofOfWorkRepository.class);

    private static final String TABLE_NAME = "POW";
    private static final String COLUMN_INITIAL_HASH = "initial_hash";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_VERSION = "version";
    private static final String COLUMN_NONCE_TRIALS_PER_BYTE = "nonce_trials_per_byte";
    private static final String COLUMN_EXTRA_BYTES = "extra_bytes";

    private final SqlHelper sql;

    public AndroidProofOfWorkRepository(SqlHelper sql) {
        this.sql = sql;
    }

    @Override
    public Item getItem(byte[] initialHash) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                COLUMN_DATA,
                COLUMN_VERSION,
                COLUMN_NONCE_TRIALS_PER_BYTE,
                COLUMN_EXTRA_BYTES
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor c = db.query(
                TABLE_NAME, projection,
                "initial_hash = X'" + Strings.hex(initialHash) + "'",
                null, null, null, null
        )) {
            c.moveToFirst();
            if (!c.isAfterLast()) {
                int version = c.getInt(c.getColumnIndex(COLUMN_VERSION));
                byte[] blob = c.getBlob(c.getColumnIndex(COLUMN_DATA));
                return new Item(
                        Factory.getObjectMessage(version, new ByteArrayInputStream(blob), blob
                                .length),
                        c.getLong(c.getColumnIndex(COLUMN_NONCE_TRIALS_PER_BYTE)),
                        c.getLong(c.getColumnIndex(COLUMN_EXTRA_BYTES))
                );
            }
        }
        throw new RuntimeException("Object requested that we don't have. Initial hash: " +
                Strings.hex(initialHash));
    }

    @Override
    public List<byte[]> getItems() {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                COLUMN_INITIAL_HASH
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        List<byte[]> result = new LinkedList<>();
        try (Cursor c = db.query(
                TABLE_NAME, projection,
                null, null, null, null, null
        )) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                byte[] initialHash = c.getBlob(c.getColumnIndex(COLUMN_INITIAL_HASH));
                result.add(initialHash);
                c.moveToNext();
            }
        }
        return result;
    }

    @Override
    public void putObject(ObjectMessage object, long nonceTrialsPerByte, long extraBytes) {
        try {
            SQLiteDatabase db = sql.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(COLUMN_INITIAL_HASH, security().getInitialHash(object));
            values.put(COLUMN_DATA, Encode.bytes(object));
            values.put(COLUMN_VERSION, object.getVersion());
            values.put(COLUMN_NONCE_TRIALS_PER_BYTE, nonceTrialsPerByte);
            values.put(COLUMN_EXTRA_BYTES, extraBytes);

            db.insertOrThrow(TABLE_NAME, null, values);
        } catch (SQLiteConstraintException e) {
            LOG.trace(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void removeObject(byte[] initialHash) {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.delete(TABLE_NAME,
                "initial_hash = X'" + Strings.hex(initialHash) + "'",
                null);
    }
}
