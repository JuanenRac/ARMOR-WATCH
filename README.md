<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-WATCH banner" width="100%">
</p>

# ⌚ ARMOR-WATCH

<p align="center">
  🇺🇸 <b>English</b> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### The minimal A.R.M.O.R. watch: alarm notices and one tap to confirm an alarm

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 or later">
  <img src="https://img.shields.io/badge/Wear OS-Kotlin-7f52ff.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Tests-124%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.0.3-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-scaffolding-FFB020.svg" alt="Maturity">
</p>

---

**Honesty check - what runs today:** The message contract, the Kotlin and Swift cores and the watch session are tested on a computer, and the Wear OS app, the Apple Watch app and the sending side of the Android and iPhone apps are built and pass their tests in CI; **nothing has run on a real watch or an emulator (only a simulated twin of the phone, the link and the watch has), and the Apple Watch app, built inside the iPhone project, has never been paired with a real iPhone.**

---

## 📋 At a glance

| | |
|---|---|
| Version | `0.0.3` |
| Maturity | Scaffolding |
| Role | The minimal watch client of A.R.M.O.R. (family: Clients, parent: ARMOR-ANDROID-CONTROL) |
| Stack | Kotlin (JVM core and the Wear OS app with Compose, Gradle), Swift (core and the Apple Watch app with SwiftUI); Python 3.11+ for the checks |
| Tests | 124 (72 in Python (`python -m unittest discover -s tests`) and 52 in Kotlin (`./gradlew test`)) |
| Messages | The switch of the relay, an alert, the clear of an alert and the confirmation back |
| Runs on | Any computer with a JDK 21 (Kotlin core) or a Mac (Swift core); a Wear OS 3+ watch or an Apple Watch paired with the phone (apps built in CI, never run) |
| On real hardware | None yet: only a simulated twin has run it; the test on a real watch is pending |
| Simulation and APK | The simulated twin (`tools/watch_simulation.py`) wires the phone, the link with faults and the watch in memory; the Wear OS app (`wear/`) and the Apple Watch app (`watchos/`) live in this one repository, and the release APK of the Wear OS app is signed outside GitHub as `ARMOR-WATCH-release.apk` |
| License | GPL-3.0-or-later |

