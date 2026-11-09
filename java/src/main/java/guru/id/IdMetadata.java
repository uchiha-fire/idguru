package guru.id;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Metadata extracted from reverse-inspecting an identifier.
 */
public final class IdMetadata {
    private final String raw;
    private final String canonical;
    private final String prefix;
    private final String algorithm;
    private final boolean valid;
    private final int version;
    private final Instant instant;
    private final long epochMs;
    private final long workerId;
    private final long sequence;
    private final int entropyBits;
    private final Map<String, Object> details;

    public IdMetadata(String raw, String canonical, String prefix, String algorithm, boolean valid,
                      int version, Instant instant, long epochMs, long workerId, long sequence,
                      int entropyBits, Map<String, Object> details) {
        this.raw = raw;
        this.canonical = canonical;
        this.prefix = prefix;
        this.algorithm = algorithm;
        this.valid = valid;
        this.version = version;
        this.instant = instant;
        this.epochMs = epochMs;
        this.workerId = workerId;
        this.sequence = sequence;
        this.entropyBits = entropyBits;
        this.details = details != null ? Collections.unmodifiableMap(details) : Collections.emptyMap();
    }

    public String getRaw() { return raw; }
    public String getCanonical() { return canonical; }
    public String getPrefix() { return prefix; }
    public String getAlgorithm() { return algorithm; }
    public boolean isValid() { return valid; }
    public int getVersion() { return version; }
    public Instant getInstant() { return instant; }
    public long getEpochMs() { return epochMs; }
    public long getWorkerId() { return workerId; }
    public long getSequence() { return sequence; }
    public int getEntropyBits() { return entropyBits; }
    public Map<String, Object> getDetails() { return details; }

    @Override
    public String toString() {
        if (!valid) {
            return "IdMetadata[invalid, raw=" + raw + "]";
        }
        return "IdMetadata[raw=" + raw + ", algorithm=" + algorithm + ", prefix=" + prefix +
               ", instant=" + instant + ", entropyBits=" + entropyBits + "]";
    }
}
