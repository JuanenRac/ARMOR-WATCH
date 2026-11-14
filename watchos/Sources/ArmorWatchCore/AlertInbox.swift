// ARMOR-WATCH - watchos/Sources/ArmorWatchCore/AlertInbox.swift
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
import Foundation

/// The alerts the watch is showing. It lives in memory only: nothing is written to the watch, and closing the app forgets everything.
///
/// The inbox starts switched off. Until the phone says (`.relay(enabled: true)`) that the option of the alerts of the watch is on, every alert is dropped; when the phone switches it off again, what was
/// pending is dropped too. `now` is a clock in seconds that never goes back.
public final class AlertInbox {
    private struct Entry {
        let alert: Alert
        let expiresAt: Int64
        var acknowledged = false
    }

    private let now: () -> Int64
    private var entries: [Entry] = []   // oldest first

    /// Whether the phone allows alerts on the watch.
    public private(set) var enabled = false

    public init(now: @escaping () -> Int64) { self.now = now }

    /// What the screen shows: the alerts not yet expired, the most serious first and the newest first among equals.
    public func pending() -> [Alert] {
        dropExpired()
        let newestFirst = Array(entries.reversed().enumerated())
        return newestFirst.sorted { left, right in
            left.element.alert.severity.rank != right.element.alert.severity.rank ? left.element.alert.severity.rank > right.element.alert.severity.rank : left.offset < right.offset
        }.map { $0.element.alert }
    }

    /// True when the person has already confirmed this alert from the watch (the confirmation was handed over).
    public func isAcknowledged(_ alertId: String) -> Bool { entries.first { $0.alert.alertId == alertId }?.acknowledged == true }

    /// Takes a message from the phone. Returns true when the screen has something new to show or to remove.
    @discardableResult
    public func accept(_ message: WatchMessage) -> Bool {
        switch message {
        case .relay(let on):
            let changed = enabled != on
            enabled = on
            if !enabled && !entries.isEmpty {
                entries.removeAll()
                return true
            }
            return changed
        case .alert(let alert):
            guard enabled else { return false }
            dropExpired()
            entries.removeAll { $0.alert.alertId == alert.alertId }
            entries.append(Entry(alert: alert, expiresAt: now() + Int64(alert.ttlSeconds)))
            while entries.count > WatchProtocol.pendingMax {
                // the oldest alert that is not critical goes first; only when all are critical the oldest of them
                let victim = entries.firstIndex { $0.alert.severity != .critical } ?? 0
                entries.remove(at: victim)
            }
            return true
        case .alertClear(let id):
            guard enabled, let index = entries.firstIndex(where: { $0.alert.alertId == id }) else { return false }
            entries.remove(at: index)
            return true
        case .alertAck:
            return false   // only the watch sends this one
        }
    }

    /// The person confirms an alert on the watch. Returns the id to hand to the phone, once: nil when the alert is unknown, expired, needs no confirmation or was already confirmed.
    public func acknowledge(_ alertId: String) -> String? {
        dropExpired()
        guard let index = entries.firstIndex(where: { $0.alert.alertId == alertId }), entries[index].alert.needsAck, !entries[index].acknowledged else { return nil }
        entries[index].acknowledged = true
        return alertId
    }

    /// Takes back the mark of `acknowledge` when the confirmation could not be handed to the phone, so the person can press again.
    public func undoAcknowledge(_ alertId: String) {
        if let index = entries.firstIndex(where: { $0.alert.alertId == alertId }) { entries[index].acknowledged = false }
    }

    private func dropExpired() {
        let limit = now()
        entries.removeAll { $0.expiresAt <= limit }
    }
}
