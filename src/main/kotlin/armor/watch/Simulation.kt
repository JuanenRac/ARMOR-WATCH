// =============================================================================
// ARMOR-WATCH - src/main/kotlin/armor/watch/Simulation.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

import org.json.JSONObject

/**
 * The whole conversation between a phone and a watch in memory, so the watch can be exercised from end to end with no watch, no phone and no
 * Data Layer (evidence: simulator). Three doubles stand in for what does not exist on a computer:
 *
 *  - [SimulatedChannel]: the Data Layer or WatchConnectivity, with faults on purpose (out of range, a message lost on the way, a duplicate, a late burst in the wrong order);
 *  - [SimulatedPhoneRelay]: the sending side of the two phone apps (the switch, the alerts, the clear, and what is done with the confirmation that comes back);
 *  - [SimulatedAlarmServer]: the part of ARMOR-SERVER that the confirmation reaches (an alarm is acknowledged once, or refused).
 *
 * [SimulatedPair] wires them with a clock that only the test moves. Nothing here reads the time, the network or a random number.
 */
class SimulatedChannel(private val deliver: (path: String, text: String) -> Unit) {
    /** False is a device out of range: [send] reports it, as the real link does, and the message is lost. */
    var up: Boolean = true

    /** The next messages are accepted by the link and lost on the way (the sender is told they were handed over). */
    var dropNext: Int = 0

    /** The next message arrives twice. */
    var duplicateNext: Boolean = false

    /** While true the link keeps what is sent and delivers it at [release]. */
    var held: Boolean = false

    private val queue = mutableListOf<Pair<String, String>>()

    /** Everything that did not arrive: sent while the link was down, or dropped on the way. */
    val lost = mutableListOf<Pair<String, String>>()

    /** Everything that arrived, in the order of arrival. */
    val arrived = mutableListOf<Pair<String, String>>()

    fun send(path: String, text: String): Boolean {
        if (!up) {
            lost += path to text
            return false
        }
        if (dropNext > 0) {
            dropNext -= 1
            lost += path to text
            return true
        }
        if (held) {
            queue += path to text
            return true
        }
        arrive(path, text)
        if (duplicateNext) {
            duplicateNext = false
            arrive(path, text)
        }
        return true
    }

    /** Delivers what was held, in the order it was sent or, with [reversed], the other way round. */
    fun release(reversed: Boolean = false) {
        held = false
        val batch = queue.toList()
        queue.clear()
        (if (reversed) batch.reversed() else batch).forEach { arrive(it.first, it.second) }
    }

    val heldCount: Int get() = queue.size

    private fun arrive(path: String, text: String) {
        arrived += path to text
        deliver(path, text)
    }
}

/** The alarms of a server that can be acknowledged once. */
class SimulatedAlarmServer {
    private val open = linkedSetOf<String>()

    /** The alarms acknowledged so far, in order. */
    val acknowledged = mutableListOf<String>()

    /** False is a server that cannot be reached: an acknowledgement fails. */
    var reachable: Boolean = true

    fun raise(alarmId: String) {
        open += alarmId
    }

    fun isOpen(alarmId: String): Boolean = alarmId in open

    /** True when the alarm was open and is now acknowledged; false when the server is down, the alarm is unknown or it was acknowledged already. */
    fun acknowledge(alarmId: String): Boolean {
        if (!reachable || !open.remove(alarmId)) return false
        acknowledged += alarmId
        return true
    }
}

/**
 * The phone's side of the relay, as the two phone apps do it: it writes the messages of the contract, remembers for a while which alert stands for which alarm,
 * and acts on a confirmation only for an alert it sent, once, with its own session. The confirmation is a request, never an order.
 */
