package guru.id;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Thread-safe, high-throughput identifier generator.
 */
public final class IdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] DEFAULT_NANOID_ALPHABET =
            "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLFGQZ_bfghjklqvwyz".toCharArray();
    public static final long DEFAULT_SNOWFLAKE_EPOCH = 1288834974657L;

    // UUIDv7 State
    private static final Object UUID_LOCK = new Object();
    private static long lastUuidMs = 0L;
    private static int uuidSeq = 0;

    // Snowflake State
    private static final Object SNOWFLAKE_LOCK = new Object();
    private static long lastSnowflakeTs = -1L;
    private static long snowflakeSeq = 0L;

    private final String algorithm;
    private final String prefix;
    private final String delimiter;
    private final boolean monotonic;
    private final int snowflakeWorkerId;
    private final long snowflakeEpoch;
    private final int nanoidLength;
    private final char[] nanoidAlphabet;

    IdGenerator(String algorithm, String prefix, String delimiter, boolean monotonic,
                int snowflakeWorkerId, long snowflakeEpoch, int nanoidLength, char[] nanoidAlphabet) {
        this.algorithm = algorithm != null ? algorithm : "uuidv7";
        this.prefix = prefix;
        this.delimiter = delimiter != null ? delimiter : "_";
        this.monotonic = monotonic;
        this.snowflakeWorkerId = snowflakeWorkerId;
        this.snowflakeEpoch = snowflakeEpoch;
        this.nanoidLength = nanoidLength > 0 ? nanoidLength : 21;
        this.nanoidAlphabet = nanoidAlphabet != null ? nanoidAlphabet : DEFAULT_NANOID_ALPHABET;
    }

    public String getAlgorithm() { return algorithm; }
    public String getPrefix() { return prefix; }
    public String getDelimiter() { return delimiter; }
    public boolean isMonotonic() { return monotonic; }

    /**
     * Generate formatted identifier string.
     */
    public String generate() {
        String raw;
        switch (algorithm) {
            case "uuidv4":
                raw = generateUuidV4();
                break;
            case "ulid":
                raw = generateUlid();
                break;
            case "ksuid":
                raw = generateKsuid();
                break;
            case "snowflake":
                raw = Long.toString(generateSnowflakeLong(snowflakeWorkerId, snowflakeEpoch));
                break;
            case "nanoid":
                raw = generateNanoId(nanoidLength, nanoidAlphabet);
                break;
            case "uuidv7":
            default:
                raw = generateUuidV7(monotonic);
                break;
        }

        if (prefix != null && !prefix.isEmpty()) {
            return prefix + delimiter + raw;
        }
        return raw;
    }

    /**
     * Generate raw byte representation (16 bytes for UUIDv7/v4).
     */
    public byte[] generateBytes() {
        if ("uuidv7".equals(algorithm)) {
            return generateUuidV7Bytes(monotonic);
        } else if ("uuidv4".equals(algorithm)) {
            return generateUuidV4Bytes();
        }
        return generate().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Generate 64-bit long integer representation (ideal for Snowflake).
     */
    public long generateLong() {
        if ("snowflake".equals(algorithm)) {
            return generateSnowflakeLong(snowflakeWorkerId, snowflakeEpoch);
        }
        String hex = generate().replaceAll("[^0-9a-fA-F]", "");
        if (hex.length() > 16) hex = hex.substring(0, 16);
        return hex.isEmpty() ? 0L : Long.parseUnsignedLong(hex, 16);
    }

    // =========================================================================
    // Core Algorithms
    // =========================================================================

    public static byte[] generateUuidV7Bytes(boolean monotonic) {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);

        long now = System.currentTimeMillis();
        int seq;

        if (monotonic) {
            synchronized (UUID_LOCK) {
                if (now > lastUuidMs) {
                    lastUuidMs = now;
                    uuidSeq = ((bytes[6] & 0x07) << 8 | (bytes[7] & 0xFF)) & 0x0FFF;
                } else {
                    uuidSeq = (uuidSeq + 1) & 0x0FFF;
                    if (uuidSeq == 0) {
                        lastUuidMs++;
                    }
                }
                now = lastUuidMs;
                seq = uuidSeq;
            }
        } else {
            seq = ((bytes[6] & 0x0F) << 8 | (bytes[7] & 0xFF)) & 0x0FFF;
        }

        // 48-bit timestamp
        bytes[0] = (byte) ((now >>> 40) & 0xFF);
        bytes[1] = (byte) ((now >>> 32) & 0xFF);
        bytes[2] = (byte) ((now >>> 24) & 0xFF);
        bytes[3] = (byte) ((now >>> 16) & 0xFF);
        bytes[4] = (byte) ((now >>> 8) & 0xFF);
        bytes[5] = (byte) (now & 0xFF);

        // Byte 6: 4-bit version (7) | 4-bit sequence high
        bytes[6] = (byte) (0x70 | ((seq >>> 8) & 0x0F));
        // Byte 7: 8-bit sequence low
        bytes[7] = (byte) (seq & 0xFF);
        // Byte 8: 2-bit variant (10xx xxxx)
        bytes[8] = (byte) (0x80 | (bytes[8] & 0x3F));

        return bytes;
    }

    public static String bytesToUuidString(byte[] b) {
        char[] hex = new char[36];
        char[] digits = "0123456789abcdef".toCharArray();
        int out = 0;
        for (int i = 0; i < 16; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                hex[out++] = '-';
            }
            int val = b[i] & 0xFF;
            hex[out++] = digits[val >>> 4];
            hex[out++] = digits[val & 0x0F];
        }
        return new String(hex);
    }

    public static String generateUuidV7(boolean monotonic) {
        return bytesToUuidString(generateUuidV7Bytes(monotonic));
    }

    public static byte[] generateUuidV4Bytes() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        bytes[6] = (byte) (0x40 | (bytes[6] & 0x0F));
        bytes[8] = (byte) (0x80 | (bytes[8] & 0x3F));
        return bytes;
    }

    public static String generateUuidV4() {
        return bytesToUuidString(generateUuidV4Bytes());
    }

    public static String generateUlid() {
        long timeMs = System.currentTimeMillis();
        char[] chars = new char[26];

        long t = timeMs;
        for (int i = 9; i >= 0; i--) {
            chars[i] = CROCKFORD[(int) (t % 32)];
            t /= 32;
        }

        byte[] randBytes = new byte[10];
        RANDOM.nextBytes(randBytes);

        for (int i = 0; i < 16; i++) {
            int byteIdx = (i * 5) / 8;
            int bitOffset = (i * 5) % 8;
            int val = (randBytes[byteIdx] & 0xFF) << 8;
            if (byteIdx + 1 < 10) {
                val |= (randBytes[byteIdx + 1] & 0xFF);
            }
            val = (val >>> (11 - bitOffset)) & 0x1F;
            chars[10 + i] = CROCKFORD[val];
        }

        return new String(chars);
    }

    public static String generateKsuid() {
        long nowSec = (System.currentTimeMillis() / 1000L) - 1400000000L;
        byte[] fullBytes = new byte[20];
        fullBytes[0] = (byte) ((nowSec >>> 24) & 0xFF);
        fullBytes[1] = (byte) ((nowSec >>> 16) & 0xFF);
        fullBytes[2] = (byte) ((nowSec >>> 8) & 0xFF);
        fullBytes[3] = (byte) (nowSec & 0xFF);

        byte[] payload = new byte[16];
        RANDOM.nextBytes(payload);
        System.arraycopy(payload, 0, fullBytes, 4, 16);

        char[] res = new char[27];
        Arrays.fill(res, '0');

        byte[] val = Arrays.copyOf(fullBytes, fullBytes.length);
        int length = 0;

        while (true) {
            int carry = 0;
            boolean allZero = true;
            for (int i = 0; i < val.length; i++) {
                int cur = carry * 256 + (val[i] & 0xFF);
                val[i] = (byte) (cur / 62);
                carry = cur % 62;
                if (val[i] != 0) {
                    allZero = false;
                }
            }
            if (length < 27) {
                res[26 - length] = BASE62[carry];
                length++;
            }
            if (allZero) break;
        }
        return new String(res);
    }

    public static long generateSnowflakeLong(int workerId, long epoch) {
        synchronized (SNOWFLAKE_LOCK) {
            long now = System.currentTimeMillis();
            if (now < lastSnowflakeTs) {
                now = lastSnowflakeTs;
            }

            if (now == lastSnowflakeTs) {
                snowflakeSeq = (snowflakeSeq + 1) & 0x0FFFL;
                if (snowflakeSeq == 0) {
                    while (now <= lastSnowflakeTs) {
                        now = System.currentTimeMillis();
                    }
                }
            } else {
                snowflakeSeq = 0L;
            }

            lastSnowflakeTs = now;
            long workerPart = ((long) (workerId & 0x3FF)) << 12;
            long timePart = (now - epoch) << 22;
            return timePart | workerPart | snowflakeSeq;
        }
    }

    public static String generateNanoId(int length, char[] alphabet) {
        if (length <= 0) length = 21;
        if (alphabet == null || alphabet.length == 0) alphabet = DEFAULT_NANOID_ALPHABET;

        int alphaLen = alphabet.length;
        int mask = (2 << (31 - Integer.numberOfLeadingZeros((alphaLen - 1) | 1))) - 1;
        int step = (int) Math.ceil(1.6 * mask * length / alphaLen);

        char[] chars = new char[length];
        byte[] bytes = new byte[step];
        int count = 0;

        while (true) {
            RANDOM.nextBytes(bytes);
            for (int i = 0; i < step; i++) {
                int idx = bytes[i] & mask;
                if (idx < alphaLen) {
                    chars[count++] = alphabet[idx];
                    if (count == length) {
                        return new String(chars);
                    }
                }
            }
        }
    }
}
