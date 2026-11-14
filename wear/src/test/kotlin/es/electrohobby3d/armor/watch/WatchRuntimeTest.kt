// =============================================================================
// ARMOR-WATCH - wear/src/test/kotlin/es/electrohobby3d/armor/watch/WatchRuntimeTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package es.electrohobby3d.armor.watch

import armor.watch.ConfirmResult
import armor.watch.SimulatedPhone
import armor.watch.WatchProtocol
import armor.watch.WatchScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The runtime that the service of the Data Layer and the screen share, tried against a phone that exists only in memory (evidence: simulator). */
class WatchRuntimeTest {
    private var clock = 1_000L
    private val phone = SimulatedPhone()
    private val runtime = WatchRuntime(phone) { clock }

    private fun relay(on: Boolean) = """{"type":"relay","protocolVersion":1,"enabled":$on}"""

    private fun alert(id: String, ttl: Int = 60) =
        """{"type":"alert","protocolVersion":1,"alertId":"$id","severity":"critical","title":"T $id","text":"X $id","needsAck":true,"ttlSeconds":$ttl}"""

    private fun receive(raw: String) = runtime.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, raw)

    @Test
    fun theFlowStartsOffAndFollowsTheMessages() {
        assertEquals(WatchScreen.Off, runtime.screen.value)
        assertTrue(receive(relay(true)))
        assertEquals(WatchScreen.Quiet, runtime.screen.value)
        assertTrue(receive(alert("a")))
        assertEquals(listOf("a"), (runtime.screen.value as WatchScreen.Alerts).lines.map { it.alertId })
    }

    @Test
    fun aMessageOnAnotherPathChangesNothing() {
        assertFalse(runtime.onPhoneMessage("/armor/other", relay(true)))
        assertEquals(WatchScreen.Off, runtime.screen.value)
    }

    @Test
    fun refreshDropsAnAlertThatExpired() {
        receive(relay(true))
        receive(alert("a", ttl = 30))
        clock += 30
        assertTrue(runtime.screen.value is WatchScreen.Alerts)   // nobody looked yet
        runtime.refresh()
        assertEquals(WatchScreen.Quiet, runtime.screen.value)
    }

    @Test
    fun confirmingMarksTheLineAndHandsOverTheAcknowledgement() {
        receive(relay(true))
        receive(alert("a"))
        assertEquals(ConfirmResult.SENT, runtime.confirm("a"))
        assertEquals(1, phone.received.size)
        val line = (runtime.screen.value as WatchScreen.Alerts).lines.single()
        assertTrue(line.confirmed)
        assertFalse(line.canConfirm)
    }

    @Test
    fun aPhoneOutOfReachLeavesTheButton() {
        receive(relay(true))
        receive(alert("a"))
        phone.reachable = false
        assertEquals(ConfirmResult.LINK_DOWN, runtime.confirm("a"))
        assertTrue((runtime.screen.value as WatchScreen.Alerts).lines.single().canConfirm)
    }
}
