// =============================================================================
// ARMOR-WATCH - src/test/kotlin/armor/watch/AlertInboxTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlertInboxTest {
    private var clock = 1_000L
    private val inbox = AlertInbox { clock }

    private fun alert(id: String, severity: Severity = Severity.WARNING, needsAck: Boolean = true, ttl: Int = 60) =
        Alert(id, severity, "Title $id", "Text $id", needsAck, ttl)

    @Test
    fun itStartsSwitchedOffAndShowsNothing() {
        assertFalse(inbox.enabled)
        assertFalse(inbox.accept(alert("a")))
        assertTrue(inbox.pending().isEmpty())
    }

    @Test
    fun anAlertIsShownOnlyAfterThePhoneSwitchesTheRelayOn() {
        assertTrue(inbox.accept(RelayState(true)))
        assertTrue(inbox.accept(alert("a")))
        assertEquals(listOf("a"), inbox.pending().map { it.alertId })
    }

    @Test
    fun switchingTheRelayOffForgetsEverything() {
        inbox.accept(RelayState(true))
        inbox.accept(alert("a"))
        assertTrue(inbox.accept(RelayState(false)))
        assertTrue(inbox.pending().isEmpty())
        assertFalse(inbox.accept(alert("b")))
    }

    @Test
    fun theMostSeriousComesFirstAndTheNewestAmongEquals() {
        inbox.accept(RelayState(true))
        inbox.accept(alert("info", Severity.INFO))
        inbox.accept(alert("w1", Severity.WARNING))
        inbox.accept(alert("crit", Severity.CRITICAL))
        inbox.accept(alert("w2", Severity.WARNING))
        assertEquals(listOf("crit", "w2", "w1", "info"), inbox.pending().map { it.alertId })
    }

    @Test
    fun anAlertWithTheSameIdReplacesTheOldOne() {
        inbox.accept(RelayState(true))
        inbox.accept(alert("a", Severity.INFO))
        inbox.accept(alert("a", Severity.CRITICAL))
        assertEquals(listOf(Severity.CRITICAL), inbox.pending().map { it.severity })
    }

    @Test
    fun anExpiredAlertDisappears() {
        inbox.accept(RelayState(true))
        inbox.accept(alert("a", ttl = 10))
        clock += 9
        assertEquals(1, inbox.pending().size)
        clock += 1
        assertTrue(inbox.pending().isEmpty())
        assertNull(inbox.acknowledge("a"))
    }

    @Test
    fun theClearOfThePhoneRemovesTheAlert() {
        inbox.accept(RelayState(true))
        inbox.accept(alert("a"))
        assertFalse(inbox.accept(AlertClear("other")))
        assertTrue(inbox.accept(AlertClear("a")))
        assertTrue(inbox.pending().isEmpty())
    }

    @Test
    fun theConfirmationGoesOutOnceAndOnlyForAnAlertThatAsksForIt() {
        inbox.accept(RelayState(true))
        inbox.accept(alert("ask", needsAck = true))
        inbox.accept(alert("notice", needsAck = false))
        assertNull(inbox.acknowledge("notice"))
        assertNull(inbox.acknowledge("unknown"))
        assertEquals(AlertAck("ask"), inbox.acknowledge("ask"))
        assertNull(inbox.acknowledge("ask"))
        assertTrue(inbox.isAcknowledged("ask"))
        // it stays on the screen until the phone clears it
        assertEquals(2, inbox.pending().size)
    }

    @Test
    fun noMoreThanTheLimitIsKeptAndACriticalOneSurvivesTheOthers() {
        inbox.accept(RelayState(true))
        inbox.accept(alert("crit", Severity.CRITICAL))
        for (i in 1..WatchProtocol.PENDING_MAX + 3) inbox.accept(alert("n$i", Severity.INFO))
        val ids = inbox.pending().map { it.alertId }
        assertEquals(WatchProtocol.PENDING_MAX, ids.size)
        assertTrue("crit" in ids)
        assertEquals("crit", ids.first())
        assertTrue("n${WatchProtocol.PENDING_MAX + 3}" in ids)
    }

    @Test
    fun anAckFromThePhoneSideIsIgnored() {
        inbox.accept(RelayState(true))
        assertFalse(inbox.accept(AlertAck("a")))
    }
}
