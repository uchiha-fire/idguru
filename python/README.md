# idguru (Python)

> Universal, zero-dependency identifier generation, fluent builder, and reverse-inspection engine for Python 3.8 - 3.13+.

[![PyPI version](https://img.shields.io/pypi/v/idguru.svg?style=flat-square)](https://pypi.org/project/idguru/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

## Installation

```bash
pip install idguru
# or
poetry add idguru
# or
uv add idguru
```

## Quick Start

### 1. Instant Generation

```python
from idguru import IdGuru

# RFC 9562 UUIDv7 (Monotonic, timestamp-sortable)
user_id = IdGuru.uuidv7() 
# "019a6b42-8e12-7a22-9f1a-b83c7d612e44"

# Other algorithms
IdGuru.uuidv4()    # Random UUIDv4
IdGuru.ulid()      # 26-char Crockford Base32
IdGuru.ksuid()     # 27-char K-Sortable Base62
IdGuru.snowflake() # 64-bit distributed snowflake
IdGuru.nanoid()    # URL-safe compact ID
```

### 2. Fluent Builder Pattern

```python
from idguru import IdGuru

# Prefixed monotonic UUIDv7
user_gen = (
    IdGuru.builder()
    .uuidv7()
    .prefix("usr")
    .delimiter("_")
    .monotonic()
    .build()
)

user_id = user_gen.generate()
# "usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44"

# Snowflake with Worker ID
order_gen = (
    IdGuru.builder()
    .snowflake(worker_id=42)
    .prefix("ord")
    .build()
)

order_id = order_gen.generate()
numeric_id = order_gen.generate_int() # 64-bit integer

# High-throughput batch generator
for batch_id in user_gen.stream(limit=1000):
    process(batch_id)
```

### 3. Universal Reverse-Inspection

```python
from idguru import IdGuru

meta = IdGuru.inspect("usr_019a6b42-8e12-7a22-9f1a-b83c7d612e44")

print(meta.is_valid)      # True
print(meta.algorithm)     # "uuidv7"
print(meta.prefix)        # "usr"
print(meta.timestamp)     # 2026-11-09 00:00:00+00:00
print(meta.epoch_ms)      # 1794192000000
print(meta.entropy_bits)  # 74
```

## License

MIT © [Shubham Sharma](https://github.com/uchiha-fire)
