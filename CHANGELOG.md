# Changelog

All notable changes to this project are documented here.

## [Unreleased]

## [0.0.1] - The message contract between the phone and the watch, and the Kotlin core that reads it

- **The message contract between the phone and the watch:** `docs/watch_alert_contract.json` names the four messages (the switch of the relay, an alert, the clear of an alert and the confirmation the watch sends back), their limits and accepted and rejected examples, and `tools/watch_contract.py` runs them in Python. Everything is off until the phone says the option of the watch is on.
- **The Kotlin core of the watch:** `WatchProtocol` reads and checks every message without ever throwing, and `AlertInbox` keeps the alerts in memory only (most serious first, a limit of eight, an alert that expires disappears, the confirmation goes out once), starting switched off. The tests run the same examples as the contract and the rules of the inbox.
- **The repository and its checks:** manifest, licence, security policy, eight-language README, pictures and the CI baseline of the family.
