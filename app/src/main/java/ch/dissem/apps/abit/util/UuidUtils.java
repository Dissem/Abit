package ch.dissem.apps.abit.util;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * SQLite has no UUID data type, and UUIDs are therefore best saved as BINARY[16]. This class
 * takes care of conversion between byte[16] and UUID.
 * <p>
 * Thanks to Brice Roncace on
 * <a href="http://stackoverflow.com/questions/17893609/convert-uuid-to-byte-that-works-when-using-uuid-nameuuidfrombytesb">
 * Stack Overflow
 * </a>
 * for providing the UUID <-> byte[] conversions.
 * </p>
 */
public class UuidUtils {
    /**
     * @param bytes that represent a UUID, or null for a random UUID
     * @return the UUID from the given bytes, or a random UUID if bytes is null.
     */
    public static UUID asUuid(byte[] bytes) {
        if (bytes == null) {
            return UUID.randomUUID();
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static byte[] asBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
