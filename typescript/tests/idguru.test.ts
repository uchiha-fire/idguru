import { describe, it, expect } from "vitest"
import { IdGuru, IdBuilder, IdGenerator, generateUuidV7, inspectId } from "../src"

describe("IdGuru TypeScript SDK", () => {
  describe("Instant Generation Facades", () => {
    it("generates a valid RFC 9562 UUIDv7", () => {
      const id = IdGuru.uuidv7()
      expect(id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
      expect(id[14]).toBe("7")
      expect(["8", "9", "a", "b"]).toContain(id[19])
    })

    it("generates strictly monotonic UUIDv7 in tight loops", () => {
      const ids: string[] = []
      for (let i = 0; i < 5000; i++) {
        ids.push(IdGuru.uuidv7(true))
      }

      // Check strictly ascending order
      for (let i = 1; i < ids.length; i++) {
        expect(ids[i]! > ids[i - 1]!).toBe(true)
      }
    })

    it("generates valid UUIDv4", () => {
      const id = IdGuru.uuidv4()
      expect(id).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/)
    })

    it("generates valid ULID (26 chars Crockford)", () => {
      const id = IdGuru.ulid()
      expect(id).toHaveLength(26)
      expect(id).toMatch(/^[0-9A-HJKMNP-TV-Z]{26}$/)
    })

    it("generates valid KSUID (27 chars Base62)", () => {
      const id = IdGuru.ksuid()
      expect(id).toHaveLength(27)
      expect(id).toMatch(/^[0-9A-Za-z]{27}$/)
    })

    it("generates valid Snowflake ID", () => {
      const id = IdGuru.snowflake(42)
      expect(id).toMatch(/^[0-9]{15,20}$/)
    })

    it("generates valid NanoID with custom lengths", () => {
      const idDefault = IdGuru.nanoid()
      expect(idDefault).toHaveLength(21)

      const idCustom = IdGuru.nanoid(32, "abcdef0123456789")
      expect(idCustom).toHaveLength(32)
      expect(idCustom).toMatch(/^[0-9a-f]{32}$/)
    })

    it("generates prefixed identifiers", () => {
      const id = IdGuru.prefixed("inv")
      expect(id.startsWith("inv_")).toBe(true)
      const rawUuid = id.substring(4)
      expect(rawUuid[14]).toBe("7")
    })
  })

  describe("Fluent Builder API", () => {
    it("builds a prefixed monotonic UUIDv7 generator", () => {
      const gen = IdGuru.builder()
        .uuidv7()
        .prefix("usr")
        .delimiter("_")
        .monotonic()
        .build()

      expect(gen.algorithm).toBe("uuidv7")
      expect(gen.prefix).toBe("usr")

      const id = gen.generate()
      expect(id.startsWith("usr_")).toBe(true)

      const raw = id.split("_")[1]!
      expect(raw[14]).toBe("7")
    })

    it("builds a Snowflake generator with numeric output", () => {
      const gen = IdGuru.builder()
        .snowflake({ workerId: 7 })
        .prefix("ord")
        .build()

      const id = gen.generate()
      expect(id.startsWith("ord_")).toBe(true)

      const big = gen.generateBigInt()
      expect(typeof big).toBe("bigint")
      expect(big > 0n).toBe(true)
    })

    it("builds a NanoID generator with options", () => {
      const gen = IdGuru.builder()
        .nanoid({ length: 16, alphabet: "ABCDEF123456" })
        .prefix("tok")
        .delimiter("-")
        .build()

      const id = gen.generate()
      expect(id.startsWith("tok-")).toBe(true)
      const token = id.split("-")[1]!
      expect(token).toHaveLength(16)
      expect(token).toMatch(/^[A-F1-6]{16}$/)
    })

    it("generates raw byte arrays", () => {
      const gen = IdGuru.builder().uuidv7().build()
      const bytes = gen.generateBytes()
      expect(bytes).toBeInstanceOf(Uint8Array)
      expect(bytes).toHaveLength(16)
    })
  })

  describe("Universal Reverse Inspection", () => {
    it("inspects UUIDv7 accurately", () => {
      const before = Date.now()
      const id = IdGuru.uuidv7()
      const after = Date.now()

      const meta = IdGuru.inspect(id)
      expect(meta.isValid).toBe(true)
      expect(meta.algorithm).toBe("uuidv7")
      expect(meta.version).toBe(7)
      expect(meta.epochMs).toBeGreaterThanOrEqual(before)
      expect(meta.epochMs).toBeLessThanOrEqual(after)
      expect(meta.timestamp).toBeInstanceOf(Date)
    })

    it("inspects Prefixed UUIDv7 correctly", () => {
      const id = "cust_019a6b42-8e12-7a22-9f1a-b83c7d612e44"
      const meta = IdGuru.inspect(id)
      expect(meta.isValid).toBe(true)
      expect(meta.prefix).toBe("cust")
      expect(meta.canonical).toBe("019a6b42-8e12-7a22-9f1a-b83c7d612e44")
      expect(meta.algorithm).toBe("uuidv7")
    })

    it("inspects ULID correctly", () => {
      const id = IdGuru.ulid()
      const meta = IdGuru.inspect(id)
      expect(meta.isValid).toBe(true)
      expect(meta.algorithm).toBe("ulid")
      expect(meta.epochMs).toBeGreaterThan(1700000000000)
    })

    it("inspects Snowflake correctly", () => {
      const id = IdGuru.snowflake(5)
      const meta = IdGuru.inspect(id)
      expect(meta.isValid).toBe(true)
      expect(meta.algorithm).toBe("snowflake")
      expect(meta.workerId).toBe(5)
    })

    it("handles invalid or garbage inputs gracefully", () => {
      const meta = IdGuru.inspect("not-an-id-!@#$%^&*")
      expect(meta.isValid).toBe(false)
      expect(meta.algorithm).toBe("unknown")
    })
  })
})

