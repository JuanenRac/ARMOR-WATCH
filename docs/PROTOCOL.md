# Messages between the phone and the watch

The phone is the only side that talks to ARMOR-SERVER. The watch has no session, no password and no key: it shows what the phone sends and sends back one thing, the identifier of the alarm the person confirmed. The file `watch_alert_contract.json` is the contract; the phone apps keep an identical copy and every side runs its examples.

## Off by default

Nothing is shown on the watch until the person turns on the option of the watch alerts in the phone app. While it is off the phone sends nothing, and the watch drops any alert it receives anyway. When the option is switched off again the phone sends `relay` with `enabled: false` and the watch forgets what it was showing.

## Messages

| Type | From | Carries |
|---|---|---|
| `relay` | phone | `enabled`: whether the watch may show alerts |
| `alert` | phone | `alertId`, `severity` (`info`, `warning`, `critical`), `title`, `text`, `needsAck`, `ttlSeconds` |
| `alert_clear` | phone | `alertId`: the alert is over (confirmed elsewhere, expired or cancelled) |
| `alert_ack` | watch | `alertId`: the person confirmed the alarm on the watch |

Every message also carries `protocolVersion` (1). The phone writes `title` and `text` in the language of the person, so the watch has no texts to translate. The limits (`rawMax`, `titleMax`, `textMax`, `ttlSeconds`, `pendingMax`) are in the contract and the Kotlin constants are tested against them.

## What the watch does

* Keeps up to eight alerts in memory, the most serious first; a critical alert is never dropped to make room for a lesser one.
* Removes an alert when its `ttlSeconds` run out or the phone clears it.
* Lets the person confirm an alert with `needsAck`; the confirmation goes out once. The alert stays, marked, until the phone clears it.
* Adds no sound or vibration of its own beyond the standard notification of the system, and writes nothing to storage.

## What the phone does

* Sends only when the option is on, and never sends the alarm of a duress PIN.
* Checks the `alert_ack` against the alarm and carries it out with its own session; an identifier it does not know is ignored.
* Sends `alert_clear` when the alarm is confirmed somewhere else.

## Not tested yet

The conversation of the watch end is written without Android and tried against a phone that exists only in memory (`WatchSession`, `WatchScreen`, `SimulatedPhone`; evidence: simulator). The Wear OS app (`wear/`: a Compose for Wear OS screen that draws `WatchScreen`, and the Data Layer service, paths `/armor/alert` to the watch and `/armor/alert/ack` to the phone, that calls `WatchSession.onPhoneMessage`) is written, compiles and passes its unit tests in GitHub Actions (`wear-build.yml`), but it has never run on a watch. The sending side is in the phone apps and off by default: ARMOR-ANDROID-CONTROL for Wear OS and ARMOR-IOS-CONTROL for the Apple Watch.

## Trying the whole conversation without a watch

`src/main/kotlin/armor/watch/Simulation.kt` (and its Python twin, `tools/watch_simulation.py`) put a phone, a watch, the link between them and a server in memory (evidence: simulator):

* `SimulatedChannel` stands for the Data Layer and for WatchConnectivity, with faults on purpose: out of range (the sender is told), a message lost on the way (the sender is not told), a duplicate, and a burst held up and let go in the wrong order.
* `SimulatedPhoneRelay` is the sending side of the two phone apps: the switch, the alerts (severity, texts cut to the limits, time to live), the clear, and the rule that a confirmation is acted on only for an alert the phone sent, once, before it expires, and only while the option is on.
* `SimulatedAlarmServer` is the part of ARMOR-SERVER that the confirmation reaches: an alarm is acknowledged once, or refused (unknown alarm, server down).
* `SimulatedPair` wires them on a clock that only the test moves; `restartWatch()` makes a watch that starts again with nothing in memory.

`SimulatedPairTest` (Kotlin, 14 scenarios) and `tests/test_watch_simulation.py` (the same scenarios in Python, with every message that crosses the link checked against the contract) run them; a test makes sure each Kotlin scenario has its Python twin. `python tools/watch_simulation.py` prints `WATCH_SIMULATION=PASS` after one alarm goes from the phone to the watch and back to the server.

Still to be proved on real hardware: the Data Layer and WatchConnectivity themselves (delivery, latency and what a real watch out of range does), the pairing of a real watch with a real phone, the notification of the system, and the screens on a watch or an emulator. The exact test: pair a watch with a phone running the app, switch the option on, raise an alarm on the server, and check that the alert shows, that the tap acknowledges the alarm once, and that walking out of range makes the button wait.

## Wear OS and Apple Watch: what is the same and what the system changes

* **The same:** the contract (`docs/watch_alert_contract.json`), the four messages, the limits, the rules (off until the phone says otherwise, memory only, one confirmation, the phone acts with its own session) and the words in the eight languages. The Kotlin core (`src/`) and the Swift core (`watchos/Sources/`) run the same examples.
* **The carrier:** Wear OS uses the Data Layer (`MessageClient`, the paths `/armor/alert` and `/armor/alert/ack`); the Apple Watch uses WatchConnectivity (`sendMessage` with the keys `path` and `text`). Neither queues an alert that was not delivered live, so a watch that comes back in reach does not show an old alert.
* **The app:** Wear OS is a Gradle project (Compose for Wear OS, built with `-ParmorWear`); the Apple Watch app is a Swift package plus a SwiftUI target described for XcodeGen and built only on a Mac, both on its own (`watchos/project.yml`) and inside the Xcode project of the iPhone app (the target `ArmorWatch` of `ARMOR-IOS-CONTROL/project.yml`, embedded in the app, which reads these sources from a copy of this repository checked out next to it). The iPhone does not talk to a Wear OS watch and the Android phone does not talk to an Apple Watch.
* **The clock:** the Kotlin core reads the elapsed time of the system, the Swift one the uptime; both only count seconds forward.
* **What is not proved on either:** nothing has run on a real watch. The Apple Watch app is built for the simulator and its core tested on a Mac in GitHub Actions; the app is built inside the iPhone app too (embedded in the Xcode project of ARMOR-IOS-CONTROL, built for the simulator in GitHub Actions), but the pairing of the two has never been tried on a real iPhone and a real watch. The proof on a real watch: pair a Wear OS watch with the phone, switch the option on, raise a test alarm, see it on the watch, confirm it with a tap and see the alarm confirmed in ARMOR-SERVER; then switch the option off and see the alert disappear.
