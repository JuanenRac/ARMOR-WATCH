// ARMOR-WATCH - watchos/Sources/ArmorWatchCore/WatchProtocol.swift
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
import Foundation

/// How serious an alert is, as the phone writes it on the wire. The order is the one of the Kotlin core: info, warning, critical.
public enum Severity: String, CaseIterable, Sendable {
    case info, warning, critical

    var rank: Int { Self.allCases.firstIndex(of: self) ?? 0 }
}

/// An alert the phone chose to send, with the texts already in the language of the person.
public struct Alert: Equatable, Sendable {
    public let alertId: String
    public let severity: Severity
    public let title: String
    public let text: String
    public let needsAck: Bool
    public let ttlSeconds: Int

    public init(alertId: String, severity: Severity, title: String, text: String, needsAck: Bool, ttlSeconds: Int) {
        self.alertId = alertId; self.severity = severity; self.title = title; self.text = text; self.needsAck = needsAck; self.ttlSeconds = ttlSeconds
    }
}

/// A message between the phone and the watch (docs/watch_alert_contract.json).
public enum WatchMessage: Equatable, Sendable {
    /// The phone says whether the watch may show alerts at all. Off until the phone says otherwise.
    case relay(enabled: Bool)
    case alert(Alert)
    /// The phone says an alert is over (confirmed elsewhere, expired or cancelled).
    case alertClear(alertId: String)
    /// The watch tells the phone that the person confirmed an alert; the phone checks it and acts with its own session.
    case alertAck(alertId: String)
}

/// A JSON value read strictly: a number is not a boolean and a boolean is not a number, as in the Kotlin core.
private enum JSONValue: Decodable {
    case bool(Bool), int(Int), double(Double), string(String), array([JSONValue]), object([String: JSONValue]), null

    init(from decoder: Decoder) throws {
        let single = try decoder.singleValueContainer()
        if single.decodeNil() { self = .null }
        else if let value = try? single.decode(Bool.self) { self = .bool(value) }
        else if let value = try? single.decode(Int.self) { self = .int(value) }
        else if let value = try? single.decode(Double.self) { self = .double(value) }
        else if let value = try? single.decode(String.self) { self = .string(value) }
        else if let value = try? single.decode([JSONValue].self) { self = .array(value) }
        else { self = .object(try single.decode([String: JSONValue].self)) }
    }
}

public enum WatchProtocol {
    public static let version = 1
    public static let pathToWatch = "/armor/alert"
    public static let pathToPhone = "/armor/alert/ack"
    public static let rawMax = 4096
    public static let titleMax = 60
    public static let textMax = 160
    public static let ttlMinSeconds = 5
    public static let ttlMaxSeconds = 600
    public static let pendingMax = 8

    /// The message in `raw`, or nil when it is not a valid message of the contract. Never throws.
    public static func parse(_ raw: String) -> WatchMessage? {
        if raw.utf16.count > rawMax { return nil }
        guard let data = raw.data(using: .utf8), let value = try? JSONDecoder().decode(JSONValue.self, from: data), case .object(let object) = value else { return nil }
        guard case .int(let protocolVersion)? = object["protocolVersion"], protocolVersion == version, case .string(let type)? = object["type"] else { return nil }
        switch type {
        case "relay":
            guard case .bool(let enabled)? = object["enabled"] else { return nil }
            return .relay(enabled: enabled)
        case "alert_clear":
            return alertId(object).map { .alertClear(alertId: $0) }
        case "alert_ack":
            return alertId(object).map { .alertAck(alertId: $0) }
        case "alert":
            return alert(object).map { .alert($0) }
        default:
            return nil
        }
    }

    private static func alertId(_ object: [String: JSONValue]) -> String? {
        guard case .string(let id)? = object["alertId"], validId(id) else { return nil }
        return id
    }

    /// Letters, digits, `_` and `-`, one to sixty-four of them.
    public static func validId(_ id: String) -> Bool {
        let count = id.utf16.count
        guard count >= 1, count <= 64 else { return false }
        return id.unicodeScalars.allSatisfy { ($0.value >= 48 && $0.value <= 57) || ($0.value >= 65 && $0.value <= 90) || ($0.value >= 97 && $0.value <= 122) || $0 == "_" || $0 == "-" }
    }

    private static func text(_ object: [String: JSONValue], _ key: String, max: Int) -> String? {
        guard case .string(let text)? = object[key], text.utf16.count <= max, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        return text
    }

    private static func alert(_ object: [String: JSONValue]) -> Alert? {
        guard let id = alertId(object), case .string(let severityText)? = object["severity"], let severity = Severity(rawValue: severityText),
              let title = text(object, "title", max: titleMax), let body = text(object, "text", max: textMax),
              case .bool(let needsAck)? = object["needsAck"], case .int(let ttl)? = object["ttlSeconds"], ttl >= ttlMinSeconds, ttl <= ttlMaxSeconds else { return nil }
        return Alert(alertId: id, severity: severity, title: title, text: body, needsAck: needsAck, ttlSeconds: ttl)
    }

    /// The text the watch sends to the phone for a message it may send (only the acknowledgement).
    public static func encodeAck(alertId: String) -> String {
        let object: [String: Any] = ["type": "alert_ack", "protocolVersion": version, "alertId": alertId]
        let data = (try? JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])) ?? Data()
        return String(data: data, encoding: .utf8) ?? ""
    }
}