**Jump to:** [Overview](#-overview) · [Architecture](#-architecture) · [Security](#-security--safety) · [Configuration](#-configuration) · [Quick start](#-quick-start) · [Testing](#-testing) · [Troubleshooting](#-troubleshooting) · [Structure](#-repository-structure) · [Related](#-related-projects)

## 🚦 Project status

Each line is one part of this repository and the light says how far it is today. It is written from what the repository can show (tests, builds, runs), not from what is planned.

🟢 Done and verified · 🟡 Works, with known limits · 🟠 Partly done or not verified yet · 🔴 Not done or not working

| Area | State | What it means here |
|---|---|---|
| Software | 🟡 | The Kotlin and Swift cores read and check every message and keep the alerts in memory; the Wear OS app, the Apple Watch app and the senders in the Android and iPhone apps are written. |
| Tests | 🟡 | 72 Python tests and the Kotlin tests run the same accepted and rejected examples of the contract; the Kotlin tests run in CI. |
| Hardware | 🟠 | No real watch or emulator has run them; a simulated twin (`tools/watch_simulation.py`) runs the whole conversation in memory (evidence: simulator), and the test on a real watch is still pending. |
| Documentation | 🟡 | Eight languages, the protocol notes and the contract are written and up to date. |

## 🎯 Overview

* **A minimal watch, not a second app.** ARMOR-ANDROID-CONTROL and ARMOR-IOS-CONTROL stay the full client; the watch only shows the alarms and notices the phone chooses to send and lets the person confirm an alarm with one tap.
* **The phone is the only side that talks to ARMOR-SERVER.** The watch holds no session, no password and no key, and keeps nothing on the watch: what it shows is in memory and is forgotten when the app closes.
* **Off by default.** Nothing reaches the watch until the person turns on the option of the watch alerts in the phone app; until the phone says so, the watch drops every alert it receives.
* **Four small messages** (`relay`, `alert`, `alert_clear`, `alert_ack`) with limits and accepted and rejected examples in `docs/watch_alert_contract.json`; the phone writes the texts in the language of the person, so the watch has nothing to translate.
* **No sound or vibration of its own:** the watch adds none beyond the standard notification of the system.

## 🧩 Role in A.R.M.O.R.

ARMOR-WATCH is the wrist end of the phone apps. [ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL) and [ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL) keep the session with [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER); the watch receives a short alert from the phone and sends back the identifier of an alarm the person confirmed, which the phone checks and carries out.

## 🏛️ Architecture

<p align="center">
  <img src="images/architecture.svg" alt="ARMOR-WATCH architecture: the person, the Kotlin core and its contract, the phone app and the server" width="100%">
</p>

## 🔒 Security & Safety

| Threat | Defence |
|---|---|
| A watch that still shows alerts after the option is turned off | The phone sends `relay` with `enabled: false` and the watch forgets what it showed; the inbox starts switched off |
| A forged or malformed message | Every message is checked against the contract (type, version, limits, exact kinds) and one that fails is dropped without an error; nothing over 4096 characters is read |
| A stolen watch with a session on it | There is none: the watch holds no session, password or key and writes nothing to storage |
| A confirmation sent twice or for an alarm that is not there | The confirmation goes out once per alert, only for an alert that asks for it and has not expired; the phone ignores an identifier it does not know |
| The alarm of a duress PIN shown on the wrist | The phone never sends it (as the banner of the phone apps never shows it) |

## ⚙️ Configuration

| Setting | Default | What it does |
|---|---|---|
| `titleMax`, `textMax` | 60, 160 | Longest title and text of an alert, in characters |
| `ttlSeconds` | 5 - 600 | How long an alert stays before it disappears by itself |
| `pendingMax` | 8 | Alerts kept at once; a critical one is never dropped for a lesser one |
| `rawMax` | 4096 | A message longer than this is not read |
| `relay.enabled` | false | Set by the phone; the watch shows nothing until it is true |
| `bindSender` | false | Wear OS only; set from the app. On, the watch listens only to the phone that switched the alerts on, until they are switched off; off, any phone in reach may send |

## 🚀 Quick start

**Requirements:** Python 3.11+ for the checks; a JDK 21 for the Kotlin core (the Gradle wrapper downloads Gradle itself).

```powershell
python tools/watch_contract.py                  # the examples of the contract in Python
python -m unittest discover -s tests            # the contract, the Kotlin constants and the protocol notes
./gradlew test                                  # the Kotlin core (WatchProtocol and AlertInbox)
```

See the [protocol notes](docs/PROTOCOL.md) for the messages and what each side does.

## 🧪 Testing

```powershell
python -m unittest discover -s tests
./gradlew test
python -I tools/armor_ci_validate.py
```

| Test file | Tests | What it covers |
|---|---|---|
| `test_watch_contract.py` | 13 | Every example of the contract, the limits, the Kotlin constants and the protocol notes |
| `test_watch_simulation.py` | 16 | The simulated conversation of the phone and the watch with faults on the link, with the same scenarios as the Kotlin ones |
| `WatchProtocolTest.kt` | 6 | The same examples against the Kotlin parser, the limits and the acknowledgement |
| `AlertInboxTest.kt` | 10 | Off by default, order, replacement, expiry, clear, one confirmation, the limit of eight |
| `WatchSessionTest.kt` | 11 | The conversation of the watch against a phone that exists only in memory |
| `SimulatedPairTest.kt` | 14 | The simulated phone, link and watch with faults on purpose, on a clock the test moves |
| `WatchRuntimeTest.kt` | 5 | The shared session of the Wear OS app, the path of the alerts and the button |
| `ArmorWatchCoreTests.swift` | 18 | The Swift core of the Apple Watch app against the same examples of the contract |

The Kotlin tests run in CI; no test has run on a watch or an emulator. The simulated twin (`tools/watch_simulation.py`) runs the whole conversation in memory, with faults on the link.

## 🩺 Troubleshooting

| You see | Because | Do |
|---|---|---|
| Nothing appears on the watch | The option of the watch alerts is off in the phone app, or the phone has not sent `relay` with `enabled: true` | Turn the option on in the phone app |
| An alert disappears by itself | Its `ttlSeconds` ran out or the phone cleared it | Raise it again from the phone |
| The confirmation does nothing | The alert does not ask for one, it expired, or it was already confirmed once | Confirm the alarm in the phone app |

## 📂 Repository Structure

<p align="center">
  <img src="images/repository-structure.svg" alt="Repository structure of ARMOR-WATCH" width="100%">
</p>

<details>
<summary>Text version of the tree</summary>

```text
ARMOR-WATCH/
├── src/
│   ├── main/                               WatchProtocol (the messages and their checks) and AlertInbox (the alerts on the screen)
│   └── test/                               the same examples as the contract, and the rules of the inbox
├── wear/
│   ├── src/                                the screen, the Data Layer service and the runtime
│   └── build.gradle.kts                    the build of the app (only with -ParmorWear)
├── watchos/
│   ├── Sources/                            the Swift core: messages, inbox, session
│   └── App/                                the screen and WatchConnectivity
├── tests/                                  the contract checked in Python
├── docs/                                   the contract and the protocol
├── tools/                                  checks and contract
├── gradle/                                 Gradle wrapper
├── .github/                                CI workflow, issue and pull-request templates
├── images/                                 banner, icon and the two pictures of the README
├── armor.project.json                      manifest: version, role, maturity, build command
├── CHANGELOG.md                            what changed in each version
├── SECURITY.md SUPPORT.md LICENSE          security policy, support and license
├── CONTRIBUTING.md CODE_OF_CONDUCT.md      how to contribute and the code of conduct
├── build.gradle.kts settings.gradle.kts    the Kotlin build
├── gradle.properties gradlew gradlew.bat   Gradle settings and the wrapper scripts
└── .gitattributes .gitignore               Git settings
```

</details>

## 🗺️ Roadmap & Known limits

**Known limits today**

* The Wear OS app (`wear/`, built with `-ParmorWear`), the Apple Watch app (`watchos/`, built only on a Mac) and the senders in ARMOR-ANDROID-CONTROL and ARMOR-IOS-CONTROL (off by default) are built and pass their tests in CI, but nothing has run on a real watch, an emulator or a simulator.
* The Apple Watch app is built inside the iPhone project (embedded in ARMOR-IOS-CONTROL and built for the simulator in CI), but the pairing of the two is still to be tried on real devices; an iPhone does not pair with a Wear OS watch and an Android phone does not pair with an Apple Watch.
* The contract is kept here; the Kotlin core, the Swift core and the phone apps run the same examples.

**Next**

* Prove it on a real watch, one of each kind: pair it, switch the option on, raise a test alarm, see it on the watch, confirm it with a tap and see it confirmed in ARMOR-SERVER; then switch the option off and see the alert disappear.

## 🌍 Languages

README and texts exist in English, Spanish, French, Italian, German, Chinese, Japanese and European Portuguese.

## 🔗 Related Projects

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) is a perimeter-security system made of independent repositories. Each one has its own version, its own tests and its own README; this is the family:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Message contracts, validators, conformance vectors and generated types
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Field-node firmware for ESP32-S3 with three radars and its own web panel
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Solar inverter and battery protocols and the messages of a gateway node
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Electrical node: meters, the message of the network's readings and the rules for switching
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Alarm node and panel: zones, arming, delays, siren and PIN, with the server or without it
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Touch panel: the state of the system on a wall screen, arming and acknowledging, and the home of the voice assistant
* **[ARMOR-NODE-SOLAR-HMI](https://github.com/JuanenRac/ARMOR-NODE-SOLAR-HMI)** - Solar touch screen: the batteries and the inverters as ARMOR-SERVER knows them (early stage: no firmware yet)
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - The local network: its devices, the internet and what changes
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Central coordinator: telemetry, alarms, devices, solar readings and cameras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Web console: cameras, radar, alarms, solar energy and the 2D/3D site designer
* **[ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL)** - Android operator client with a live 2D/3D radar
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - iPhone operator client
* **ARMOR-WATCH** (this repository) - Minimal watch client: alarm notices and alarm confirmation
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Visual inference policy that explains its decisions and never actuates
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Camera zones, masks and detection rules for the Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Offline voice intents with a confirmation that cannot be forged
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Enclosures, electronics and the bench acceptance matrix
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Deployment, the CM5 test bench, backup and TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Offline telemetry simulator with repeatable faults
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Detects, installs and updates the ecosystem's own repositories
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Architecture, security baseline and the capability matrix

## 📚 Documentation

* [Protocol notes](docs/PROTOCOL.md): how the phone and the watch talk
* [Message contract](docs/watch_alert_contract.json): the messages, the limits and the accepted and rejected examples
* [Capability matrix: what is proven and what is not](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Project catalogue: versions and how the repositories depend on each other](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Changelog of this repository](CHANGELOG.md)

## 🤝 Contributing & Community

* Before a pull request run `python -m unittest discover -s tests` and `python -I tools/armor_ci_validate.py`: it must pass. See [CONTRIBUTING.md](CONTRIBUTING.md) and the [code of conduct](CODE_OF_CONDUCT.md).
* The README exists in eight languages and is generated from the sources in ARMOR-DOCS: a change to the text is made there.
* A change to a message between components starts in ARMOR-COMMON.
* Do not describe as working on a board what has only been built.
* Report a vulnerability privately: [SECURITY.md](SECURITY.md). Questions, ideas and reports: electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTHOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LICENSE

GPL-3.0-or-later - see [LICENSE](LICENSE).
