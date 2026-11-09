package guru.id;

import java.time.Instant;
import java.util.Objects;

public class IdGuruTest {

    private static void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Assertion failed: expected true");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertFalse(boolean condition) {
        if (condition) throw new AssertionError("Assertion failed: expected false");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Assertion failed: expected [" + expected + "] but got [" + actual + "]");
        }
    }

    private static void assertNotNull(Object obj) {
        if (obj == null) throw new AssertionError("Assertion failed: expected non-null object");
    }

    public void testUuidV7Format() {
        String id = IdGuru.uuidv7();
        assertNotNull(id);
        assertTrue(id.matches("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"));
        assertEquals('7', id.charAt(14));
        char v = id.charAt(19);
        assertTrue(v == '8' || v == '9' || v == 'a' || v == 'b');
    }

    public void testUuidV7Monotonicity() {
        final int count = 5000;
        String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = IdGuru.uuidv7(true);
        }

        for (int i = 1; i < count; i++) {
            assertTrue(ids[i].compareTo(ids[i - 1]) > 0,
                    "UUIDv7 sequence must be strictly ascending: " + ids[i] + " <= " + ids[i - 1]);
        }
    }

    public void testUuidV4() {
        String id = IdGuru.uuidv4();
        assertTrue(id.matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"));
    }

    public void testUlid() {
        String id = IdGuru.ulid();
        assertEquals(26, id.length());
        assertTrue(id.matches("^[0-9A-HJKMNP-TV-Z]{26}$"));
    }

    public void testKsuid() {
        String id = IdGuru.ksuid();
        assertEquals(27, id.length());
        assertTrue(id.matches("^[0-9A-Za-z]{27}$"));
    }

    public void testSnowflake() {
        String id = IdGuru.snowflake(42);
        assertTrue(id.matches("^[0-9]{15,20}$"));
    }

    public void testNanoId() {
        String id21 = IdGuru.nanoid(21);
        assertEquals(21, id21.length());

        String id32 = IdGuru.nanoid(32);
        assertEquals(32, id32.length());
    }

    public void testPrefixed() {
        String id = IdGuru.prefixed("inv");
        assertTrue(id.startsWith("inv_"));
        String raw = id.substring(4);
        assertEquals('7', raw.charAt(14));
    }

    public void testBuilderUuidV7Prefixed() {
        IdGenerator gen = IdGuru.builder()
                .uuidv7()
                .prefix("usr")
                .delimiter("_")
                .monotonic(true)
                .build();

        assertEquals("uuidv7", gen.getAlgorithm());
        assertEquals("usr", gen.getPrefix());

        String userId = gen.generate();
        assertTrue(userId.startsWith("usr_"));

        byte[] bytes = gen.generateBytes();
        assertEquals(16, bytes.length);
    }

    public void testBuilderSnowflake() {
        IdGenerator gen = IdGuru.builder()
                .snowflake(15)
                .prefix("ord")
                .build();

        String orderId = gen.generate();
        assertTrue(orderId.startsWith("ord_"));

        long numeric = gen.generateLong();
        assertTrue(numeric > 0);
    }

    public void testBuilderNanoId() {
        IdGenerator gen = IdGuru.builder()
                .nanoid(16, "ABCDEF123456")
                .prefix("tok")
                .delimiter("-")
                .build();

        String token = gen.generate();
        assertTrue(token.startsWith("tok-"));
        String raw = token.substring(4);
        assertEquals(16, raw.length());
        assertTrue(raw.matches("^[A-F1-6]{16}$"));
    }

    public void testInspectUuidV7() {
        long before = System.currentTimeMillis() - 1000;
        String id = IdGuru.uuidv7();
        long after = System.currentTimeMillis() + 1000;

        IdMetadata meta = IdGuru.inspect(id);
        assertTrue(meta.isValid());
        assertEquals("uuidv7", meta.getAlgorithm());
        assertEquals(7, meta.getVersion());
        assertTrue(meta.getEpochMs() >= before && meta.getEpochMs() <= after);
        assertNotNull(meta.getInstant());
    }

    public void testInspectPrefixed() {
        String id = "cust_019a6b42-8e12-7a22-9f1a-b83c7d612e44";
        IdMetadata meta = IdGuru.inspect(id);
        assertTrue(meta.isValid());
        assertEquals("cust", meta.getPrefix());
        assertEquals("019a6b42-8e12-7a22-9f1a-b83c7d612e44", meta.getCanonical());
        assertEquals("uuidv7", meta.getAlgorithm());
    }

    public void testInspectUlid() {
        String id = IdGuru.ulid();
        IdMetadata meta = IdGuru.inspect(id);
        assertTrue(meta.isValid());
        assertEquals("ulid", meta.getAlgorithm());
        assertTrue(meta.getEpochMs() > 1700000000000L);
    }

    public void testInspectSnowflake() {
        String id = IdGuru.snowflake(8);
        IdMetadata meta = IdGuru.inspect(id);
        assertTrue(meta.isValid());
        assertEquals("snowflake", meta.getAlgorithm());
        assertEquals(8L, meta.getWorkerId());
    }

    public void testInspectInvalid() {
        IdMetadata meta = IdGuru.inspect("invalid_garbage_!@#$%");
        assertFalse(meta.isValid());
        assertEquals("unknown", meta.getAlgorithm());
    }
}
