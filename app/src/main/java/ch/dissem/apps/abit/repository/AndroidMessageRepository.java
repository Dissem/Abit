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
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.InventoryVector;
import ch.dissem.bitmessage.entity.valueobject.Label;
import ch.dissem.bitmessage.ports.AbstractMessageRepository;
import ch.dissem.bitmessage.ports.MessageRepository;
import ch.dissem.bitmessage.utils.Encode;

import static java.lang.String.valueOf;

/**
 * {@link MessageRepository} implementation using the Android SQL API.
 */
public class AndroidMessageRepository extends AbstractMessageRepository {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidMessageRepository.class);

    private static final String TABLE_NAME = "Message";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_IV = "iv";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_SENDER = "sender";
    private static final String COLUMN_RECIPIENT = "recipient";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_ACK_DATA = "ack_data";
    private static final String COLUMN_SENT = "sent";
    private static final String COLUMN_RECEIVED = "received";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_TTL = "ttl";
    private static final String COLUMN_RETRIES = "retries";
    private static final String COLUMN_NEXT_TRY = "next_try";
    private static final String COLUMN_INITIAL_HASH = "initial_hash";

    private static final String JOIN_TABLE_NAME = "Message_Label";
    private static final String JT_COLUMN_MESSAGE = "message_id";
    private static final String JT_COLUMN_LABEL = "label_id";

    private static final String LBL_TABLE_NAME = "Label";
    private static final String LBL_COLUMN_ID = "id";
    private static final String LBL_COLUMN_LABEL = "label";
    private static final String LBL_COLUMN_TYPE = "type";
    private static final String LBL_COLUMN_COLOR = "color";
    private static final String LBL_COLUMN_ORDER = "ord";
    private final SqlHelper sql;
    private final Context context;

    public AndroidMessageRepository(SqlHelper sql, Context ctx) {
        this.sql = sql;
        this.context = ctx;
    }

    public List<Label> findLabels(String where) {
        List<Label> result = new LinkedList<>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
            LBL_COLUMN_ID,
            LBL_COLUMN_LABEL,
            LBL_COLUMN_TYPE,
            LBL_COLUMN_COLOR
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor c = db.query(
            LBL_TABLE_NAME, projection,
            where,
            null, null, null,
            LBL_COLUMN_ORDER
        )) {
            while (c.moveToNext()) {
                result.add(getLabel(c));
            }
        }
        return result;
    }

    private Label getLabel(Cursor c) {
        String typeName = c.getString(c.getColumnIndex(LBL_COLUMN_TYPE));
        Label.Type type = typeName == null ? null : Label.Type.valueOf(typeName);
        String text;
        if (type == null) {
            text = c.getString(c.getColumnIndex(LBL_COLUMN_LABEL));
        } else {
            switch (type) {
                case INBOX:
                    text = context.getString(R.string.inbox);
                    break;
                case DRAFT:
                    text = context.getString(R.string.draft);
                    break;
                case SENT:
                    text = context.getString(R.string.sent);
                    break;
                case UNREAD:
                    text = context.getString(R.string.unread);
                    break;
                case TRASH:
                    text = context.getString(R.string.trash);
                    break;
                case BROADCAST:
                    text = context.getString(R.string.broadcasts);
                    break;
                default:
                    text = c.getString(c.getColumnIndex(LBL_COLUMN_LABEL));
            }
        }
        Label label = new Label(
            text,
            type,
            c.getInt(c.getColumnIndex(LBL_COLUMN_COLOR)));
        label.setId(c.getLong(c.getColumnIndex(LBL_COLUMN_ID)));
        return label;
    }

    @Override
    public int countUnread(Label label) {
        String[] args;
        String where;
        if (label != null) {
            where = "id IN (SELECT message_id FROM Message_Label WHERE label_id=?) AND ";
            args = new String[]{
                label.getId().toString(),
                Label.Type.UNREAD.name()
            };
        } else {
            where = "";
            args = new String[]{
                Label.Type.UNREAD.name()
            };
        }
        SQLiteDatabase db = sql.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME,
            where + "id IN (SELECT message_id FROM Message_Label WHERE label_id IN (" +
                "SELECT id FROM Label WHERE type=?))",
            args
        );
    }

    protected List<Plaintext> find(String where) {
        List<Plaintext> result = new LinkedList<>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
            COLUMN_ID,
            COLUMN_IV,
            COLUMN_TYPE,
            COLUMN_SENDER,
            COLUMN_RECIPIENT,
            COLUMN_DATA,
            COLUMN_ACK_DATA,
            COLUMN_SENT,
            COLUMN_RECEIVED,
            COLUMN_STATUS,
            COLUMN_TTL,
            COLUMN_RETRIES,
            COLUMN_NEXT_TRY
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor c = db.query(
            TABLE_NAME, projection,
            where,
            null, null, null,
            COLUMN_RECEIVED + " DESC"
        )) {
            while (c.moveToNext()) {
                byte[] iv = c.getBlob(c.getColumnIndex(COLUMN_IV));
                byte[] data = c.getBlob(c.getColumnIndex(COLUMN_DATA));
                Plaintext.Type type = Plaintext.Type.valueOf(c.getString(c.getColumnIndex
                    (COLUMN_TYPE)));
                Plaintext.Builder builder = Plaintext.readWithoutSignature(type, new
                    ByteArrayInputStream(data));
                long id = c.getLong(c.getColumnIndex(COLUMN_ID));
                builder.id(id);
                builder.IV(new InventoryVector(iv));
                builder.from(ctx.getAddressRepository().getAddress(c.getString(c.getColumnIndex
                    (COLUMN_SENDER))));
                builder.to(ctx.getAddressRepository().getAddress(c.getString(c.getColumnIndex
                    (COLUMN_RECIPIENT))));
                builder.ackData(c.getBlob(c.getColumnIndex(COLUMN_ACK_DATA)));
                builder.sent(c.getLong(c.getColumnIndex(COLUMN_SENT)));
                builder.received(c.getLong(c.getColumnIndex(COLUMN_RECEIVED)));
                builder.status(Plaintext.Status.valueOf(c.getString(c.getColumnIndex
                    (COLUMN_STATUS))));
                builder.ttl(c.getLong(c.getColumnIndex(COLUMN_TTL)));
                builder.retries(c.getInt(c.getColumnIndex(COLUMN_RETRIES)));
                int nextTryColumn = c.getColumnIndex(COLUMN_NEXT_TRY);
                if (!c.isNull(nextTryColumn)) {
                    builder.nextTry(c.getLong(nextTryColumn));
                }
                builder.labels(findLabels(id));
                result.add(builder.build());
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return result;
    }

    private Collection<Label> findLabels(long id) {
        return findLabels("id IN (SELECT label_id FROM Message_Label WHERE message_id=" + id + ")");
    }

    @Override
    public void save(Plaintext message) {
        SQLiteDatabase db = sql.getWritableDatabase();
        try {
            db.beginTransaction();

            // save from address if necessary
            if (message.getId() == null) {
                BitmessageAddress savedAddress = ctx.getAddressRepository().getAddress(message
                    .getFrom().getAddress());
                if (savedAddress == null || savedAddress.getPrivateKey() == null) {
                    if (savedAddress != null && savedAddress.getAlias() != null) {
                        message.getFrom().setAlias(savedAddress.getAlias());
                    }
                    ctx.getAddressRepository().save(message.getFrom());
                }
            }

            // save message
            if (message.getId() == null) {
                insert(db, message);
            } else {
                update(db, message);
            }

            // remove existing labels
            db.delete(JOIN_TABLE_NAME, "message_id=?", new String[]{valueOf(message.getId())});

            // save labels
            ContentValues values = new ContentValues();
            for (Label label : message.getLabels()) {
                values.put(JT_COLUMN_LABEL, (Long) label.getId());
                values.put(JT_COLUMN_MESSAGE, (Long) message.getId());
                db.insertOrThrow(JOIN_TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } catch (SQLiteConstraintException e) {
            LOG.trace(e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    private void insert(SQLiteDatabase db, Plaintext message) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_IV, message.getInventoryVector() == null ? null : message
            .getInventoryVector().getHash());
        values.put(COLUMN_TYPE, message.getType().name());
        values.put(COLUMN_SENDER, message.getFrom().getAddress());
        values.put(COLUMN_RECIPIENT, message.getTo() == null ? null : message.getTo().getAddress());
        values.put(COLUMN_DATA, Encode.bytes(message));
        values.put(COLUMN_ACK_DATA, message.getAckData());
        values.put(COLUMN_SENT, message.getSent());
        values.put(COLUMN_RECEIVED, message.getReceived());
        values.put(COLUMN_STATUS, message.getStatus() == null ? null : message.getStatus().name());
        values.put(COLUMN_INITIAL_HASH, message.getInitialHash());
        values.put(COLUMN_TTL, message.getTTL());
        values.put(COLUMN_RETRIES, message.getRetries());
        values.put(COLUMN_NEXT_TRY, message.getNextTry());
        long id = db.insertOrThrow(TABLE_NAME, null, values);
        message.setId(id);
    }

    private void update(SQLiteDatabase db, Plaintext message) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_IV, message.getInventoryVector() == null ? null : message
            .getInventoryVector().getHash());
        values.put(COLUMN_TYPE, message.getType().name());
        values.put(COLUMN_SENDER, message.getFrom().getAddress());
        values.put(COLUMN_RECIPIENT, message.getTo() == null ? null : message.getTo().getAddress());
        values.put(COLUMN_DATA, Encode.bytes(message));
        values.put(COLUMN_ACK_DATA, message.getAckData());
        values.put(COLUMN_SENT, message.getSent());
        values.put(COLUMN_RECEIVED, message.getReceived());
        values.put(COLUMN_STATUS, message.getStatus() == null ? null : message.getStatus().name());
        values.put(COLUMN_INITIAL_HASH, message.getInitialHash());
        values.put(COLUMN_TTL, message.getTTL());
        values.put(COLUMN_RETRIES, message.getRetries());
        values.put(COLUMN_NEXT_TRY, message.getNextTry());
        db.update(TABLE_NAME, values, "id = " + message.getId(), null);
    }

    @Override
    public void remove(Plaintext message) {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.delete(TABLE_NAME, "id = " + message.getId(), null);
    }
}
