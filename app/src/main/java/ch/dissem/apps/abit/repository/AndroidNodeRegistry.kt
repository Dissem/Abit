package ch.dissem.apps.abit.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDoneException
import android.database.sqlite.SQLiteStatement
import ch.dissem.bitmessage.entity.valueobject.NetworkAddress
import ch.dissem.bitmessage.ports.NodeRegistry
import ch.dissem.bitmessage.ports.NodeRegistryHelper.loadStableNodes
import ch.dissem.bitmessage.utils.Collections
import ch.dissem.bitmessage.utils.SqlStrings
import ch.dissem.bitmessage.utils.Strings.hex
import ch.dissem.bitmessage.utils.UnixTime
import ch.dissem.bitmessage.utils.UnixTime.DAY
import ch.dissem.bitmessage.utils.UnixTime.MINUTE
import ch.dissem.bitmessage.utils.UnixTime.now
import ch.dissem.bitmessage.utils.max
import org.jetbrains.anko.db.transaction
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.getOrSet

const val MAX_ENTRY_AGE = 7 * DAY

/**
 * @author Christian Basler
 */
class AndroidNodeRegistry(private val sql: SqlHelper) : NodeRegistry {

    private val loadExistingStatement = ThreadLocal<SQLiteStatement>()
    private var stableNodes: Map<Long, Set<NetworkAddress>> = emptyMap()
        get() {
            if (field.isEmpty())
                field = loadStableNodes()
            return field
        }

    init {
        cleanUp()
    }

    private fun cleanUp() {
        sql.writableDatabase.delete(TABLE_NAME, "time < ?", arrayOf((now - MAX_ENTRY_AGE).toString()))
    }

    override fun clear() {
        sql.writableDatabase.delete(TABLE_NAME, null, null)
    }

    private fun loadExistingTime(node: NetworkAddress): Long? {
        val statement: SQLiteStatement = loadExistingStatement.getOrSet {
            sql.writableDatabase.compileStatement(
                "SELECT $COLUMN_TIME FROM $TABLE_NAME WHERE stream=? AND address=? AND port=?"
            )
        }
        statement.bindLong(1, node.stream)
        statement.bindBlob(2, node.IPv6)
        statement.bindLong(3, node.port.toLong())
        return try {
            statement.simpleQueryForLong()
        } catch (e: SQLiteDoneException) {
            null
        }
    }

    override fun getKnownAddresses(limit: Int, vararg streams: Long): List<NetworkAddress> {
        val result = LinkedList<NetworkAddress>()

        sql.readableDatabase.query(
            TABLE_NAME,
            arrayOf(COLUMN_STREAM, COLUMN_ADDRESS, COLUMN_PORT, COLUMN_SERVICES, COLUMN_TIME),
            "stream IN (?)",
            arrayOf(SqlStrings.join(*streams)), null, null,
            "time DESC",
            limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                result.add(NetworkAddress(
                    time = c.getLong(c.getColumnIndex(COLUMN_TIME)),
                    stream = c.getLong(c.getColumnIndex(COLUMN_STREAM)),
                    services = c.getLong(c.getColumnIndex(COLUMN_SERVICES)),
                    IPv6 = c.getBlob(c.getColumnIndex(COLUMN_ADDRESS)),
                    port = c.getInt(c.getColumnIndex(COLUMN_PORT))
                ))
            }
        }

        if (result.isEmpty()) {
            streams
                .asSequence()
                .mapNotNull { stableNodes[it] }
                .filterNot { it.isEmpty() }
                .mapTo(result) { Collections.selectRandom(it) }
        }
        return result
    }

    override fun offerAddresses(nodes: List<NetworkAddress>) {
        sql.writableDatabase.transaction {
            cleanUp()
            nodes
                .filter {
                    // Don't accept nodes from the future, it might be a trap
                    it.time < now + 5 * MINUTE && it.time > now - MAX_ENTRY_AGE
                }
                .forEach { node ->
                    synchronized(this) {
                        val existing = loadExistingTime(node)
                        if (existing == null) {
                            insert(node)
                        } else if (node.time > existing) {
                            update(node)
                        }
                    }
                }
        }
    }

    private fun insert(node: NetworkAddress) {
        try {
            // Create a new map of values, where column names are the keys
            val values = ContentValues().apply {
                put(COLUMN_STREAM, node.stream)
                put(COLUMN_ADDRESS, node.IPv6)
                put(COLUMN_PORT, node.port)
                put(COLUMN_SERVICES, node.services)
                put(COLUMN_TIME,
                    if (node.time > UnixTime.now) {
                        // This might be an attack, let's not use those nodes with priority
                        UnixTime.now - 7 * UnixTime.DAY
                    } else {
                        node.time
                    }
                )
            }

            sql.writableDatabase.insertOrThrow(TABLE_NAME, null, values)
        } catch (e: SQLiteConstraintException) {
            LOG.trace(e.message, e)
        }
    }

    override fun update(node: NetworkAddress) {
        try {
            val time = if (node.time > UnixTime.now) {
                // This might be an attack, let's not use those nodes with priority
                UnixTime.now - 7 * UnixTime.DAY
            } else {
                node.time
            }

            // Create a new map of values, where column names are the keys
            val values = ContentValues().apply {
                put(COLUMN_SERVICES, node.services)
                put(COLUMN_TIME, max(node.time, time))
            }

            sql.writableDatabase.update(
                TABLE_NAME,
                values,
                "stream=${node.stream} AND address=X'${hex(node.IPv6)}' AND port=${node.port}",
                null
            )
        } catch (e: SQLiteConstraintException) {
            LOG.trace(e.message, e)
        }
    }

    override fun remove(node: NetworkAddress) {
        try {
            sql.writableDatabase.delete(
                TABLE_NAME,
                "stream=${node.stream} AND address=X'${hex(node.IPv6)}' AND port=${node.port}",
                null
            )
        } catch (e: SQLiteConstraintException) {
            LOG.trace(e.message, e)
        }
    }

    override fun cleanup() {
        try {
            sql.writableDatabase.delete(
                TABLE_NAME,
                "time<${UnixTime.now - 8 * DAY}",
                null
            )
        } catch (e: SQLiteConstraintException) {
            LOG.trace(e.message, e)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AndroidInventory::class.java)

        private const val TABLE_NAME = "Node"
        private const val COLUMN_STREAM = "stream"
        private const val COLUMN_ADDRESS = "address"
        private const val COLUMN_PORT = "port"
        private const val COLUMN_SERVICES = "services"
        private const val COLUMN_TIME = "time"
    }
}
