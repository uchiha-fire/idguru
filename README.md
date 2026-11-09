# 🆔 `idguru`

<div align="center">

**Universal, Zero-Dependency Unique Identifier Generation & Reverse-Inspection Suite.**

[![NPM](https://img.shields.io/npm/v/idguru.svg?style=for-the-badge&logo=npm&color=CB3837)](https://www.npmjs.com/package/idguru)
[![PyPI](https://img.shields.io/pypi/v/idguru.svg?style=for-the-badge&logo=pypi&color=3775A9)](https://pypi.org/project/idguru/)
[![Maven Central](https://img.shields.io/badge/Maven_Central-io.github.uchiha--fire%3Aidguru-C71A36?style=for-the-badge&logo=apache-maven)](https://central.sonatype.com/artifact/io.github.uchiha-fire/idguru)
[![Go Reference](https://img.shields.io/badge/Go_Pkg-github.com%2Fuchiha--fire%2Fidguru-00ADD8?style=for-the-badge&logo=go)](https://pkg.go.dev/github.com/uchiha-fire/idguru)
[![crates.io](https://img.shields.io/crates/v/idguru.svg?style=for-the-badge&logo=rust&color=DEA584)](https://crates.io/crates/idguru)

[![License: MIT](https://img.shields.io/badge/License-MIT-emerald?style=for-the-badge)](LICENSE)
[![Companion Web App](https://img.shields.io/badge/Live_Tool-uuidv7.guru-orange?style=for-the-badge)](https://uuidv7.guru)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=for-the-badge&logo=linkedin)](https://www.linkedin.com/in/shubham-sharma-28487819b/)

</div>

---

## 🌟 Why `idguru`?

Modern systems need identifiers for primary keys, distributed events, API keys, and voucher codes. Traditionally, developers have had to pull in multiple different libraries (`uuid`, `ulid`, `snowflake`, `nanoid`) with inconsistent APIs, heavy transitive dependencies, and no way to inspect or decode them.

**`idguru` solves this by delivering a single, zero-dependency, idiomatic library with identical ergonomics across all major languages:**

1. **Zero Runtime Dependencies**: Every language implementation relies 100% on native platform cryptographic APIs (`Web Crypto` in JS/TS, `secrets` in Python, `java.security.SecureRandom` in Java, `crypto/rand` in Go, `getrandom` in Rust).
2. **Pure Fluent Builder**: Configure prefixes, delimiters, sub-millisecond monotonicity, custom alphabets, and node IDs without clunky enums or strings.
3. **Universal Bidirectional Engine**: Not just ID generation, but **reverse-inspection** — decode timestamps, version, epoch, entropy, and bit allocations from any ID.
4. **Java 8+ Compatibility**: The Java library is compiled with `--release 8` bytecode, running seamlessly on Java 8, 11, 17, 21+, and Android.
5. **Interactive Companion**: Test, inspect, and benchmark all identifiers visually at [uuidv7.guru](https://uuidv7.guru).

---

## ⚡ Quick Installation

| Language | Registry | Command |
| :--- | :--- | :--- |
| **TypeScript / Node** | NPM | `npm install idguru` |
| **Python (3.8+)** | PyPI | `pip install idguru` |
| **Go** | Go Modules | `go get github.com/uchiha-fire/idguru` |
| **Java (8+) / Maven** | Maven Central | `<dependency><groupId>io.github.uchiha-fire</groupId><artifactId>idguru</artifactId><version>1.0.0</version></dependency>` |
| **Kotlin / Gradle** | Maven Central | `implementation("io.github.uchiha-fire:idguru:1.0.0")` |
| **Rust** | crates.io | `cargo add idguru` |

---

## 🚀 Fluent Code Examples

### 1. TypeScript & JavaScript

```typescript
import { IdGuru } from "idguru";

// Instant RFC 9562 UUIDv7
const id = IdGuru.uuidv7(); 

// Fluent Builder
const userGen = IdGuru.builder()
  .uuidv7()
  .prefix("usr")
  .delimiter("_")
  .monotonic()
  .build();

const userId = userGen.generate(); // "usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44"

// Reverse-Inspection
const meta = IdGuru.inspect(userId);
console.log(meta.timestamp, meta.algorithm, meta.entropyBits);
```

### 2. Python (3.8 - 3.13+)

```python
from idguru import IdGuru

# Instant generation
user_id = IdGuru.uuidv7()

# Fluent Builder
generator = (
    IdGuru.builder()
    .uuidv7()
    .prefix("inv")
    .monotonic()
    .build()
)

invoice_id = generator.generate() # "inv_019a6b42-8e12-..."

# Reverse-Inspection
meta = IdGuru.inspect(invoice_id)
print(meta.timestamp, meta.algorithm)
```

### 3. Go

```go
package main

import (
    "fmt"
    "github.com/uchiha-fire/idguru"
)

func main() {
    // Fluent Builder
    userGen := idguru.New().
        UUIDv7().
        Prefix("usr").
        Monotonic(true).
        Build()

    userId := userGen.Generate()
    fmt.Println(userId)

    // Reverse-Inspection
    meta := idguru.Inspect(userId)
    fmt.Printf("Timestamp: %v, Algorithm: %s\n", meta.Timestamp, meta.Algorithm)
}
```

### 4. Java 8, 11, 17, 21+ & Android

```java
import guru.id.IdGuru;
import guru.id.IdGenerator;
import guru.id.IdMetadata;

public class Main {
    private static final IdGenerator USERS = IdGuru.builder()
            .uuidv7()
            .prefix("usr")
            .monotonic(true)
            .build();

    public static void main(String[] args) {
        String userId = USERS.generate();
        System.out.println(userId);

        IdMetadata meta = IdGuru.inspect(userId);
        System.out.println("Created at: " + meta.getInstant());
    }
}
```

### 5. Rust

```rust
use idguru::IdGuru;

fn main() {
    let user_gen = IdGuru::builder()
        .uuidv7()
        .prefix("usr")
        .monotonic(true)
        .build();

    let user_id = user_gen.generate();
    println!("{}", user_id);

    let meta = IdGuru::inspect(&user_id);
    println!("Timestamp ms: {}", meta.epoch_ms);
}
```

---

## 📊 Identifier Strategy Matrix

| Algorithm | Bit Length | Output Length | Sortable? | Standard / Spec | Best Use Case |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **UUIDv7** | 128-bit | 36 chars | ✅ Millisecond | RFC 9562 | Relational DB Primary Keys (PostgreSQL, MySQL, SQLite) |
| **UUIDv4** | 128-bit | 36 chars | ❌ Random | RFC 4122 / 9562 | High-entropy ephemeral tokens & non-ordered IDs |
| **ULID** | 128-bit | 26 chars | ✅ Millisecond | Crockford Base32 | URL-friendly, compact sortable IDs |
| **KSUID** | 160-bit | 27 chars | ✅ Second | Segment Base62 | Payload tracking & payload-heavy distributed tracing |
| **Snowflake** | 64-bit | 19 digits | ✅ Millisecond | Twitter Spec | High-scale distributed integer primary keys |
| **NanoID** | Variable | 21 chars | ❌ Random | URL-Safe Alphabet | Short URLs, public tokens & human-friendly codes |
| **Prefixed** | Variable | Custom | Inherited | Stripe Convention | Type-safe entity identifiers (`usr_...`, `inv_...`) |

---

## 🧪 Testing

Every language implementation in `idguru` is extensively tested for specification compliance, variant/version bit fields, sub-millisecond monotonicity, and reverse inspection:

```bash
# TypeScript
cd typescript && npm test

# Python
cd python && python3 -m unittest discover tests

# Go
cd go && go test -v ./...

# Java
cd java && javac --release 8 -d bin src/main/java/guru/id/*.java src/test/java/guru/id/*.java && java -cp bin guru.id.TestRunner
```

---

## 👤 Author

**Shubham Sharma**
- **GitHub**: [@uchiha-fire](https://github.com/uchiha-fire)
- **LinkedIn**: [shubham-sharma-28487819b](https://www.linkedin.com/in/shubham-sharma-28487819b/)
- **Web**: [uuidv7.guru](https://uuidv7.guru)

---

## 📄 License

MIT © [Shubham Sharma](https://github.com/uchiha-fire)

