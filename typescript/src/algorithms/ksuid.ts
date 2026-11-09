/**
 * Segment KSUID (K-Sortable Unique Identifier)
 * 20 bytes: 4 bytes timestamp (epoch 1400000000) + 16 bytes randomness
 * Base62 encoded to 27 characters
 */

const KSUID_EPOCH = 1400000000 // 2014-05-13T16:53:20Z
const BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

export function generateKsuid(): string {
  const bytes = new Uint8Array(20)
  const nowSec = Math.floor(Date.now() / 1000) - KSUID_EPOCH

  bytes[0] = (nowSec >>> 24) & 0xff
  bytes[1] = (nowSec >>> 16) & 0xff
  bytes[2] = (nowSec >>> 8) & 0xff
  bytes[3] = nowSec & 0xff

  const rand = new Uint8Array(16)
  globalThis.crypto.getRandomValues(rand)
  bytes.set(rand, 4)

  // Base62 encode 20 bytes
  let num = 0n
  for (let i = 0; i < 20; i++) {
    num = (num << 8n) | BigInt(bytes[i]!)
  }

  let str = ""
  while (num > 0n) {
    const rem = Number(num % 62n)
    str = BASE62[rem] + str
    num = num / 62n
  }

  return str.padStart(27, "0")
}

