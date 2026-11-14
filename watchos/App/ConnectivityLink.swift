// ARMOR-WATCH - watchos/App/ConnectivityLink.swift
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
import ArmorWatchCore
import Foundation
import WatchConnectivity

/// The way to the iPhone through WatchConnectivity. Messages are live ones (`sendMessage`): an alert that was not delivered while the watch was out of reach is not queued and shown late, as the Data
/// Layer of Wear OS does not queue it either. A confirmation goes back as a message on the same keys: `path` and `text`.
final class ConnectivityLink: NSObject, PhoneLink, WCSessionDelegate {
    /// Called with the path and the text of a message of the phone.
    var onMessage: ((String, String) -> Void)?

    func start() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    // The phone is asked to hand over the confirmation; true when the message was handed to WatchConnectivity (the phone checks it and acts with its own session).
    func send(path: String, text: String) -> Bool {
        let session = WCSession.default
        guard WCSession.isSupported(), session.activationState == .activated, session.isReachable else { return false }
        session.sendMessage(["path": path, "text": text], replyHandler: nil, errorHandler: nil)
        return true
    }

    func session(_ session: WCSession, activationDidCompleteWith activationState: WCSessionActivationState, error: Error?) {}

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        guard let path = message["path"] as? String, let text = message["text"] as? String else { return }
        onMessage?(path, text)
    }
}
