// ARMOR-WATCH - watchos/Sources/ArmorWatchCore/WatchSession.swift
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
import Foundation

/// The way to the phone. On the watch it is WatchConnectivity; here it is only what the session needs: hand over a text on a path.
public protocol PhoneLink: AnyObject {
    /// True when the text was handed to the link; false when the phone cannot be reached now.
    func send(path: String, text: String) -> Bool
}

/// One alert as the screen draws it.
public struct AlertLine: Equatable, Sendable {
    public let alertId: String
    public let severity: Severity
    public let title: String
    public let text: String
    /// The person can confirm it from the watch (it asks for a confirmation and has not been confirmed yet).
    public let canConfirm: Bool
    /// The person already confirmed it from the watch; the line stays until the phone clears it.
    public let confirmed: Bool
}

/// What the screen of the watch shows. It carries no logic: the session decides, the screen only draws.
public enum WatchScreen: Equatable, Sendable {
    /// The phone has not switched the alerts of the watch on (or switched them off again): nothing is shown.
    case off
    /// The alerts are on and nothing is pending.
    case quiet
    /// The pending alerts, the most serious first.
    case alerts([AlertLine])
}

/// What happened when the person pressed the confirm button of an alert.
public enum ConfirmResult: Equatable, Sendable {
    case sent
    /// The alert is unknown, expired, needs no confirmation or was already confirmed: nothing was sent.
    case nothingToConfirm
    /// The phone cannot be reached: nothing was sent and the person can press the button again.
    case linkDown
}

/// The watch end of the conversation with the phone, without WatchConnectivity: it takes the texts that arrive, keeps the alerts (in memory only) and tells the screen what to draw.
public final class WatchSession {
    private let link: PhoneLink
    private let inbox: AlertInbox

    public init(link: PhoneLink, now: @escaping () -> Int64) {
        self.link = link
        self.inbox = AlertInbox(now: now)
    }

    /// A text that came from the phone on `path`. Only the path of the alerts is listened to, and only what the contract allows is taken. Returns true when the screen has changed.
    @discardableResult
    public func onPhoneMessage(path: String, raw: String) -> Bool {
        guard path == WatchProtocol.pathToWatch, let message = WatchProtocol.parse(raw) else { return false }
        return inbox.accept(message)
    }

    /// What to draw now. Alerts that expired since the last call are gone.
    public func screen() -> WatchScreen {
        guard inbox.enabled else { return .off }
        let pending = inbox.pending()
        if pending.isEmpty { return .quiet }
        return .alerts(pending.map { alert in
            let confirmed = inbox.isAcknowledged(alert.alertId)
            return AlertLine(alertId: alert.alertId, severity: alert.severity, title: alert.title, text: alert.text, canConfirm: alert.needsAck && !confirmed, confirmed: confirmed)
        })
    }

    /// The person confirms `alertId`. The phone checks it and acts with its own session; the watch never acts on its own.
    public func confirm(_ alertId: String) -> ConfirmResult {
        guard let id = inbox.acknowledge(alertId) else { return .nothingToConfirm }
        if link.send(path: WatchProtocol.pathToPhone, text: WatchProtocol.encodeAck(alertId: id)) { return .sent }
        inbox.undoAcknowledge(id)
        return .linkDown
    }
}

/// A phone that exists only in memory, for the tests and for trying the watch on a computer (evidence: simulator).
public final class SimulatedPhone: PhoneLink {
    /// Every (path, text) the watch handed over while the phone was reachable.
    public private(set) var received: [(path: String, text: String)] = []
    /// False makes `send` fail, as a watch out of reach of its phone does.
    public var reachable = true

    public init() {}

    public func send(path: String, text: String) -> Bool {
        guard reachable else { return false }
        received.append((path, text))
        return true
    }

    /// The phone sends `text` to `session` on the path of the alerts; returns what the session answers.
    @discardableResult
    public func push(_ session: WatchSession, _ text: String) -> Bool { session.onPhoneMessage(path: WatchProtocol.pathToWatch, raw: text) }
}

/// What the Apple Watch parts share: one session behind a lock (WatchConnectivity calls on its own queue, the screen on the main one) and a callback when the screen changes. No SwiftUI here.
public final class WatchRuntime {
    private let lock = NSLock()
    private let session: WatchSession
    private var latest: WatchScreen = .off

    /// Called, outside the lock, with the new screen whenever it changed.
    public var onChange: ((WatchScreen) -> Void)?

    public init(link: PhoneLink, now: @escaping () -> Int64) { session = WatchSession(link: link, now: now) }

    public var screen: WatchScreen { lock.lock(); defer { lock.unlock() }; return latest }

    @discardableResult
    public func onPhoneMessage(path: String, raw: String) -> Bool {
        let (changed, screen) = locked { ($0.onPhoneMessage(path: path, raw: raw), $0.screen()) }
        publish(screen)
        return changed
    }

    /// Looks again at the clock: an alert that expired leaves the screen.
    public func refresh() { publish(locked { $0.screen() }) }

    /// The person pressed the confirm button of `alertId`. It waits for the link, so it is not called on the main thread.
    public func confirm(_ alertId: String) -> ConfirmResult {
        let (result, screen) = locked { ($0.confirm(alertId), $0.screen()) }
        publish(screen)
        return result
    }

    private func locked<T>(_ body: (WatchSession) -> T) -> T { lock.lock(); defer { lock.unlock() }; return body(session) }

    private func publish(_ screen: WatchScreen) {
        lock.lock()
        let changed = screen != latest
        latest = screen
        lock.unlock()
        if changed { onChange?(screen) }
    }
}
