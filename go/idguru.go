// Package idguru provides universal, zero-dependency identifier generation,
// fluent builder, and reverse-inspection engine for Go.
package idguru

import (
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"sync"
	"time"
)

var (
	uuidLock   sync.Mutex
	lastMs     int64
	uuidSeq    uint16
	crockford  = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
	base62     = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
	defaultNanoidAlpha = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLFGQZ_bfghjklqvwyz"

	snowflakeLock    sync.Mutex
	snowflakeLastTs  int64 = -1
	snowflakeSequence int64
)

const defaultSnowflakeEpoch int64 = 1288834974657 // Twitter Snowflake epoch

// GenerateUUIDv7Bytes generates 16 raw bytes representing an RFC 9562 UUIDv7.
func GenerateUUIDv7Bytes(monotonic bool) [16]byte {
	var b [16]byte
	if _, err := rand.Read(b[:]); err != nil {
		panic(err)
	}

	now := time.Now().UnixMilli()

	uuidLock.Lock()
	if monotonic {
		if now > lastMs {
			lastMs = now
			uuidSeq = (uint16(b[6]&0x07) << 8) | uint16(b[7])
			uuidSeq &= 0x0FFF
		} else {
			uuidSeq = (uuidSeq + 1) & 0x0FFF
			if uuidSeq == 0 {
				lastMs++
			}
		}
		now = lastMs
	} else {
		uuidSeq = (uint16(b[6]&0x0F) << 8) | uint16(b[7])
	}
	seq := uuidSeq
	uuidLock.Unlock()

	// 48-bit timestamp
	b[0] = byte(now >> 40)
	b[1] = byte(now >> 32)
	b[2] = byte(now >> 24)
	b[3] = byte(now >> 16)
	b[4] = byte(now >> 8)
	b[5] = byte(now)

	// Byte 6: 4-bit version (7) | 4-bit sequence high
	b[6] = 0x70 | byte((seq>>8)&0x0F)
	// Byte 7: 8-bit sequence low
	b[7] = byte(seq & 0xFF)
	// Byte 8: 2-bit variant (10xx xxxx)
	b[8] = 0x80 | (b[8] & 0x3F)

	return b
}

// BytesToUUID formats 16 bytes into standard canonical 8-4-4-4-12 hex string.
func BytesToUUID(b [16]byte) string {
	buf := make([]byte, 36)
	hex.Encode(buf[0:8], b[0:4])
	buf[8] = '-'
	hex.Encode(buf[9:13], b[4:6])
	buf[13] = '-'
	hex.Encode(buf[14:18], b[6:8])
	buf[18] = '-'
	hex.Encode(buf[19:23], b[8:10])
	buf[23] = '-'
	hex.Encode(buf[24:36], b[10:16])
	return string(buf)
}

// UUIDv7 generates an RFC 9562 UUIDv7 string.
func UUIDv7() string {
	return BytesToUUID(GenerateUUIDv7Bytes(true))
}

// UUIDv4 generates a cryptographically random RFC 4122/9562 UUIDv4 string.
func UUIDv4() string {
	var b [16]byte
	if _, err := rand.Read(b[:]); err != nil {
		panic(err)
	}
	b[6] = 0x40 | (b[6] & 0x0F) // Version 4
	b[8] = 0x80 | (b[8] & 0x3F) // Variant
	return BytesToUUID(b)
}

// ULID generates a 26-character Crockford Base32 ULID.
func ULID() string {
	now := time.Now().UnixMilli()

	timeChars := make([]byte, 10)
	t := now
	for i := 9; i >= 0; i-- {
		timeChars[i] = crockford[t%32]
		t /= 32
	}

	var randBytes [10]byte
	if _, err := rand.Read(randBytes[:]); err != nil {
		panic(err)
	}

	randChars := make([]byte, 16)
	for i := 0; i < 16; i++ {
		byteIdx := (i * 5) / 8
		bitOffset := (i * 5) % 8
		val := int(randBytes[byteIdx]) << 8
		if byteIdx+1 < 10 {
			val |= int(randBytes[byteIdx+1])
		}
		val = (val >> (11 - bitOffset)) & 0x1F
		randChars[i] = crockford[val]
	}

	return string(timeChars) + string(randChars)
}

// KSUID generates a 27-character Segment KSUID (Base62).
func KSUID() string {
	const ksuidEpoch int64 = 1400000000
	nowSec := uint32(time.Now().Unix() - ksuidEpoch)

	var payload [16]byte
	if _, err := rand.Read(payload[:]); err != nil {
		panic(err)
	}

	// 20 bytes total (4 bytes timestamp + 16 bytes payload)
	fullBytes := make([]byte, 20)
	fullBytes[0] = byte(nowSec >> 24)
	fullBytes[1] = byte(nowSec >> 16)
	fullBytes[2] = byte(nowSec >> 8)
	fullBytes[3] = byte(nowSec)
	copy(fullBytes[4:], payload[:])

	// Base62 encode
	res := make([]byte, 27)
	for i := range res {
		res[i] = '0'
	}

	// Base62 division
	length := 0
	val := make([]byte, len(fullBytes))
	copy(val, fullBytes)

	for {
		carry := 0
		allZero := true
		for i := 0; i < len(val); i++ {
			cur := carry*256 + int(val[i])
			val[i] = byte(cur / 62)
			carry = cur % 62
			if val[i] != 0 {
				allZero = false
			}
		}
		if length < 27 {
			res[26-length] = base62[carry]
			length++
		}
		if allZero {
			break
		}
	}

	return string(res)
}

// Snowflake generates a 64-bit distributed snowflake ID string and int64.
func Snowflake(workerID int64) (string, int64) {
	snowflakeLock.Lock()
	defer snowflakeLock.Unlock()

	now := time.Now().UnixMilli()
	if now < snowflakeLastTs {
		now = snowflakeLastTs
	}

	if now == snowflakeLastTs {
		snowflakeSequence = (snowflakeSequence + 1) & 0x0FFF
		if snowflakeSequence == 0 {
			for now <= snowflakeLastTs {
				now = time.Now().UnixMilli()
			}
		}
	} else {
		snowflakeSequence = 0
	}

	snowflakeLastTs = now
	workerPart := (workerID & 0x3FF) << 12
	timePart := (now - defaultSnowflakeEpoch) << 22
	id := timePart | workerPart | snowflakeSequence

	return fmt.Sprintf("%d", id), id
}

// NanoID generates a uniform rejection-sampled NanoID string.
func NanoID(length int) string {
	if length <= 0 {
		length = 21
	}
	alphaLen := len(defaultNanoidAlpha)
	mask := 63 // 2^6 - 1 = 63, covers 64 chars

	chars := make([]byte, 0, length)
	step := int(1.6 * float64(mask*length) / float64(alphaLen)) + 1
	buf := make([]byte, step)

	for len(chars) < length {
		if _, err := rand.Read(buf); err != nil {
			panic(err)
		}
		for _, b := range buf {
			idx := int(b) & mask
			if idx < alphaLen {
				chars = append(chars, defaultNanoidAlpha[idx])
				if len(chars) == length {
					return string(chars)
				}
			}
		}
	}
	return string(chars)
}

// Prefixed generates a prefixed UUIDv7 string.
func Prefixed(prefix string) string {
	return prefix + "_" + UUIDv7()
}
