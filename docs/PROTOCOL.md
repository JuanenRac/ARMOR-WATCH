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

The conversation of the watch end is written without Android and tried against a phone that exists only in memory (`WatchSession`, `WatchScreen`, `SimulatedPhone`; evidence: simulator). The Wear OS screen that draws `WatchScreen`, the Data Layer service (paths `/armor/alert` to the watch and `/armor/alert/ack` to the phone) that calls `WatchSession.onPhoneMessage` and the sending side in the phone apps are not written. The proof on a real watch: pair a Wear OS watch with the phone, switch the option on, raise a test alarm, see it on the watch, confirm it with a tap and see the alarm confirmed in ARMOR-SERVER; then switch the option off and see the alert disappear.
