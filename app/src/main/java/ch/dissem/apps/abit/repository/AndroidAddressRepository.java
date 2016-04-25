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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.payload.Pubkey;
import ch.dissem.bitmessage.entity.payload.V3Pubkey;
import ch.dissem.bitmessage.entity.payload.V4Pubkey;
import ch.dissem.bitmessage.entity.valueobject.PrivateKey;
import ch.dissem.bitmessage.factory.Factory;
import ch.dissem.bitmessage.ports.AddressRepository;
import ch.dissem.bitmessage.utils.Encode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link AddressRepository} implementation using the Android SQL API.
 */
public class AndroidAddressRepository implements AddressRepository {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidAddressRepository.class);

    private static final String TABLE_NAME = "Address";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_VERSION = "version";
    private static final String COLUMN_ALIAS = "alias";
    private static final String COLUMN_PUBLIC_KEY = "public_key";
    private static final String COLUMN_PRIVATE_KEY = "private_key";
    private static final String COLUMN_SUBSCRIBED = "subscribed";
    private static final String COLUMN_CHAN = "chan";

    private final SqlHelper sql;

    public AndroidAddressRepository(SqlHelper sql) {
        this.sql = sql;
    }

    @Override
    public BitmessageAddress findContact(byte[] ripeOrTag) {
        for (BitmessageAddress address : find("public_key is null")) {
            if (address.getVersion() > 3) {
                if (Arrays.equals(ripeOrTag, address.getTag())) return address;
            } else {
                if (Arrays.equals(ripeOrTag, address.getRipe())) return address;
            }
        }
        return null;
    }

    @Override
    public BitmessageAddress findIdentity(byte[] ripeOrTag) {
        for (BitmessageAddress address : find("private_key is not null")) {
            if (address.getVersion() > 3) {
                if (Arrays.equals(ripeOrTag, address.getTag())) return address;
            } else {
                if (Arrays.equals(ripeOrTag, address.getRipe())) return address;
            }
        }
        return null;
    }

    @Override
    public List<BitmessageAddress> getIdentities() {
        return find("private_key IS NOT NULL");
    }

    @Override
    public List<BitmessageAddress> getChans() {
        return find("chan = '1'");
    }

    @Override
    public List<BitmessageAddress> getSubscriptions() {
        return find("subscribed = '1'");
    }

    @Override
    public List<BitmessageAddress> getSubscriptions(long broadcastVersion) {
        if (broadcastVersion > 4) {
            return find("subscribed = '1' AND version > 3");
        } else {
            return find("subscribed = '1' AND version <= 3");
        }
    }

    @Override
    public List<BitmessageAddress> getContacts() {
        return find("private_key IS NULL");
    }

    private List<BitmessageAddress> find(String where) {
        List<BitmessageAddress> result = new LinkedList<>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                COLUMN_ADDRESS,
                COLUMN_ALIAS,
                COLUMN_PUBLIC_KEY,
                COLUMN_PRIVATE_KEY,
                COLUMN_SUBSCRIBED,
                COLUMN_CHAN
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor c = db.query(
                TABLE_NAME, projection,
                where,
                null, null, null, null
        )) {
            c.moveToFirst();
            while (!c.isAfterLast()) {
                BitmessageAddress address;

                byte[] privateKeyBytes = c.getBlob(c.getColumnIndex(COLUMN_PRIVATE_KEY));
                if (privateKeyBytes != null) {
                    PrivateKey privateKey = PrivateKey.read(new ByteArrayInputStream
                            (privateKeyBytes));
                    address = new BitmessageAddress(privateKey);
                } else {
                    address = new BitmessageAddress(c.getString(c.getColumnIndex(COLUMN_ADDRESS)));
                    byte[] publicKeyBytes = c.getBlob(c.getColumnIndex(COLUMN_PUBLIC_KEY));
                    if (publicKeyBytes != null) {
                        Pubkey pubkey = Factory.readPubkey(address.getVersion(), address
                                        .getStream(),
                                new ByteArrayInputStream(publicKeyBytes), publicKeyBytes.length,
                                false);
                        if (address.getVersion() == 4 && pubkey instanceof V3Pubkey) {
                            pubkey = new V4Pubkey((V3Pubkey) pubkey);
                        }
                        address.setPubkey(pubkey);
                    }
                }
                address.setAlias(c.getString(c.getColumnIndex(COLUMN_ALIAS)));
                address.setChan(c.getInt(c.getColumnIndex(COLUMN_CHAN)) == 1);
                address.setSubscribed(c.getInt(c.getColumnIndex(COLUMN_SUBSCRIBED)) == 1);

                result.add(address);
                c.moveToNext();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void save(BitmessageAddress address) {
        try {
            if (exists(address)) {
                update(address);
            } else {
                insert(address);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private boolean exists(BitmessageAddress address) {
        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM Address WHERE address='" + address
                .getAddress() + "'", null)) {
            cursor.moveToFirst();
            return cursor.getInt(0) > 0;
        }
    }

    private void update(BitmessageAddress address) throws IOException {
        try {
            SQLiteDatabase db = sql.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(COLUMN_ALIAS, address.getAlias());
            if (address.getPubkey() != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                address.getPubkey().writeUnencrypted(out);
                values.put(COLUMN_PUBLIC_KEY, out.toByteArray());
            } else {
                values.put(COLUMN_PUBLIC_KEY, (byte[]) null);
            }
            if (address.getPrivateKey() != null) {
                values.put(COLUMN_PRIVATE_KEY, Encode.bytes(address.getPrivateKey()));
            }
            values.put(COLUMN_CHAN, address.isChan());
            values.put(COLUMN_SUBSCRIBED, address.isSubscribed());

            int update = db.update(TABLE_NAME, values, "address = '" + address.getAddress() +
                    "'", null);
            if (update < 0) {
                LOG.error("Could not update address " + address);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void insert(BitmessageAddress address) throws IOException {
        try {
            SQLiteDatabase db = sql.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(COLUMN_ADDRESS, address.getAddress());
            values.put(COLUMN_VERSION, address.getVersion());
            values.put(COLUMN_ALIAS, address.getAlias());
            if (address.getPubkey() != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                address.getPubkey().writeUnencrypted(out);
                values.put(COLUMN_PUBLIC_KEY, out.toByteArray());
            } else {
                values.put(COLUMN_PUBLIC_KEY, (byte[]) null);
            }
            values.put(COLUMN_PRIVATE_KEY, Encode.bytes(address.getPrivateKey()));
            values.put(COLUMN_CHAN, address.isChan());
            values.put(COLUMN_SUBSCRIBED, address.isSubscribed());

            long insert = db.insert(TABLE_NAME, null, values);
            if (insert < 0) {
                LOG.error("Could not insert address " + address);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void remove(BitmessageAddress address) {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.delete(TABLE_NAME, "address = ?", new String[]{address.getAddress()});
    }

    @Override
    public BitmessageAddress getAddress(String address) {
        List<BitmessageAddress> result = find("address = '" + address + "'");
        if (result.size() > 0) return result.get(0);
        return null;
    }
}
