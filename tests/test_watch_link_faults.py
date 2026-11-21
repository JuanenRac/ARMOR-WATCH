# =============================================================================
# ARMOR-WATCH - tests/test_watch_link_faults.py
# Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
# GPL-3.0-or-later - see LICENSE
# =============================================================================
"""More faults on the link between the phone and the watch (evidence: simulator): boundaries of the time to live, replays, wrong paths, odd texts and a seeded random run
that checks the rules that must hold whatever the order of the events."""
import json
import random
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import watch_simulation as sim  # noqa: E402


def message(kind, **fields):
    return json.dumps({"type": kind, "protocolVersion": sim.CONTRACT["protocolVersion"], **fields})


def alert(alert_id, severity="warning", ttl=60, needs_ack=True):
    return message("alert", alertId=alert_id, severity=severity, title="T", text="x", needsAck=needs_ack, ttlSeconds=ttl)


class WatchTests(unittest.TestCase):
    def setUp(self):
        self.pair = sim.Pair()
        self.watch = self.pair.watch
        self.watch.on_phone_message(sim.PATH_TO_WATCH, message("relay", enabled=True))

    def feed(self, text, path=sim.PATH_TO_WATCH):
        return self.watch.on_phone_message(path, text)

    def test_an_alert_on_a_path_that_is_not_the_phones_is_ignored(self):
        self.assertFalse(self.feed(alert("a"), path="/other"))
        self.assertEqual(self.watch.screen(), ("quiet", []))

    def test_text_that_is_not_json_is_ignored(self):
        for text in ("", "not json", "{", "[]", "null", "42", '"alert"'):
            self.assertFalse(self.feed(text), text)
        self.assertEqual(self.watch.screen(), ("quiet", []))

    def test_a_message_over_the_raw_limit_is_ignored(self):
        big = alert("a")[:-1] + ',"pad":"' + "x" * sim.LIMITS["rawMax"] + '"}'
        self.assertFalse(self.feed(big))

    def test_an_id_that_breaks_the_pattern_is_ignored(self):
        for bad in ("", "has space", "a/b", "x" * 65, "ñ", "a.b"):
            self.assertFalse(self.feed(alert(bad)), bad)

    def test_an_id_at_the_length_limit_is_kept(self):
        self.assertTrue(self.feed(alert("x" * 64)))
        self.assertEqual(self.watch.screen()[1], ["x" * 64])

    def test_a_time_to_live_outside_the_limits_is_ignored_and_the_limits_are_kept(self):
        low, high = sim.LIMITS["ttlSeconds"]
        self.assertFalse(self.feed(alert("a", ttl=low - 1)))
        self.assertFalse(self.feed(alert("b", ttl=high + 1)))
        self.assertTrue(self.feed(alert("c", ttl=low)))
        self.assertTrue(self.feed(alert("d", ttl=high)))

    def test_an_alert_goes_exactly_at_its_time_to_live(self):
        self.feed(alert("a", ttl=10))
        self.pair.advance(9)
        self.assertEqual(self.watch.screen()[1], ["a"])
        self.pair.advance(1)
        self.assertEqual(self.watch.screen(), ("quiet", []))

    def test_the_same_alert_twice_is_shown_once_with_the_later_time_to_live(self):
        self.feed(alert("a", ttl=10))
        self.pair.advance(8)
        self.feed(alert("a", ttl=10))
        self.pair.advance(5)
        self.assertEqual(self.watch.screen()[1], ["a"])

    def test_a_clear_of_an_unknown_alert_changes_nothing(self):
        self.feed(alert("a"))
        self.assertFalse(self.feed(message("alert_clear", alertId="zzz")))
        self.assertEqual(self.watch.screen()[1], ["a"])

    def test_a_clear_twice_changes_only_the_first_time(self):
        self.feed(alert("a"))
        self.assertTrue(self.feed(message("alert_clear", alertId="a")))
        self.assertFalse(self.feed(message("alert_clear", alertId="a")))

    def test_the_same_relay_twice_changes_nothing_the_second_time(self):
        self.assertFalse(self.feed(message("relay", enabled=True)))

    def test_a_relay_off_while_empty_reports_only_the_change(self):
        self.assertTrue(self.feed(message("relay", enabled=False)))
        self.assertFalse(self.feed(message("relay", enabled=False)))

    def test_alerts_and_clears_are_ignored_while_off(self):
        self.feed(message("relay", enabled=False))
        self.assertFalse(self.feed(alert("a")))
        self.assertFalse(self.feed(message("alert_clear", alertId="a")))
        self.assertEqual(self.watch.screen(), ("off", []))

    def test_a_message_the_watch_never_receives_is_ignored(self):
        self.assertFalse(self.feed(message("alert_ack", alertId="a")))

    def test_with_only_critical_alerts_the_oldest_goes_when_the_ninth_arrives(self):
        for index in range(sim.LIMITS["pendingMax"] + 1):
            self.feed(alert(f"c{index}", severity="critical"))
        ids = self.watch.screen()[1]
        self.assertEqual(len(ids), sim.LIMITS["pendingMax"])
        self.assertNotIn("c0", ids)
        self.assertIn(f"c{sim.LIMITS['pendingMax']}", ids)

    def test_the_lesser_alert_goes_before_a_critical_one(self):
        self.feed(alert("keep", severity="critical"))
        for index in range(sim.LIMITS["pendingMax"]):
            self.feed(alert(f"w{index}", severity="warning"))
        ids = self.watch.screen()[1]
        self.assertIn("keep", ids)
        self.assertEqual(len(ids), sim.LIMITS["pendingMax"])

    def test_an_alert_that_needs_no_confirmation_cannot_be_confirmed(self):
        self.feed(alert("a", needs_ack=False))
        self.assertEqual(self.watch.confirm("a"), "NOTHING_TO_CONFIRM")

    def test_a_confirmation_after_expiry_is_nothing_to_confirm(self):
        self.feed(alert("a", ttl=10))
        self.pair.advance(10)
        self.assertEqual(self.watch.confirm("a"), "NOTHING_TO_CONFIRM")

    def test_a_confirmation_of_an_unknown_alert_sends_nothing(self):
        self.assertEqual(self.watch.confirm("nope"), "NOTHING_TO_CONFIRM")
        self.assertEqual(self.pair.to_phone.arrived, [])

    def test_a_second_tap_after_a_sent_confirmation_sends_nothing(self):
        self.pair.phone.set_option(True)
        self.pair.raise_alarm("a")
        self.assertEqual(self.watch.confirm("a"), "SENT")
        self.assertEqual(self.watch.confirm("a"), "NOTHING_TO_CONFIRM")
        self.assertEqual(len(self.pair.to_phone.arrived), 1)


