/**
 * ULID (Universally Unique Lexicographically Sortable Identifier)
 * 128-bit compatible: 48-bit timestamp + 80-bit randomness encoded in Crockford Base32
 */

const CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

export function generateUlid(timestampMs: number = Date.now()): string {
  // 10 chars timestamp (48 bits)
  let timeStr = ""
  let time = timestampMs
  for (let i = 9; i >= 0; i--) {
    const mod = time % 32
    timeStr = CROCKFORD_ALPHABET[mod] + timeStr
    time = Math.floor(time / 32)
  }

  // 16 chars randomness (80 bits = 10 bytes)
  const randBytes = new Uint8Array(10)
  globalThis.crypto.getRandomValues(randBytes)

  let randStr = ""
  for (let i = 0; i < 16; i++) {
    // Pick 5 bits per char
    const byteIdx = Math.floor((i * 5) / 8)
    const bitOffset = (i * 5) % 8
    let val = (randBytes[byteIdx]! << 8) | (randBytes[byteIdx + 1] ?? 0)
    val = (val >> (11 - bitOffset)) & 0x1f
    randStr += CROCKFORD_ALPHABET[val]
  }

  return timeStr + randStr
}

