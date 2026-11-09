import type { IdInspectionResult, AlgorithmType } from "./types"

const UUID_REGEX =
  /^[0-9a-f]{8}-[0-9a-f]{4}-([1-8])[0-9a-f]{3}-([89ab])[0-9a-f]{3}-[0-9a-f]{12}$/i
const ULID_REGEX = /^[0-9A-HJKMNP-TV-Z]{26}$/
const KSUID_REGEX = /^[0-9A-Za-z]{27}$/
const SNOWFLAKE_REGEX = /^[0-9]{15,20}$/
const CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
const DEFAULT_SNOWFLAKE_EPOCH = 1288834974657

export function inspectId(rawInput: string): IdInspectionResult {
  const trimmed = rawInput.trim()
  let prefix: string | undefined
  let canonical = trimmed

  // Only extract prefix if input has an underscore delimiter (e.g. usr_...)
  // or hyphen prefix where remainder itself matches a valid format
  const underscoreIdx = trimmed.indexOf("_")
  if (underscoreIdx > 0 && underscoreIdx <= 15) {
    const candidatePrefix = trimmed.substring(0, underscoreIdx)
    const remainder = trimmed.substring(underscoreIdx + 1)
    if (/^[a-zA-Z0-9]+$/.test(candidatePrefix) && remainder.length >= 10) {
      prefix = candidatePrefix
      canonical = remainder
    }
  } else if (!UUID_REGEX.test(trimmed)) {
    // Check if hyphen is a prefix delimiter (e.g. ord-019a6b42-...)
    const firstHyphen = trimmed.indexOf("-")
    if (firstHyphen > 0 && firstHyphen <= 15) {
      const candidatePrefix = trimmed.substring(0, firstHyphen)
      const remainder = trimmed.substring(firstHyphen + 1)
      if (UUID_REGEX.test(remainder) || ULID_REGEX.test(remainder)) {
        prefix = candidatePrefix
        canonical = remainder
      }
    }
  }

  // 1. UUID Check
  const uuidMatch = canonical.match(UUID_REGEX)
  if (uuidMatch) {
    const version = parseInt(uuidMatch[1]!, 16)
    const variantChar = uuidMatch[2]!.toLowerCase()
    const variant = ["8", "9", "a", "b"].includes(variantChar)
      ? "RFC 4122/9562 Variant 1"
      : "Reserved"

    if (version === 7) {
      const hex = canonical.replace(/-/g, "")
      const timeHex = hex.substring(0, 12)
      const epochMs = parseInt(timeHex, 16)
      const seqHex = hex.substring(13, 16)
      const sequence = parseInt(seqHex, 16)

      return {
        raw: trimmed,
        canonical,
        prefix,
        algorithm: "uuidv7",
        isValid: true,
        version: 7,
        variant,
        timestamp: new Date(epochMs),
        epochMs,
        sequence,
        entropyBits: 74,
        details: {
          rfcStandard: "RFC 9562",
          subMillisecondSequence: sequence,
          randomBits: 62,
        },
      }
    }

    if (version === 4) {
      return {
        raw: trimmed,
        canonical,
        prefix,
        algorithm: "uuidv4",
        isValid: true,
        version: 4,
        variant,
        entropyBits: 122,
        details: {
          rfcStandard: "RFC 9562 / RFC 4122",
          randomBits: 122,
        },
      }
    }
  }

  // 2. ULID Check
  if (canonical.length === 26 && ULID_REGEX.test(canonical)) {
    const timePart = canonical.substring(0, 10)
    let epochMs = 0
    for (let i = 0; i < 10; i++) {
      epochMs = epochMs * 32 + CROCKFORD.indexOf(timePart[i]!)
    }

    return {
      raw: trimmed,
      canonical,
      prefix,
      algorithm: "ulid",
      isValid: true,
      timestamp: new Date(epochMs),
      epochMs,
      entropyBits: 80,
      details: {
        encoding: "Crockford Base32",
        randomBits: 80,
      },
    }
  }

  // 3. KSUID Check
  if (canonical.length === 27 && KSUID_REGEX.test(canonical)) {
    return {
      raw: trimmed,
      canonical,
      prefix,
      algorithm: "ksuid",
      isValid: true,
      entropyBits: 128,
      details: {
        encoding: "Base62",
        randomBits: 128,
      },
    }
  }

  // 4. Snowflake Check
  if (SNOWFLAKE_REGEX.test(canonical)) {
    try {
      const num = BigInt(canonical)
      const timestampPart = Number(num >> 22n)
      const workerId = Number((num >> 12n) & 0x3ffn)
      const sequence = Number(num & 0xfffn)
      const epochMs = timestampPart + DEFAULT_SNOWFLAKE_EPOCH

      return {
        raw: trimmed,
        canonical,
        prefix,
        algorithm: "snowflake",
        isValid: true,
        timestamp: new Date(epochMs),
        epochMs,
        workerId,
        sequence,
        entropyBits: 22,
        details: {
          baseEpoch: DEFAULT_SNOWFLAKE_EPOCH,
          workerId,
          sequence,
        },
      }
    } catch {
      // Ignore BigInt parse failures
    }
  }

  // 5. NanoID Fallback
  if (/^[A-Za-z0-9_-]{10,64}$/.test(canonical)) {
    return {
      raw: trimmed,
      canonical,
      prefix,
      algorithm: "nanoid",
      isValid: true,
      entropyBits: Math.round(canonical.length * 5.95),
      details: {
        length: canonical.length,
      },
    }
  }

  return {
    raw: trimmed,
    canonical,
    prefix,
    algorithm: "unknown",
    isValid: false,
    details: {},
  }
}