class PhoneTests(unittest.TestCase):
    def setUp(self):
        self.pair = sim.Pair()
        self.phone = self.pair.phone
        self.phone.set_option(True)

    def test_a_blank_title_or_text_is_not_sent(self):
        self.assertFalse(self.phone.notice("a", "al", "high", "   ", "text"))
        self.assertFalse(self.phone.notice("a", "al", "high", "title", ""))
        self.assertEqual([p for p, _ in self.pair.to_watch.arrived if p == sim.PATH_TO_WATCH and '"alert"' in _], [])

    def test_texts_are_cut_to_the_limits_after_trimming(self):
        self.phone.notice("a", None, "critical", "  " + "t" * 200 + "  ", "x" * 500)
        sent = json.loads(self.pair.to_watch.arrived[-1][1])
        self.assertEqual(len(sent["title"]), sim.LIMITS["titleMax"])
        self.assertEqual(len(sent["text"]), sim.LIMITS["textMax"])

    def test_an_alert_without_an_alarm_needs_no_confirmation(self):
        self.phone.notice("a", None, "high", "T", "x")
        self.assertFalse(json.loads(self.pair.to_watch.arrived[-1][1])["needsAck"])

    def test_the_severity_names_of_the_app_become_the_three_of_the_contract(self):
        for given, wire in (("critical", "critical"), ("high", "warning"), ("medium", "info"), ("low", "info"), ("anything", "info")):
            self.phone.notice("a", None, given, "T", "x")
            self.assertEqual(json.loads(self.pair.to_watch.arrived[-1][1])["severity"], wire)

    def test_an_ack_on_the_wrong_path_is_refused(self):
        self.assertFalse(self.phone.on_watch_message("/other", message("alert_ack", alertId="a")))
        self.assertEqual(self.phone.refusals, ["path"])

    def test_an_ack_with_the_option_off_is_refused(self):
        self.phone.set_option(False)
        self.assertFalse(self.phone.on_watch_message(sim.PATH_TO_PHONE, message("alert_ack", alertId="a")))
        self.assertEqual(self.phone.refusals, ["option off"])

    def test_garbage_and_wrong_types_are_refused_as_shape(self):
        for text in ("", "nope", "[]", alert("a"), message("relay", enabled=True)):
            self.assertFalse(self.phone.on_watch_message(sim.PATH_TO_PHONE, text), text)
        self.assertEqual(set(self.phone.refusals), {"shape"})

    def test_an_ack_of_an_alert_never_sent_is_refused_as_unknown(self):
        self.assertFalse(self.phone.on_watch_message(sim.PATH_TO_PHONE, message("alert_ack", alertId="ghost")))
        self.assertEqual(self.phone.refusals, ["unknown"])

    def test_an_ack_exactly_at_the_end_of_the_time_is_refused_as_expired(self):
        self.pair.raise_alarm("a", severity="high")      # 120 s
        self.pair.advance(120)
        self.assertFalse(self.phone.on_watch_message(sim.PATH_TO_PHONE, message("alert_ack", alertId="a")))
        self.assertEqual(self.phone.refusals, ["expired"])
        self.assertIn("a", self.pair.server.open)

    def test_an_ack_one_second_before_the_end_is_accepted(self):
        self.pair.raise_alarm("a", severity="high")
        self.pair.advance(119)
        self.assertTrue(self.phone.on_watch_message(sim.PATH_TO_PHONE, message("alert_ack", alertId="a")))
        self.assertEqual(self.pair.server.acknowledged, ["a"])

    def test_a_replayed_ack_is_refused_and_the_server_acts_once(self):
        self.pair.raise_alarm("a")
        text = message("alert_ack", alertId="a")
        self.assertTrue(self.phone.on_watch_message(sim.PATH_TO_PHONE, text))
        self.assertFalse(self.phone.on_watch_message(sim.PATH_TO_PHONE, text))
        self.assertEqual(self.pair.server.acknowledged, ["a"])

    def test_an_alarm_closed_on_the_server_meanwhile_is_refused_as_server(self):
        self.pair.raise_alarm("a")
        self.pair.server.open.clear()
        self.assertFalse(self.phone.on_watch_message(sim.PATH_TO_PHONE, message("alert_ack", alertId="a")))
        self.assertEqual(self.phone.refusals, ["server"])

    def test_a_clear_forgets_the_alert_so_a_late_ack_is_unknown(self):
        self.pair.raise_alarm("a")
        self.phone.clear("a")
        self.assertFalse(self.phone.on_watch_message(sim.PATH_TO_PHONE, message("alert_ack", alertId="a")))
        self.assertEqual(self.phone.refusals, ["unknown"])


