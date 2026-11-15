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
  🇨🇳 <b>简体中文</b> |
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### A.R.M.O.R. 的极简手表：警报通知，轻点一下确认警报

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 or later">
  <img src="https://img.shields.io/badge/Wear OS-Kotlin-7f52ff.svg" alt="Wear OS">
  <img src="https://img.shields.io/badge/Tests-29%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.0.2-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-scaffolding-FFB020.svg" alt="Maturity">
</p>

---

**诚实性检查 - 今天真正能运行的部分：** 消息契约、Kotlin 和 Swift 核心以及手表会话已在电脑上测试，Wear OS 应用、Apple Watch 应用以及 Android 和 iPhone 应用的发送端在 CI 中构建并通过测试；**尚未在真实手表或模拟器上运行（只有手机、链路和手表的模拟孪生体运行过），构建在 iPhone 工程内的 Apple Watch 应用也从未与真实 iPhone 配对。**

---

## 📋 速览

| | |
|---|---|
| 版本 | `0.0.2` |
| 成熟度 | 雏形 |
| 角色 | A.R.M.O.R. 的极简手表客户端（家族：Clients，上级：ARMOR-ANDROID-CONTROL） |
| 技术栈 | Kotlin（JVM 核心和使用 Compose 的 Wear OS 应用，Gradle），Swift（核心和使用 SwiftUI 的 Apple Watch 应用）；检查用 Python 3.11+ |
| 测试 | Python 中 29 个（`python -m unittest discover -s tests`）和 Kotlin 测试（`./gradlew test`） |
| 消息 | 转发开关、一条警报、警报的清除以及回传的确认 |
| 运行于 | 任何带 JDK 21 的电脑（Kotlin 核心）或 Mac（Swift 核心）；与手机配对的 Wear OS 3+ 手表或 Apple Watch（应用已在 CI 中构建，尚未运行） |
| 真实硬件 | 尚无：只有模拟孪生体运行过；真实手表上的测试待进行 |
| 模拟与 APK | 模拟孪生体（`tools/watch_simulation.py`）在内存中连接手机、带故障的链路和手表；Wear OS 应用（`wear/`）和 Apple Watch 应用（`watchos/`）在同一个仓库中，Wear OS 的发布 APK 在 GitHub 之外签名，命名为 `ARMOR-WATCH-release.apk` |
| 许可证 | GPL-3.0-or-later |

