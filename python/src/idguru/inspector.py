"""Universal identifier inspector and reverse decoder."""

import re
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any, Dict, Optional

UUID_REGEX = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-([1-8])[0-9a-f]{3}-([89ab])[0-9a-f]{3}-[0-9a-f]{12}$",
    re.IGNORECASE,
)
ULID_REGEX = re.compile(r"^[0-9A-HJKMNP-TV-Z]{26}$")
KSUID_REGEX = re.compile(r"^[0-9A-Za-z]{27}$")
SNOWFLAKE_REGEX = re.compile(r"^[0-9]{15,20}$")
CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
DEFAULT_SNOWFLAKE_EPOCH = 1288834974657


@dataclass
class IdInspectionResult:
    raw: str
    canonical: str
    algorithm: str
    is_valid: bool
    prefix: Optional[str] = None
    timestamp: Optional[datetime] = None
    epoch_ms: Optional[int] = None
    entropy_bits: Optional[int] = None
    version: Optional[int] = None
    variant: Optional[str] = None
    worker_id: Optional[int] = None
    sequence: Optional[int] = None
    details: Dict[str, Any] = field(default_factory=dict)


def inspect_id(raw_input: str) -> IdInspectionResult:
    """Inspect and reverse-decode any supported identifier."""
    trimmed = raw_input.strip()
    prefix: Optional[str] = None
    canonical = trimmed

    # Detect prefix with underscore delimiter
    underscore_idx = trimmed.find("_")
    if 0 < underscore_idx <= 15:
        candidate_prefix = trimmed[:underscore_idx]
        remainder = trimmed[underscore_idx + 1 :]
        if candidate_prefix.isalnum() and len(remainder) >= 10:
            prefix = candidate_prefix
            canonical = remainder
    elif not UUID_REGEX.match(trimmed):
        first_hyphen = trimmed.find("-")
        if 0 < first_hyphen <= 15:
            candidate_prefix = trimmed[:first_hyphen]
            remainder = trimmed[first_hyphen + 1 :]
            if UUID_REGEX.match(remainder) or ULID_REGEX.match(remainder):
                prefix = candidate_prefix
                canonical = remainder

    # 1. UUID Check
    uuid_match = UUID_REGEX.match(canonical)
    if uuid_match:
        version = int(uuid_match.group(1), 16)
        variant_char = uuid_match.group(2).lower()
        variant = (
            "RFC 4122/9562 Variant 1"
            if variant_char in ("8", "9", "a", "b")
            else "Reserved"
        )

        if version == 7:
            hex_str = canonical.replace("-", "")
            epoch_ms = int(hex_str[0:12], 16)
            sequence = int(hex_str[13:16], 16)
            ts = datetime.fromtimestamp(epoch_ms / 1000.0, tz=timezone.utc)

            return IdInspectionResult(
                raw=trimmed,
                canonical=canonical,
                prefix=prefix,
                algorithm="uuidv7",
                is_valid=True,
                version=7,
                variant=variant,
                timestamp=ts,
                epoch_ms=epoch_ms,
                sequence=sequence,
                entropy_bits=74,
                details={"sub_millisecond_sequence": sequence, "random_bits": 62},
            )

        if version == 4:
            return IdInspectionResult(
                raw=trimmed,
                canonical=canonical,
                prefix=prefix,
                algorithm="uuidv4",
                is_valid=True,
                version=4,
                variant=variant,
                entropy_bits=122,
                details={"random_bits": 122},
            )

    # 2. ULID Check
    if len(canonical) == 26 and ULID_REGEX.match(canonical):
        time_part = canonical[0:10]
        epoch_ms = 0
        for char in time_part:
            epoch_ms = epoch_ms * 32 + CROCKFORD.index(char)
        ts = datetime.fromtimestamp(epoch_ms / 1000.0, tz=timezone.utc)

        return IdInspectionResult(
            raw=trimmed,
            canonical=canonical,
            prefix=prefix,
            algorithm="ulid",
            is_valid=True,
            timestamp=ts,
            epoch_ms=epoch_ms,
            entropy_bits=80,
            details={"encoding": "Crockford Base32", "random_bits": 80},
        )

    # 3. KSUID Check
    if len(canonical) == 27 and KSUID_REGEX.match(canonical):
        return IdInspectionResult(
            raw=trimmed,
            canonical=canonical,
            prefix=prefix,
            algorithm="ksuid",
            is_valid=True,
            entropy_bits=128,
            details={"encoding": "Base62", "random_bits": 128},
        )

    # 4. Snowflake Check
    if SNOWFLAKE_REGEX.match(canonical):
        try:
            num = int(canonical)
            timestamp_part = num >> 22
            worker_id = (num >> 12) & 0x3FF
            seq = num & 0x0FFF
            epoch_ms = timestamp_part + DEFAULT_SNOWFLAKE_EPOCH
            ts = datetime.fromtimestamp(epoch_ms / 1000.0, tz=timezone.utc)

            return IdInspectionResult(
                raw=trimmed,
                canonical=canonical,
                prefix=prefix,
                algorithm="snowflake",
                is_valid=True,
                timestamp=ts,
                epoch_ms=epoch_ms,
                worker_id=worker_id,
                sequence=seq,
                entropy_bits=22,
                details={"worker_id": worker_id, "sequence": seq},
            )
        except ValueError:
            pass

    # 5. NanoID Fallback
    if re.match(r"^[A-Za-z0-9_-]{10,64}$", canonical):
        return IdInspectionResult(
            raw=trimmed,
            canonical=canonical,
            prefix=prefix,
            algorithm="nanoid",
            is_valid=True,
            entropy_bits=int(len(canonical) * 5.95),
            details={"length": len(canonical)},
        )

    return IdInspectionResult(
        raw=trimmed,
        canonical=canonical,
        prefix=prefix,
        algorithm="unknown",
        is_valid=False,
    )
