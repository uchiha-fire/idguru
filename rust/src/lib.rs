//! # idguru
//!
//! Universal, high-performance identifier generation, fluent builder,
//! and reverse-inspection engine for Rust.

use std::sync::atomic::{AtomicI64, AtomicU16, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

const CROCKFORD: &[u8; 32] = b"0123456789ABCDEFGHJKMNPQRSTVWXYZ";
const BASE62: &[u8; 62] = b"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
const DEFAULT_NANOID_ALPHABET: &[u8; 64] =
    b"useandom-26T198340PX75pxJACKVERYMINDBUSHWOLFGQZ_bfghjklqvwyz---";
const DEFAULT_SNOWFLAKE_EPOCH: i64 = 1288834974657;

static LAST_MS: AtomicI64 = AtomicI64::new(0);
static UUID_SEQ: AtomicU16 = AtomicU16::new(0);

static SNOWFLAKE_LAST_TS: AtomicI64 = AtomicI64::new(-1);
static SNOWFLAKE_SEQ: AtomicU16 = AtomicU16::new(0);

fn get_now_ms() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as i64
}

fn fill_random(bytes: &mut [u8]) {
    getrandom::getrandom(bytes).expect("Failed to acquire cryptographically secure random bytes");
}

/// Generate 16 raw bytes representing an RFC 9562 UUIDv7.
pub fn generate_uuidv7_bytes(monotonic: bool) -> [u8; 16] {
    let mut bytes = [0u8; 16];
    fill_random(&mut bytes);

    let now = get_now_ms();
    let (timestamp, seq) = if monotonic {
        let prev_ms = LAST_MS.load(Ordering::Acquire);
        if now > prev_ms {
            LAST_MS.store(now, Ordering::Release);
            let init_seq = (((bytes[6] & 0x07) as u16) << 8 | (bytes[7] as u16)) & 0x0FFF;
            UUID_SEQ.store(init_seq, Ordering::Release);
            (now, init_seq)
        } else {
            let next_seq = (UUID_SEQ.fetch_add(1, Ordering::AcqRel) + 1) & 0x0FFF;
            if next_seq == 0 {
                LAST_MS.fetch_add(1, Ordering::AcqRel);
            }
            (LAST_MS.load(Ordering::Acquire), next_seq)
        }
    } else {
        let raw_seq = (((bytes[6] & 0x0F) as u16) << 8 | (bytes[7] as u16)) & 0x0FFF;
        (now, raw_seq)
    };

    // 48-bit timestamp
    bytes[0] = (timestamp >> 40) as u8;
    bytes[1] = (timestamp >> 32) as u8;
    bytes[2] = (timestamp >> 24) as u8;
    bytes[3] = (timestamp >> 16) as u8;
    bytes[4] = (timestamp >> 8) as u8;
    bytes[5] = timestamp as u8;

    // Byte 6: 4-bit version (7) | 4-bit sequence high
    bytes[6] = 0x70 | ((seq >> 8) & 0x0F) as u8;
    // Byte 7: 8-bit sequence low
    bytes[7] = (seq & 0xFF) as u8;
    // Byte 8: 2-bit variant (10xx xxxx)
    bytes[8] = 0x80 | (bytes[8] & 0x3F);

    bytes
}

/// Format 16 bytes into canonical 8-4-4-4-12 hex UUID string.
pub fn bytes_to_uuid_string(b: &[u8; 16]) -> String {
    format!(
        "{:02x}{:02x}{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}",
        b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8], b[9], b[10], b[11], b[12], b[13], b[14], b[15]
    )
}

/// Instant RFC 9562 UUIDv7 generator.
pub fn generate_uuidv7() -> String {
    bytes_to_uuid_string(&generate_uuidv7_bytes(true))
}

