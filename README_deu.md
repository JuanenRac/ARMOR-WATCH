<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-WATCH-Banner" width="100%">
</p>

# ⌚ ARMOR-WATCH

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  🇩🇪 <b>Deutsch</b> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### Die minimale A.R.M.O.R.-Uhr: Alarmmeldungen und ein Tipp zum Bestätigen eines Alarms

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 oder neuer">
  <img src="https://img.shields.io/badge/Wear OS-Kotlin-7f52ff.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Tests-10%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.0.1-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-scaffolding-FFB020.svg" alt="Reifegrad">
</p>

---

**Ehrlichkeitsprüfung - was heute läuft:** Der Nachrichtenvertrag, der Kotlin-Kern und die Uhr-Sitzung werden auf einem Computer getestet, und die Wear-OS-App und die Sendeseite der Android-App werden in CI gebaut und bestehen ihre Tests; **nichts lief auf einer echten Uhr oder einem Emulator, und das iPhone hat keine Entsprechung (es koppelt nicht mit einer Wear-OS-Uhr).**

---

## 📋 Auf einen Blick

| | |
|---|---|
| Version | `0.0.1` |
| Reifegrad | Gerüst |
| Rolle | Der minimale Uhren-Client von A.R.M.O.R. (Familie: Clients, Eltern: ARMOR-ANDROID-CONTROL) |
| Stack | Kotlin (JVM-Kern und die Wear-OS-App mit Compose, Gradle); Python 3.11+ für die Prüfungen |
| Tests | 10 in Python (`python -m unittest discover -s tests`) und die Kotlin-Tests (`./gradlew test`) |
| Nachrichten | Der Schalter des Relays, ein Alarm, das Löschen eines Alarms und die Bestätigung zurück |
| Läuft auf | Jedem Computer mit JDK 21 (Kern); einer Wear-OS-3+-Uhr, gekoppelt mit dem Telefon (App in CI gebaut, nie ausgeführt) |
| Auf echter Hardware | Noch nicht: keine Uhr und kein Emulator hat es ausgeführt |
| Lizenz | GPL-3.0-or-later |

