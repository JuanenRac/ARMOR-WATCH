// ARMOR-WATCH - watchos/Tests/ArmorWatchCoreTests/ArmorWatchCoreTests.swift
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
import Foundation
import XCTest
@testable import ArmorWatchCore

/// The Swift core against docs/watch_alert_contract.json (the same accepted and rejected examples the Kotlin core and tools/watch_contract.py run), and the rules of the inbox and the session.
final class ContractTests: XCTestCase {
    private func contract() throws -> [String: Any] {
        let url = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent().appendingPathComponent("docs/watch_alert_contract.json")
        return try JSONSerialization.jsonObject(with: Data(contentsOf: url)) as! [String: Any]
    }

    /// An example may write a long text as {"repeat": "x", "count": 161}; this gives the example as the text that travels.
    private func expand(_ value: Any) -> Any {
        if let object = value as? [String: Any] {
            if object.count == 2, let unit = object["repeat"] as? String, let count = object["count"] as? Int { return String(repeating: unit, count: count) }
            return object.mapValues { expand($0) }
        }
        if let array = value as? [Any] { return array.map { expand($0) } }
        return value
    }

    private func text(_ object: [String: Any]) -> String {
        String(data: try! JSONSerialization.data(withJSONObject: object), encoding: .utf8)!
    }

    func testEveryValidExampleIsAccepted() throws {
        let valid = try contract()["valid"] as! [Any]
        XCTAssertGreaterThanOrEqual(valid.count, 7)
        for (index, example) in valid.enumerated() {
            XCTAssertNotNil(WatchProtocol.parse(text(expand(example) as! [String: Any])), "valid[\(index)] was refused")
        }
    }

    func testEveryInvalidExampleIsRefused() throws {
        let invalid = try contract()["invalid"] as! [Any]
        XCTAssertGreaterThanOrEqual(invalid.count, 20)
        for (index, example) in invalid.enumerated() {
            var object = expand(example) as! [String: Any]
            let why = object.removeValue(forKey: "why") ?? ""
            XCTAssertNil(WatchProtocol.parse(text(object)), "invalid[\(index)] was accepted: \(why)")
        }
    }

    func testTheLimitsOfTheCodeAreTheLimitsOfTheContract() throws {
        let contract = try contract()
        let limits = contract["limits"] as! [String: Any]
        XCTAssertEqual(limits["rawMax"] as? Int, WatchProtocol.rawMax)
        XCTAssertEqual(limits["titleMax"] as? Int, WatchProtocol.titleMax)
        XCTAssertEqual(limits["textMax"] as? Int, WatchProtocol.textMax)
        XCTAssertEqual(limits["pendingMax"] as? Int, WatchProtocol.pendingMax)
        let ttl = limits["ttlSeconds"] as! [Int]
        XCTAssertEqual(ttl, [WatchProtocol.ttlMinSeconds, WatchProtocol.ttlMaxSeconds])
        XCTAssertEqual(contract["protocolVersion"] as? Int, WatchProtocol.version)
        let paths = contract["paths"] as! [String: String]
        XCTAssertEqual(paths["toWatch"], WatchProtocol.pathToWatch)
        XCTAssertEqual(paths["toPhone"], WatchProtocol.pathToPhone)
        XCTAssertEqual(contract["severities"] as? [String], Severity.allCases.map(\.rawValue))
    }

