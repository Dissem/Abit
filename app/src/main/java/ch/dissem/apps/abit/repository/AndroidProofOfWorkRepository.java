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
import java.util.LinkedList;
import java.util.List;

import ch.dissem.bitmessage.InternalContext;
import ch.dissem.bitmessage.entity.ObjectMessage;
import ch.dissem.bitmessage.factory.Factory;
import ch.dissem.bitmessage.ports.ProofOfWorkRepository;
import ch.dissem.bitmessage.utils.Encode;

import static ch.dissem.bitmessage.utils.Singleton.cryptography;
import static ch.dissem.bitmessage.utils.Strings.hex;

/**
 * @author Christian Basler
 */
public class AndroidProofOfWorkRepository implements ProofOfWorkRepository, InternalContext
    .ContextHolder {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidProofOfWorkRepository.class);

    private static final String TABLE_NAME = "POW";
    private static final String COLUMN_INITIAL_HASH = "initial_hash";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_VERSION = "version";
    private static final String COLUMN_NONCE_TRIALS_PER_BYTE = "nonce_trials_per_byte";
    private static final String COLUMN_EXTRA_BYTES = "extra_bytes";
    private static final String COLUMN_EXPIRATION_TIME = "expiration_time";
    private static final String COLUMN_MESSAGE_ID = "message_id";

    private final SqlHelper sql;
    private InternalContext bmc;

    public AndroidProofOfWorkRepository(SqlHelper sql) {
        this.sql = sql;
    }

    @Override
    public void setContext(InternalContext internalContext) {
        this.bmc = internalContext;
    }

    @Override
    public Item getItem(byte[] initialHash) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
            COLUMN_DATA,
            COLUMN_VERSION,
            COLUMN_NONCE_TRIALS_PER_BYTE,
            COLUMN_EXTRA_BYTES,
            COLUMN_EXPIRATION_TIME,
            COLUMN_MESSAGE_ID
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor c = db.query(
            TABLE_NAME, projection,
            "initial_hash=X'" + hex(initialHash) + "'",
            null, null, null, null
        )) {
            if (c.moveToFirst()) {
                int version = c.getInt(c.getColumnIndex(COLUMN_VERSION));
                byte[] blob = c.getBlob(c.getColumnIndex(COLUMN_DATA));
                if (c.isNull(c.getColumnIndex(COLUMN_MESSAGE_ID))) {
                    return new Item(
                        Factory.getObjectMessage(version, new ByteArrayInputStream(blob), blob
                            .length),
                        c.getLong(c.getColumnIndex(COLUMN_NONCE_TRIALS_PER_BYTE)),
                        c.getLong(c.getColumnIndex(COLUMN_EXTRA_BYTES))
                    );
                } else {
                    return new Item(
                        Factory.getObjectMessage(version, new ByteArrayInputStream(blob), blob
                            .length),
                        c.getLong(c.getColumnIndex(COLUMN_NONCE_TRIALS_PER_BYTE)),
                        c.getLong(c.getColumnIndex(COLUMN_EXTRA_BYTES)),
                        c.getLong(c.getColumnIndex(COLUMN_EXPIRATION_TIME)),
                        bmc.getMessageRepository().getMessage(
                            c.getLong(c.getColumnIndex(COLUMN_MESSAGE_ID)))
                    );
                }
            }
        }
        throw new RuntimeException("Object requested that we don't have. Initial hash: " +
            hex(initialHash));
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
            while (c.moveToNext()) {
                byte[] initialHash = c.getBlob(c.getColumnIndex(COLUMN_INITIAL_HASH));
                result.add(initialHash);
            }
        }
        return result;
    }

    @Override
    public void putObject(Item item) {
        try {
            SQLiteDatabase db = sql.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(COLUMN_INITIAL_HASH, cryptography().getInitialHash(item.object));
            values.put(COLUMN_DATA, Encode.bytes(item.object));
            values.put(COLUMN_VERSION, item.object.getVersion());
            values.put(COLUMN_NONCE_TRIALS_PER_BYTE, item.nonceTrialsPerByte);
            values.put(COLUMN_EXTRA_BYTES, item.extraBytes);
            if (item.message != null) {
                values.put(COLUMN_EXPIRATION_TIME, item.expirationTime);
                values.put(COLUMN_MESSAGE_ID, (Long) item.message.getId());
            }

            db.insertOrThrow(TABLE_NAME, null, values);
        } catch (SQLiteConstraintException e) {
            LOG.trace(e.getMessage(), e);
        }
    }

    @Override
    public void putObject(ObjectMessage object, long nonceTrialsPerByte, long extraBytes) {
        putObject(new Item(object, nonceTrialsPerByte, extraBytes));
    }

    @Override
    public void removeObject(byte[] initialHash) {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.delete(
            TABLE_NAME,
            "initial_hash=X'" + hex(initialHash) + "'",
            null
        );
    }
}
