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
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import ch.dissem.apps.abit.R;
import ch.dissem.bitmessage.InternalContext;
import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.InventoryVector;
import ch.dissem.bitmessage.entity.valueobject.Label;
import ch.dissem.bitmessage.ports.MessageRepository;
import ch.dissem.bitmessage.utils.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static ch.dissem.apps.abit.repositories.SqlHelper.join;

/**
 * {@link MessageRepository} implementation using the Android SQL API.
 */
public class AndroidMessageRepository implements MessageRepository, InternalContext.ContextHolder {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidMessageRepository.class);

    private static final String TABLE_NAME = "Message";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_IV = "iv";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_SENDER = "sender";
    private static final String COLUMN_RECIPIENT = "recipient";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_SENT = "sent";
    private static final String COLUMN_RECEIVED = "received";
    private static final String COLUMN_STATUS = "status";

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
    private final Context ctx;
    private InternalContext bmc;

    public AndroidMessageRepository(SqlHelper sql, Context ctx) {
        this.sql = sql;
        this.ctx = ctx;
    }

    @Override
    public void setContext(InternalContext context) {
        bmc = context;
    }

    @Override
    public List<Label> getLabels() {
        return findLabels(null);
    }

    @Override
    public List<Label> getLabels(Label.Type... types) {
        return findLabels("type IN (" + join(types) + ")");
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
        Cursor c = db.query(
                LBL_TABLE_NAME, projection,
                where,
                null, null, null,
                LBL_COLUMN_ORDER
        );
        c.moveToFirst();
        while (!c.isAfterLast()) {
            result.add(getLabel(c));
            c.moveToNext();
        }
        return result;
    }

    private Label getLabel(Cursor c) {
        String typeName = c.getString(c.getColumnIndex(LBL_COLUMN_TYPE));
        Label.Type type = typeName == null ? null : Label.Type.valueOf(typeName);
        String text;
        switch (type) {
            case INBOX:
                text = ctx.getString(R.string.inbox);
                break;
            case DRAFT:
                text = ctx.getString(R.string.draft);
                break;
            case SENT:
                text = ctx.getString(R.string.sent);
                break;
            case UNREAD:
                text = ctx.getString(R.string.unread);
                break;
            case TRASH:
                text = ctx.getString(R.string.trash);
                break;
            case BROADCAST:
                text = ctx.getString(R.string.broadcast);
                break;
            default:
                text = c.getString(c.getColumnIndex(LBL_COLUMN_LABEL));
        }
        Label label = new Label(
                text,
                type,
                c.getInt(c.getColumnIndex(LBL_COLUMN_COLOR)));
        label.setId(c.getLong(c.getColumnIndex(LBL_COLUMN_ID)));
        return label;
    }

    @Override
    public List<Plaintext> findMessages(Label label) {
        if (label != null) {
            return find("id IN (SELECT message_id FROM Message_Label WHERE label_id=" + label.getId() + ")");
        } else {
            return find("id NOT IN (SELECT message_id FROM Message_Label)");
        }
    }

    @Override
    public List<Plaintext> findMessages(Plaintext.Status status, BitmessageAddress recipient) {
        return find("status='" + status.name() + "' AND recipient='" + recipient.getAddress() + "'");
    }

    @Override
    public List<Plaintext> findMessages(Plaintext.Status status) {
        return find("status='" + status.name() + "'");
    }

    private List<Plaintext> find(String where) {
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
                COLUMN_SENT,
                COLUMN_RECEIVED,
                COLUMN_STATUS
        };

        try {
            SQLiteDatabase db = sql.getReadableDatabase();
            Cursor c = db.query(
                    TABLE_NAME, projection,
                    where,
                    null, null, null, null
            );
            c.moveToFirst();
            while (!c.isAfterLast()) {
                byte[] iv = c.getBlob(c.getColumnIndex(COLUMN_IV));
                byte[] data = c.getBlob(c.getColumnIndex(COLUMN_DATA));
                Plaintext.Type type = Plaintext.Type.valueOf(c.getString(c.getColumnIndex(COLUMN_TYPE)));
                Plaintext.Builder builder = Plaintext.readWithoutSignature(type, new ByteArrayInputStream(data));
                long id = c.getLong(c.getColumnIndex(COLUMN_ID));
                builder.id(id);
                builder.IV(new InventoryVector(iv));
                builder.from(bmc.getAddressRepo().getAddress(c.getString(c.getColumnIndex(COLUMN_SENDER))));
                builder.to(bmc.getAddressRepo().getAddress(c.getString(c.getColumnIndex(COLUMN_RECIPIENT))));
                builder.sent(c.getLong(c.getColumnIndex(COLUMN_SENT)));
                builder.received(c.getLong(c.getColumnIndex(COLUMN_RECEIVED)));
                builder.status(Plaintext.Status.valueOf(c.getString(c.getColumnIndex(COLUMN_STATUS))));
                builder.labels(findLabels(id));
                result.add(builder.build());
                c.moveToNext();
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
        try {
            SQLiteDatabase db = sql.getWritableDatabase();
            db.beginTransaction();

            // save from address if necessary
            if (message.getId() == null) {
                BitmessageAddress savedAddress = bmc.getAddressRepo().getAddress(message.getFrom().getAddress());
                if (savedAddress == null || savedAddress.getPrivateKey() == null) {
                    if (savedAddress != null && savedAddress.getAlias() != null) {
                        message.getFrom().setAlias(savedAddress.getAlias());
                    }
                    bmc.getAddressRepo().save(message.getFrom());
                }
            }

            // save message
            if (message.getId() == null) {
                insert(db, message);
            } else {
                update(db, message);
            }

            // remove existing labels
            db.delete(JOIN_TABLE_NAME, "message_id=" + message.getId(), null);

            // save labels
            ContentValues values = new ContentValues();
            for (Label label : message.getLabels()) {
                values.put(JT_COLUMN_LABEL, (Long) label.getId());
                values.put(JT_COLUMN_MESSAGE, (Long) message.getId());
                db.insertOrThrow(JOIN_TABLE_NAME, null, values);
            }
            db.endTransaction();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void insert(SQLiteDatabase db, Plaintext message) throws IOException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_IV, message.getInventoryVector() == null ? null : message.getInventoryVector().getHash());
        values.put(COLUMN_TYPE, message.getType().name());
        values.put(COLUMN_SENDER, message.getFrom().getAddress());
        values.put(COLUMN_RECIPIENT, message.getTo() == null ? null : message.getTo().getAddress());
        values.put(COLUMN_DATA, Encode.bytes(message));
        values.put(COLUMN_SENT, message.getSent());
        values.put(COLUMN_RECEIVED, message.getReceived());
        values.put(COLUMN_STATUS, message.getStatus() == null ? null : message.getStatus().name());
        long id = db.insertOrThrow(TABLE_NAME, null, values);
        message.setId(id);
    }

    private void update(SQLiteDatabase db, Plaintext message) throws IOException {
        ContentValues values = new ContentValues();
        values.put(COLUMN_IV, message.getInventoryVector() == null ? null : message.getInventoryVector().getHash());
        values.put(COLUMN_TYPE, message.getType().name());
        values.put(COLUMN_SENDER, message.getFrom().getAddress());
        values.put(COLUMN_RECIPIENT, message.getTo() == null ? null : message.getTo().getAddress());
        values.put(COLUMN_DATA, Encode.bytes(message));
        values.put(COLUMN_SENT, message.getSent());
        values.put(COLUMN_RECEIVED, message.getReceived());
        values.put(COLUMN_STATUS, message.getStatus() == null ? null : message.getStatus().name());
        db.update(TABLE_NAME, values, "id = " + message.getId(), null);
    }

    @Override
    public void remove(Plaintext message) {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.delete(TABLE_NAME, "id = " + message.getId(), null);
    }
}
