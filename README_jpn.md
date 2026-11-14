<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-WATCH banner" width="100%">
</p>

# ⌚ ARMOR-WATCH

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  🇯🇵 <b>日本語</b> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### A.R.M.O.R. の最小ウォッチ：アラーム通知と、タップ 1 回でのアラーム確認

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 or later">
  <img src="https://img.shields.io/badge/Wear OS-Kotlin-7f52ff.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Tests-10%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.0.1-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-scaffolding-FFB020.svg" alt="Maturity">
</p>

---

**正直さのチェック - 今日動いているもの:** メッセージ契約、Kotlin コア、ウォッチのセッションはコンピューター上でテストされ、Wear OS アプリと Android アプリの送信側は CI でビルドされてテストに合格しています。**実機のウォッチやエミュレーターではまだ何も動かしていません。iPhone には対応するものがありません（Wear OS ウォッチとはペアリングできません）。**

---

## 📋 ひと目で

| | |
|---|---|
| バージョン | `0.0.1` |
| 成熟度 | 足場段階 |
| 役割 | A.R.M.O.R. の最小ウォッチクライアント（ファミリー：Clients、親：ARMOR-ANDROID-CONTROL） |
| スタック | Kotlin（JVM コアと Compose の Wear OS アプリ、Gradle）。検査は Python 3.11+ |
| テスト | Python に 10 件（`python -m unittest discover -s tests`）と Kotlin テスト（`./gradlew test`） |
| メッセージ | リレーのスイッチ、アラート、アラートの解除、返信の確認 |
| 動作環境 | JDK 21 のある任意のコンピューター（コア）、スマートフォンとペアリングした Wear OS 3+ ウォッチ（アプリは CI でビルド済み、実行は未） |
| 実機での動作 | まだなし：ウォッチでもエミュレーターでも実行されていません |
| ライセンス | GPL-3.0-or-later |

