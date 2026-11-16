// =============================================================================
// ARMOR-WATCH - src/main/kotlin/armor/watch/WatchProtocol.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

import org.json.JSONException
import org.json.JSONObject

/** How serious an alert is, as the phone writes it on the wire. */
enum class Severity(val wire: String) {
    INFO("info"),
    WARNING("warning"),
    CRITICAL("critical");

    companion object {
        fun fromWire(text: String?): Severity? = entries.firstOrNull { it.wire == text }
    }
}

/** A message between the phone and the watch (docs/watch_alert_contract.json). */
sealed interface WatchMessage

/** The phone says whether the watch may show alerts at all. Off until the phone says otherwise. */
data class RelayState(val enabled: Boolean) : WatchMessage

/** An alert the phone chose to send, with the texts already in the language of the person. */
data class Alert(
    val alertId: String,
    val severity: Severity,
    val title: String,
    val text: String,
    val needsAck: Boolean,
    val ttlSeconds: Int,
) : WatchMessage

/** The phone says an alert is over (confirmed elsewhere, expired or cancelled). */
data class AlertClear(val alertId: String) : WatchMessage

/** The watch tells the phone that the person confirmed an alert; the phone checks it and acts with its own session. */
data class AlertAck(val alertId: String) : WatchMessage

object WatchProtocol {
    const val VERSION = 1
    const val PATH_TO_WATCH = "/armor/alert"
    const val PATH_TO_PHONE = "/armor/alert/ack"

    /** The capability the phone app declares and the watch looks for, to find the phone (a node) it must talk to instead of any device that is connected. */
    const val CAPABILITY_PHONE = "armor_phone_alerts"

    /** The capability the watch app declares and the phone looks for. */
    const val CAPABILITY_WATCH = "armor_watch_alerts"

    /**
     * The nodes a message goes to: those that declare the capability of the other app, or, when none does (the capability is not known yet, an older
     * build of the other app), the nodes that are connected. A message is only delivered to the app of the other side: the Data Layer matches it by package
     * name and signature, which is why the two apps share the key and, for the watch, the package the phone app names in the capability.
     */
    fun <N> targets(capable: Collection<N>, connected: Collection<N>): List<N> = if (capable.isNotEmpty()) capable.toList() else connected.toList()
    const val RAW_MAX = 4096
    const val TITLE_MAX = 60
    const val TEXT_MAX = 160
    const val TTL_MIN_SECONDS = 5
    const val TTL_MAX_SECONDS = 600
    const val PENDING_MAX = 8

    private val alertIdPattern = Regex("^[A-Za-z0-9_-]{1,64}$")

    private fun JSONObject.strictInt(key: String): Int? = opt(key) as? Int
    private fun JSONObject.strictString(key: String): String? = opt(key) as? String
    private fun JSONObject.strictBoolean(key: String): Boolean? = opt(key) as? Boolean

    /** The message in `raw`, or null when it is not a valid message of the contract. Never throws. */
    fun parse(raw: String): WatchMessage? {
        if (raw.length > RAW_MAX) return null
        val obj = try {
            JSONObject(raw)
        } catch (_: JSONException) {
            return null
        }
        if (obj.strictInt("protocolVersion") != VERSION) return null
        return when (obj.strictString("type")) {
            "relay" -> obj.strictBoolean("enabled")?.let { RelayState(it) }
            "alert_clear" -> obj.alertId()?.let { AlertClear(it) }
            "alert_ack" -> obj.alertId()?.let { AlertAck(it) }
            "alert" -> obj.alert()
            else -> null
        }
    }

    private fun JSONObject.alertId(): String? = strictString("alertId")?.takeIf { alertIdPattern.matches(it) }

    private fun JSONObject.alert(): Alert? {
        val id = alertId() ?: return null
        val severity = Severity.fromWire(strictString("severity")) ?: return null
        val title = strictString("title")?.takeIf { it.isNotBlank() && it.length <= TITLE_MAX } ?: return null
        val text = strictString("text")?.takeIf { it.isNotBlank() && it.length <= TEXT_MAX } ?: return null
        val needsAck = strictBoolean("needsAck") ?: return null
        val ttl = strictInt("ttlSeconds")?.takeIf { it in TTL_MIN_SECONDS..TTL_MAX_SECONDS } ?: return null
        return Alert(id, severity, title, text, needsAck, ttl)
    }

    /** The text the watch sends to the phone for a message it may send (only the acknowledgement). */
    fun encode(message: AlertAck): String =
        JSONObject().put("type", "alert_ack").put("protocolVersion", VERSION).put("alertId", message.alertId).toString()
}
