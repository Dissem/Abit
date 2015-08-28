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

package ch.dissem.apps.abit.repositories;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import ch.dissem.bitmessage.entity.ObjectMessage;
import ch.dissem.bitmessage.entity.payload.ObjectType;
import ch.dissem.bitmessage.entity.valueobject.InventoryVector;
import ch.dissem.bitmessage.factory.Factory;
import ch.dissem.bitmessage.ports.Inventory;
import ch.dissem.bitmessage.utils.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static ch.dissem.apps.abit.repositories.SqlHelper.join;
import static ch.dissem.bitmessage.utils.UnixTime.now;

/**
 * {@link Inventory} implementation using the Android SQL API.
 */
public class AndroidInventory implements Inventory {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidInventory.class);

    private static final String TABLE_NAME = "Inventory";
    private static final String COLUMN_HASH = "hash";
    private static final String COLUMN_STREAM = "stream";
    private static final String COLUMN_EXPIRES = "expires";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_VERSION = "version";

    private final SqlHelper sql;

    public AndroidInventory(SqlHelper sql) {
        this.sql = sql;
    }

    @Override
    public List<InventoryVector> getInventory(long... streams) {
        return getInventory(false, streams);
    }

    public List<InventoryVector> getInventory(boolean includeExpired, long... streams) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                COLUMN_HASH
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NAME, projection,
                (includeExpired ? "" : "expires > " + now() + " AND ") + "stream IN (" + join(streams) + ")",
                null, null, null, null
        );
        c.moveToFirst();
        List<InventoryVector> result = new LinkedList<>();
        while (!c.isAfterLast()) {
            byte[] blob = c.getBlob(c.getColumnIndex(COLUMN_HASH));
            result.add(new InventoryVector(blob));
            c.moveToNext();
        }
        return result;
    }

    @Override
    public List<InventoryVector> getMissing(List<InventoryVector> offer, long... streams) {
        offer.removeAll(getInventory(true, streams));
        return offer;
    }

    @Override
    public ObjectMessage getObject(InventoryVector vector) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                COLUMN_VERSION,
                COLUMN_DATA
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NAME, projection,
                "hash = X'" + vector + "'",
                null, null, null, null
        );
        c.moveToFirst();
        if (c.isAfterLast()) {
            LOG.info("Object requested that we don't have. IV: " + vector);
            return null;
        }

        int version = c.getInt(c.getColumnIndex(COLUMN_VERSION));
        byte[] blob = c.getBlob(c.getColumnIndex(COLUMN_DATA));
        return Factory.getObjectMessage(version, new ByteArrayInputStream(blob), blob.length);
    }

    @Override
    public List<ObjectMessage> getObjects(long stream, long version, ObjectType... types) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                COLUMN_VERSION,
                COLUMN_DATA
        };
        StringBuilder where = new StringBuilder("1=1");
        if (stream > 0) {
            where.append(" AND stream = ").append(stream);
        }
        if (version > 0) {
            where.append(" AND version = ").append(version);
        }
        if (types.length > 0) {
            where.append(" AND type IN (").append(join(types)).append(")");
        }

        SQLiteDatabase db = sql.getReadableDatabase();
        Cursor c = db.query(
                TABLE_NAME, projection,
                where.toString(),
                null, null, null, null
        );
        c.moveToFirst();
        List<ObjectMessage> result = new LinkedList<>();
        while (!c.isAfterLast()) {
            int objectVersion = c.getInt(c.getColumnIndex(COLUMN_VERSION));
            byte[] blob = c.getBlob(c.getColumnIndex(COLUMN_DATA));
            result.add(Factory.getObjectMessage(objectVersion, new ByteArrayInputStream(blob), blob.length));
            c.moveToNext();
        }
        return result;
    }

    @Override
    public void storeObject(ObjectMessage object) {
        InventoryVector iv = object.getInventoryVector();
        LOG.trace("Storing object " + iv);

        try {
            SQLiteDatabase db = sql.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(COLUMN_HASH, object.getInventoryVector().getHash());
            values.put(COLUMN_STREAM, object.getStream());
            values.put(COLUMN_EXPIRES, object.getExpiresTime());
            values.put(COLUMN_DATA, Encode.bytes(object));
            values.put(COLUMN_TYPE, object.getType());
            values.put(COLUMN_VERSION, object.getVersion());

            db.insertOrThrow(TABLE_NAME, null, values);
        } catch (SQLiteConstraintException e) {
            LOG.trace(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void cleanup() {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.delete(TABLE_NAME, "expires < " + (now() - 300), null);
    }
}