class ChannelTests(unittest.TestCase):
    def test_a_link_down_loses_every_message_and_says_so(self):
        got = []
        channel = sim.Channel(lambda path, text: got.append(text))
        channel.up = False
        self.assertFalse(channel.send("/p", "a"))
        self.assertEqual((got, channel.lost), ([], [("/p", "a")]))

    def test_dropped_messages_look_sent_to_the_sender(self):
        channel = sim.Channel(lambda path, text: None)
        channel.drop_next = 2
        self.assertTrue(channel.send("/p", "a"))
        self.assertTrue(channel.send("/p", "b"))
        channel.send("/p", "c")
        self.assertEqual([t for _, t in channel.lost], ["a", "b"])
        self.assertEqual([t for _, t in channel.arrived], ["c"])

    def test_a_duplicate_arrives_twice_and_only_once_per_request(self):
        channel = sim.Channel(lambda path, text: None)
        channel.duplicate_next = True
        channel.send("/p", "a")
        channel.send("/p", "b")
        self.assertEqual([t for _, t in channel.arrived], ["a", "a", "b"])

    def test_a_held_burst_is_let_go_in_order_or_reversed(self):
        for reverse, expected in ((False, ["a", "b", "c"]), (True, ["c", "b", "a"])):
            channel = sim.Channel(lambda path, text: None)
            channel.held = True
            for text in "abc":
                channel.send("/p", text)
            self.assertEqual(channel.arrived, [])
            channel.release(reversed_order=reverse)
            self.assertEqual([t for _, t in channel.arrived], expected)
            self.assertFalse(channel.held)

    def test_a_link_that_comes_back_works_again(self):
        channel = sim.Channel(lambda path, text: None)
        channel.up = False
        channel.send("/p", "a")
        channel.up = True
        self.assertTrue(channel.send("/p", "b"))
        self.assertEqual(len(channel.arrived), 1)