**Springe zu:** [Überblick](#-überblick) · [Architektur](#-architektur) · [Sicherheit](#-sicherheit-und-schutz) · [Konfiguration](#-konfiguration) · [Schnellstart](#-schnellstart) · [Tests](#-tests) · [Probleme](#-fehlerbehebung) · [Struktur](#-struktur-des-repositorys) · [Verwandte](#-verwandte-projekte)

## 🚦 Projektstatus

Jede Zeile ist ein Teil dieses Repositorys, und die Ampel sagt, wie weit er heute ist. Sie beruht auf dem, was das Repository zeigen kann (Tests, Builds, Läufe), nicht auf dem, was geplant ist.

🟢 Fertig und überprüft · 🟡 Funktioniert, mit bekannten Grenzen · 🟠 Teilweise fertig oder noch nicht überprüft · 🔴 Nicht gemacht oder funktioniert nicht

| Bereich | Stand | Was das hier bedeutet |
|---|---|---|
| Software | 🟡 | Der Kotlin-Kern liest und prüft jede Nachricht und hält die Alarme im Speicher; die Wear-OS-App (Bildschirm und Data-Layer-Dienst) und der Sender in der Android-App sind geschrieben. |
| Tests | 🟡 | 10 Python-Tests und die Kotlin-Tests führen dieselben angenommenen und abgelehnten Beispiele des Vertrags aus; die Kotlin-Tests laufen in der CI. |
| Hardware | 🔴 | Keine Uhr und kein Emulator hat sie ausgeführt; App und Data Layer werden nur in CI gebaut und per Unit-Test geprüft. |
| Dokumentation | 🟡 | Acht Sprachen, die Protokollnotizen und der Vertrag sind geschrieben und aktuell. |

## 🎯 Überblick

* **Eine minimale Uhr, keine zweite App.** ARMOR-ANDROID-CONTROL und ARMOR-IOS-CONTROL bleiben der vollständige Client; die Uhr zeigt nur die Alarme und Hinweise, die das Telefon senden will, und lässt einen Alarm mit einem Tipp bestätigen.
* **Nur das Telefon spricht mit ARMOR-SERVER.** Die Uhr hält keine Sitzung, kein Passwort und keinen Schlüssel und speichert nichts auf der Uhr: Was sie zeigt, liegt im Speicher und wird beim Schließen der App vergessen.
* **Standardmäßig aus.** Nichts erreicht die Uhr, bis die Person die Option der Uhr-Alarme in der Telefon-App einschaltet; solange das Telefon es nicht sagt, verwirft die Uhr jeden empfangenen Alarm.
* **Vier kleine Nachrichten** (`relay`, `alert`, `alert_clear`, `alert_ack`) mit Grenzen sowie angenommenen und abgelehnten Beispielen in `docs/watch_alert_contract.json`; das Telefon schreibt die Texte in der Sprache der Person, die Uhr muss nichts übersetzen.
* **Kein eigener Ton und keine eigene Vibration:** Die Uhr fügt über die Standardbenachrichtigung des Systems hinaus nichts hinzu.

## 🧩 Rolle in A.R.M.O.R.

ARMOR-WATCH ist das Handgelenk-Ende der Telefon-Apps. [ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL) und [ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL) halten die Sitzung mit [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER); die Uhr empfängt vom Telefon einen kurzen Alarm und sendet die Kennung eines bestätigten Alarms zurück, die das Telefon prüft und ausführt.

## 🏛️ Architektur

<p align="center">
  <img src="images/architecture.svg" alt="ARMOR-WATCH architecture: the person, the Kotlin core and its contract, the phone app and the server" width="100%">
</p>

## 🔒 Sicherheit und Schutz

| Bedrohung | Abwehr |
|---|---|
| Eine Uhr, die nach dem Ausschalten der Option noch Alarme zeigt | Das Telefon sendet `relay` mit `enabled: false` und die Uhr vergisst, was sie zeigte; der Posteingang startet ausgeschaltet |
| Eine gefälschte oder fehlerhafte Nachricht | Jede Nachricht wird gegen den Vertrag geprüft (Typ, Version, Grenzen, genaue Typen) und eine, die durchfällt, wird ohne Fehler verworfen; über 4096 Zeichen wird nichts gelesen |
| Eine gestohlene Uhr mit einer Sitzung darauf | Es gibt keine: Die Uhr hält weder Sitzung noch Passwort oder Schlüssel und schreibt nichts in den Speicher |
| Eine doppelt gesendete Bestätigung oder für einen nicht vorhandenen Alarm | Die Bestätigung geht einmal pro Alarm hinaus, nur für einen Alarm, der sie verlangt und nicht abgelaufen ist; das Telefon ignoriert eine unbekannte Kennung |
| Der Alarm einer Zwangs-PIN am Handgelenk | Das Telefon sendet ihn nie (wie das Banner der Telefon-Apps ihn nie zeigt) |

## ⚙️ Konfiguration

| Einstellung | Standard | Was sie tut |
|---|---|---|
| `titleMax`, `textMax` | 60, 160 | Längster Titel und Text eines Alarms in Zeichen |
| `ttlSeconds` | 5 - 600 | Wie lange ein Alarm bleibt, bevor er von selbst verschwindet |
| `pendingMax` | 8 | Gleichzeitig gehaltene Alarme; ein kritischer wird nie für einen geringeren verworfen |
| `rawMax` | 4096 | Eine längere Nachricht wird nicht gelesen |
| `relay.enabled` | false | Vom Telefon gesetzt; die Uhr zeigt nichts, bis er wahr ist |

## 🚀 Schnellstart

**Voraussetzungen:** Python 3.11+ für die Prüfungen; ein JDK 21 für den Kotlin-Kern (der Gradle-Wrapper lädt Gradle selbst herunter).

```powershell
python tools/watch_contract.py                  # the examples of the contract in Python
python -m unittest discover -s tests            # the contract, the Kotlin constants and the protocol notes
./gradlew test                                  # the Kotlin core (WatchProtocol and AlertInbox)
```

Die Nachrichten und was jede Seite tut, stehen in den [Protokollnotizen](docs/PROTOCOL.md).

## 🧪 Tests

```powershell
python -m unittest discover -s tests
./gradlew test
python -I tools/armor_ci_validate.py
```

| Testdatei | Tests | Was sie abdeckt |
|---|---|---|
| `test_watch_contract.py` | 10 | Jedes Beispiel des Vertrags, die Grenzen, die Kotlin-Konstanten und die Protokollnotizen |
| `WatchProtocolTest.kt` | 6 | Dieselben Beispiele gegen den Kotlin-Parser, die Grenzen und die Bestätigung |
| `AlertInboxTest.kt` | 10 | Standardmäßig aus, Reihenfolge, Ersetzen, Ablauf, Löschen, eine Bestätigung, das Limit von acht |

Die Kotlin-Tests laufen in der CI; kein Test lief auf einer Uhr oder einem Emulator.

## 🩺 Fehlerbehebung

| Du siehst | Weil | Tu |
|---|---|---|
| Auf der Uhr erscheint nichts | Die Option der Uhr-Alarme ist in der Telefon-App aus, oder das Telefon hat kein `relay` mit `enabled: true` gesendet | Schalte die Option in der Telefon-App ein |
| Ein Alarm verschwindet von selbst | Sein `ttlSeconds` ist abgelaufen oder das Telefon hat ihn gelöscht | Löse ihn vom Telefon aus erneut aus |
| Die Bestätigung bewirkt nichts | Der Alarm verlangt keine, er ist abgelaufen oder wurde schon einmal bestätigt | Bestätige den Alarm in der Telefon-App |

## 📂 Struktur des Repositorys

<p align="center">
  <img src="images/repository-structure.svg" alt="Repository structure of ARMOR-WATCH" width="100%">
</p>

<details>
<summary>Textversion des Baums</summary>

```text
ARMOR-WATCH/
├── src/
│   ├── main/                               WatchProtocol (die Nachrichten und ihre Prüfungen) und AlertInbox (die Alarme auf dem Bildschirm)
│   └── test/                               dieselben Beispiele wie der Vertrag und die Regeln des Posteingangs
├── wear/
│   ├── src/                                der Bildschirm, der Data-Layer-Dienst und die Laufzeit
│   └── build.gradle.kts                    der Build der App (nur mit -ParmorWear)
├── watchos/
│   ├── Sources/                            der Swift-Kern: Nachrichten, Posteingang, Sitzung
│   └── App/                                der Bildschirm und WatchConnectivity
├── tests/                                  der Vertrag in Python geprüft
├── docs/                                   der Vertrag und das Protokoll
├── tools/                                  Prüfungen, Vertrag
├── gradle/                                 Gradle-Wrapper
├── .github/                                CI-Workflow sowie Vorlagen für Issues und Pull Requests
├── images/                                 Banner, Symbol und die zwei Bilder des READMEs
├── armor.project.json                      Manifest: Version, Rolle, Reifegrad, Build-Befehl
├── CHANGELOG.md                            was sich in jeder Version geändert hat
├── SECURITY.md SUPPORT.md LICENSE          Sicherheitsrichtlinie, Support und Lizenz
├── CONTRIBUTING.md CODE_OF_CONDUCT.md      wie man beiträgt und der Verhaltenskodex
├── build.gradle.kts settings.gradle.kts    der Kotlin-Build
├── gradle.properties gradlew gradlew.bat   Gradle-Einstellungen und Wrapper-Skripte
└── .gitattributes .gitignore               Git-Einstellungen
```

</details>

## 🗺️ Roadmap und bekannte Grenzen

**Bekannte Grenzen heute**

* Die Wear-OS-App (`wear/`, gebaut mit `-ParmorWear`) und der Sender in ARMOR-ANDROID-CONTROL (standardmäßig aus) werden in CI gebaut und bestehen ihre Unit-Tests, aber nichts lief auf einer echten Uhr oder einem Emulator.
* Ein iPhone koppelt nicht mit einer Wear-OS-Uhr, daher hat ARMOR-IOS-CONTROL keine Entsprechung (eine Apple Watch wäre eine andere App).
* Der Vertrag liegt hier; die Android-App enthält dieselben Regeln und führt dieselben Beispiele aus.

**Als Nächstes**

* Auf einer echten Uhr beweisen: koppeln, die Option einschalten, einen Testalarm auslösen, ihn auf der Uhr sehen, mit einem Tipp bestätigen und die Bestätigung in ARMOR-SERVER sehen; dann die Option ausschalten und die Meldung verschwinden sehen.

## 🌍 Sprachen

README und Texte gibt es auf Englisch, Spanisch, Französisch, Italienisch, Deutsch, Chinesisch, Japanisch und europäischem Portugiesisch.

## 🔗 Verwandte Projekte

**A.R.M.O.R.** (Autonomous Radar & Multimodal Observation Range) ist ein Perimeter-Sicherheitssystem aus unabhängigen Repositorys. Jedes hat eine eigene Version, eigene Tests und ein eigenes README; hier ist die Familie:

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - Nachrichtenverträge, Validierer, Konformitätsvektoren und generierte Typen
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - Feldknoten-Firmware für ESP32-S3 mit drei Radaren und eigenem Web-Panel
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - Protokolle für Solar-Wechselrichter und -Batterien und die Nachrichten eines Gateway-Knotens
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - Elektroknoten: Zähler, die Nachricht der Netzmesswerte und die Regeln fürs Schalten
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - Alarmknoten und Alarmzentrale: Zonen, Scharfschalten, Verzögerungen, Sirene und PIN, mit dem Server oder ohne ihn
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - Touch-Panel: der Systemzustand auf einem Wandbildschirm, Scharf- und Quittieren sowie das Zuhause des Sprachassistenten
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - Das lokale Netzwerk: seine Geräte, das Internet und was sich ändert
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - Zentraler Koordinator: Telemetrie, Alarme, Geräte, Solarmesswerte und Kameras
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Web-Konsole: Kameras, Radar, Alarme, Solarenergie und 2D/3D-Standortdesigner
* **[ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL)** - Android-Bedienclient mit Live-Radar in 2D/3D
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - iPhone-Bedienclient
* **ARMOR-WATCH** (dieses Repository) - Minimaler Uhren-Client: Alarmmeldungen und Alarmbestätigung
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - Visuelle Inferenzrichtlinie, die ihre Entscheidungen erklärt und nie handelt
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Kamerazonen, Masken und Erkennungsregeln für den Jetson Orin NX
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - Offline-Sprachabsichten mit einer nicht fälschbaren Bestätigung
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - Gehäuse, Elektronik und die Abnahmematrix am Prüfstand
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - Bereitstellung, CM5-Prüfstand, Backup und TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - Offline-Telemetriesimulator mit wiederholbaren Fehlern
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - Erkennt, installiert und aktualisiert die eigenen Repositorys des Ökosystems
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - Architektur, Sicherheitsgrundlage und die Fähigkeitsmatrix

## 📚 Dokumentation

* [Protokollnotizen](docs/PROTOCOL.md): wie Telefon und Uhr miteinander sprechen
* [Nachrichtenvertrag](docs/watch_alert_contract.json): die Nachrichten, die Grenzen und die angenommenen und abgelehnten Beispiele
* [Fähigkeitsmatrix: was belegt ist und was nicht](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [Projektkatalog: Versionen und wie die Repositorys voneinander abhängen](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [Änderungsverlauf dieses Repositorys](CHANGELOG.md)

## 🤝 Mitwirken und Community

* Führe vor einem Pull Request `python -m unittest discover -s tests` und `python -I tools/armor_ci_validate.py` aus: Es muss bestehen. Siehe [CONTRIBUTING.md](CONTRIBUTING.md) und den [Verhaltenskodex](CODE_OF_CONDUCT.md).
* Das README gibt es in acht Sprachen, und es wird aus den Quellen in ARMOR-DOCS erzeugt: Eine Textänderung erfolgt dort.
* Eine Änderung an einer Nachricht zwischen Komponenten beginnt in ARMOR-COMMON.
* Beschreibe nichts als auf einer Platine funktionierend, was nur gebaut wurde.
* Melde eine Schwachstelle privat: [SECURITY.md](SECURITY.md). Fragen, Ideen und Meldungen: electrohobby3d@gmail.com ([SUPPORT.md](SUPPORT.md)).

## 👤 AUTOR

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 LIZENZ

GPL-3.0-or-later - siehe [LICENSE](LICENSE).
