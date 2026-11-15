#!/usr/bin/env python3
# =============================================================================
# ARMOR-WATCH - tools/watch_simulation.py
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
# GPL-3.0-or-later - see LICENSE
# =============================================================================
"""A phone, a watch, the link between them and a server, all in memory (evidence: simulator).

It is the Python twin of src/main/kotlin/armor/watch/Simulation.kt: the same doubles, the same rules, the same names, and every message that crosses the link is checked
against docs/watch_alert_contract.json with tools/watch_contract.py. It lets the whole conversation be tried on a computer without a watch, a phone or a Data Layer.

    python tools/watch_simulation.py          # runs the demonstration and prints WATCH_SIMULATION=PASS or FAIL
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import watch_contract as wc  # noqa: E402

CONTRACT = wc.load()
LIMITS = CONTRACT["limits"]
PATH_TO_WATCH = CONTRACT["paths"]["toWatch"]
PATH_TO_PHONE = CONTRACT["paths"]["toPhone"]
RANK = {"info": 0, "warning": 1, "critical": 2}


class Channel:
    """The Data Layer or WatchConnectivity with faults on purpose: out of range, a message lost on the way, a duplicate, a burst held up and let go."""

    def __init__(self, deliver):
        self.deliver = deliver
        self.up = True
        self.drop_next = 0
        self.duplicate_next = False
        self.held = False
        self.queue: list[tuple[str, str]] = []
        self.lost: list[tuple[str, str]] = []
        self.arrived: list[tuple[str, str]] = []

    def send(self, path: str, text: str) -> bool:
        if not self.up:
            self.lost.append((path, text))
            return False
        if self.drop_next > 0:
            self.drop_next -= 1
            self.lost.append((path, text))
            return True
        if self.held:
            self.queue.append((path, text))
            return True
        self._arrive(path, text)
        if self.duplicate_next:
            self.duplicate_next = False
            self._arrive(path, text)
        return True

    def release(self, reversed_order: bool = False) -> None:
        self.held = False
        batch, self.queue = self.queue, []
        for path, text in (reversed(batch) if reversed_order else batch):
            self._arrive(path, text)

    def _arrive(self, path: str, text: str) -> None:
        self.arrived.append((path, text))
        self.deliver(path, text)


class AlarmServer:
    """The part of ARMOR-SERVER that a confirmation reaches: an alarm is acknowledged once."""

    def __init__(self):
        self.open: set[str] = set()
        self.acknowledged: list[str] = []
        self.reachable = True

    def raise_alarm(self, alarm_id: str) -> None:
        self.open.add(alarm_id)

    def acknowledge(self, alarm_id: str) -> bool:
        if not self.reachable or alarm_id not in self.open:
            return False
        self.open.discard(alarm_id)
        self.acknowledged.append(alarm_id)
        return True


class Watch:
    """The watch: off until the phone says otherwise, keeps at most `pendingMax` alerts in memory, drops what expired, never acts on its own."""

    def __init__(self, link: Channel, clock):
        self.link, self.clock = link, clock
        self.enabled = False
        self.entries: dict[str, dict] = {}

    def on_phone_message(self, path: str, raw: str) -> bool:
        if path != PATH_TO_WATCH:
            return False
        try:
            message = json.loads(raw)
        except ValueError:
            return False
        if not wc.validate(message, CONTRACT):
            return False
        kind = message["type"]
        if kind == "relay":
            changed = self.enabled != message["enabled"]
            self.enabled = message["enabled"]
            if not self.enabled and self.entries:
                self.entries.clear()
                return True
            return changed
        if kind == "alert" and self.enabled:
            self._expire()
            self.entries.pop(message["alertId"], None)
            self.entries[message["alertId"]] = {"alert": message, "expires": self.clock() + message["ttlSeconds"], "confirmed": False}
            while len(self.entries) > LIMITS["pendingMax"]:
                victim = next((k for k, e in self.entries.items() if e["alert"]["severity"] != "critical"), next(iter(self.entries)))
                del self.entries[victim]
            return True
        if kind == "alert_clear" and self.enabled:
            return self.entries.pop(message["alertId"], None) is not None
        return False

    def _expire(self) -> None:
        now = self.clock()
        for key in [k for k, e in self.entries.items() if e["expires"] <= now]:
            del self.entries[key]

    def screen(self) -> tuple[str, list[str]]:
        """('off' | 'quiet' | 'alerts', ids): the most serious first, the newest first among equals."""
        if not self.enabled:
            return "off", []
        self._expire()
        if not self.entries:
            return "quiet", []
        ordered = sorted(reversed(list(self.entries.values())), key=lambda e: -RANK[e["alert"]["severity"]])
        return "alerts", [e["alert"]["alertId"] for e in ordered]

    def can_confirm(self, alert_id: str) -> bool:
        self._expire()
        entry = self.entries.get(alert_id)
        return bool(entry and entry["alert"]["needsAck"] and not entry["confirmed"])

    def confirm(self, alert_id: str) -> str:
        if not self.enabled or not self.can_confirm(alert_id):
            return "NOTHING_TO_CONFIRM"
        entry = self.entries[alert_id]
        entry["confirmed"] = True
        text = json.dumps({"type": "alert_ack", "protocolVersion": CONTRACT["protocolVersion"], "alertId": alert_id})
        if self.link.send(PATH_TO_PHONE, text):
            return "SENT"
        entry["confirmed"] = False
        return "LINK_DOWN"


class Phone:
    """The sending side of the two phone apps: writes the messages of the contract and acts on a confirmation only for an alert it sent, once."""

    def __init__(self, channel: Channel, server: AlarmServer, clock):
        self.channel, self.server, self.clock = channel, server, clock
        self.option_on = False
        self.sent: dict[str, tuple[str, int]] = {}
        self.acknowledged_for_watch: list[str] = []
        self.refusals: list[str] = []

    def set_option(self, on: bool) -> None:
        self.option_on = on
        if not on:
            self.sent.clear()
        self.channel.send(PATH_TO_WATCH, json.dumps({"type": "relay", "protocolVersion": CONTRACT["protocolVersion"], "enabled": on}))

    def notice(self, alert_id: str, alarm_id: str | None, severity: str, title: str, text: str) -> bool:
        if not self.option_on or not title.strip() or not text.strip():
            return False
        wire = {"critical": "critical", "high": "warning"}.get(severity, "info")
        ttl = LIMITS["ttlSeconds"][1] if severity == "critical" else 120
        if alarm_id is not None:
            self.sent[alert_id] = (alarm_id, self.clock() + ttl)
        message = {"type": "alert", "protocolVersion": CONTRACT["protocolVersion"], "alertId": alert_id, "severity": wire,
                   "title": title.strip()[: LIMITS["titleMax"]], "text": text.strip()[: LIMITS["textMax"]], "needsAck": alarm_id is not None, "ttlSeconds": ttl}
        return self.channel.send(PATH_TO_WATCH, json.dumps(message))

    def clear(self, alert_id: str) -> None:
        self.sent.pop(alert_id, None)
        self.channel.send(PATH_TO_WATCH, json.dumps({"type": "alert_clear", "protocolVersion": CONTRACT["protocolVersion"], "alertId": alert_id}))

    def on_watch_message(self, path: str, raw: str) -> bool:
        if path != PATH_TO_PHONE:
            return self._refuse("path")
        if not self.option_on:
            return self._refuse("option off")
        try:
            message = json.loads(raw)
        except ValueError:
            return self._refuse("shape")
        if not wc.validate(message, CONTRACT) or message.get("type") != "alert_ack":
            return self._refuse("shape")
        entry = self.sent.pop(message["alertId"], None)
        if entry is None:
            return self._refuse("unknown")
        alarm_id, expires = entry
        if expires <= self.clock():
            return self._refuse("expired")
        if not self.server.acknowledge(alarm_id):
            return self._refuse("server")
        self.acknowledged_for_watch.append(alarm_id)
        return True

    def _refuse(self, reason: str) -> bool:
        self.refusals.append(reason)
        return False


class Pair:
    """A phone, a watch, the link between them and a server on a clock that only the caller moves."""

    def __init__(self, start: int = 1000):
        self.clock_value = start
        clock = lambda: self.clock_value  # noqa: E731
        self.server = AlarmServer()
        self.to_watch = Channel(lambda path, text: self.watch.on_phone_message(path, text))
        self.to_phone = Channel(lambda path, text: self.phone.on_watch_message(path, text))
        self.phone = Phone(self.to_watch, self.server, clock)
        self._clock = clock
        self.watch = Watch(self.to_phone, clock)

    def advance(self, seconds: int) -> None:
        self.clock_value += seconds

    def restart_watch(self) -> None:
        self.watch = Watch(self.to_phone, self._clock)

    def raise_alarm(self, alarm_id: str, severity: str = "high") -> None:
        self.server.raise_alarm(alarm_id)
        self.phone.notice(alarm_id, alarm_id, severity, "Intrusion", "Radar 1 saw movement")

    def every_message_is_in_the_contract(self) -> bool:
        """Everything that crossed the link, in both directions, is a message of the contract."""
        for _path, text in self.to_watch.arrived + self.to_phone.arrived + self.to_watch.lost + self.to_phone.lost:
            try:
                if not wc.validate(json.loads(text), CONTRACT):
                    return False
            except ValueError:
                return False
        return True


def demonstration() -> list[str]:
    """One alarm, from the phone to the watch and back to the server; the problems found, empty when all is as expected."""
    problems = []
    pair = Pair()
    pair.phone.set_option(True)
    pair.raise_alarm("al-1")
    if pair.watch.screen() != ("alerts", ["al-1"]):
        problems.append("the alert did not reach the watch")
    if pair.watch.confirm("al-1") != "SENT" or pair.server.acknowledged != ["al-1"]:
        problems.append("the confirmation did not reach the server")
    if not pair.every_message_is_in_the_contract():
        problems.append("a message left the contract")
    return problems


def main() -> int:
    problems = demonstration()
    for line in problems:
        print(line)
    print(f"WATCH_SIMULATION={'FAIL' if problems else 'PASS'}")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
