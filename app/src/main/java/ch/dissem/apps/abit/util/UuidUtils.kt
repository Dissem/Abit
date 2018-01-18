package ch.dissem.apps.abit.util

import java.nio.ByteBuffer
import java.util.UUID

/**
 * SQLite has no UUID data type, and UUIDs are therefore best saved as BINARY[16]. This class
 * takes care of conversion between byte[16] and UUID.
 *
 *
 * Thanks to Brice Roncace on
 * [
 * Stack Overflow
](http://stackoverflow.com/questions/17893609/convert-uuid-to-byte-that-works-when-using-uuid-nameuuidfrombytesb) *
 * for providing the UUID <-> byte[] conversions.
 *
 */
object UuidUtils {
    /**
     * @param bytes that represent a UUID, or null for a random UUID
     * @return the UUID from the given bytes, or a random UUID if bytes is null.
     */
    fun asUuid(bytes: ByteArray?): UUID {
        if (bytes == null) {
            return UUID.randomUUID()
        }
        val bb = ByteBuffer.wrap(bytes)
        val firstLong = bb.long
        val secondLong = bb.long
        return UUID(firstLong, secondLong)
    }

    fun asBytes(uuid: UUID): ByteArray {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }
}
