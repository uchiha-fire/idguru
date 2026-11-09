package guru.id;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal IdGuru Facade for Java 8+.
 */
public final class IdGuru {
    private static final Pattern UUID_REGEX =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-([1-8])[0-9a-f]{3}-([89ab])[0-9a-f]{3}-[0-9a-f]{12}$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern ULID_REGEX = Pattern.compile("^[0-9A-HJKMNP-TV-Z]{26}$");
    private static final Pattern KSUID_REGEX = Pattern.compile("^[0-9A-Za-z]{27}$");
    private static final Pattern SNOWFLAKE_REGEX = Pattern.compile("^[0-9]{15,20}$");
    private static final Pattern NANOID_REGEX = Pattern.compile("^[A-Za-z0-9_-]{10,64}$");
    private static final String CROCKFORD_CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    private IdGuru() {}

    /**
     * Start a fluent builder.
     */
    public static IdBuilder builder() {
        return new IdBuilder();
    }

    /**
     * Instant RFC 9562 UUIDv7 generator (monotonic).
     */
    public static String uuidv7() {
        return IdGenerator.generateUuidV7(true);
    }

    /**
     * Instant RFC 9562 UUIDv7 generator.
     */
    public static String uuidv7(boolean monotonic) {
        return IdGenerator.generateUuidV7(monotonic);
    }

    /**
     * Instant UUIDv4 generator.
     */
    public static String uuidv4() {
        return IdGenerator.generateUuidV4();
    }

    /**
     * Instant ULID generator.
     */
    public static String ulid() {
        return IdGenerator.generateUlid();
    }

    /**
     * Instant KSUID generator.
     */
    public static String ksuid() {
        return IdGenerator.generateKsuid();
    }

    /**
     * Instant Snowflake ID generator.
     */
    public static String snowflake(int workerId) {
        return Long.toString(IdGenerator.generateSnowflakeLong(workerId, IdGenerator.DEFAULT_SNOWFLAKE_EPOCH));
    }

    /**
     * Instant NanoID generator.
     */
    public static String nanoid(int length) {
        return IdGenerator.generateNanoId(length, null);
    }

    /**
     * Instant Prefixed UUIDv7 generator.
     */
    public static String prefixed(String prefix) {
        return prefix + "_" + uuidv7();
    }

    /**
     * Universal reverse-decoder and inspector.
     */
    public static IdMetadata inspect(String rawInput) {
        if (rawInput == null) {
            return new IdMetadata(null, null, null, "unknown", false, 0, null, 0, 0, 0, 0, null);
        }

        String trimmed = rawInput.trim();
        String canonical = trimmed;
        String prefix = null;

        int underscoreIdx = trimmed.indexOf('_');
        if (underscoreIdx > 0 && underscoreIdx <= 15) {
            String cand = trimmed.substring(0, underscoreIdx);
            String rem = trimmed.substring(underscoreIdx + 1);
            if (rem.length() >= 10 && cand.matches("^[a-zA-Z0-9]+$")) {
                prefix = cand;
                canonical = rem;
            }
        } else if (!UUID_REGEX.matcher(trimmed).matches()) {
            int hyphenIdx = trimmed.indexOf('-');
            if (hyphenIdx > 0 && hyphenIdx <= 15) {
                String cand = trimmed.substring(0, hyphenIdx);
                String rem = trimmed.substring(hyphenIdx + 1);
                if (UUID_REGEX.matcher(rem).matches() || ULID_REGEX.matcher(rem).matches()) {
                    prefix = cand;
                    canonical = rem;
                }
            }
        }

        // 1. UUID Check
        Matcher uuidMatcher = UUID_REGEX.matcher(canonical);
        if (uuidMatcher.matches()) {
            int version = Integer.parseInt(uuidMatcher.group(1), 16);
            if (version == 7) {
                String hex = canonical.replace("-", "");
                long epochMs = Long.parseUnsignedLong(hex.substring(0, 12), 16);
                long seq = Long.parseLong(hex.substring(13, 16), 16);
                Instant ts = Instant.ofEpochMilli(epochMs);

                Map<String, Object> details = new HashMap<String, Object>();
                details.put("subMillisecondSequence", seq);
                details.put("randomBits", 62);

                return new IdMetadata(trimmed, canonical, prefix, "uuidv7", true,
                        7, ts, epochMs, 0, seq, 74, details);
            }

            if (version == 4) {
                Map<String, Object> details = new HashMap<String, Object>();
                details.put("randomBits", 122);
                return new IdMetadata(trimmed, canonical, prefix, "uuidv4", true,
                        4, null, 0, 0, 0, 122, details);
            }
        }

        // 2. ULID Check
        if (canonical.length() == 26 && ULID_REGEX.matcher(canonical).matches()) {
            long epochMs = 0L;
            for (int i = 0; i < 10; i++) {
                char c = canonical.charAt(i);
                int idx = CROCKFORD_CHARS.indexOf(c);
                if (idx >= 0) {
                    epochMs = epochMs * 32 + idx;
                }
            }
            Instant ts = Instant.ofEpochMilli(epochMs);
            Map<String, Object> details = new HashMap<String, Object>();
            details.put("encoding", "Crockford Base32");
            details.put("randomBits", 80);

            return new IdMetadata(trimmed, canonical, prefix, "ulid", true,
                    0, ts, epochMs, 0, 0, 80, details);
        }

        // 3. KSUID Check
        if (canonical.length() == 27 && KSUID_REGEX.matcher(canonical).matches()) {
            Map<String, Object> details = new HashMap<String, Object>();
            details.put("encoding", "Base62");
            details.put("randomBits", 128);

            return new IdMetadata(trimmed, canonical, prefix, "ksuid", true,
                    0, null, 0, 0, 0, 128, details);
        }

        // 4. Snowflake Check
        if (SNOWFLAKE_REGEX.matcher(canonical).matches()) {
            try {
                long num = Long.parseUnsignedLong(canonical);
                long timestampPart = num >>> 22;
                long workerId = (num >>> 12) & 0x3FFL;
                long seq = num & 0x0FFFL;
                long epochMs = timestampPart + IdGenerator.DEFAULT_SNOWFLAKE_EPOCH;
                Instant ts = Instant.ofEpochMilli(epochMs);

                Map<String, Object> details = new HashMap<String, Object>();
                details.put("workerId", workerId);
                details.put("sequence", seq);

                return new IdMetadata(trimmed, canonical, prefix, "snowflake", true,
                        0, ts, epochMs, workerId, seq, 22, details);
            } catch (NumberFormatException ignored) {}
        }

        // 5. NanoID Fallback
        if (NANOID_REGEX.matcher(canonical).matches()) {
            Map<String, Object> details = new HashMap<String, Object>();
            details.put("length", canonical.length());

            return new IdMetadata(trimmed, canonical, prefix, "nanoid", true,
                    0, null, 0, 0, 0, (int) (canonical.length() * 5.95), details);
        }

        return new IdMetadata(trimmed, canonical, prefix, "unknown", false,
                0, null, 0, 0, 0, 0, null);
    }
}
