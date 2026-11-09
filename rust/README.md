# idguru (Rust)

> Universal, zero-dependency identifier generation, fluent builder, and reverse-inspection engine for Rust.

[![crates.io](https://img.shields.io/crates/v/idguru.svg?style=flat-square)](https://crates.io/crates/idguru)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

## Installation

```bash
cargo add idguru
```

Or in `Cargo.toml`:
```toml
[dependencies]
idguru = "1.0.0"
```

## Quick Start

### 1. Instant Generation

```rust
use idguru::IdGuru;

fn main() {
    // RFC 9562 UUIDv7 (Monotonic, sortable)
    let id = IdGuru::uuidv7();
    println!("{}", id); // "019a6b42-8e12-7a22-9f1a-b83c7d612e44"

    // Other formats
    IdGuru::uuidv4();    // Random UUIDv4
    IdGuru::ulid();      // 26-char Crockford Base32
    IdGuru::ksuid();     // 27-char K-Sortable Base62
    IdGuru::snowflake(1);// 64-bit distributed snowflake
    IdGuru::nanoid(21);  // URL-safe compact ID
}
```

### 2. Fluent Builder Pattern (Thread-Safe)

```rust
use idguru::IdGuru;

fn main() {
    let user_gen = IdGuru::builder()
        .uuidv7()
        .prefix("usr")
        .delimiter("_")
        .monotonic(true)
        .build();

    let user_id = user_gen.generate();
    println!("{}", user_id); // "usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44"

    let raw_bytes: Vec<u8> = user_gen.generate_bytes();

    // Snowflake generator
    let order_gen = IdGuru::builder()
        .snowflake(42)
        .prefix("ord")
        .build();

    let order_id = order_gen.generate();
    let numeric_id = order_gen.generate_i64();
}
```

### 3. Universal Reverse-Inspection

```rust
use idguru::IdGuru;

fn main() {
    let meta = IdGuru::inspect("usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44");

    println!("Valid: {}", meta.is_valid);
    println!("Algorithm: {}", meta.algorithm);
    println!("Prefix: {:?}", meta.prefix);
    println!("Epoch Ms: {}", meta.epoch_ms);
}
```

## License

MIT © [Shubham Sharma](https://github.com/uchiha-fire)
