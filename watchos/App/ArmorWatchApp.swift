// ARMOR-WATCH - watchos/App/ArmorWatchApp.swift
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
import ArmorWatchCore
import SwiftUI

/// The Apple Watch app: the screen draws what the session of the core says, WatchConnectivity carries the messages of the iPhone. It has no login, no network of its own, no keys, and writes nothing to
/// storage; the alerts are kept in memory only. Evidence: simulator; it has never run on a watch.
@main
struct ArmorWatchApp: App {
    @StateObject private var model = WatchModel()

    var body: some Scene {
        WindowGroup { ContentView(model: model) }
    }
}

/// The screen as an observable object: the runtime calls back from any thread, the screen is updated on the main one.
@MainActor
final class WatchModel: ObservableObject {
    @Published private(set) var screen: WatchScreen = .off
    @Published private(set) var linkDown = false

    private let link = ConnectivityLink()
    let runtime: WatchRuntime
    private var ticker: Timer?

    init() {
        let started = ProcessInfo.processInfo.systemUptime
        runtime = WatchRuntime(link: link, now: { Int64(ProcessInfo.processInfo.systemUptime - started) })
        runtime.onChange = { [weak self] screen in Task { @MainActor in self?.screen = screen } }
        link.onMessage = { [runtime] path, text in runtime.onPhoneMessage(path: path, raw: text) }
        link.start()
        // an alert that expires leaves the screen without any message, so the screen looks at the clock once a second
        ticker = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [runtime] _ in runtime.refresh() }
    }

    /// The Data Layer is waited for: never on the main thread.
    func confirm(_ alertId: String) {
        let runtime = runtime
        Task.detached {
            let result = runtime.confirm(alertId)
            await MainActor.run { self.linkDown = result == .linkDown }
        }
    }
}
