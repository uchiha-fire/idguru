# idguru (Java 8+)

> Universal, zero-dependency identifier generation, fluent builder, and reverse-inspection engine for Java 8, 11, 17, 21+, and Android.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.uchiha-fire/idguru.svg?style=flat-square)](https://central.sonatype.com/artifact/io.github.uchiha-fire/idguru)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

## Installation

### Gradle (Kotlin)
```kotlin
implementation("io.github.uchiha-fire:idguru:1.0.0")
```

### Gradle (Groovy)
```groovy
implementation 'io.github.uchiha-fire:idguru:1.0.0'
```

### Maven
```xml
<dependency>
    <groupId>io.github.uchiha-fire</groupId>
    <artifactId>idguru</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Instant Generation

```java
import guru.id.IdGuru;

public class App {
    public static void main(String[] args) {
        // RFC 9562 UUIDv7 (Timestamp-sortable, monotonic)
        String id = IdGuru.uuidv7(); 
        // "019a6b42-8e12-7a22-9f1a-b83c7d612e44"

        // Other algorithms
        IdGuru.uuidv4();    // Random UUIDv4
        IdGuru.ulid();      // 26-char Crockford Base32
        IdGuru.ksuid();     // 27-char K-Sortable Base62
        IdGuru.snowflake(1);// 64-bit distributed snowflake
        IdGuru.nanoid(21);  // URL-safe compact ID
    }
}
```

### 2. Fluent Builder Pattern (Thread-Safe)

```java
import guru.id.IdGuru;
import guru.id.IdGenerator;

public class App {
    // Reusable, thread-safe generator singleton
    private static final IdGenerator USER_IDS = IdGuru.builder()
            .uuidv7()
            .prefix("usr")
            .delimiter("_")
            .monotonic(true)
            .build();

    public static void main(String[] args) {
        String userId = USER_IDS.generate();
        // "usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44"

        // Snowflake with worker ID
        IdGenerator orderGen = IdGuru.builder()
                .snowflake(42)
                .prefix("ord")
                .build();

        String orderId = orderGen.generate();
        long numericId = orderGen.generateLong(); // 64-bit long
    }
}
```

### 3. Universal Reverse-Inspection

```java
import guru.id.IdGuru;
import guru.id.IdMetadata;

public class App {
    public static void main(String[] args) {
        IdMetadata meta = IdGuru.inspect("usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44");

        System.out.println("Valid: " + meta.isValid());          // true
        System.out.println("Algorithm: " + meta.getAlgorithm()); // "uuidv7"
        System.out.println("Prefix: " + meta.getPrefix());       // "usr"
        System.out.println("Timestamp: " + meta.getInstant());   // 2026-11-09T00:00:00Z
        System.out.println("EpochMs: " + meta.getEpochMs());     // 1794192000000
    }
}
```

## License

MIT © [Shubham Sharma](https://github.com/uchiha-fire) • [LinkedIn](https://www.linkedin.com/in/shubham-sharma-28487819b/)
