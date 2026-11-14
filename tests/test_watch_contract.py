# =============================================================================
# ARMOR-WATCH - tests/test_watch_contract.py
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
# GPL-3.0-or-later - see LICENSE
# =============================================================================
"""The contract of the messages, its Python reference, the Kotlin constants and the protocol notes agree."""
import re
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import watch_contract as wc  # noqa: E402

KOTLIN = ROOT / "src" / "main" / "kotlin" / "armor" / "watch" / "WatchProtocol.kt"


class ContractExamples(unittest.TestCase):
    def setUp(self):
        self.contract = wc.load()

    def test_every_example_behaves_as_the_contract_says(self):
        self.assertEqual(wc.run_examples(self.contract), [])

    def test_there_are_enough_examples_of_both_kinds(self):
        self.assertGreaterEqual(len(self.contract["valid"]), 7)
        self.assertGreaterEqual(len(self.contract["invalid"]), 20)

    def test_every_type_has_an_accepted_example_and_every_invalid_one_says_why(self):
        types = {example["type"] for example in self.contract["valid"]}
        self.assertEqual(types, set(self.contract["types"]["toWatch"] + self.contract["types"]["toPhone"]))
        for example in self.contract["invalid"]:
            self.assertTrue(example.get("why"))

    def test_messages_that_are_not_objects_are_refused(self):
        for value in (None, [], "alert", 3, True):
            self.assertFalse(wc.validate(value, self.contract))

    def test_a_boolean_is_not_a_number(self):
        message = {"type": "alert", "protocolVersion": 1, "alertId": "a", "severity": "info", "title": "t", "text": "x", "needsAck": False, "ttlSeconds": True}
        self.assertFalse(wc.validate(message, self.contract))
        message["ttlSeconds"] = 30
        self.assertTrue(wc.validate(message, self.contract))

    def test_the_limits_are_inclusive(self):
        base = {"type": "alert", "protocolVersion": 1, "alertId": "a" * 64, "severity": "critical", "title": "t" * 60, "text": "x" * 160, "needsAck": True, "ttlSeconds": 5}
        self.assertTrue(wc.validate(base, self.contract))
        self.assertFalse(wc.validate({**base, "alertId": "a" * 65}, self.contract))
        self.assertFalse(wc.validate({**base, "title": "t" * 61}, self.contract))
        self.assertFalse(wc.validate({**base, "text": "x" * 161}, self.contract))
        self.assertTrue(wc.validate({**base, "ttlSeconds": 600}, self.contract))
        self.assertFalse(wc.validate({**base, "ttlSeconds": 601}, self.contract))


class KotlinMatchesTheContract(unittest.TestCase):
    def setUp(self):
        self.source = KOTLIN.read_text(encoding="utf-8")
        self.contract = wc.load()

    def constant(self, name):
        match = re.search(r"const val " + name + r" = (\d+)", self.source)
        self.assertIsNotNone(match, name)
        return int(match.group(1))

    def test_the_numbers(self):
        limits = self.contract["limits"]
        self.assertEqual(self.constant("RAW_MAX"), limits["rawMax"])
        self.assertEqual(self.constant("TITLE_MAX"), limits["titleMax"])
        self.assertEqual(self.constant("TEXT_MAX"), limits["textMax"])
        self.assertEqual(self.constant("PENDING_MAX"), limits["pendingMax"])
        self.assertEqual([self.constant("TTL_MIN_SECONDS"), self.constant("TTL_MAX_SECONDS")], limits["ttlSeconds"])
        self.assertEqual(self.constant("VERSION"), self.contract["protocolVersion"])

    def test_the_paths_and_severities(self):
        for key, name in (("toWatch", "PATH_TO_WATCH"), ("toPhone", "PATH_TO_PHONE")):
            self.assertIn(f'const val {name} = "{self.contract["paths"][key]}"', self.source)
        for severity in self.contract["severities"]:
            self.assertIn(f'("{severity}")', self.source)

    def test_the_types(self):
        for kind in self.contract["types"]["toWatch"] + self.contract["types"]["toPhone"]:
            self.assertIn(f'"{kind}"', self.source)


class ProtocolNotes(unittest.TestCase):
    def test_the_notes_name_every_type_and_the_off_by_default_rule(self):
        text = (ROOT / "docs" / "PROTOCOL.md").read_text(encoding="utf-8")
        contract = wc.load()
        for kind in contract["types"]["toWatch"] + contract["types"]["toPhone"]:
            self.assertIn(f"`{kind}`", text)
        self.assertIn("Off by default", text)


if __name__ == "__main__":
    unittest.main()
