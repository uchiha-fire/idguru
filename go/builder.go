package idguru

import (
	"fmt"
)

// Generator represents a configured, reusable identifier generator.
type Generator struct {
	algorithm         string
	prefix            string
	delimiter         string
	monotonic         bool
	snowflakeWorkerID int64
	nanoidLength      int
}

// Generate returns a formatted identifier string.
func (g *Generator) Generate() string {
	var raw string
	switch g.algorithm {
	case "uuidv7":
		raw = BytesToUUID(GenerateUUIDv7Bytes(g.monotonic))
	case "uuidv4":
		raw = UUIDv4()
	case "ulid":
		raw = ULID()
	case "ksuid":
		raw = KSUID()
	case "snowflake":
		raw, _ = Snowflake(g.snowflakeWorkerID)
	case "nanoid":
		raw = NanoID(g.nanoidLength)
	default:
		raw = UUIDv7()
	}

	if g.prefix != "" {
		return fmt.Sprintf("%s%s%s", g.prefix, g.delimiter, raw)
	}
	return raw
}

// GenerateBytes returns 16 raw bytes (for UUIDv7/v4).
func (g *Generator) GenerateBytes() []byte {
	switch g.algorithm {
	case "uuidv7":
		b := GenerateUUIDv7Bytes(g.monotonic)
		return b[:]
	default:
		return []byte(g.Generate())
	}
}

// GenerateInt64 returns a 64-bit integer (for Snowflake).
func (g *Generator) GenerateInt64() int64 {
	if g.algorithm == "snowflake" {
		_, val := Snowflake(g.snowflakeWorkerID)
		return val
	}
	return 0
}

// Builder provides a fluent API to configure a Generator.
type Builder struct {
	algorithm         string
	prefix            string
	delimiter         string
	monotonic         bool
	snowflakeWorkerID int64
	nanoidLength      int
}

// New starts a new fluent Builder.
func New() *Builder {
	return &Builder{
		algorithm:    "uuidv7",
		delimiter:    "_",
		monotonic:    true,
		nanoidLength: 21,
	}
}

// UUIDv7 configures the generator to produce RFC 9562 UUIDv7.
func (b *Builder) UUIDv7() *Builder {
	b.algorithm = "uuidv7"
	return b
}

// UUIDv4 configures the generator to produce RFC 4122 UUIDv4.
func (b *Builder) UUIDv4() *Builder {
	b.algorithm = "uuidv4"
	return b
}

// ULID configures the generator to produce ULID.
func (b *Builder) ULID() *Builder {
	b.algorithm = "ulid"
	return b
}

// KSUID configures the generator to produce KSUID.
func (b *Builder) KSUID() *Builder {
	b.algorithm = "ksuid"
	return b
}

// Snowflake configures the generator to produce distributed 64-bit Snowflake IDs.
func (b *Builder) Snowflake(workerID int64) *Builder {
	b.algorithm = "snowflake"
	b.snowflakeWorkerID = workerID
	return b
}

// NanoID configures the generator to produce NanoID with custom length.
func (b *Builder) NanoID(length int) *Builder {
	b.algorithm = "nanoid"
	b.nanoidLength = length
	return b
}

// Prefixed configures UUIDv7 with a prefix.
func (b *Builder) Prefixed(prefix string) *Builder {
	b.algorithm = "uuidv7"
	b.prefix = prefix
	return b
}

// Prefix sets the custom prefix string.
func (b *Builder) Prefix(prefix string) *Builder {
	b.prefix = prefix
	return b
}

// Delimiter sets the prefix delimiter (defaults to "_").
func (b *Builder) Delimiter(delimiter string) *Builder {
	b.delimiter = delimiter
	return b
}

// Monotonic enables or disables sub-millisecond sequence counter.
func (b *Builder) Monotonic(enable bool) *Builder {
	b.monotonic = enable
	return b
}

// Build creates the configured Generator instance.
func (b *Builder) Build() *Generator {
	delim := b.delimiter
	if delim == "" {
		delim = "_"
	}
	return &Generator{
		algorithm:         b.algorithm,
		prefix:            b.prefix,
		delimiter:         delim,
		monotonic:         b.monotonic,
		snowflakeWorkerID: b.snowflakeWorkerID,
		nanoidLength:      b.nanoidLength,
	}
}
