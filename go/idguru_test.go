package idguru_test

import (
	"regexp"
	"strings"
	"testing"
	"time"

	"github.com/uchiha-fire/idguru"
)

func TestUUIDv7Format(t *testing.T) {
	id := idguru.UUIDv7()
	pattern := `^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`
	matched, err := regexp.MatchString(pattern, id)
	if err != nil || !matched {
		t.Fatalf("UUIDv7 does not match RFC 9562 pattern: %s", id)
	}

	if id[14] != '7' {
		t.Fatalf("UUIDv7 version byte is not 7: %c", id[14])
	}

	varChar := id[19]
	if varChar != '8' && varChar != '9' && varChar != 'a' && varChar != 'b' {
		t.Fatalf("UUIDv7 variant character invalid: %c", varChar)
	}
}

func TestUUIDv7Monotonicity(t *testing.T) {
	const count = 5000
	ids := make([]string, count)
	for i := 0; i < count; i++ {
		ids[i] = idguru.UUIDv7()
	}

	for i := 1; i < count; i++ {
		if ids[i] <= ids[i-1] {
			t.Fatalf("UUIDv7 not strictly monotonic at index %d: %s <= %s", i, ids[i], ids[i-1])
		}
	}
}

func TestUUIDv4(t *testing.T) {
	id := idguru.UUIDv4()
	pattern := `^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`
	matched, err := regexp.MatchString(pattern, id)
	if err != nil || !matched {
		t.Fatalf("UUIDv4 format invalid: %s", id)
	}
}

func TestULID(t *testing.T) {
	id := idguru.ULID()
	if len(id) != 26 {
		t.Fatalf("ULID length is not 26: %d", len(id))
	}
	matched, _ := regexp.MatchString(`^[0-9A-HJKMNP-TV-Z]{26}$`, id)
	if !matched {
		t.Fatalf("ULID characters invalid: %s", id)
	}
}

func TestKSUID(t *testing.T) {
	id := idguru.KSUID()
	if len(id) != 27 {
		t.Fatalf("KSUID length is not 27: %d", len(id))
	}
	matched, _ := regexp.MatchString(`^[0-9A-Za-z]{27}$`, id)
	if !matched {
		t.Fatalf("KSUID characters invalid: %s", id)
	}
}

func TestSnowflake(t *testing.T) {
	idStr, idInt := idguru.Snowflake(42)
	if len(idStr) < 15 {
		t.Fatalf("Snowflake ID too short: %s", idStr)
	}
	if idInt <= 0 {
		t.Fatalf("Snowflake int non-positive: %d", idInt)
	}
}

func TestNanoID(t *testing.T) {
	idDefault := idguru.NanoID(21)
	if len(idDefault) != 21 {
		t.Fatalf("NanoID length not 21: %s", idDefault)
	}

	idCustom := idguru.NanoID(32)
	if len(idCustom) != 32 {
		t.Fatalf("NanoID length not 32: %s", idCustom)
	}
}

func TestPrefixed(t *testing.T) {
	id := idguru.Prefixed("inv")
	if !strings.HasPrefix(id, "inv_") {
		t.Fatalf("Prefixed ID missing prefix: %s", id)
	}
	raw := strings.TrimPrefix(id, "inv_")
	if raw[14] != '7' {
		t.Fatalf("Prefixed ID raw part is not UUIDv7: %s", raw)
	}
}

func TestBuilderUUIDv7Prefixed(t *testing.T) {
	gen := idguru.New().
		UUIDv7().
		Prefix("usr").
		Delimiter("_").
		Monotonic(true).
		Build()

	userId := gen.Generate()
	if !strings.HasPrefix(userId, "usr_") {
		t.Fatalf("Generated ID does not have prefix: %s", userId)
	}

	bytes := gen.GenerateBytes()
	if len(bytes) != 16 {
		t.Fatalf("GenerateBytes length not 16: %d", len(bytes))
	}
}

func TestBuilderSnowflake(t *testing.T) {
	gen := idguru.New().
		Snowflake(10).
		Prefix("ord").
		Build()

	orderId := gen.Generate()
	if !strings.HasPrefix(orderId, "ord_") {
		t.Fatalf("Snowflake missing prefix: %s", orderId)
	}

	intVal := gen.GenerateInt64()
	if intVal <= 0 {
		t.Fatalf("Snowflake int non-positive: %d", intVal)
	}
}

func TestInspectorUUIDv7(t *testing.T) {
	before := time.Now().Add(-1 * time.Second).UnixMilli()
	id := idguru.UUIDv7()
	after := time.Now().Add(1 * time.Second).UnixMilli()

	meta := idguru.Inspect(id)
	if !meta.IsValid {
		t.Fatalf("UUIDv7 inspection marked invalid: %s", id)
	}
	if meta.Algorithm != "uuidv7" {
		t.Fatalf("UUIDv7 algorithm mismatch: %s", meta.Algorithm)
	}
	if meta.Version != 7 {
		t.Fatalf("UUIDv7 version mismatch: %d", meta.Version)
	}
	if meta.EpochMs < before || meta.EpochMs > after {
		t.Fatalf("UUIDv7 epoch ms out of bounds: %d (before: %d, after: %d)", meta.EpochMs, before, after)
	}
}

func TestInspectorPrefixed(t *testing.T) {
	id := "cust_019a6b42-8e12-7a22-9f1a-b83c7d612e44"
	meta := idguru.Inspect(id)
	if !meta.IsValid {
		t.Fatalf("Prefixed inspection invalid: %s", id)
	}
	if meta.Prefix != "cust" {
		t.Fatalf("Prefixed prefix mismatch: %s", meta.Prefix)
	}
	if meta.Canonical != "019a6b42-8e12-7a22-9f1a-b83c7d612e44" {
		t.Fatalf("Canonical mismatch: %s", meta.Canonical)
	}
	if meta.Algorithm != "uuidv7" {
		t.Fatalf("Algorithm mismatch: %s", meta.Algorithm)
	}
}

func TestInspectorULID(t *testing.T) {
	id := idguru.ULID()
	meta := idguru.Inspect(id)
	if !meta.IsValid || meta.Algorithm != "ulid" {
		t.Fatalf("ULID inspection failed: %+v", meta)
	}
	if meta.EpochMs < 1700000000000 {
		t.Fatalf("ULID epoch timestamp suspicious: %d", meta.EpochMs)
	}
}

func TestInspectorSnowflake(t *testing.T) {
	id, _ := idguru.Snowflake(7)
	meta := idguru.Inspect(id)
	if !meta.IsValid || meta.Algorithm != "snowflake" {
		t.Fatalf("Snowflake inspection failed: %+v", meta)
	}
	if meta.WorkerID != 7 {
		t.Fatalf("Snowflake worker ID mismatch: %d", meta.WorkerID)
	}
}

func TestInspectorInvalid(t *testing.T) {
	meta := idguru.Inspect("invalid-id-garbage-!@#$")
	if meta.IsValid || meta.Algorithm != "unknown" {
		t.Fatalf("Garbage input should be invalid: %+v", meta)
	}
}
