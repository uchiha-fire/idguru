"""Fluent builder and generator classes for IdGuru Python SDK."""

from typing import Iterator, Optional
from idguru.algorithms import (
    generate_uuidv7,
    generate_uuidv7_bytes,
    generate_uuidv4,
    generate_uuidv4_bytes,
    generate_ulid,
    generate_ksuid,
    generate_snowflake,
    generate_nanoid,
    DEFAULT_SNOWFLAKE_EPOCH,
    DEFAULT_NANOID_ALPHABET,
)


class IdGenerator:
    """Configured generator instance."""

    def __init__(
        self,
        algorithm: str = "uuidv7",
        prefix: Optional[str] = None,
        delimiter: str = "_",
        monotonic: bool = True,
        snowflake_worker_id: int = 1,
        snowflake_epoch: int = DEFAULT_SNOWFLAKE_EPOCH,
        nanoid_length: int = 21,
        nanoid_alphabet: str = DEFAULT_NANOID_ALPHABET,
    ):
        self.algorithm = algorithm
        self.prefix = prefix
        self.delimiter = delimiter
        self.monotonic = monotonic
        self.snowflake_worker_id = snowflake_worker_id
        self.snowflake_epoch = snowflake_epoch
        self.nanoid_length = nanoid_length
        self.nanoid_alphabet = nanoid_alphabet

    def generate(self) -> str:
        """Generate formatted identifier string."""
        if self.algorithm == "uuidv7":
            raw = generate_uuidv7(self.monotonic)
        elif self.algorithm == "uuidv4":
            raw = generate_uuidv4()
        elif self.algorithm == "ulid":
            raw = generate_ulid()
        elif self.algorithm == "ksuid":
            raw = generate_ksuid()
        elif self.algorithm == "snowflake":
            raw, _ = generate_snowflake(
                self.snowflake_worker_id, self.snowflake_epoch
            )
        elif self.algorithm == "nanoid":
            raw = generate_nanoid(self.nanoid_length, self.nanoid_alphabet)
        else:
            raw = generate_uuidv7(self.monotonic)

        if self.prefix:
            return f"{self.prefix}{self.delimiter}{raw}"
        return raw

    def generate_bytes(self) -> bytes:
        """Generate binary representation."""
        if self.algorithm == "uuidv7":
            return generate_uuidv7_bytes(self.monotonic)
        if self.algorithm == "uuidv4":
            return generate_uuidv4_bytes()
        return self.generate().encode("utf-8")

    def generate_int(self) -> int:
        """Generate numeric/integer representation (ideal for Snowflake)."""
        if self.algorithm == "snowflake":
            _, int_val = generate_snowflake(
                self.snowflake_worker_id, self.snowflake_epoch
            )
            return int_val
        hex_str = self.generate().replace("-", "")
        clean_hex = "".join(c for c in hex_str if c in "0123456789abcdefABCDEF")
        return int(clean_hex, 16) if clean_hex else 0

    def stream(self, limit: int = 1000) -> Iterator[str]:
        """High-throughput generator stream."""
        for _ in range(limit):
            yield self.generate()


class IdBuilder:
    """Fluent type-safe builder for IdGuru."""

    def __init__(self):
        self._algorithm = "uuidv7"
        self._prefix: Optional[str] = None
        self._delimiter = "_"
        self._monotonic = True
        self._snowflake_worker_id = 1
        self._snowflake_epoch = DEFAULT_SNOWFLAKE_EPOCH
        self._nanoid_length = 21
        self._nanoid_alphabet = DEFAULT_NANOID_ALPHABET

    def uuidv7(self) -> "IdBuilder":
        self._algorithm = "uuidv7"
        return self

    def uuidv4(self) -> "IdBuilder":
        self._algorithm = "uuidv4"
        return self

    def ulid(self) -> "IdBuilder":
        self._algorithm = "ulid"
        return self

    def ksuid(self) -> "IdBuilder":
        self._algorithm = "ksuid"
        return self

    def snowflake(
        self, worker_id: int = 1, epoch: int = DEFAULT_SNOWFLAKE_EPOCH
    ) -> "IdBuilder":
        self._algorithm = "snowflake"
        self._snowflake_worker_id = worker_id
        self._snowflake_epoch = epoch
        return self

    def nanoid(
        self, length: int = 21, alphabet: str = DEFAULT_NANOID_ALPHABET
    ) -> "IdBuilder":
        self._algorithm = "nanoid"
        self._nanoid_length = length
        self._nanoid_alphabet = alphabet
        return self

    def prefixed(self, prefix: str) -> "IdBuilder":
        self._algorithm = "uuidv7"
        self._prefix = prefix
        return self

    def prefix(self, prefix: str) -> "IdBuilder":
        self._prefix = prefix
        return self

    def delimiter(self, delimiter: str) -> "IdBuilder":
        self._delimiter = delimiter
        return self

    def monotonic(self, enable: bool = True) -> "IdBuilder":
        self._monotonic = enable
        return self

    def build(self) -> IdGenerator:
        return IdGenerator(
            algorithm=self._algorithm,
            prefix=self._prefix,
            delimiter=self._delimiter,
            monotonic=self._monotonic,
            snowflake_worker_id=self._snowflake_worker_id,
            snowflake_epoch=self._snowflake_epoch,
            nanoid_length=self._nanoid_length,
            nanoid_alphabet=self._nanoid_alphabet,
        )
