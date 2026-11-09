/**
 * Twitter Snowflake 64-bit ID generator
 * 1 sign bit (0)
 * 41 bits millisecond timestamp from epoch
 * 10 bits worker/machine ID (0-1023)
 * 12 bits sequence number (0-4095)
 */

const DEFAULT_EPOCH = 1288834974657n // Nov 04 2010 01:42:54 UTC

let lastTimestamp = -1n
let sequence = 0n

export function generateSnowflake(
  workerId = 1,
  epochMs: number = Number(DEFAULT_EPOCH)
): { string: string; bigint: bigint } {
  const epoch = BigInt(epochMs)
  const worker = BigInt(workerId & 0x3ff)
  let now = BigInt(Date.now())

  if (now < lastTimestamp) {
    // Clock moved backwards, handle clock drift
    now = lastTimestamp
  }

  if (now === lastTimestamp) {
    sequence = (sequence + 1n) & 0xfffn
    if (sequence === 0n) {
      // Sequence exhausted in current ms, spin until next ms
      while (BigInt(Date.now()) <= lastTimestamp) {
        // Spin
      }
      now = BigInt(Date.now())
    }
  } else {
    sequence = 0n
  }

  lastTimestamp = now

  const timestampPart = (now - epoch) << 22n
  const workerPart = worker << 12n
  const id = timestampPart | workerPart | sequence

  return {
    string: id.toString(),
    bigint: id,
  }
}