    func testAnAlertIsReadWithAllItsFields() {
        let message = WatchProtocol.parse(#"{"type":"alert","protocolVersion":1,"alertId":"al-1","severity":"critical","title":"Door","text":"Opened","needsAck":true,"ttlSeconds":60}"#)
        XCTAssertEqual(message, .alert(Alert(alertId: "al-1", severity: .critical, title: "Door", text: "Opened", needsAck: true, ttlSeconds: 60)))
    }

    func testGarbageNeverCrashes() {
        for raw in ["", "not json", "[]", "null", "3", "{", #"{"type":null}"#, String(repeating: "{", count: 5000)] { XCTAssertNil(WatchProtocol.parse(raw), raw) }
    }

    func testTheAcknowledgementIsWhatTheContractAcceptsAndTheWatchMayOnlySendThat() {
        let text = WatchProtocol.encodeAck(alertId: "al-1")
        XCTAssertEqual(WatchProtocol.parse(text), .alertAck(alertId: "al-1"))
        XCTAssertEqual(text, #"{"alertId":"al-1","protocolVersion":1,"type":"alert_ack"}"#)
    }
}

final class InboxAndSessionTests: XCTestCase {
    private var clock: Int64 = 1_000
    private let phone = SimulatedPhone()
    private lazy var session = WatchSession(link: phone, now: { [unowned self] in self.clock })

    private func relay(_ on: Bool) -> String { #"{"type":"relay","protocolVersion":1,"enabled":\#(on)}"# }
    private func alert(_ id: String, severity: String = "warning", needsAck: Bool = true, ttl: Int = 60) -> String {
        #"{"type":"alert","protocolVersion":1,"alertId":"\#(id)","severity":"\#(severity)","title":"Title \#(id)","text":"Text \#(id)","needsAck":\#(needsAck),"ttlSeconds":\#(ttl)}"#
    }
    private func clear(_ id: String) -> String { #"{"type":"alert_clear","protocolVersion":1,"alertId":"\#(id)"}"# }
    private func lines() -> [AlertLine] {
        guard case .alerts(let lines) = session.screen() else { XCTFail("no alerts on the screen"); return [] }
        return lines
    }

    func testTheScreenIsOffUntilThePhoneSwitchesTheAlertsOn() {
        XCTAssertEqual(session.screen(), .off)
        XCTAssertFalse(phone.push(session, alert("a")))
        XCTAssertEqual(session.screen(), .off)
        XCTAssertTrue(phone.push(session, relay(true)))
        XCTAssertEqual(session.screen(), .quiet)
    }

    func testAnAlertAppearsSeriousFirstAndGoesWhenThePhoneClearsIt() {
        phone.push(session, relay(true))
        phone.push(session, alert("a", severity: "info"))
        phone.push(session, alert("b", severity: "critical"))
        XCTAssertEqual(lines().map(\.alertId), ["b", "a"])
        XCTAssertTrue(phone.push(session, clear("b")))
        XCTAssertEqual(lines().map(\.alertId), ["a"])
        XCTAssertTrue(phone.push(session, clear("a")))
        XCTAssertEqual(session.screen(), .quiet)
    }

    func testTheNewestComesFirstAmongEquals() {
        phone.push(session, relay(true))
        for id in ["a", "b", "c"] { phone.push(session, alert(id)) }
        XCTAssertEqual(lines().map(\.alertId), ["c", "b", "a"])
    }

    func testAnAlertThatExpiresLeavesTheScreenWithoutAnyMessage() {
        phone.push(session, relay(true))
        phone.push(session, alert("a", ttl: 30))
        clock += 29
        XCTAssertEqual(lines().count, 1)
        clock += 1
        XCTAssertEqual(session.screen(), .quiet)
    }

    func testSwitchingTheRelayOffForgetsEverythingAndRefusesTheConfirmation() {
        phone.push(session, relay(true))
        phone.push(session, alert("a"))
        phone.push(session, relay(false))
        XCTAssertEqual(session.screen(), .off)
        XCTAssertEqual(session.confirm("a"), .nothingToConfirm)
    }

    func testOnlyThePathOfTheAlertsIsListenedToAndBadMessagesChangeNothing() {
        XCTAssertFalse(session.onPhoneMessage(path: "/armor/other", raw: relay(true)))
        XCTAssertFalse(session.onPhoneMessage(path: WatchProtocol.pathToPhone, raw: relay(true)))
        phone.push(session, relay(true))
        for bad in ["", "not json", "{}", alert("a").replacingOccurrences(of: "\"warning\"", with: "\"fatal\"")] { XCTAssertFalse(phone.push(session, bad), bad) }
        XCTAssertEqual(session.screen(), .quiet)
    }

    func testConfirmingHandsTheAcknowledgementToThePhoneOnceAndMarksTheLine() {
        phone.push(session, relay(true))
        phone.push(session, alert("a"))
        XCTAssertTrue(lines()[0].canConfirm)
        XCTAssertEqual(session.confirm("a"), .sent)
        XCTAssertEqual(phone.received.count, 1)
        XCTAssertEqual(phone.received[0].path, WatchProtocol.pathToPhone)
        XCTAssertEqual(phone.received[0].text, WatchProtocol.encodeAck(alertId: "a"))
        XCTAssertTrue(lines()[0].confirmed)
        XCTAssertFalse(lines()[0].canConfirm)
        XCTAssertEqual(session.confirm("a"), .nothingToConfirm)
        XCTAssertEqual(phone.received.count, 1)
    }

    func testAnAlertThatAsksForNoConfirmationOffersNoButton() {
        phone.push(session, relay(true))
        phone.push(session, alert("a", needsAck: false))
        XCTAssertFalse(lines()[0].canConfirm)
        XCTAssertEqual(session.confirm("a"), .nothingToConfirm)
        XCTAssertTrue(phone.received.isEmpty)
    }

    func testAPhoneOutOfReachLeavesTheButtonToPressAgain() {
        phone.push(session, relay(true))
        phone.push(session, alert("a"))
        phone.reachable = false
        XCTAssertEqual(session.confirm("a"), .linkDown)
        XCTAssertTrue(lines()[0].canConfirm)
        XCTAssertFalse(lines()[0].confirmed)
        phone.reachable = true
        XCTAssertEqual(session.confirm("a"), .sent)
    }

    func testTheLimitOfEightDropsTheOldestThatIsNotCritical() {
        phone.push(session, relay(true))
        phone.push(session, alert("crit", severity: "critical"))
        for index in 1...8 { phone.push(session, alert("w\(index)")) }
        let ids = lines().map(\.alertId)
        XCTAssertEqual(ids.count, WatchProtocol.pendingMax)
        XCTAssertTrue(ids.contains("crit"))
        XCTAssertFalse(ids.contains("w1"))
    }

    func testTheRuntimeSharesTheSessionAndTellsTheScreenWhenItChanged() {
        let runtime = WatchRuntime(link: phone, now: { [unowned self] in self.clock })
        var seen: [WatchScreen] = []
        runtime.onChange = { seen.append($0) }
        XCTAssertEqual(runtime.screen, .off)
        XCTAssertTrue(runtime.onPhoneMessage(path: WatchProtocol.pathToWatch, raw: relay(true)))
        XCTAssertTrue(runtime.onPhoneMessage(path: WatchProtocol.pathToWatch, raw: alert("a", ttl: 30)))
        runtime.refresh()   // nothing changed: nothing is told
        XCTAssertEqual(seen.count, 2)
        clock += 30
        runtime.refresh()
        XCTAssertEqual(runtime.screen, .quiet)
        XCTAssertEqual(seen.last, .quiet)
    }

    func testTheSimulatedPhoneRecordsOnlyWhatReachedIt() {
        XCTAssertTrue(phone.send(path: "/x", text: "one"))
        phone.reachable = false
        XCTAssertFalse(phone.send(path: "/x", text: "two"))
        XCTAssertEqual(phone.received.count, 1)
    }
}
