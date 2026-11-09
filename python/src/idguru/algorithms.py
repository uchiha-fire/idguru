"""Core identifier generation algorithms using Python standard library."""

import secrets
import threading
import time
from typing import Tuple

_lock = threading.Lock()
_last_ms: int = 0
_seq: int = 0

CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
DEFAULT_NANOID_ALPHABET = "useandom-26T198340PX75pxJACKVERYMINDBUSHWOLFGQZ_bfghjklqvwyz"
DEFAULT_SNOWFLAKE_EPOCH = 1288834974657  # Twitter Snowflake epoch

_snowflake_lock = threading.Lock()
_snowflake_last_ts = -1
_snowflake_sequence = 0


def generate_uuidv7_bytes(monotonic: bool = True) -> bytes:
    """Generate 16 raw bytes representing an RFC 9562 UUIDv7."""
    global _last_ms, _seq

    rand_bytes = bytearray(secrets.token_bytes(16))
    now = int(time.time() * 1000)

    if monotonic:
        with _lock:
            if now > _last_ms:
                _last_ms = now
                # Randomize initial sequence offset
                _seq = ((rand_bytes[6] & 0x07) << 8 | rand_bytes[7]) & 0x0FFF
            else:
                _seq = (_seq + 1) & 0x0FFF
                if _seq == 0:
                    _last_ms += 1
            timestamp = _last_ms
            curr_seq = _seq
    else:
        timestamp = now
        curr_seq = ((rand_bytes[6] & 0x0F) << 8 | rand_bytes[7]) & 0x0FFF

    # Byte 0-5: 48-bit timestamp
    rand_bytes[0] = (timestamp >> 40) & 0xFF
    rand_bytes[1] = (timestamp >> 32) & 0xFF
    rand_bytes[2] = (timestamp >> 24) & 0xFF
    rand_bytes[3] = (timestamp >> 16) & 0xFF
    rand_bytes[4] = (timestamp >> 8) & 0xFF
    rand_bytes[5] = timestamp & 0xFF

    # Byte 6: 4-bit version (7) | 4-bit sequence high
    rand_bytes[6] = 0x70 | ((curr_seq >> 8) & 0x0F)

    # Byte 7: 8-bit sequence low
    rand_bytes[7] = curr_seq & 0xFF

    # Byte 8: 2-bit variant (10xx xxxx)
    rand_bytes[8] = 0x80 | (rand_bytes[8] & 0x3F)

    return bytes(rand_bytes)


def bytes_to_uuid_str(b: bytes) -> str:
    """Format 16 bytes into canonical 8-4-4-4-12 hex string."""
    h = b.hex()
    return f"{h[0:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:32]}"


def generate_uuidv7(monotonic: bool = True) -> str:
    """Generate RFC 9562 UUIDv7 string."""
    return bytes_to_uuid_str(generate_uuidv7_bytes(monotonic))


def generate_uuidv4_bytes() -> bytes:
    """Generate 16 raw bytes representing a UUIDv4."""
    b = bytearray(secrets.token_bytes(16))
    b[6] = 0x40 | (b[6] & 0x0F)  # Version 4
    b[8] = 0x80 | (b[8] & 0x3F)  # Variant RFC 4122
    return bytes(b)


def generate_uuidv4() -> str:
    """Generate UUIDv4 string."""
    return bytes_to_uuid_str(generate_uuidv4_bytes())


def generate_ulid() -> str:
    """Generate 26-char Crockford Base32 ULID."""
    time_ms = int(time.time() * 1000)

    # 10 chars timestamp
    time_chars = []
    t = time_ms
    for _ in range(10):
        time_chars.append(CROCKFORD[t % 32])
        t //= 32
    time_str = "".join(reversed(time_chars))

    # 16 chars randomness (80 bits)
    rand_bytes = secrets.token_bytes(10)
    rand_chars = []
    for i in range(16):
        byte_idx = (i * 5) // 8
        bit_offset = (i * 5) % 8
        val = (rand_bytes[byte_idx] << 8) | (
            rand_bytes[byte_idx + 1] if byte_idx + 1 < 10 else 0
        )
        val = (val >> (11 - bit_offset)) & 0x1F
        rand_chars.append(CROCKFORD[val])

    return time_str + "".join(rand_chars)


def generate_ksuid() -> str:
    """Generate 27-char Segment KSUID (Base62)."""
    ksuid_epoch = 1400000000
    now_sec = int(time.time()) - ksuid_epoch

    time_bytes = now_sec.to_bytes(4, byteorder="big")
    payload = secrets.token_bytes(16)
    full_bytes = time_bytes + payload

    num = int.from_bytes(full_bytes, byteorder="big")
    chars = []
    while num > 0:
        chars.append(BASE62[num % 62])
        num //= 62

    return "".join(reversed(chars)).rjust(27, "0")


def generate_snowflake(
    worker_id: int = 1, epoch: int = DEFAULT_SNOWFLAKE_EPOCH
) -> Tuple[str, int]:
    """Generate 64-bit Snowflake ID (returns (str, int))."""
    global _snowflake_last_ts, _snowflake_sequence

    with _snowflake_lock:
        now = int(time.time() * 1000)
        if now < _snowflake_last_ts:
            now = _snowflake_last_ts

        if now == _snowflake_last_ts:
            _snowflake_sequence = (_snowflake_sequence + 1) & 0x0FFF
            if _snowflake_sequence == 0:
                while now <= _snowflake_last_ts:
                    now = int(time.time() * 1000)
        else:
            _snowflake_sequence = 0

        _snowflake_last_ts = now
        worker_part = (worker_id & 0x3FF) << 12
        time_part = (now - epoch) << 22
        id_int = time_part | worker_part | _snowflake_sequence

        return str(id_int), id_int


def generate_nanoid(length: int = 21, alphabet: str = DEFAULT_NANOID_ALPHABET) -> str:
    """Generate uniform rejection-sampled NanoID."""
    alpha_len = len(alphabet)
    mask = (2 << (31 - (alpha_len - 1).bit_length())) - 1
    step = int(1.6 * mask * length / alpha_len) + 1

    chars = []
    while len(chars) < length:
        raw_bytes = secrets.token_bytes(step)
        for b in raw_bytes:
            idx = b & mask
            if idx < alpha_len:
                chars.append(alphabet[idx])
                if len(chars) == length:
                    return "".join(chars)
    return "".join(chars)
