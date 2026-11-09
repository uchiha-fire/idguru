package guru.id;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("Running Java 8 IdGuru Unit Tests...");
        IdGuruTest test = new IdGuruTest();
        int passed = 0;

        try {
            test.testUuidV7Format();
            passed++;
            test.testUuidV7Monotonicity();
            passed++;
            test.testUuidV4();
            passed++;
            test.testUlid();
            passed++;
            test.testKsuid();
            passed++;
            test.testSnowflake();
            passed++;
            test.testNanoId();
            passed++;
            test.testPrefixed();
            passed++;
            test.testBuilderUuidV7Prefixed();
            passed++;
            test.testBuilderSnowflake();
            passed++;
            test.testBuilderNanoId();
            passed++;
            test.testInspectUuidV7();
            passed++;
            test.testInspectPrefixed();
            passed++;
            test.testInspectUlid();
            passed++;
            test.testInspectSnowflake();
            passed++;
            test.testInspectInvalid();
            passed++;

            System.out.println("SUCCESS: All " + passed + " Java unit tests passed!");
        } catch (Throwable t) {
            System.err.println("TEST FAILED at step " + (passed + 1) + ": " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }
}