/// Instant UUIDv4 generator.
pub fn generate_uuidv4() -> String {
    let mut bytes = [0u8; 16];
    fill_random(&mut bytes);
    bytes[6] = 0x40 | (bytes[6] & 0x0F);
    bytes[8] = 0x80 | (bytes[8] & 0x3F);
    bytes_to_uuid_string(&bytes)
}

/// Instant ULID generator (26 chars Crockford Base32).
pub fn generate_ulid() -> String {
    let now = get_now_ms() as u64;
    let mut chars = [0u8; 26];

    let mut t = now;
    for i in (0..10).rev() {
        chars[i] = CROCKFORD[(t % 32) as usize];
        t /= 32;
    }

    let mut rand_bytes = [0u8; 10];
    fill_random(&mut rand_bytes);

    for i in 0..16 {
        let byte_idx = (i * 5) / 8;
        let bit_offset = (i * 5) % 8;
        let mut val = (rand_bytes[byte_idx] as u16) << 8;
        if byte_idx + 1 < 10 {
            val |= rand_bytes[byte_idx + 1] as u16;
        }
        val = (val >> (11 - bit_offset)) & 0x1F;
        chars[10 + i] = CROCKFORD[val as usize];
    }

    String::from_utf8(chars.to_vec()).unwrap_or_default()
}

/// Instant KSUID generator (27 chars Base62).
pub fn generate_ksuid() -> String {
    let ksuid_epoch = 1400000000i64;
    let now_sec = (SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_secs() as i64 - ksuid_epoch) as u32;

    let mut full = [0u8; 20];
    full[0..4].copy_from_slice(&now_sec.to_be_bytes());
    fill_random(&mut full[4..20]);

    let mut num = u128::from_be_bytes(full[4..20].try_into().unwrap());
    let high = u32::from_be_bytes(full[0..4].try_into().unwrap()) as u128;
    num |= high << 128;

    let mut res = vec!['0' as u8; 27];
    let mut idx = 26;
    while num > 0 && idx < 27 {
        res[idx] = BASE62[(num % 62) as usize];
        num /= 62;
        if idx == 0 { break; }
        idx -= 1;
    }

    String::from_utf8(res).unwrap_or_default()
}

/// Instant Snowflake generator.
pub fn generate_snowflake(worker_id: i64) -> (String, i64) {
    let mut now = get_now_ms();
    let last = SNOWFLAKE_LAST_TS.load(Ordering::Acquire);
    if now < last {
        now = last;
    }

    let seq = if now == last {
        let next = (SNOWFLAKE_SEQ.fetch_add(1, Ordering::AcqRel) + 1) & 0x0FFF;
        if next == 0 {
            while get_now_ms() <= last {}
            now = get_now_ms();
        }
        next
    } else {
        SNOWFLAKE_SEQ.store(0, Ordering::Release);
        0
    };

    SNOWFLAKE_LAST_TS.store(now, Ordering::Release);
    let worker_part = (worker_id & 0x3FF) << 12;
    let time_part = (now - DEFAULT_SNOWFLAKE_EPOCH) << 22;
    let id = time_part | worker_part | (seq as i64);

    (id.to_string(), id)
}

/// Instant NanoID generator.
pub fn generate_nanoid(length: usize) -> String {
    let len = if length == 0 { 21 } else { length };
    let mut bytes = vec![0u8; len];
    fill_random(&mut bytes);

    bytes
        .into_iter()
        .map(|b| DEFAULT_NANOID_ALPHABET[(b & 63) as usize] as char)
        .collect()
}

// =============================================================================
// Fluent Builder
// =============================================================================

#[derive(Clone, Debug)]
pub struct IdGenerator {
    algorithm: String,
    prefix: Option<String>,
    delimiter: String,
    monotonic: bool,
    snowflake_worker_id: i64,
    nanoid_length: usize,
}

