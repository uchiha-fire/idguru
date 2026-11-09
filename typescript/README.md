# idguru (TypeScript & JavaScript)

> Universal, zero-dependency identifier generation, fluent builder, and reverse-inspection engine for Node.js, Bun, Deno, and the Browser.

[![npm version](https://img.shields.io/npm/v/idguru.svg?style=flat-square)](https://www.npmjs.com/package/idguru)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

## Installation

```bash
npm install idguru
# or
yarn add idguru
# or
pnpm add idguru
# or
bun add idguru
```

## Quick Start

### 1. Instant Generation

```typescript
import { IdGuru } from "idguru";

// RFC 9562 UUIDv7 (Timestamp-sortable, monotonic)
const id = IdGuru.uuidv7(); 
// "019a6b42-8e12-7a22-9f1a-b83c7d612e44"

// Other algorithms
IdGuru.uuidv4();   // Cryptographic random UUIDv4
IdGuru.ulid();     // 26-char Crockford Base32
IdGuru.ksuid();    // 27-char K-Sortable Base62
IdGuru.snowflake();// 64-bit distributed snowflake
IdGuru.nanoid();   // URL-safe compact ID
```

### 2. Fluent Builder Pattern (Zero Clunky Enums)

```typescript
import { IdGuru } from "idguru";

// Custom prefixed UUIDv7
const userIds = IdGuru.builder()
  .uuidv7()
  .prefix("usr")
  .delimiter("_")
  .monotonic()
  .build();

const userId = userIds.generate(); 
// "usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44"

// Custom NanoID
const tokenGen = IdGuru.builder()
  .nanoid({ length: 32, alphabet: "0123456789abcdef" })
  .prefix("tok")
  .build();

// Snowflake with Worker ID
const orderGen = IdGuru.builder()
  .snowflake({ workerId: 42 })
  .prefix("ord")
  .build();

const numericId = orderGen.generateBigInt();
```

### 3. Universal Reverse-Inspection

Decode metadata, timestamp, epoch millisecond, version, and entropy from any identifier:

```typescript
import { IdGuru } from "idguru";

const meta = IdGuru.inspect("usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44");

console.log(meta.isValid);      // true
console.log(meta.algorithm);    // "uuidv7"
console.log(meta.prefix);       // "usr"
console.log(meta.timestamp);    // 2026-11-09T00:00:00.000Z
console.log(meta.epochMs);      // 1794192000000
console.log(meta.entropyBits);  // 74
```

## License

MIT © [Shubham Sharma](https://github.com/uchiha-fire)
