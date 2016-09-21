package ch.dissem.apps.abit.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.dissem.bitmessage.entity.valueobject.NetworkAddress;
import ch.dissem.bitmessage.exception.ApplicationException;
import ch.dissem.bitmessage.ports.NodeRegistry;
import ch.dissem.bitmessage.utils.Collections;
import ch.dissem.bitmessage.utils.SqlStrings;

import static ch.dissem.bitmessage.ports.NodeRegistryHelper.loadStableNodes;
import static ch.dissem.bitmessage.utils.Strings.hex;
import static ch.dissem.bitmessage.utils.UnixTime.DAY;
import static ch.dissem.bitmessage.utils.UnixTime.MINUTE;
import static ch.dissem.bitmessage.utils.UnixTime.now;
import static java.lang.String.valueOf;

/**
 * @author Christian Basler
 */
public class AndroidNodeRegistry implements NodeRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidInventory.class);

    private static final String TABLE_NAME = "Node";
    private static final String COLUMN_STREAM = "stream";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_PORT = "port";
    private static final String COLUMN_SERVICES = "services";
    private static final String COLUMN_TIME = "time";

    private final ThreadLocal<SQLiteStatement> loadExistingStatement = new ThreadLocal<>();

    private final SqlHelper sql;
    private Map<Long, Set<NetworkAddress>> stableNodes;

    public AndroidNodeRegistry(SqlHelper sql) {
        this.sql = sql;
        cleanUp();
    }

    private void cleanUp() {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.delete(TABLE_NAME, "time < ?", new String[]{valueOf(now(-28 * DAY))});
    }

    private Long loadExistingTime(NetworkAddress node) {
        SQLiteStatement statement = loadExistingStatement.get();
        if (statement == null) {
            statement = sql.getWritableDatabase().compileStatement(
                "SELECT " + COLUMN_TIME +
                    " FROM " + TABLE_NAME +
                    " WHERE stream=? AND address=? AND port=?"
            );
            loadExistingStatement.set(statement);
        }
        statement.bindLong(1, node.getStream());
        statement.bindBlob(2, node.getIPv6());
        statement.bindLong(3, node.getPort());
        try {
            return statement.simpleQueryForLong();
        } catch (SQLiteDoneException e) {
            return null;
        }
    }

    @Override
    public List<NetworkAddress> getKnownAddresses(int limit, long... streams) {
        String[] projection = {
            COLUMN_STREAM,
            COLUMN_ADDRESS,
            COLUMN_PORT,
            COLUMN_SERVICES,
            COLUMN_TIME
        };

        List<NetworkAddress> result = new LinkedList<>();
        SQLiteDatabase db = sql.getReadableDatabase();
        try (Cursor c = db.query(
            TABLE_NAME, projection,
            "stream IN (?)",
            new String[]{SqlStrings.join(streams).toString()},
            null, null,
            "time DESC",
            valueOf(limit)
        )) {
            while (c.moveToNext()) {
                result.add(
                    new NetworkAddress.Builder()
                        .stream(c.getLong(c.getColumnIndex(COLUMN_STREAM)))
                        .ipv6(c.getBlob(c.getColumnIndex(COLUMN_ADDRESS)))
                        .port(c.getInt(c.getColumnIndex(COLUMN_PORT)))
                        .services(c.getLong(c.getColumnIndex(COLUMN_SERVICES)))
                        .time(c.getLong(c.getColumnIndex(COLUMN_TIME)))
                        .build()
                );
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new ApplicationException(e);
        }
        if (result.isEmpty()) {
            synchronized (this) {
                if (stableNodes == null) {
                    stableNodes = loadStableNodes();
                }
            }
            for (long stream : streams) {
                Set<NetworkAddress> nodes = stableNodes.get(stream);
                if (nodes != null && !nodes.isEmpty()) {
                    result.add(Collections.selectRandom(nodes));
                }
            }
        }
        return result;
    }

    @Override
    public void offerAddresses(List<NetworkAddress> nodes) {
        SQLiteDatabase db = sql.getWritableDatabase();
        db.beginTransaction();
        try {
            cleanUp();
            for (NetworkAddress node : nodes) {
                if (node.getTime() < now(+5 * MINUTE) && node.getTime() > now(-28 * DAY)) {
                    synchronized (this) {
                        Long existing = loadExistingTime(node);
                        if (existing == null) {
                            insert(node);
                        } else if (node.getTime() > existing) {
                            update(node);
                        }
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insert(NetworkAddress node) {
        try {
            SQLiteDatabase db = sql.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(COLUMN_STREAM, node.getStream());
            values.put(COLUMN_ADDRESS, node.getIPv6());
            values.put(COLUMN_PORT, node.getPort());
            values.put(COLUMN_SERVICES, node.getServices());
            values.put(COLUMN_TIME, node.getTime());

            db.insertOrThrow(TABLE_NAME, null, values);
        } catch (SQLiteConstraintException e) {
            LOG.trace(e.getMessage(), e);
        }
    }

    private void update(NetworkAddress node) {
        try {
            SQLiteDatabase db = sql.getWritableDatabase();
            // Create a new map of values, where column names are the keys
            ContentValues values = new ContentValues();
            values.put(COLUMN_SERVICES, node.getServices());
            values.put(COLUMN_TIME, node.getTime());

            db.update(TABLE_NAME, values,
                "stream=" + node.getStream() + " AND address=X'" + hex(node.getIPv6()) + "' AND " +
                    "port=" + node.getPort(),
                null);
        } catch (SQLiteConstraintException e) {
            LOG.trace(e.getMessage(), e);
        }
    }
}
