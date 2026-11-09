/**
 * Core Algorithm types supported by IdGuru
 */
export type AlgorithmType =
  | "uuidv7"
  | "uuidv4"
  | "ulid"
  | "ksuid"
  | "snowflake"
  | "nanoid"
  | "prefixed"

export interface SnowflakeOptions {
  workerId?: number
  epoch?: number // Epoch in ms, defaults to Twitter epoch (1288834974657)
}

export interface NanoIdOptions {
  length?: number
  alphabet?: string
}

export interface GeneratorOptions {
  algorithm?: AlgorithmType
  prefix?: string
  delimiter?: string
  monotonic?: boolean
  snowflake?: SnowflakeOptions
  nanoid?: NanoIdOptions
}

export interface IdInspectionResult {
  raw: string
  canonical: string
  prefix?: string
  algorithm: AlgorithmType | "unknown"
  isValid: boolean
  timestamp?: Date
  epochMs?: number
  entropyBits?: number
  version?: number
  variant?: string
  workerId?: number
  sequence?: number
  details: Record<string, unknown>
}