impl IdGenerator {
    pub fn generate(&self) -> String {
        let raw = match self.algorithm.as_str() {
            "uuidv4" => generate_uuidv4(),
            "ulid" => generate_ulid(),
            "ksuid" => generate_ksuid(),
            "snowflake" => generate_snowflake(self.snowflake_worker_id).0,
            "nanoid" => generate_nanoid(self.nanoid_length),
            _ => bytes_to_uuid_string(&generate_uuidv7_bytes(self.monotonic)),
        };

        if let Some(ref p) = self.prefix {
            format!("{}{}{}", p, self.delimiter, raw)
        } else {
            raw
        }
    }

    pub fn generate_bytes(&self) -> Vec<u8> {
        match self.algorithm.as_str() {
            "uuidv7" => generate_uuidv7_bytes(self.monotonic).to_vec(),
            _ => self.generate().into_bytes(),
        }
    }

    pub fn generate_i64(&self) -> i64 {
        if self.algorithm == "snowflake" {
            generate_snowflake(self.snowflake_worker_id).1
        } else {
            0
        }
    }
}

pub struct IdBuilder {
    algorithm: String,
    prefix: Option<String>,
    delimiter: String,
    monotonic: bool,
    snowflake_worker_id: i64,
    nanoid_length: usize,
}

impl IdBuilder {
    pub fn new() -> Self {
        Self {
            algorithm: "uuidv7".to_string(),
            prefix: None,
            delimiter: "_".to_string(),
            monotonic: true,
            snowflake_worker_id: 1,
            nanoid_length: 21,
        }
    }

    pub fn uuidv7(mut self) -> Self {
        self.algorithm = "uuidv7".to_string();
        self
    }

    pub fn uuidv4(mut self) -> Self {
        self.algorithm = "uuidv4".to_string();
        self
    }

    pub fn ulid(mut self) -> Self {
        self.algorithm = "ulid".to_string();
        self
    }

    pub fn ksuid(mut self) -> Self {
        self.algorithm = "ksuid".to_string();
        self
    }

    pub fn snowflake(mut self, worker_id: i64) -> Self {
        self.algorithm = "snowflake".to_string();
        self.snowflake_worker_id = worker_id;
        self
    }

    pub fn nanoid(mut self, length: usize) -> Self {
        self.algorithm = "nanoid".to_string();
        self.nanoid_length = length;
        self
    }

    pub fn prefixed(mut self, prefix: &str) -> Self {
        self.algorithm = "uuidv7".to_string();
        self.prefix = Some(prefix.to_string());
        self
    }

    pub fn prefix(mut self, prefix: &str) -> Self {
        self.prefix = Some(prefix.to_string());
        self
    }

    pub fn delimiter(mut self, delimiter: &str) -> Self {
        self.delimiter = delimiter.to_string();
        self
    }

    pub fn monotonic(mut self, enable: bool) -> Self {
        self.monotonic = enable;
        self
    }

    pub fn build(self) -> IdGenerator {
        IdGenerator {
            algorithm: self.algorithm,
            prefix: self.prefix,
            delimiter: self.delimiter,
            monotonic: self.monotonic,
            snowflake_worker_id: self.snowflake_worker_id,
            nanoid_length: self.nanoid_length,
        }
    }
}

// =============================================================================
// Reverse Inspection
// =============================================================================

#[derive(Clone, Debug, Default)]
pub struct IdMetadata {
    pub raw: String,
    pub canonical: String,
    pub prefix: Option<String>,
    pub algorithm: String,
    pub is_valid: bool,
    pub version: u8,
    pub epoch_ms: i64,
    pub worker_id: i64,
    pub sequence: u16,
    pub entropy_bits: u16,
}