**跳转到：** [概述](#-概述) · [架构](#-架构) · [安全](#-安全与防护) · [配置](#-配置) · [快速开始](#-快速开始) · [测试](#-测试) · [故障排除](#-故障排除) · [结构](#-仓库结构) · [相关项目](#-相关项目)

## 🚦 项目状态

每一行是本仓库的一个部分，灯号表示它今天做到了哪一步。内容依据仓库能够展示的证据（测试、构建、运行），而不是计划。

🟢 已完成并验证 · 🟡 可用，有已知限制 · 🟠 部分完成或尚未验证 · 🔴 未完成或不可用

| 领域 | 状态 | 在这里的含义 |
|---|---|---|
| 软件 | 🟡 | Kotlin 和 Swift 核心读取并检查每条消息，并把警报保存在内存中；Wear OS 应用、Apple Watch 应用以及 Android 和 iPhone 应用中的发送端已编写。 |
| 测试 | 🟡 | 29 个 Python 测试和 Kotlin 测试运行契约中相同的被接受和被拒绝示例；Kotlin 测试在 CI 中运行。 |
| 硬件 | 🟠 | 没有真实手表或模拟器运行过它们；模拟孪生体（`tools/watch_simulation.py`）在内存中运行整个对话（证据：模拟器），真实手表上的测试仍待进行。 |
| 文档 | 🟡 | 八种语言、协议说明和契约均已编写并保持最新。 |

## 🎯 概述

* **极简手表，而不是第二个应用。** ARMOR-ANDROID-CONTROL 和 ARMOR-IOS-CONTROL 仍是完整客户端；手表只显示手机选择发送的警报和通知，并允许轻点一下确认警报。
* **只有手机与 ARMOR-SERVER 通信。** 手表不持有会话、密码或密钥，也不在手表上保存任何内容：它显示的内容在内存中，关闭应用后即被遗忘。
* **默认关闭。** 在手机应用中打开手表警报选项之前，手表什么也收不到；手机未允许时，手表会丢弃收到的所有警报。
* **四条小消息**（`relay`、`alert`、`alert_clear`、`alert_ack`），限制及被接受和被拒绝的示例见 `docs/watch_alert_contract.json`；文本由手机按用户的语言写好，手表无需翻译。
* **没有自己的声音或振动：** 除系统标准通知外，手表不添加任何内容。

## 🧩 在 A.R.M.O.R. 中的角色

ARMOR-WATCH 是手机应用在手腕上的一端。[ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL) 和 [ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL) 与 [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) 保持会话；手表从手机接收简短警报，并回传用户已确认警报的标识符，由手机检查并执行。

## 🏛️ 架构

<p align="center">
  <img src="images/architecture.svg" alt="ARMOR-WATCH architecture: the person, the Kotlin core and its contract, the phone app and the server" width="100%">
</p>

## 🔒 安全与防护

| 威胁 | 防御 |
|---|---|
| 关闭选项后仍显示警报的手表 | 手机发送 `enabled: false` 的 `relay`，手表忘记已显示的内容；收件箱默认处于关闭状态 |
| 伪造或格式错误的消息 | 每条消息都会对照契约（类型、版本、限制、精确的类型）检查，不通过的会被无错误地丢弃；超过 4096 个字符的内容不会被读取 |
| 带着会话被偷走的手表 | 没有会话：手表不持有会话、密码或密钥，也不向存储写入任何内容 |
| 发送两次的确认，或针对不存在的警报的确认 | 每条警报只发出一次确认，且仅限要求确认且未过期的警报；手机会忽略它不认识的标识符 |
| 胁迫 PIN 的警报显示在手腕上 | 手机从不发送它（就像手机应用的横幅从不显示它一样） |

## ⚙️ 配置

| 设置 | 默认值 | 作用 |
|---|---|---|
| `titleMax`, `textMax` | 60, 160 | 警报标题和正文的最大字符数 |
| `ttlSeconds` | 5 - 600 | 警报在自动消失前保留多久 |
| `pendingMax` | 8 | 同时保留的警报数；严重警报不会为较轻的警报而被丢弃 |
| `rawMax` | 4096 | 更长的消息不会被读取 |
| `relay.enabled` | false | 由手机设置；在其为 true 之前手表不显示任何内容 |

## 🚀 快速开始

**要求：** 检查需要 Python 3.11+；Kotlin 核心需要 JDK 21（Gradle 包装器会自行下载 Gradle）。

```powershell
python tools/watch_contract.py                  # the examples of the contract in Python
python -m unittest discover -s tests            # the contract, the Kotlin constants and the protocol notes
./gradlew test                                  # the Kotlin core (WatchProtocol and AlertInbox)
```

消息以及双方的行为见[协议说明](docs/PROTOCOL.md)。

## 🧪 测试

```powershell
python -m unittest discover -s tests
./gradlew test
python -I tools/armor_ci_validate.py
```

| 测试文件 | 测试数 | 覆盖内容 |
|---|---|---|
| `test_watch_contract.py` | 13 | 契约的每个示例、限制、Kotlin 常量和协议说明 |
| `test_watch_simulation.py` | 16 | 手机与手表之间带链路故障的模拟对话（与 Kotlin 相同的场景） |
| `WatchProtocolTest.kt` | 6 | 针对 Kotlin 解析器的相同示例、限制和确认 |
| `AlertInboxTest.kt` | 10 | 默认关闭、顺序、替换、过期、清除、仅一次确认、八条上限 |
| `WatchSessionTest.kt` | 11 | 手表一端与仅存在于内存中的手机之间的对话 |
| `SimulatedPairTest.kt` | 14 | 带有意设置故障的模拟手机、链路和手表，运行在由测试推进的时钟上 |
| `WatchRuntimeTest.kt` | 5 | Wear OS 应用的共享会话、警报路径和按钮 |
| `ArmorWatchCoreTests.swift` | 18 | Apple Watch 应用的 Swift 核心,对照契约的同一批示例 |

Kotlin 测试在 CI 中运行；没有任何测试在手表或模拟器上运行过。模拟孪生体（`tools/watch_simulation.py`）在内存中运行整个对话，并在链路上注入故障。

## 🩺 故障排除

| 你看到 | 原因 | 处理 |
|---|---|---|
| 手表上什么也没有出现 | 手机应用中手表警报选项已关闭，或手机未发送 `enabled: true` 的 `relay` | 在手机应用中打开该选项 |
| 警报自己消失了 | 它的 `ttlSeconds` 已用完，或手机已将其清除 | 从手机重新触发 |
| 确认没有任何反应 | 该警报不要求确认、已过期，或已确认过一次 | 在手机应用中确认警报 |

## 📂 仓库结构

<p align="center">
  <img src="images/repository-structure.svg" alt="Repository structure of ARMOR-WATCH" width="100%">
</p>

<details>
<summary>目录树的文本版本</summary>

```text
ARMOR-WATCH/
├── src/
│   ├── main/                               WatchProtocol（消息及其检查）和 AlertInbox（屏幕上的警报）
│   └── test/                               与契约相同的示例以及收件箱的规则
├── wear/                               Wear OS 应用：界面、Data Layer 服务和运行时
├── watchos/                            Apple Watch 应用：Swift 核心、界面和 WatchConnectivity
├── tests/                                  用 Python 检查的契约
├── docs/                                   契约与协议
├── tools/                                  检查与契约
├── gradle/                                 Gradle 包装器
├── .github/                                CI 工作流以及 Issue 和 Pull Request 模板
├── images/                                 横幅图、图标和 README 的两幅图
├── armor.project.json                      清单：版本、角色、成熟度、构建命令
├── CHANGELOG.md                            每个版本的变化
├── SECURITY.md SUPPORT.md LICENSE          安全策略、支持和许可证
├── CONTRIBUTING.md CODE_OF_CONDUCT.md      如何贡献及行为准则
├── build.gradle.kts settings.gradle.kts    Kotlin 构建
├── gradle.properties gradlew gradlew.bat   Gradle 设置和包装器脚本
└── .gitattributes .gitignore               Git 设置
```

</details>

## 🗺️ 路线图与已知限制

**当前已知限制**

* Wear OS 应用（`wear/`，用 `-ParmorWear` 构建）、Apple Watch 应用（`watchos/`，只在 Mac 上构建）以及 ARMOR-ANDROID-CONTROL 和 ARMOR-IOS-CONTROL 的发送端（默认关闭）在 CI 中构建并通过单元测试，但尚未在真实手表、模拟器或仿真器上运行。
* Apple Watch 应用已构建在 iPhone 工程内（嵌入 ARMOR-IOS-CONTROL，并在 CI 中针对模拟器构建），但两者的配对尚待在真实设备上试验；iPhone 不能与 Wear OS 手表配对，Android 手机也不能与 Apple Watch 配对。
* 契约保存在这里；Kotlin 核心、Swift 核心和手机应用运行相同的示例。

**下一步**

* 在每种真实手表上验证：配对，打开选项，触发测试警报，在手表上看到它，轻点确认，并在 ARMOR-SERVER 中看到已确认；然后关闭选项，看到提醒消失。

## 🌍 语言

README 和文本提供英语、西班牙语、法语、意大利语、德语、汉语、日语和欧洲葡萄牙语版本。

## 🔗 相关项目

**A.R.M.O.R.**（Autonomous Radar & Multimodal Observation Range）是一个由相互独立的仓库组成的周界安全系统。每个仓库都有自己的版本、自己的测试和自己的 README；这是整个系列：

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - 消息契约、验证器、一致性向量和生成的类型
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - 面向 ESP32-S3 的现场节点固件，配三个雷达和自带的网页面板
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - 太阳能逆变器和电池的协议，以及网关节点的消息
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - 电气节点：电表、电网读数的消息和切换规则
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - 报警节点与面板：防区、布防、延时、警笛和 PIN，可配合服务器也可独立使用
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - 触摸面板：壁挂屏幕上的系统状态、布防与确认，以及语音助手的所在
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - 本地网络：其设备、互联网以及变化
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - 中央协调器：遥测、报警、设备、太阳能读数和摄像头
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - 网页控制台：摄像头、雷达、报警、太阳能以及 2D/3D 场地设计器
* **[ARMOR-ANDROID-CONTROL](https://github.com/JuanenRac/ARMOR-ANDROID-CONTROL)** - 带实时 2D/3D 雷达的 Android 操作员客户端
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - iPhone 操作员客户端
* **ARMOR-WATCH**（本仓库） - 极简手表客户端：警报通知与警报确认
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - 视觉推理策略，会解释其决策且从不驱动设备
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - 面向 Jetson Orin NX 的摄像头区域、遮罩和检测规则
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - 离线语音意图，附带无法伪造的确认
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - 外壳、电子器件和台架验收矩阵
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - 部署、CM5 测试台、备份和 TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - 带可重复故障的离线遥测模拟器
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - 检测、安装和更新生态系统自身的仓库
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - 架构、安全基线和能力矩阵

## 📚 文档

* [协议说明](docs/PROTOCOL.md)：手机与手表如何通信
* [消息契约](docs/watch_alert_contract.json)：消息、限制以及被接受和被拒绝的示例
* [能力矩阵：哪些已被证明，哪些没有](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [项目目录：版本以及各仓库之间如何相互依赖](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [本仓库的更新日志](CHANGELOG.md)

## 🤝 参与贡献与社区

* 提交 Pull Request 之前运行 `python -m unittest discover -s tests` 和 `python -I tools/armor_ci_validate.py`：必须通过。参见 [CONTRIBUTING.md](CONTRIBUTING.md) 和[行为准则](CODE_OF_CONDUCT.md)。
* README 提供八种语言，由 ARMOR-DOCS 中的源文件生成：文本的修改在那里进行。
* 组件之间消息的变更从 ARMOR-COMMON 开始。
* 不要把仅仅构建过的东西描述为已在板子上运行。
* 请私下报告漏洞：[SECURITY.md](SECURITY.md)。问题、想法和报告：electrohobby3d@gmail.com（[SUPPORT.md](SUPPORT.md)）。

## 👤 作者

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 许可证

GPL-3.0-or-later - 见 [LICENSE](LICENSE)。
