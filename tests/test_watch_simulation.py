# =============================================================================
# ARMOR-WATCH - tests/test_watch_simulation.py
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
# GPL-3.0-or-later - see LICENSE
# =============================================================================
"""The phone, the watch, the link and the server in memory (evidence: simulator): the scenarios of the Kotlin twin, run in Python against the contract."""
import json
import re
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import watch_simulation as sim  # noqa: E402

KOTLIN_TEST = ROOT / "src" / "test" / "kotlin" / "armor" / "watch" / "SimulatedPairTest.kt"


def snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def ack(alert_id: str) -> str:
    return json.dumps({"type": "alert_ack", "protocolVersion": 1, "alertId": alert_id})


class Scenarios(unittest.TestCase):
    def setUp(self):
        self.pair = sim.Pair()

    def ids(self):
        return self.pair.watch.screen()[1]

    def test_nothing_reaches_the_watch_while_the_option_is_off(self):
        self.pair.server.raise_alarm("al-1")
        self.assertFalse(self.pair.phone.notice("al-1", "al-1", "high", "Intrusion", "Radar 1"))
        self.assertEqual(self.pair.to_watch.arrived, [])
        self.assertEqual(self.pair.watch.screen()[0], "off")

    def test_an_alarm_reaches_the_watch_and_a_tap_acknowledges_it_on_the_server_once(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("al-1")
        self.assertEqual(self.ids(), ["al-1"])
        self.assertEqual(self.pair.watch.confirm("al-1"), "SENT")
        self.assertEqual(self.pair.server.acknowledged, ["al-1"])
        self.pair.phone.clear("al-1")
        self.assertEqual(self.pair.watch.screen()[0], "quiet")
        self.assertEqual(self.pair.watch.confirm("al-1"), "NOTHING_TO_CONFIRM")
        self.assertFalse(self.pair.phone.on_watch_message(sim.PATH_TO_PHONE, ack("al-1")))
        self.assertEqual(len(self.pair.server.acknowledged), 1)
        self.assertTrue(self.pair.every_message_is_in_the_contract())

    def test_a_duplicated_confirmation_on_the_link_acknowledges_only_once(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("al-1")
        self.pair.to_phone.duplicate_next = True
        self.assertEqual(self.pair.watch.confirm("al-1"), "SENT")
        self.assertEqual(len(self.pair.to_phone.arrived), 2)
        self.assertEqual(self.pair.server.acknowledged, ["al-1"])
        self.assertEqual(self.pair.phone.refusals, ["unknown"])

    def test_a_phone_out_of_range_leaves_the_button_to_press_again_and_the_alarm_open(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("al-1")
        self.pair.to_phone.up = False
        self.assertEqual(self.pair.watch.confirm("al-1"), "LINK_DOWN")
        self.assertIn("al-1", self.pair.server.open)
        self.assertTrue(self.pair.watch.can_confirm("al-1"))
        self.pair.to_phone.up = True
        self.assertEqual(self.pair.watch.confirm("al-1"), "SENT")
        self.assertNotIn("al-1", self.pair.server.open)

    def test_a_confirmation_lost_on_the_way_looks_sent_on_the_watch_and_never_acts_on_the_server(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("al-1")
        self.pair.to_phone.drop_next = 1
        self.assertEqual(self.pair.watch.confirm("al-1"), "SENT")
        self.assertIn("al-1", self.pair.server.open)
        self.assertTrue(self.pair.watch.entries["al-1"]["confirmed"])
        self.assertEqual(len(self.pair.to_phone.lost), 1)
        self.pair.phone.clear("al-1")
        self.assertEqual(self.pair.watch.screen()[0], "quiet")

    def test_an_alert_that_was_held_up_on_the_link_is_refused_when_it_arrives_after_its_time_to_live(self):
        self.pair.phone.set_option(True)
        self.pair.to_watch.held = True
        self.pair.raise_alarm("al-1")
        self.pair.advance(5)
        self.pair.to_watch.release()
        self.assertEqual(self.ids(), ["al-1"])
        self.pair.advance(120)
        self.assertEqual(self.pair.watch.screen()[0], "quiet")
        self.assertFalse(self.pair.phone.on_watch_message(sim.PATH_TO_PHONE, ack("al-1")))
        self.assertEqual(self.pair.phone.refusals, ["expired"])
        self.assertIn("al-1", self.pair.server.open)

    def test_the_watch_that_starts_again_is_off_until_the_phone_switches_it_on_again(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("al-1")
        self.pair.restart_watch()
        self.assertEqual(self.pair.watch.screen()[0], "off")
        self.assertEqual(self.pair.watch.confirm("al-1"), "NOTHING_TO_CONFIRM")
        self.pair.raise_alarm("al-2")
        self.assertEqual(self.pair.watch.screen()[0], "off")
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("al-3")
        self.assertEqual(self.ids(), ["al-3"])

    def test_a_burst_that_arrives_in_the_wrong_order_still_keeps_the_most_serious_first_and_the_switch_wins(self):
        self.pair.phone.set_option(True)
        self.pair.to_watch.held = True
        self.pair.raise_alarm("lesser", "warning")
        self.pair.raise_alarm("worst", "critical")
        self.pair.to_watch.release(reversed_order=True)
        self.assertEqual(self.ids(), ["worst", "lesser"])
        self.pair.to_watch.held = True
        self.pair.phone.set_option(False)
        self.pair.to_watch.release()
        self.assertEqual(self.pair.watch.screen()[0], "off")

    def test_the_watch_keeps_only_eight_alerts_and_the_critical_ones_stay(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("crit", "critical")
        for index in range(1, 11):
            self.pair.raise_alarm(f"lesser-{index}")
        shown = self.ids()
        self.assertEqual(len(shown), 8)
        self.assertEqual(shown[0], "crit")
        self.assertNotIn("lesser-1", shown)
        self.assertIn("lesser-10", shown)

    def test_switching_the_option_off_clears_the_watch_and_the_confirmations_stop_being_accepted(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("al-1")
        self.pair.phone.set_option(False)
        self.assertEqual(self.pair.watch.screen()[0], "off")
        self.assertEqual(self.pair.watch.confirm("al-1"), "NOTHING_TO_CONFIRM")
        self.assertFalse(self.pair.phone.on_watch_message(sim.PATH_TO_PHONE, ack("al-1")))
        self.assertEqual(self.pair.phone.refusals, ["option off"])
        self.assertIn("al-1", self.pair.server.open)

    def test_a_server_that_cannot_be_reached_makes_the_confirmation_fail_on_the_phone_not_on_the_watch(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("al-1")
        self.pair.server.reachable = False
        self.assertEqual(self.pair.watch.confirm("al-1"), "SENT")
        self.assertEqual(self.pair.phone.refusals, ["server"])
        self.assertEqual(self.pair.phone.acknowledged_for_watch, [])
        self.assertIn("al-1", self.pair.server.open)

    def test_the_watch_ignores_what_the_contract_forbids_even_when_the_phone_sends_it(self):
        self.pair.phone.set_option(True)
        base = {"type": "alert", "protocolVersion": 1, "alertId": "a", "severity": "info", "title": "t", "text": "x", "needsAck": False, "ttlSeconds": 60}
        self.assertTrue(self.pair.to_watch.send(sim.PATH_TO_WATCH, json.dumps({**base, "alertId": "a b"})))
        self.assertTrue(self.pair.to_watch.send(sim.PATH_TO_WATCH, json.dumps({**base, "ttlSeconds": 4})))
        self.assertTrue(self.pair.to_watch.send("/armor/other", json.dumps({"type": "relay", "protocolVersion": 1, "enabled": False})))
        self.assertEqual(self.pair.watch.screen()[0], "quiet")

    def test_the_notice_of_the_lesser_kinds_travels_as_info_with_its_text_cut_to_the_limits(self):
        self.pair.phone.set_option(True)
        self.assertTrue(self.pair.phone.notice("n7", None, "warning", "T" * 100, "X" * 500))
        entry = self.pair.watch.entries["n7"]["alert"]
        self.assertEqual(entry["severity"], "info")
        self.assertEqual(len(entry["title"]), 60)
        self.assertEqual(len(entry["text"]), 160)
        self.assertFalse(self.pair.watch.can_confirm("n7"))

    def test_the_channel_records_what_was_lost_and_what_arrived(self):
        channel = sim.Channel(lambda path, text: None)
        channel.up = False
        self.assertFalse(channel.send("/x", "one"))
        channel.up = True
        channel.drop_next = 1
        self.assertTrue(channel.send("/x", "two"))
        self.assertTrue(channel.send("/x", "three"))
        self.assertEqual(channel.lost, [("/x", "one"), ("/x", "two")])
        self.assertEqual(channel.arrived, [("/x", "three")])


class Twins(unittest.TestCase):
    def test_every_scenario_of_the_kotlin_twin_has_its_python_twin(self):
        kotlin = re.findall(r"@Test\s+fun (\w+)\(", KOTLIN_TEST.read_text(encoding="utf-8"))
        self.assertGreaterEqual(len(kotlin), 14)
        python = {name for name in dir(Scenarios) if name.startswith("test_")}
        missing = [name for name in kotlin if "test_" + snake(name) not in python]
        self.assertEqual(missing, [])

    def test_the_demonstration_passes(self):
        self.assertEqual(sim.demonstration(), [])


if __name__ == "__main__":
    unittest.main()
