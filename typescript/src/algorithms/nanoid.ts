/**
 * Uniform rejection-sampling NanoID generator
 */

const DEFAULT_ALPHABET =
  "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLFGQZ_bfghjklqvwyz"

export function generateNanoId(
  size = 21,
  alphabet = DEFAULT_ALPHABET
): string {
  const len = alphabet.length
  // Power of 2 mask
  const mask = (2 << (31 - Math.clz32((len - 1) | 1))) - 1
  const step = Math.ceil((1.6 * mask * size) / len)

  let id = ""
  while (true) {
    const bytes = new Uint8Array(step)
    globalThis.crypto.getRandomValues(bytes)
    for (let i = 0; i < step; i++) {
      const byte = bytes[i]! & mask
      if (alphabet[byte]) {
        id += alphabet[byte]
        if (id.length === size) return id
      }
    }
  }
}