pub fn inspect(raw_input: &str) -> IdMetadata {
    let trimmed = raw_input.trim();
    let mut canonical = trimmed;
    let mut prefix = None;

    if let Some(idx) = trimmed.find('_') {
        if idx > 0 && idx <= 15 && trimmed.len() > idx + 10 {
            prefix = Some(trimmed[..idx].to_string());
            canonical = &trimmed[idx + 1..];
        }
    }

    // UUIDv7 check
    if canonical.len() == 36 && canonical.chars().filter(|&c| c == '-').count() == 4 {
        let parts: Vec<&str> = canonical.split('-').collect();
        if parts.len() == 5
            && parts[0].len() == 8
            && parts[1].len() == 4
            && parts[2].len() == 4
            && parts[3].len() == 4
            && parts[4].len() == 12
        {
            if parts[2].starts_with('7') {
                let hex_clean = canonical.replace('-', "");
                if let Ok(epoch_ms) = i64::from_str_radix(&hex_clean[0..12], 16) {
                    let seq = u16::from_str_radix(&hex_clean[13..16], 16).unwrap_or(0);
                    return IdMetadata {
                        raw: trimmed.to_string(),
                        canonical: canonical.to_string(),
                        prefix,
                        algorithm: "uuidv7".to_string(),
                        is_valid: true,
                        version: 7,
                        epoch_ms,
                        sequence: seq,
                        entropy_bits: 74,
                        ..Default::default()
                    };
                }
            } else if parts[2].starts_with('4') {
                return IdMetadata {
                    raw: trimmed.to_string(),
                    canonical: canonical.to_string(),
                    prefix,
                    algorithm: "uuidv4".to_string(),
                    is_valid: true,
                    version: 4,
                    entropy_bits: 122,
                    ..Default::default()
                };
            }
        }
    }

    // ULID check
    if canonical.len() == 26 && canonical.chars().all(|c| c.is_ascii_alphanumeric()) {
        let mut epoch_ms = 0i64;
        for b in canonical[0..10].bytes() {
            if let Some(pos) = CROCKFORD.iter().position(|&x| x == b) {
                epoch_ms = epoch_ms * 32 + (pos as i64);
            }
        }
        return IdMetadata {
            raw: trimmed.to_string(),
            canonical: canonical.to_string(),
            prefix,
            algorithm: "ulid".to_string(),
            is_valid: true,
            epoch_ms,
            entropy_bits: 80,
            ..Default::default()
        };
    }

    // Snowflake check
    if canonical.len() >= 15 && canonical.len() <= 20 && canonical.chars().all(|c| c.is_ascii_digit()) {
        if let Ok(num) = canonical.parse::<i64>() {
            let timestamp_part = num >> 22;
            let worker_id = (num >> 12) & 0x3FF;
            let seq = (num & 0x0FFF) as u16;
            let epoch_ms = timestamp_part + DEFAULT_SNOWFLAKE_EPOCH;

            return IdMetadata {
                raw: trimmed.to_string(),
                canonical: canonical.to_string(),
                prefix,
                algorithm: "snowflake".to_string(),
                is_valid: true,
                epoch_ms,
                worker_id,
                sequence: seq,
                entropy_bits: 22,
                ..Default::default()
            };
        }
    }

    IdMetadata {
        raw: trimmed.to_string(),
        canonical: canonical.to_string(),
        prefix,
        algorithm: "unknown".to_string(),
        is_valid: false,
        ..Default::default()
    }
}

// =============================================================================
// IdGuru Facade
// =============================================================================

pub struct IdGuru;

impl IdGuru {
    pub fn builder() -> IdBuilder {
        IdBuilder::new()
    }

    pub fn uuidv7() -> String {
        generate_uuidv7()
    }

    pub fn uuidv4() -> String {
        generate_uuidv4()
    }

    pub fn ulid() -> String {
        generate_ulid()
    }

    pub fn ksuid() -> String {
        generate_ksuid()
    }

    pub fn snowflake(worker_id: i64) -> String {
        generate_snowflake(worker_id).0
    }

    pub fn nanoid(length: usize) -> String {
        generate_nanoid(length)
    }

    pub fn prefixed(prefix: &str) -> String {
        format!("{}_{}", prefix, generate_uuidv7())
    }

    pub fn inspect(id: &str) -> IdMetadata {
        inspect(id)
    }
}
