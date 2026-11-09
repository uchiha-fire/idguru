"""IdGuru: Universal identifier generation, fluent builder, and inspection suite."""

from idguru.algorithms import (
    generate_uuidv7,
    generate_uuidv7_bytes,
    generate_uuidv4,
    generate_uuidv4_bytes,
    generate_ulid,
    generate_ksuid,
    generate_snowflake,
    generate_nanoid,
)
from idguru.builder import IdBuilder, IdGenerator
from idguru.inspector import IdInspectionResult, inspect_id

__version__ = "1.0.0"


class IdGuru:
    """Universal IdGuru Facade."""

    @staticmethod
    def builder() -> IdBuilder:
        """Start a fluent builder."""
        return IdBuilder()

    @staticmethod
    def uuidv7(monotonic: bool = True) -> str:
        """Instant RFC 9562 UUIDv7 generator."""
        return generate_uuidv7(monotonic)

    @staticmethod
    def uuidv4() -> str:
        """Instant UUIDv4 generator."""
        return generate_uuidv4()

    @staticmethod
    def ulid() -> str:
        """Instant ULID generator."""
        return generate_ulid()

    @staticmethod
    def ksuid() -> str:
        """Instant KSUID generator."""
        return generate_ksuid()

    @staticmethod
    def snowflake(worker_id: int = 1) -> str:
        """Instant Snowflake ID generator."""
        id_str, _ = generate_snowflake(worker_id)
        return id_str

    @staticmethod
    def nanoid(length: int = 21) -> str:
        """Instant NanoID generator."""
        return generate_nanoid(length)

    @staticmethod
    def prefixed(prefix: str, delimiter: str = "_") -> str:
        """Instant Prefixed UUIDv7 generator."""
        return f"{prefix}{delimiter}{generate_uuidv7(True)}"

    @staticmethod
    def inspect(id_str: str) -> IdInspectionResult:
        """Inspect and reverse-decode any identifier."""
        return inspect_id(id_str)


__all__ = [
    "IdGuru",
    "IdBuilder",
    "IdGenerator",
    "IdInspectionResult",
    "inspect_id",
    "generate_uuidv7",
    "generate_uuidv7_bytes",
    "generate_uuidv4",
    "generate_uuidv4_bytes",
    "generate_ulid",
    "generate_ksuid",
    "generate_snowflake",
    "generate_nanoid",
]
