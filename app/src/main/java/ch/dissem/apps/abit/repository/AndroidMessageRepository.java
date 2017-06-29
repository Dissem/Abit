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
import android.support.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import ch.dissem.apps.abit.util.Labels;
import ch.dissem.apps.abit.util.UuidUtils;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.valueobject.InventoryVector;
import ch.dissem.bitmessage.entity.valueobject.Label;
import ch.dissem.bitmessage.ports.AbstractMessageRepository;
import ch.dissem.bitmessage.ports.MessageRepository;
import ch.dissem.bitmessage.utils.Encode;

import static ch.dissem.apps.abit.util.UuidUtils.asUuid;
import static ch.dissem.bitmessage.utils.Strings.hex;
import static java.lang.String.valueOf;

/**
 * {@link MessageRepository} implementation using the Android SQL API.
 */
public class AndroidMessageRepository extends AbstractMessageRepository {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidMessageRepository.class);

    public static final Label LABEL_ARCHIVE = new Label("archive", null, 0);

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
    private static final String COLUMN_CONVERSATION = "conversation";

    private static final String PARENTS_TABLE_NAME = "Message_Parent";

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

    @NonNull
    @Override
    public List<Plaintext> findMessages(Label label) {
        if (label == LABEL_ARCHIVE) {
            return super.findMessages((Label) null);
        } else {
            return super.findMessages(label);
        }
    }

    @NonNull
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
        String text = Labels.getText(type, null, context);
        if (text == null) {
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
    public int countUnread(Label label) {
        String[] args;
        String where;
        if (label == null) {
            return 0;
        }
        if (label == LABEL_ARCHIVE) {
            where = "";
            args = new String[]{
                Label.Type.UNREAD.name()
            };
        } else {
            where = "id IN (SELECT message_id FROM Message_Label WHERE label_id=?) AND ";
            args = new String[]{
                String.valueOf(label.getId()),
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

    @NonNull
    @Override
    public List<UUID> findConversations(Label label) {
        String[] projection = {
            COLUMN_CONVERSATION,
        };

        String where;
        if (label == null) {
            where = "id NOT IN (SELECT message_id FROM Message_Label)";
        } else {
            where = "id IN (SELECT message_id FROM Message_Label WHERE label_id=" + label.getId() + ")";
        }
        List<UUID> result = new LinkedList<>();
        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor c = db.query(
            TABLE_NAME, projection,
            where,
            null, null, null, null
        )) {
            while (c.moveToNext()) {
                byte[] uuidBytes = c.getBlob(c.getColumnIndex(COLUMN_CONVERSATION));
                result.add(asUuid(uuidBytes));
            }
        }
        return result;
    }


    private void updateParents(SQLiteDatabase db, Plaintext message) {
        if (message.getInventoryVector() == null || message.getParents().isEmpty()) {
            // There are no parents to save yet (they are saved in the extended data, that's enough for now)
            return;
        }
        byte[] childIV = message.getInventoryVector().getHash();
        db.delete(PARENTS_TABLE_NAME, "child=?", new String[]{hex(childIV)});

        // save new parents
        int order = 0;
        for (InventoryVector parentIV : message.getParents()) {
            Plaintext parent = getMessage(parentIV);
            if (parent != null) {
                mergeConversations(db, parent.getConversationId(), message.getConversationId());
                order++;
                ContentValues values = new ContentValues();
                values.put("parent", parentIV.getHash());
                values.put("child", childIV);
                values.put("pos", order);
                values.put("conversation", UuidUtils.asBytes(message.getConversationId()));
                db.insertOrThrow(PARENTS_TABLE_NAME, null, values);
            }
        }
    }

    /**
     * Replaces every occurrence of the source conversation ID with the target ID
     *
     * @param db     is used to keep everything within one transaction
     * @param source ID of the conversation to be merged
     * @param target ID of the merge target
     */
    private void mergeConversations(SQLiteDatabase db, UUID source, UUID target) {
        ContentValues values = new ContentValues();
        values.put("conversation", UuidUtils.asBytes(target));
        String[] whereArgs = {hex(UuidUtils.asBytes(source))};
        db.update(TABLE_NAME, values, "conversation=?", whereArgs);
        db.update(PARENTS_TABLE_NAME, values, "conversation=?", whereArgs);
    }

    @NonNull
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
            COLUMN_NEXT_TRY,
            COLUMN_CONVERSATION
        };

        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor c = db.query(
            TABLE_NAME, projection,
            where,
            null, null, null,
            COLUMN_RECEIVED + " DESC, " + COLUMN_SENT + " DESC"
        )) {
            while (c.moveToNext()) {
                byte[] iv = c.getBlob(c.getColumnIndex(COLUMN_IV));
                byte[] data = c.getBlob(c.getColumnIndex(COLUMN_DATA));
                Plaintext.Type type = Plaintext.Type.valueOf(c.getString(c.getColumnIndex
                    (COLUMN_TYPE)));
                Plaintext.Builder builder = Plaintext.readWithoutSignature(type,
                    new ByteArrayInputStream(data));
                long id = c.getLong(c.getColumnIndex(COLUMN_ID));
                builder.id(id);
                builder.IV(InventoryVector.fromHash(iv));
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
                builder.conversation(asUuid(c.getBlob(c.getColumnIndex(COLUMN_CONVERSATION))));
                builder.labels(findLabels(id));
                result.add(builder.build());
            }
        }
        return result;
    }

    private Collection<Label> findLabels(long id) {
        return findLabels("id IN (SELECT label_id FROM Message_Label WHERE message_id=" + id + ")");
    }

    @Override
    public void save(Plaintext message) {
        saveContactIfNecessary(message.getFrom());
        saveContactIfNecessary(message.getTo());
        SQLiteDatabase db = sql.getWritableDatabase();
        try {
            db.beginTransaction();

            // save message
            if (message.getId() == null) {
                insert(db, message);
            } else {
                update(db, message);
            }

            updateParents(db, message);

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

    private ContentValues getValues(Plaintext message) {
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
        values.put(COLUMN_STATUS, message.getStatus().name());
        values.put(COLUMN_INITIAL_HASH, message.getInitialHash());
        values.put(COLUMN_TTL, message.getTTL());
        values.put(COLUMN_RETRIES, message.getRetries());
        values.put(COLUMN_NEXT_TRY, message.getNextTry());
        values.put(COLUMN_CONVERSATION, UuidUtils.asBytes(message.getConversationId()));
        return values;
    }

    private void insert(SQLiteDatabase db, Plaintext message) {
        long id = db.insertOrThrow(TABLE_NAME, null, getValues(message));
        message.setId(id);
    }

    private void update(SQLiteDatabase db, Plaintext message) {
        db.update(TABLE_NAME, getValues(message), "id = " + message.getId(), null);
    }

    @Override
    public void remove(Plaintext message) {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.delete(TABLE_NAME, "id = " + message.getId(), null);
    }
}
