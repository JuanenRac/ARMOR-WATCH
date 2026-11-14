// =============================================================================
// ARMOR-WATCH - src/main/kotlin/armor/watch/WatchSession.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

/** The way to the phone. On a watch it is the Data Layer; here it is only what the session needs: hand over a text on a path. */
fun interface PhoneLink {
    /** True when the text was handed to the link; false when the phone cannot be reached now. */
    fun send(path: String, text: String): Boolean
}

/** One alert as the screen draws it. */
data class AlertLine(
    val alertId: String,
    val severity: Severity,
    val title: String,
    val text: String,
    /** The person can confirm it from the watch (it asks for a confirmation and has not been confirmed yet). */
    val canConfirm: Boolean,
    /** The person already confirmed it from the watch; the line stays until the phone clears it. */
    val confirmed: Boolean,
)

/** What the screen of the watch shows. It carries no logic: the session decides, the screen only draws. */
sealed interface WatchScreen {
    /** The phone has not switched the alerts of the watch on (or switched them off again): nothing is shown. */
    data object Off : WatchScreen

    /** The alerts are on and nothing is pending. */
    data object Quiet : WatchScreen

    /** The pending alerts, the most serious first. */
    data class Alerts(val lines: List<AlertLine>) : WatchScreen
}

/** What happened when the person pressed the confirm button of an alert. */
enum class ConfirmResult {
    /** The confirmation was handed to the phone. */
    SENT,

    /** The alert is unknown, expired, needs no confirmation or was already confirmed: nothing was sent. */
    NOTHING_TO_CONFIRM,

    /** The phone cannot be reached: nothing was sent and the person can press the button again. */
    LINK_DOWN,
}

/**
 * The watch end of the conversation with the phone, without Android: it takes the texts that arrive, keeps the alerts (in memory only) and
 * tells the screen what to draw. The Wear OS service that owns the Data Layer calls [onPhoneMessage]; the screen calls [screen] and [confirm].
 */
class WatchSession(private val link: PhoneLink, now: () -> Long) {
    private val inbox = AlertInbox(now)

    /**
     * A text that came from the phone on [path]. Only the path of the alerts is listened to, and only what the contract allows is taken.
     * Returns true when the screen has changed.
     */
    fun onPhoneMessage(path: String, raw: String): Boolean {
        if (path != WatchProtocol.PATH_TO_WATCH) return false
        val message = WatchProtocol.parse(raw) ?: return false
        return inbox.accept(message)
    }

    /** What to draw now. Alerts that expired since the last call are gone. */
    fun screen(): WatchScreen {
        if (!inbox.enabled) return WatchScreen.Off
        val pending = inbox.pending()
        if (pending.isEmpty()) return WatchScreen.Quiet
        return WatchScreen.Alerts(
            pending.map {
                val confirmed = inbox.isAcknowledged(it.alertId)
                AlertLine(it.alertId, it.severity, it.title, it.text, canConfirm = it.needsAck && !confirmed, confirmed = confirmed)
            },
        )
    }

    /** The person confirms [alertId]. The phone checks it and acts with its own session; the watch never acts on its own. */
    fun confirm(alertId: String): ConfirmResult {
        val ack = inbox.acknowledge(alertId) ?: return ConfirmResult.NOTHING_TO_CONFIRM
        if (link.send(WatchProtocol.PATH_TO_PHONE, WatchProtocol.encode(ack))) return ConfirmResult.SENT
        inbox.undoAcknowledge(alertId)
        return ConfirmResult.LINK_DOWN
    }
}

/**
 * A phone that exists only in memory, for the tests and for trying the watch on a computer (evidence: simulator). It records what the
 * watch sends, can be made unreachable, and pushes messages into a session as the Data Layer would.
 */
class SimulatedPhone : PhoneLink {
    /** Every (path, text) the watch handed over while the phone was reachable. */
    val received = mutableListOf<Pair<String, String>>()

    /** False makes [send] fail, as a watch out of reach of its phone does. */
    var reachable: Boolean = true

    override fun send(path: String, text: String): Boolean {
        if (!reachable) return false
        received += path to text
        return true
    }

    /** The phone sends [text] to [session] on the path of the alerts; returns what the session answers. */
    fun push(session: WatchSession, text: String): Boolean = session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, text)
}
