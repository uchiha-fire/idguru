package guru.id;

/**
 * Fluent, type-safe builder for constructing {@link IdGenerator} instances.
 */
public final class IdBuilder {
    private String algorithm = "uuidv7";
    private String prefix = null;
    private String delimiter = "_";
    private boolean monotonic = true;
    private int snowflakeWorkerId = 1;
    private long snowflakeEpoch = IdGenerator.DEFAULT_SNOWFLAKE_EPOCH;
    private int nanoidLength = 21;
    private char[] nanoidAlphabet = null;

    public IdBuilder uuidv7() {
        this.algorithm = "uuidv7";
        return this;
    }

    public IdBuilder uuidv4() {
        this.algorithm = "uuidv4";
        return this;
    }

    public IdBuilder ulid() {
        this.algorithm = "ulid";
        return this;
    }

    public IdBuilder ksuid() {
        this.algorithm = "ksuid";
        return this;
    }

    public IdBuilder snowflake(int workerId) {
        this.algorithm = "snowflake";
        this.snowflakeWorkerId = workerId;
        return this;
    }

    public IdBuilder snowflake(int workerId, long customEpoch) {
        this.algorithm = "snowflake";
        this.snowflakeWorkerId = workerId;
        this.snowflakeEpoch = customEpoch;
        return this;
    }

    public IdBuilder nanoid(int length) {
        this.algorithm = "nanoid";
        this.nanoidLength = length;
        return this;
    }

    public IdBuilder nanoid(int length, String alphabet) {
        this.algorithm = "nanoid";
        this.nanoidLength = length;
        this.nanoidAlphabet = alphabet != null ? alphabet.toCharArray() : null;
        return this;
    }

    public IdBuilder prefixed(String prefix) {
        this.algorithm = "uuidv7";
        this.prefix = prefix;
        return this;
    }

    public IdBuilder prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public IdBuilder delimiter(String delimiter) {
        this.delimiter = delimiter != null ? delimiter : "_";
        return this;
    }

    public IdBuilder monotonic(boolean monotonic) {
        this.monotonic = monotonic;
        return this;
    }

    public IdBuilder monotonic() {
        this.monotonic = true;
        return this;
    }

    public IdGenerator build() {
        return new IdGenerator(
                algorithm,
                prefix,
                delimiter,
                monotonic,
                snowflakeWorkerId,
                snowflakeEpoch,
                nanoidLength,
                nanoidAlphabet
        );
    }
}
