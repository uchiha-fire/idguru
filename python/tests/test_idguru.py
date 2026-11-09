"""Extensive unit tests for IdGuru Python SDK."""

import re
import sys
import os
import unittest
from datetime import datetime, timezone

# Add src to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "../src")))

from idguru import IdGuru, IdBuilder, IdGenerator, inspect_id


class TestIdGuru(unittest.TestCase):

    def test_instant_uuidv7_format(self):
        id_str = IdGuru.uuidv7()
        pattern = r"^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
        self.assertRegex(id_str, pattern)
        self.assertEqual(id_str[14], "7")
        self.assertIn(id_str[19], ("8", "9", "a", "b"))

    def test_uuidv7_monotonicity(self):
        ids = [IdGuru.uuidv7(monotonic=True) for _ in range(5000)]
        for i in range(1, len(ids)):
            self.assertGreater(ids[i], ids[i - 1])

    def test_instant_uuidv4(self):
        id_str = IdGuru.uuidv4()
        pattern = r"^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$"
        self.assertRegex(id_str, pattern)

    def test_instant_ulid(self):
        id_str = IdGuru.ulid()
        self.assertEqual(len(id_str), 26)
        self.assertRegex(id_str, r"^[0-9A-HJKMNP-TV-Z]{26}$")

    def test_instant_ksuid(self):
        id_str = IdGuru.ksuid()
        self.assertEqual(len(id_str), 27)
        self.assertRegex(id_str, r"^[0-9A-Za-z]{27}$")

    def test_instant_snowflake(self):
        id_str = IdGuru.snowflake(worker_id=42)
        self.assertTrue(id_str.isdigit())
        self.assertGreaterEqual(len(id_str), 15)

    def test_instant_nanoid(self):
        id_default = IdGuru.nanoid()
        self.assertEqual(len(id_default), 21)

        id_custom = IdGuru.nanoid(length=32)
        self.assertEqual(len(id_custom), 32)

    def test_instant_prefixed(self):
        id_str = IdGuru.prefixed("inv")
        self.assertTrue(id_str.startswith("inv_"))
        raw = id_str.split("_")[1]
        self.assertEqual(raw[14], "7")

    def test_builder_uuidv7_prefixed(self):
        gen = (
            IdGuru.builder()
            .uuidv7()
            .prefix("usr")
            .delimiter("_")
            .monotonic(True)
            .build()
        )
        self.assertEqual(gen.algorithm, "uuidv7")
        self.assertEqual(gen.prefix, "usr")

        user_id = gen.generate()
        self.assertTrue(user_id.startswith("usr_"))
        raw = user_id.split("_")[1]
        self.assertEqual(raw[14], "7")

    def test_builder_snowflake_and_numeric_int(self):
        gen = IdGuru.builder().snowflake(worker_id=15).prefix("ord").build()

        order_id = gen.generate()
        self.assertTrue(order_id.startswith("ord_"))

        num = gen.generate_int()
        self.assertIsInstance(num, int)
        self.assertGreater(num, 0)

    def test_builder_nanoid_custom_alphabet(self):
        gen = (
            IdGuru.builder()
            .nanoid(length=12, alphabet="ABCDEF123456")
            .prefix("tok")
            .delimiter("-")
            .build()
        )
        token = gen.generate()
        self.assertTrue(token.startswith("tok-"))
        payload = token.split("-")[1]
        self.assertEqual(len(payload), 12)
        self.assertRegex(payload, r"^[A-F1-6]{12}$")

    def test_builder_stream(self):
        gen = IdGuru.builder().uuidv7().build()
        items = list(gen.stream(limit=50))
        self.assertEqual(len(items), 50)
        self.assertEqual(len(set(items)), 50)

    def test_inspector_uuidv7(self):
        before = int(datetime.now(timezone.utc).timestamp() * 1000)
        id_str = IdGuru.uuidv7()
        after = int(datetime.now(timezone.utc).timestamp() * 1000)

        meta = IdGuru.inspect(id_str)
        self.assertTrue(meta.is_valid)
        self.assertEqual(meta.algorithm, "uuidv7")
        self.assertEqual(meta.version, 7)
        self.assertGreaterEqual(meta.epoch_ms, before - 1000)
        self.assertLessEqual(meta.epoch_ms, after + 1000)
        self.assertIsInstance(meta.timestamp, datetime)

    def test_inspector_prefixed_id(self):
        id_str = "cust_019a6b42-8e12-7a22-9f1a-b83c7d612e44"
        meta = IdGuru.inspect(id_str)
        self.assertTrue(meta.is_valid)
        self.assertEqual(meta.prefix, "cust")
        self.assertEqual(meta.canonical, "019a6b42-8e12-7a22-9f1a-b83c7d612e44")
        self.assertEqual(meta.algorithm, "uuidv7")

    def test_inspector_ulid(self):
        id_str = IdGuru.ulid()
        meta = IdGuru.inspect(id_str)
        self.assertTrue(meta.is_valid)
        self.assertEqual(meta.algorithm, "ulid")
        self.assertGreater(meta.epoch_ms, 1700000000000)

    def test_inspector_snowflake(self):
        id_str = IdGuru.snowflake(worker_id=7)
        meta = IdGuru.inspect(id_str)
        self.assertTrue(meta.is_valid)
        self.assertEqual(meta.algorithm, "snowflake")
        self.assertEqual(meta.worker_id, 7)

    def test_inspector_invalid(self):
        meta = IdGuru.inspect("invalid_garbage_!@#$%^&*")
        self.assertFalse(meta.is_valid)
        self.assertEqual(meta.algorithm, "unknown")


if __name__ == "__main__":
    unittest.main()
