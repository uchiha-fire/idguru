import type { AlgorithmType, GeneratorOptions, SnowflakeOptions, NanoIdOptions } from "./types"
import { generateUuidV7, generateUuidV7Bytes } from "./algorithms/uuidv7"
import { generateUuidV4, generateUuidV4Bytes } from "./algorithms/uuidv4"
import { generateUlid } from "./algorithms/ulid"
import { generateKsuid } from "./algorithms/ksuid"
import { generateSnowflake } from "./algorithms/snowflake"
import { generateNanoId } from "./algorithms/nanoid"

export class IdGenerator {
  readonly algorithm: AlgorithmType
  readonly prefix?: string
  readonly delimiter: string
  readonly monotonic: boolean
  readonly snowflakeOpts?: SnowflakeOptions
  readonly nanoidOpts?: NanoIdOptions

  constructor(options: GeneratorOptions = {}) {
    this.algorithm = options.algorithm ?? "uuidv7"
    this.prefix = options.prefix
    this.delimiter = options.delimiter ?? "_"
    this.monotonic = options.monotonic ?? true
    this.snowflakeOpts = options.snowflake
    this.nanoidOpts = options.nanoid
  }

  generate(): string {
    let raw: string
    switch (this.algorithm) {
      case "uuidv7":
        raw = generateUuidV7(this.monotonic)
        break
      case "uuidv4":
        raw = generateUuidV4()
        break
      case "ulid":
        raw = generateUlid()
        break
      case "ksuid":
        raw = generateKsuid()
        break
      case "snowflake": {
        const workerId = this.snowflakeOpts?.workerId ?? 1
        const epoch = this.snowflakeOpts?.epoch
        raw = generateSnowflake(workerId, epoch).string
        break
      }
      case "nanoid": {
        const size = this.nanoidOpts?.length ?? 21
        const alphabet = this.nanoidOpts?.alphabet
        raw = generateNanoId(size, alphabet)
        break
      }
      case "prefixed":
      default:
        raw = generateUuidV7(this.monotonic)
        break
    }

    if (this.prefix) {
      return `${this.prefix}${this.delimiter}${raw}`
    }
    return raw
  }

  generateBytes(): Uint8Array {
    switch (this.algorithm) {
      case "uuidv7":
        return generateUuidV7Bytes(this.monotonic)
      case "uuidv4":
        return generateUuidV4Bytes()
      default: {
        const text = this.generate()
        return new TextEncoder().encode(text)
      }
    }
  }

  generateBigInt(): bigint {
    if (this.algorithm === "snowflake") {
      const workerId = this.snowflakeOpts?.workerId ?? 1
      const epoch = this.snowflakeOpts?.epoch
      return generateSnowflake(workerId, epoch).bigint
    }
    const hex = this.generate().replace(/[^0-9a-fA-F]/g, "")
    return BigInt("0x" + (hex || "0"))
  }
}

export class IdBuilder {
  private options: GeneratorOptions = {
    algorithm: "uuidv7",
    delimiter: "_",
    monotonic: true,
  }

  uuidv7(): this {
    this.options.algorithm = "uuidv7"
    return this
  }

  uuidv4(): this {
    this.options.algorithm = "uuidv4"
    return this
  }

  ulid(): this {
    this.options.algorithm = "ulid"
    return this
  }

  ksuid(): this {
    this.options.algorithm = "ksuid"
    return this
  }

  snowflake(opts?: SnowflakeOptions | number): this {
    this.options.algorithm = "snowflake"
    if (typeof opts === "number") {
      this.options.snowflake = { workerId: opts }
    } else if (opts) {
      this.options.snowflake = opts
    }
    return this
  }

  nanoid(opts?: NanoIdOptions | number): this {
    this.options.algorithm = "nanoid"
    if (typeof opts === "number") {
      this.options.nanoid = { length: opts }
    } else if (opts) {
      this.options.nanoid = opts
    }
    return this
  }

  prefixed(prefix: string): this {
    this.options.algorithm = "uuidv7"
    this.options.prefix = prefix
    return this
  }

  prefix(prefix: string): this {
    this.options.prefix = prefix
    return this
  }

  delimiter(delimiter: string): this {
    this.options.delimiter = delimiter
    return this
  }

  monotonic(enable = true): this {
    this.options.monotonic = enable
    return this
  }

  build(): IdGenerator {
    return new IdGenerator(this.options)
  }
}

