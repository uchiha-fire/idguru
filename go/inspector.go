package idguru

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"time"
)

var (
	uuidRegex      = regexp.MustCompile(`(?i)^[0-9a-f]{8}-[0-9a-f]{4}-([1-8])[0-9a-f]{3}-([89ab])[0-9a-f]{3}-[0-9a-f]{12}$`)
	ulidRegex      = regexp.MustCompile(`^[0-9A-HJKMNP-TV-Z]{26}$`)
	ksuidRegex     = regexp.MustCompile(`^[0-9A-Za-z]{27}$`)
	snowflakeRegex = regexp.MustCompile(`^[0-9]{15,20}$`)
	nanoidRegex    = regexp.MustCompile(`^[A-Za-z0-9_-]{10,64}$`)
)

// InspectionResult contains metadata decoded from an identifier.
type InspectionResult struct {
	Raw         string
	Canonical   string
	Prefix      string
	Algorithm   string
	IsValid     bool
	Version     int
	Timestamp   time.Time
	EpochMs     int64
	WorkerID    int64
	Sequence    int64
	EntropyBits int
}

// Inspect decodes and reverse-engineers any supported identifier.
func Inspect(rawInput string) InspectionResult {
	trimmed := strings.TrimSpace(rawInput)
	canonical := trimmed
	var prefix string

	underscoreIdx := strings.Index(trimmed, "_")
	if underscoreIdx > 0 && underscoreIdx <= 15 {
		cand := trimmed[:underscoreIdx]
		rem := trimmed[underscoreIdx+1:]
		if len(rem) >= 10 {
			prefix = cand
			canonical = rem
		}
	} else if !uuidRegex.MatchString(trimmed) {
		hyphenIdx := strings.Index(trimmed, "-")
		if hyphenIdx > 0 && hyphenIdx <= 15 {
			cand := trimmed[:hyphenIdx]
			rem := trimmed[hyphenIdx+1:]
			if uuidRegex.MatchString(rem) || ulidRegex.MatchString(rem) {
				prefix = cand
				canonical = rem
			}
		}
	}

	// 1. UUID Check
	if matches := uuidRegex.FindStringSubmatch(canonical); matches != nil {
		version, _ := strconv.ParseInt(matches[1], 16, 32)
		if version == 7 {
			hexClean := strings.ReplaceAll(canonical, "-", "")
			epochMs, _ := strconv.ParseInt(hexClean[0:12], 16, 64)
			seq, _ := strconv.ParseInt(hexClean[13:16], 16, 64)

			return InspectionResult{
				Raw:         trimmed,
				Canonical:   canonical,
				Prefix:      prefix,
				Algorithm:   "uuidv7",
				IsValid:     true,
				Version:     7,
				Timestamp:   time.UnixMilli(epochMs).UTC(),
				EpochMs:     epochMs,
				Sequence:    seq,
				EntropyBits: 74,
			}
		}

		if version == 4 {
			return InspectionResult{
				Raw:         trimmed,
				Canonical:   canonical,
				Prefix:      prefix,
				Algorithm:   "uuidv4",
				IsValid:     true,
				Version:     4,
				EntropyBits: 122,
			}
		}
	}

	// 2. ULID Check
	if len(canonical) == 26 && ulidRegex.MatchString(canonical) {
		var epochMs int64
		for i := 0; i < 10; i++ {
			charIdx := strings.IndexByte(crockford, canonical[i])
			if charIdx >= 0 {
				epochMs = epochMs*32 + int64(charIdx)
			}
		}

		return InspectionResult{
			Raw:         trimmed,
			Canonical:   canonical,
			Prefix:      prefix,
			Algorithm:   "ulid",
			IsValid:     true,
			Timestamp:   time.UnixMilli(epochMs).UTC(),
			EpochMs:     epochMs,
			EntropyBits: 80,
		}
	}

	// 3. KSUID Check
	if len(canonical) == 27 && ksuidRegex.MatchString(canonical) {
		return InspectionResult{
			Raw:         trimmed,
			Canonical:   canonical,
			Prefix:      prefix,
			Algorithm:   "ksuid",
			IsValid:     true,
			EntropyBits: 128,
		}
	}

	// 4. Snowflake Check
	if snowflakeRegex.MatchString(canonical) {
		if num, err := strconv.ParseInt(canonical, 10, 64); err == nil {
			timestampPart := num >> 22
			workerID := (num >> 12) & 0x3FF
			seq := num & 0x0FFF
			epochMs := timestampPart + defaultSnowflakeEpoch

			return InspectionResult{
				Raw:         trimmed,
				Canonical:   canonical,
				Prefix:      prefix,
				Algorithm:   "snowflake",
				IsValid:     true,
				Timestamp:   time.UnixMilli(epochMs).UTC(),
				EpochMs:     epochMs,
				WorkerID:    workerID,
				Sequence:    seq,
				EntropyBits: 22,
			}
		}
	}

	// 5. NanoID Fallback
	if nanoidRegex.MatchString(canonical) {
		return InspectionResult{
			Raw:         trimmed,
			Canonical:   canonical,
			Prefix:      prefix,
			Algorithm:   "nanoid",
			IsValid:     true,
			EntropyBits: int(float64(len(canonical)) * 5.95),
		}
	}

	return InspectionResult{
		Raw:       trimmed,
		Canonical: canonical,
		Prefix:    prefix,
		Algorithm: "unknown",
		IsValid:   false,
	}
}

// String provides a human-readable representation of InspectionResult.
func (r InspectionResult) String() string {
	if !r.IsValid {
		return fmt.Sprintf("Invalid ID: %s", r.Raw)
	}
	return fmt.Sprintf("ID[%s, algorithm=%s, timestamp=%v]", r.Raw, r.Algorithm, r.Timestamp)
}
