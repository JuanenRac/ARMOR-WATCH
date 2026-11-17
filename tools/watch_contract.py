#!/usr/bin/env python3
# =============================================================================
# ARMOR-WATCH - tools/watch_contract.py
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
# GPL-3.0-or-later - see LICENSE
# =============================================================================
"""The messages between the phone and the watch, checked in Python against docs/watch_alert_contract.json.

This is the reference the Kotlin code of src/ is held to: both read the same file and run the same accepted and rejected examples.

    python tools/watch_contract.py            # runs every example of the contract and prints WATCH_CONTRACT=PASS or FAIL
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTRACT_FILE = ROOT / "docs" / "watch_alert_contract.json"


def load(path: Path = CONTRACT_FILE) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def expand(value):
    """An example may write a long text as {"repeat": "x", "count": 161}; this gives the text."""
    if isinstance(value, dict) and set(value) == {"repeat", "count"}:
        return value["repeat"] * value["count"]
    if isinstance(value, dict):
        return {key: expand(item) for key, item in value.items()}
    if isinstance(value, list):
        return [expand(item) for item in value]
    return value


def _is_int(value) -> bool:
    return isinstance(value, int) and not isinstance(value, bool)


def validate(message, contract: dict | None = None) -> bool:
    """True when `message` (a dictionary) is a message of the contract."""
    contract = contract or load()
    limits = contract["limits"]
    if not isinstance(message, dict) or len(json.dumps(message, ensure_ascii=False)) > limits["rawMax"]:
        return False
    kind = message.get("type")
    if kind not in contract["types"]["toWatch"] + contract["types"]["toPhone"]:
        return False
    if not _is_int(message.get("protocolVersion")) or message["protocolVersion"] != contract["protocolVersion"]:
        return False
    if kind == "relay":
        return isinstance(message.get("enabled"), bool)
    identifier = message.get("alertId")
    if not isinstance(identifier, str) or not re.fullmatch(limits["alertIdPattern"], identifier):
        return False
    if kind in ("alert_clear", "alert_ack"):
        return True
    title, text = message.get("title"), message.get("text")
    if not isinstance(title, str) or not title.strip() or len(title) > limits["titleMax"]:
        return False
    if not isinstance(text, str) or not text.strip() or len(text) > limits["textMax"]:
        return False
    if message.get("severity") not in contract["severities"]:
        return False
    if not isinstance(message.get("needsAck"), bool):
        return False
    ttl = message.get("ttlSeconds")
    low, high = limits["ttlSeconds"]
    return _is_int(ttl) and low <= ttl <= high


def run_examples(contract: dict | None = None) -> list[str]:
    """The examples that behave differently from what the contract says (empty when all agree)."""
    contract = contract or load()
    problems = []
    for index, example in enumerate(contract["valid"]):
        if not validate(expand(example), contract):
            problems.append(f"valid[{index}] was refused: {example.get('type')}")
    for index, example in enumerate(contract["invalid"]):
        body = {key: value for key, value in expand(example).items() if key != "why"}
        if validate(body, contract):
            problems.append(f"invalid[{index}] was accepted: {example['why']}")
    return problems


def settings_problems(contract: dict) -> list[str]:
    """What is wrong with the settings the watch apps offer: each has a type, a default of that type and the platforms that offer it."""
    problems = []
    for name, setting in contract.get("settings", {}).items():
        if setting.get("type") != "boolean" or not isinstance(setting.get("default"), bool):
            problems.append(f"settings.{name} must be a boolean with a boolean default")
        if setting.get("default") is not False:
            problems.append(f"settings.{name} must be off by default")
        if not setting.get("platforms") or not set(setting["platforms"]) <= {"wear", "watchos"}:
            problems.append(f"settings.{name} must name the platforms that offer it")
    return problems


def main() -> int:
    problems = run_examples() + settings_problems(load())
    for line in problems:
        print(line)
    contract = load()
    print(f"WATCH_CONTRACT={'FAIL' if problems else 'PASS'} valid={len(contract['valid'])} invalid={len(contract['invalid'])}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
