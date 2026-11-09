/**
 * RFC 9562 UUIDv7 Implementation
 *
 * 48-bit big-endian millisecond Unix epoch
 * 4-bit version (0b0111 = 7)
 * 12-bit fractional / sub-ms counter (Method 3 monotonic sequence)
 * 2-bit variant (0b10)
 * 62-bit cryptographically secure pseudorandom number
 */

let lastMs = 0
let seq = 0

function getCrypto(): Crypto {
  if (typeof globalThis !== "undefined" && globalThis.crypto) {
    return globalThis.crypto
  }
  throw new Error("No Web Crypto API available in this environment")
}

export function generateUuidV7Bytes(monotonic = true): Uint8Array {
  const crypto = getCrypto()
  const bytes = new Uint8Array(16)
  crypto.getRandomValues(bytes)

  const now = Date.now()

  if (monotonic) {
    if (now > lastMs) {
      lastMs = now
      // Reset sequence with a random offset within 12 bits to prevent counter prediction
      seq = (bytes[6]! & 0x07) << 8 | bytes[7]!
      seq = seq & 0x0fff
    } else {
      // Same millisecond or backward clock: increment sequence
      seq = (seq + 1) & 0x0fff
      if (seq === 0) {
        // Counter rollover within same ms: simulate next ms
        lastMs += 1
      }
    }
  } else {
    lastMs = now
    seq = (bytes[6]! & 0x0f) << 8 | bytes[7]!
  }

  const timestamp = monotonic ? lastMs : now

  // Byte 0-5: 48-bit timestamp
  bytes[0] = (timestamp / 0x10000000000) & 0xff
  bytes[1] = (timestamp / 0x100000000) & 0xff
  bytes[2] = (timestamp / 0x1000000) & 0xff
  bytes[3] = (timestamp / 0x10000) & 0xff
  bytes[4] = (timestamp / 0x100) & 0xff
  bytes[5] = timestamp & 0xff

  // Byte 6: 4-bit version (7) | top 4 bits of sequence
  bytes[6] = 0x70 | ((seq >> 8) & 0x0f)

  // Byte 7: bottom 8 bits of sequence
  bytes[7] = seq & 0xff

  // Byte 8: 2-bit variant (10xx xxxx) | top 6 bits of random
  bytes[8] = 0x80 | (bytes[8]! & 0x3f)

  // Bytes 9-15 remain random
  return bytes
}

export function bytesToUuidString(b: Uint8Array): string {
  const hex: string[] = []
  for (let i = 0; i < 16; i++) {
    hex.push(b[i]!.toString(16).padStart(2, "0"))
  }
  return `${hex[0]}${hex[1]}${hex[2]}${hex[3]}-${hex[4]}${hex[5]}-${hex[6]}${hex[7]}-${hex[8]}${hex[9]}-${hex[10]}${hex[11]}${hex[12]}${hex[13]}${hex[14]}${hex[15]}`
}

export function generateUuidV7(monotonic = true): string {
  return bytesToUuidString(generateUuidV7Bytes(monotonic))
}