**移動先：** [概要](#-概要) · [アーキテクチャ](#-アーキテクチャ) · [セキュリティ](#-セキュリティと安全) · [設定](#-設定) · [クイックスタート](#-クイックスタート) · [テスト](#-テスト) · [トラブルシューティング](#-トラブルシューティング) · [構成](#-リポジトリの構成) · [関連](#-関連プロジェクト)

## 🚦 プロジェクトの状況

各行はこのリポジトリの一部分で、信号は今日どこまで進んでいるかを示します。計画ではなく、リポジトリが示せるもの（テスト、ビルド、実行）に基づいて書かれています。

🟢 完了・検証済み · 🟡 動作する（既知の制限あり） · 🟠 一部のみ、または未検証 · 🔴 未実装または動作しない

| 領域 | 状態 | ここでの意味 |
|---|---|---|
| ソフトウェア | 🟡 | Kotlin コアはすべてのメッセージを読み取って検査し、アラートをメモリ上に保持します。Wear OS アプリ（画面と Data Layer サービス）と Android アプリの送信側は実装済みです。 |
| テスト | 🟡 | Python テスト 10 件と Kotlin テストが、契約の同じ受理例・拒否例を実行します。Kotlin テストは CI で動きます。 |
| ハードウェア | 🔴 | ウォッチでもエミュレーターでも実行されていません。アプリと Data Layer は CI でのビルドと単体テストのみです。 |
| ドキュメント | 🟡 | 8 言語、プロトコルの説明、契約が書かれ、最新です。 |

## 🎯 概要

* **最小限のウォッチであり、2 つ目のアプリではありません。** ARMOR-ANDROID-CONTROL と ARMOR-IOS-CONTROL が引き続き完全なクライアントです。ウォッチはスマートフォンが送ると決めたアラームと通知を表示し、タップ 1 回でアラームを確認できるだけです。
* **スマートフォンだけが ARMOR-SERVER と通信します。** ウォッチはセッション、パスワード、鍵を持たず、ウォッチには何も保存しません。表示内容はメモリ上にあり、アプリを閉じると忘れられます。
* **既定ではオフです。** スマートフォンアプリでウォッチのアラート設定をオンにするまで、何もウォッチには届きません。スマートフォンが許可するまで、ウォッチは受信したアラートをすべて破棄します。
* **4 つの小さなメッセージ**（`relay`、`alert`、`alert_clear`、`alert_ack`）。上限と受理例・拒否例は `docs/watch_alert_contract.json` にあります。テキストはスマートフォンが本人の言語で書くので、ウォッチに翻訳するものはありません。
* **独自の音や振動はありません。** システム標準の通知以外は追加しません。

## 🧩 A.R.M.O.R. での役割

ARMOR-WATCH は、スマートフォンアプリの手首側です。[ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL) と [ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL) が [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) とのセッションを保持し、ウォッチはスマートフォンから短いアラートを受け取り、本人が確認したアラームの識別子を返します。スマートフォンがそれを検査して実行します。

## 🏛️ アーキテクチャ

<p align="center">
  <img src="images/architecture.svg" alt="ARMOR-WATCH architecture: the person, the Kotlin core and its contract, the phone app and the server" width="100%">
</p>

## 🔒 セキュリティと安全

| 脅威 | 防御 |
|---|---|
| オプションをオフにした後もアラートを表示し続けるウォッチ | スマートフォンが `enabled: false` の `relay` を送り、ウォッチは表示していた内容を忘れます。受信箱はオフの状態で始まります |
| 偽造または不正な形式のメッセージ | すべてのメッセージを契約（種類、バージョン、上限、厳密な型）に照らして検査し、失敗したものはエラーなしで破棄します。4096 文字を超えるものは読みません |
| セッションが入ったまま盗まれたウォッチ | セッションはありません。ウォッチはセッション、パスワード、鍵を持たず、ストレージにも何も書きません |
| 2 回送られた確認や、存在しないアラームへの確認 | 確認はアラートごとに 1 回だけ、確認を求めていて期限が切れていないアラートに対してのみ送られます。スマートフォンは知らない識別子を無視します |
| 強制 PIN のアラームが手首に表示されること | スマートフォンは決して送りません（スマートフォンアプリのバナーも表示しません） |

## ⚙️ 設定

| 設定 | 既定値 | 内容 |
|---|---|---|
| `titleMax`, `textMax` | 60, 160 | アラートのタイトルと本文の最大文字数 |
| `ttlSeconds` | 5 - 600 | アラートが自動で消えるまでの時間 |
| `pendingMax` | 8 | 同時に保持するアラート数。重大なものは軽いもののために破棄されません |
| `rawMax` | 4096 | これより長いメッセージは読みません |
| `relay.enabled` | false | スマートフォンが設定。true になるまでウォッチは何も表示しません |

## 🚀 クイックスタート

**要件：** 検査には Python 3.11+、Kotlin コアには JDK 21（Gradle ラッパーが Gradle 自体をダウンロードします）。

```powershell
python tools/watch_contract.py                  # the examples of the contract in Python
python -m unittest discover -s tests            # the contract, the Kotlin constants and the protocol notes
./gradlew test                                  # the Kotlin core (WatchProtocol and AlertInbox)
```

メッセージと各側の動作は[プロトコルの説明](docs/PROTOCOL.md)を参照してください。

## 🧪 テスト

```powershell
python -m unittest discover -s tests
./gradlew test
python -I tools/armor_ci_validate.py
```

| テストファイル | テスト数 | 対象 |
|---|---|---|
| `test_watch_contract.py` | 10 | 契約のすべての例、上限、Kotlin の定数、プロトコルの説明 |
| `WatchProtocolTest.kt` | 6 | Kotlin パーサーに対する同じ例、上限、確認 |
| `AlertInboxTest.kt` | 10 | 既定でオフ、順序、置換、期限切れ、解除、確認は 1 回、上限 8 件 |

Kotlin テストは CI で動きます。ウォッチでもエミュレーターでもテストは実行されていません。

## 🩺 トラブルシューティング

| 表示 | 原因 | 対処 |
|---|---|---|
| ウォッチに何も表示されない | スマートフォンアプリでウォッチのアラートがオフ、またはスマートフォンが `enabled: true` の `relay` を送っていません | スマートフォンアプリでオプションをオンにする |
| アラートが勝手に消える | `ttlSeconds` が切れたか、スマートフォンが解除しました | スマートフォンからもう一度発生させる |
| 確認しても何も起きない | アラートが確認を求めていない、期限切れ、または既に 1 回確認済みです | スマートフォンアプリでアラームを確認する |

## 📂 リポジトリの構成

<p align="center">
  <img src="images/repository-structure.svg" alt="Repository structure of ARMOR-WATCH" width="100%">
</p>

<details>
<summary>ツリーのテキスト版</summary>

```text
ARMOR-WATCH/
├── src/
│   ├── main/                               WatchProtocol（メッセージとその検査）と AlertInbox（画面上のアラート）
│   └── test/                               契約と同じ例と受信箱のルール
├── wear/                               Wear OS アプリ：画面、Data Layer サービス、ランタイム
├── tests/                                  Python で検査した契約
├── docs/                                   契約とプロトコル
├── tools/                                  検査と契約
├── gradle/                                 Gradle ラッパー
├── .github/                                CI ワークフローと Issue・Pull Request のテンプレート
├── images/                                 バナー、アイコン、README の 2 つの図
├── armor.project.json                      マニフェスト：バージョン、役割、成熟度、ビルドコマンド
├── CHANGELOG.md                            各バージョンの変更点
├── SECURITY.md SUPPORT.md LICENSE          セキュリティポリシー、サポート、ライセンス
├── CONTRIBUTING.md CODE_OF_CONDUCT.md      貢献方法と行動規範
├── build.gradle.kts settings.gradle.kts    Kotlin のビルド
├── gradle.properties gradlew gradlew.bat   Gradle 設定とラッパースクリプト
└── .gitattributes .gitignore               Git の設定
```

</details>

## 🗺️ ロードマップと既知の制限

**現時点の既知の制限**

* Wear OS アプリ（`wear/`、`-ParmorWear` でビルド）と ARMOR-ANDROID-CONTROL の送信側（既定ではオフ）は CI でビルドされ単体テストに合格していますが、実機のウォッチやエミュレーターでは何も動かしていません。
* iPhone は Wear OS ウォッチとペアリングできないため、ARMOR-IOS-CONTROL には対応するものがありません（Apple Watch なら別のアプリになります）。
* 契約はここで管理されます。Android アプリは同じルールを持ち、同じ例を実行します。

**次のステップ**

* 実機のウォッチで確認する：ペアリングし、オプションをオンにし、テストアラームを発生させ、ウォッチで見て、タップで確認し、ARMOR-SERVER で確認済みになるのを見る。その後オプションをオフにして通知が消えるのを見る。

## 🌍 言語

README とテキストは、英語、スペイン語、フランス語、イタリア語、ドイツ語、中国語、日本語、ヨーロッパポルトガル語で提供されています。

## 🔗 関連プロジェクト

**A.R.M.O.R.**（Autonomous Radar & Multimodal Observation Range）は、独立したリポジトリで構成される境界セキュリティシステムです。各リポジトリは独自のバージョン、テスト、README を持ちます。ファミリーは次のとおりです：

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - メッセージのコントラクト、バリデーター、適合ベクトル、生成された型
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - 3 つのレーダーと独自の Web パネルを備えた ESP32-S3 向けフィールドノードのファームウェア
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - ソーラーインバーターとバッテリーのプロトコル、ゲートウェイノードのメッセージ
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - 電気ノード：メーター、系統の読み取り値のメッセージ、切り替えのルール
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - 警報ノードとパネル：ゾーン、警戒、遅延、サイレン、PIN。サーバーありでもなしでも動作
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - タッチパネル：壁面画面でのシステムの状態、警戒と確認応答、そして音声アシスタントの置き場
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - ローカルネットワーク：そのデバイス、インターネット、そして変化
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - 中央コーディネーター：テレメトリ、警報、デバイス、ソーラーの読み取り値、カメラ
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Web コンソール：カメラ、レーダー、警報、太陽光エネルギー、2D/3D のサイトデザイナー
* **[ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL)** - ライブの 2D/3D レーダーを備えた Android オペレータークライアント
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - iPhone オペレータークライアント
* **ARMOR-WATCH**（このリポジトリ） - 最小のウォッチクライアント：アラーム通知とアラームの確認
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - 判断を説明し、決して作動させない視覚推論ポリシー
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Jetson Orin NX 向けのカメラゾーン、マスク、検出ルール
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - 偽造できない確認つきのオフライン音声インテント
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - 筐体、電子回路、ベンチ受け入れマトリクス
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - デプロイ、CM5 テストベンチ、バックアップ、TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - 再現可能な障害を持つオフラインのテレメトリシミュレーター
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - エコシステム自身のリポジトリを検出、インストール、更新
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - アーキテクチャ、セキュリティのベースライン、機能マトリクス

## 📚 ドキュメント

* [プロトコルの説明](docs/PROTOCOL.md)：スマートフォンとウォッチの通信方法
* [メッセージ契約](docs/watch_alert_contract.json)：メッセージ、上限、受理される例と拒否される例
* [機能マトリクス：何が証明され、何が証明されていないか](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [プロジェクトカタログ：バージョンと、リポジトリ同士の依存関係](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [このリポジトリの変更履歴](CHANGELOG.md)

## 🤝 コントリビュートとコミュニティ

* プルリクエストの前に `python -m unittest discover -s tests` と `python -I tools/armor_ci_validate.py` を実行してください。合格する必要があります。[CONTRIBUTING.md](CONTRIBUTING.md) と[行動規範](CODE_OF_CONDUCT.md)を参照してください。
* README は 8 言語で提供され、ARMOR-DOCS のソースから生成されます。文面の変更はそちらで行います。
* コンポーネント間のメッセージの変更は ARMOR-COMMON から始めます。
* ビルドしただけのものを、ボード上で動いているかのように書かないでください。
* 脆弱性は非公開で報告してください：[SECURITY.md](SECURITY.md)。質問、アイデア、報告：electrohobby3d@gmail.com（[SUPPORT.md](SUPPORT.md)）。

## 👤 作者

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 ライセンス

GPL-3.0-or-later - [LICENSE](LICENSE) を参照してください。
