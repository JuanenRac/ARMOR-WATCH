// =============================================================================
// ARMOR-WATCH - src/main/kotlin/armor/watch/AlertInbox.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

/**
 * The alerts the watch is showing. It lives in memory only: nothing is written to the watch, and closing the app forgets everything.
 *
 * The inbox starts switched off. Until the phone says (RelayState(true)) that the option of the alerts of the watch is on, every alert is
 * dropped; when the phone switches it off again, what was pending is dropped too. `now` is a clock in seconds that never goes back.
 */
class AlertInbox(private val now: () -> Long) {
    private class Entry(val alert: Alert, val expiresAt: Long, var acknowledged: Boolean = false)

    private val entries = LinkedHashMap<String, Entry>()

    /** Whether the phone allows alerts on the watch. */
    var enabled: Boolean = false
        private set

    /** What the screen shows: the alerts not yet expired, the most serious first and the newest first among equals. */
    fun pending(): List<Alert> {
        dropExpired()
        return entries.values.map { it.alert }.reversed().sortedByDescending { it.severity.ordinal }
    }

    /** True when the person has already confirmed this alert from the watch (the confirmation was handed over). */
    fun isAcknowledged(alertId: String): Boolean = entries[alertId]?.acknowledged == true

    /** Takes a message from the phone. Returns true when the screen has something new to show or to remove. */
    fun accept(message: WatchMessage): Boolean = when (message) {
        is RelayState -> {
            val changed = enabled != message.enabled
            enabled = message.enabled
            if (!enabled && entries.isNotEmpty()) {
                entries.clear()
                true
            } else {
                changed
            }
        }
        is Alert -> if (!enabled) false else {
            dropExpired()
            entries.remove(message.alertId)
            entries[message.alertId] = Entry(message, now() + message.ttlSeconds)
            while (entries.size > WatchProtocol.PENDING_MAX) {
                // the oldest alert that is not critical goes first; only when all are critical the oldest of them
                val victim = entries.entries.firstOrNull { it.value.alert.severity != Severity.CRITICAL } ?: entries.entries.first()
                entries.remove(victim.key)
            }
            true
        }
        is AlertClear -> enabled && entries.remove(message.alertId) != null
        is AlertAck -> false // only the watch sends this one
    }

    /**
     * The person confirms an alert on the watch. Returns the message for the phone, once: null when the alert is unknown, expired,
     * needs no confirmation or was already confirmed. The alert stays on the screen, marked, until the phone clears it.
     */
    fun acknowledge(alertId: String): AlertAck? {
        dropExpired()
        val entry = entries[alertId] ?: return null
        if (!entry.alert.needsAck || entry.acknowledged) return null
        entry.acknowledged = true
        return AlertAck(alertId)
    }

    /** Takes back the mark of [acknowledge] when the confirmation could not be handed to the phone, so the person can press again. */
    fun undoAcknowledge(alertId: String) {
        entries[alertId]?.acknowledged = false
    }

    private fun dropExpired() {
        val limit = now()
        entries.values.removeAll { it.expiresAt <= limit }
    }
}