class SeededRunTests(unittest.TestCase):
    """A random order of events, always the same one for a seed: whatever happens, the rules hold."""

    def run_events(self, seed, steps=300):
        rng = random.Random(seed)
        pair = sim.Pair()
        ids = [f"al-{n}" for n in range(12)]
        for _ in range(steps):
            action = rng.choice(["on", "off", "alarm", "clear", "confirm", "tick", "drop", "dup", "hold", "release", "down", "up", "restart", "close"])
            alert_id = rng.choice(ids)
            if action == "on":
                pair.phone.set_option(True)
            elif action == "off":
                pair.phone.set_option(False)
            elif action == "alarm":
                pair.raise_alarm(alert_id, rng.choice(["critical", "high", "low"]))
            elif action == "clear":
                pair.phone.clear(alert_id)
            elif action == "confirm":
                pair.watch.confirm(alert_id)
            elif action == "tick":
                pair.advance(rng.randint(1, 90))
            elif action == "drop":
                rng.choice([pair.to_watch, pair.to_phone]).drop_next += 1
            elif action == "dup":
                rng.choice([pair.to_watch, pair.to_phone]).duplicate_next = True
            elif action == "hold":
                rng.choice([pair.to_watch, pair.to_phone]).held = True
            elif action == "release":
                rng.choice([pair.to_watch, pair.to_phone]).release(rng.random() < 0.5)
            elif action == "down":
                rng.choice([pair.to_watch, pair.to_phone]).up = False
            elif action == "up":
                pair.to_watch.up = pair.to_phone.up = True
            elif action == "restart":
                pair.restart_watch()
            elif action == "close":
                pair.server.open.discard(alert_id)
            self.check(pair, seed)
        return pair

    def check(self, pair, seed):
        self.assertLessEqual(len(pair.watch.entries), sim.LIMITS["pendingMax"], seed)
        self.assertEqual(len(pair.server.acknowledged), len(set(pair.server.acknowledged)), seed)
        self.assertTrue(pair.every_message_is_in_the_contract(), seed)
        state, shown = pair.watch.screen()
        self.assertEqual(state == "off", not pair.watch.enabled, seed)
        self.assertEqual(len(shown), len(set(shown)), seed)
        if state != "alerts":
            self.assertEqual(shown, [], seed)

    def test_the_rules_hold_for_twenty_seeds(self):
        for seed in range(20):
            self.run_events(seed)

    def test_the_same_seed_gives_the_same_end_state(self):
        first, second = self.run_events(99), self.run_events(99)
        self.assertEqual(first.server.acknowledged, second.server.acknowledged)
        self.assertEqual(first.watch.screen(), second.watch.screen())

    def test_an_acknowledged_alarm_was_once_open_on_the_server(self):
        for seed in range(20, 30):
            pair = self.run_events(seed)
            for alarm in pair.server.acknowledged:
                self.assertNotIn(alarm, pair.server.open, seed)


if __name__ == "__main__":
    unittest.main()
