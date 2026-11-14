// swift-tools-version:5.9
// ARMOR-WATCH - watchos/Package.swift
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
//
// The Swift core of the Apple Watch app: the same messages, inbox and session as the Kotlin core of the root project, written from the same contract (docs/watch_alert_contract.json), with no
// SwiftUI and no WatchConnectivity in it, so its tests run on a Mac with `swift test`. The app (App/) is built by the workflow watchos-build.yml with XcodeGen.
import PackageDescription

let package = Package(
    name: "ArmorWatchCore",
    platforms: [.watchOS(.v10), .macOS(.v13), .iOS(.v16)],
    products: [.library(name: "ArmorWatchCore", targets: ["ArmorWatchCore"])],
    targets: [
        .target(name: "ArmorWatchCore"),
        .testTarget(name: "ArmorWatchCoreTests", dependencies: ["ArmorWatchCore"]),
    ]
)
