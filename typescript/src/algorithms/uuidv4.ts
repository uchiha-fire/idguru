import { bytesToUuidString } from "./uuidv7"

export function generateUuidV4Bytes(): Uint8Array {
  const bytes = new Uint8Array(16)
  globalThis.crypto.getRandomValues(bytes)

  // Version 4: 0b0100
  bytes[6] = 0x40 | (bytes[6]! & 0x0f)
  // Variant: 0b10
  bytes[8] = 0x80 | (bytes[8]! & 0x3f)

  return bytes
}

export function generateUuidV4(): string {
  return bytesToUuidString(generateUuidV4Bytes())
}

