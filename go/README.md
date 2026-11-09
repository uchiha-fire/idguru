# idguru (Go)

> Universal, zero-dependency identifier generation, fluent builder, and reverse-inspection engine for Go.

[![Go Reference](https://pkg.go.dev/badge/github.com/uchiha-fire/idguru.svg)](https://pkg.go.dev/github.com/uchiha-fire/idguru)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

## Installation

```bash
go get github.com/uchiha-fire/idguru
```

## Quick Start

### 1. Instant Generation

```go
package main

import (
	"fmt"
	"github.com/uchiha-fire/idguru"
)

func main() {
	// RFC 9562 UUIDv7 (Timestamp-sortable, monotonic)
	id := idguru.UUIDv7()
	fmt.Println(id) // "019a6b42-8e12-7a22-9f1a-b83c7d612e44"

	// Other algorithms
	idguru.UUIDv4()    // Random UUIDv4
	idguru.ULID()      // 26-char Crockford Base32
	idguru.KSUID()     // 27-char K-Sortable Base62
	idguru.Snowflake(1)// 64-bit distributed snowflake
	idguru.NanoID(21)  // URL-safe compact ID
}
```

### 2. Fluent Builder Pattern

```go
package main

import (
	"fmt"
	"github.com/uchiha-fire/idguru"
)

func main() {
	// Prefixed monotonic UUIDv7
	userGen := idguru.New().
		UUIDv7().
		Prefix("usr").
		Delimiter("_").
		Monotonic(true).
		Build()

	userId := userGen.Generate()
	fmt.Println(userId) // "usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44"

	// Snowflake with worker ID
	orderGen := idguru.New().
		Snowflake(42).
		Prefix("ord").
		Build()

	orderId := orderGen.Generate()
	numericId := orderGen.GenerateInt64() // 64-bit int
	fmt.Printf("Order: %s, Numeric: %d\n", orderId, numericId)
}
```

### 3. Universal Reverse-Inspection

```go
package main

import (
	"fmt"
	"github.com/uchiha-fire/idguru"
)

func main() {
	meta := idguru.Inspect("usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44")

	fmt.Println("Valid:", meta.IsValid)       // true
	fmt.Println("Algorithm:", meta.Algorithm) // "uuidv7"
	fmt.Println("Prefix:", meta.Prefix)       // "usr"
	fmt.Println("Timestamp:", meta.Timestamp) // 2026-11-09 00:00:00 +0000 UTC
	fmt.Println("EpochMs:", meta.EpochMs)     // 1794192000000
}
```

## License

MIT © [Shubham Sharma](https://github.com/uchiha-fire)