class SimulatedPhoneRelay(
    private val channel: SimulatedChannel,
    private val server: SimulatedAlarmServer,
    private val now: () -> Long,
) {
    private class Sent(val alarmId: String, val expiresAt: Long)

    private val sent = LinkedHashMap<String, Sent>()

    /** The option "show the alarms on the watch". Off by default; nothing is sent while it is off. */
    var optionOn: Boolean = false
        private set

    /** The alarms the phone acknowledged because the watch asked. */
    val acknowledgedForWatch = mutableListOf<String>()

    /** What the phone did not act on, with the reason (path, shape, unknown, expired, server). */
    val refusals = mutableListOf<String>()

    fun setOption(on: Boolean) {
        optionOn = on
        if (!on) sent.clear()
        channel.send(WatchProtocol.PATH_TO_WATCH, JSONObject().put("type", "relay").put("protocolVersion", WatchProtocol.VERSION).put("enabled", on).toString())
    }

    /** A notice the person would see. [severity] is the one of the phone ("critical", "high" or another). Returns false when nothing was sent. */
    fun notice(alertId: String, alarmId: String?, severity: String, title: String, text: String): Boolean {
        if (!optionOn || title.isBlank() || text.isBlank()) return false
        val wire = when (severity) {
            "critical" -> "critical"
            "high" -> "warning"
            else -> "info"
        }
        val ttl = if (severity == "critical") WatchProtocol.TTL_MAX_SECONDS else 120
        if (alarmId != null) sent[alertId] = Sent(alarmId, now() + ttl)
        val message = JSONObject()
            .put("type", "alert").put("protocolVersion", WatchProtocol.VERSION).put("alertId", alertId).put("severity", wire)
            .put("title", title.trim().take(WatchProtocol.TITLE_MAX)).put("text", text.trim().take(WatchProtocol.TEXT_MAX))
            .put("needsAck", alarmId != null).put("ttlSeconds", ttl)
        return channel.send(WatchProtocol.PATH_TO_WATCH, message.toString())
    }

    /** The alert is over on the phone (confirmed there, expired or cancelled): the watch is told to drop it. */
    fun clear(alertId: String) {
        sent.remove(alertId)
        channel.send(WatchProtocol.PATH_TO_WATCH, JSONObject().put("type", "alert_clear").put("protocolVersion", WatchProtocol.VERSION).put("alertId", alertId).toString())
    }

    /** What arrives from the watch. Returns true when an alarm was acknowledged because of it. */
    fun onWatchMessage(path: String, raw: String): Boolean {
        if (path != WatchProtocol.PATH_TO_PHONE) return refuse("path")
        if (!optionOn) return refuse("option off")
        val ack = WatchProtocol.parse(raw) as? AlertAck ?: return refuse("shape")
        val entry = sent[ack.alertId] ?: return refuse("unknown")
        if (entry.expiresAt <= now()) {
            sent.remove(ack.alertId)
            return refuse("expired")
        }
        sent.remove(ack.alertId) // one confirmation only: a repeat finds nothing
        if (!server.acknowledge(entry.alarmId)) return refuse("server")
        acknowledgedForWatch += entry.alarmId
        return true
    }

    private fun refuse(reason: String): Boolean {
        refusals += reason
        return false
    }
}

/** A phone, a watch, the link between them and a server, on a clock that the test moves. [restartWatch] makes a watch that starts again with nothing in memory. */
class SimulatedPair(startSeconds: Long = 1_000L) {
    var clock: Long = startSeconds

    val server = SimulatedAlarmServer()
    val toWatch = SimulatedChannel { path, text -> watch.onPhoneMessage(path, text) }
    val toPhone = SimulatedChannel { path, text -> phone.onWatchMessage(path, text) }
    val phone = SimulatedPhoneRelay(toWatch, server) { clock }
    var watch: WatchSession = WatchSession({ path, text -> toPhone.send(path, text) }) { clock }
        private set

    fun advance(seconds: Long) {
        clock += seconds
    }

    fun restartWatch() {
        watch = WatchSession({ path, text -> toPhone.send(path, text) }) { clock }
    }
}
