import { IdBuilder, IdGenerator } from "./builder"
import { generateUuidV7, generateUuidV7Bytes } from "./algorithms/uuidv7"
import { generateUuidV4, generateUuidV4Bytes } from "./algorithms/uuidv4"
import { generateUlid } from "./algorithms/ulid"
import { generateKsuid } from "./algorithms/ksuid"
import { generateSnowflake } from "./algorithms/snowflake"
import { generateNanoId } from "./algorithms/nanoid"
import { inspectId } from "./inspector"

export * from "./types"
export { IdBuilder, IdGenerator } from "./builder"
export { generateUuidV7, generateUuidV7Bytes } from "./algorithms/uuidv7"
export { generateUuidV4, generateUuidV4Bytes } from "./algorithms/uuidv4"
export { generateUlid } from "./algorithms/ulid"
export { generateKsuid } from "./algorithms/ksuid"
export { generateSnowflake } from "./algorithms/snowflake"
export { generateNanoId } from "./algorithms/nanoid"
export { inspectId } from "./inspector"

/**
 * Universal IdGuru Facade
 */
export const IdGuru = {
  /**
   * Start a fluent, type-safe builder
   */
  builder(): IdBuilder {
    return new IdBuilder()
  },

  /**
   * Instant RFC 9562 UUIDv7 generator
   */
  uuidv7(monotonic = true): string {
    return generateUuidV7(monotonic)
  },

  /**
   * Instant UUIDv4 generator
   */
  uuidv4(): string {
    return generateUuidV4()
  },

  /**
   * Instant ULID generator
   */
  ulid(): string {
    return generateUlid()
  },

  /**
   * Instant KSUID generator
   */
  ksuid(): string {
    return generateKsuid()
  },

  /**
   * Instant Snowflake ID generator
   */
  snowflake(workerId = 1): string {
    return generateSnowflake(workerId).string
  },

  /**
   * Instant NanoID generator
   */
  nanoid(size = 21, alphabet?: string): string {
    return generateNanoId(size, alphabet)
  },

  /**
   * Instant Prefixed ID generator
   */
  prefixed(prefix: string, delimiter = "_"): string {
    return `${prefix}${delimiter}${generateUuidV7(true)}`
  },

  /**
   * Universal ID Inspector & Reverse Decoder
   */
  inspect(id: string) {
    return inspectId(id)
  },
}

export default IdGuru

