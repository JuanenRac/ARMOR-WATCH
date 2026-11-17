// =============================================================================
// ARMOR-WATCH - src/test/kotlin/armor/watch/WatchSessionTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The watch end of the conversation, tried against a phone that exists only in memory (evidence: simulator). */
class WatchSessionTest {
    private var clock = 1_000L
    private val phone = SimulatedPhone()
    private val session = WatchSession(phone) { clock }

    private fun relay(on: Boolean) = """{"type":"relay","protocolVersion":1,"enabled":$on}"""

    private fun alert(id: String, severity: String = "warning", needsAck: Boolean = true, ttl: Int = 60) =
        """{"type":"alert","protocolVersion":1,"alertId":"$id","severity":"$severity","title":"Title $id","text":"Text $id","needsAck":$needsAck,"ttlSeconds":$ttl}"""

    private fun clear(id: String) = """{"type":"alert_clear","protocolVersion":1,"alertId":"$id"}"""

    private fun lines() = assertIs<WatchScreen.Alerts>(session.screen()).lines

    @Test
    fun theScreenIsOffUntilThePhoneSwitchesTheAlertsOn() {
        assertEquals(WatchScreen.Off, session.screen())
        assertFalse(phone.push(session, alert("a")))
        assertEquals(WatchScreen.Off, session.screen())
        assertTrue(phone.push(session, relay(true)))
        assertEquals(WatchScreen.Quiet, session.screen())
    }

    @Test
    fun anAlertAppearsSeriousFirstAndGoesWhenThePhoneClearsIt() {
        phone.push(session, relay(true))
        phone.push(session, alert("a", "info"))
        phone.push(session, alert("b", "critical"))
        assertEquals(listOf("b", "a"), lines().map { it.alertId })
        assertTrue(phone.push(session, clear("b")))
        assertEquals(listOf("a"), lines().map { it.alertId })
        assertTrue(phone.push(session, clear("a")))
        assertEquals(WatchScreen.Quiet, session.screen())
    }

    @Test
    fun anAlertThatExpiresLeavesTheScreenWithoutAnyMessage() {
        phone.push(session, relay(true))
        phone.push(session, alert("a", ttl = 30))
        clock += 29
        assertEquals(1, lines().size)
        clock += 1
        assertEquals(WatchScreen.Quiet, session.screen())
    }

    @Test
    fun withTheSenderBoundOnlyThePhoneThatSwitchedTheAlertsOnIsListenedTo() {
        session.bindSender = true
        assertTrue(session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, relay(true), "phone-a"))
        assertFalse(session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, alert("x1"), "node-b"))
        assertFalse(session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, relay(false), "node-b"))
        assertIs<WatchScreen.Quiet>(session.screen())
        assertTrue(session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, alert("x1"), "phone-a"))
        assertEquals(1, lines().size)
        // switched off by the same phone, it is free again for the next one
        session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, relay(false), "phone-a")
        assertTrue(session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, relay(true), "phone-c"))
    }

    @Test
    fun withoutTheSwitchAnyPhoneCanSendAsBefore() {
        assertTrue(session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, relay(true), "phone-a"))
        assertTrue(session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, alert("x2"), "node-b"))
        assertTrue(session.onPhoneMessage(WatchProtocol.PATH_TO_WATCH, alert("x3")))
    }

    @Test
    fun onlyThePathOfTheAlertsIsListenedTo() {
        assertFalse(session.onPhoneMessage("/armor/other", relay(true)))
        assertFalse(session.onPhoneMessage(WatchProtocol.PATH_TO_PHONE, relay(true)))
        assertEquals(WatchScreen.Off, session.screen())
    }

    @Test
    fun aMessageThatIsNotInTheContractChangesNothing() {
        phone.push(session, relay(true))
        for (bad in listOf("", "not json", "{}", """{"type":"alert","protocolVersion":2}""", alert("a").replace("\"warning\"", "\"fatal\""))) {
            assertFalse(phone.push(session, bad), bad)
        }
        assertEquals(WatchScreen.Quiet, session.screen())
    }

    @Test
    fun confirmingHandsTheAcknowledgementToThePhoneOnceAndMarksTheLine() {
        phone.push(session, relay(true))
        phone.push(session, alert("a"))
        assertTrue(lines().single().canConfirm)
        assertEquals(ConfirmResult.SENT, session.confirm("a"))
        assertEquals(listOf(WatchProtocol.PATH_TO_PHONE to WatchProtocol.encode(AlertAck("a"))), phone.received)
        val line = lines().single()
        assertTrue(line.confirmed)
        assertFalse(line.canConfirm)
        assertEquals(ConfirmResult.NOTHING_TO_CONFIRM, session.confirm("a"))
        assertEquals(1, phone.received.size)
    }

    @Test
    fun anAlertThatAsksForNoConfirmationOffersNoButton() {
        phone.push(session, relay(true))
        phone.push(session, alert("a", needsAck = false))
        assertFalse(lines().single().canConfirm)
        assertEquals(ConfirmResult.NOTHING_TO_CONFIRM, session.confirm("a"))
        assertTrue(phone.received.isEmpty())
    }

    @Test
    fun anUnknownOrExpiredAlertCannotBeConfirmed() {
        phone.push(session, relay(true))
        assertEquals(ConfirmResult.NOTHING_TO_CONFIRM, session.confirm("nope"))
        phone.push(session, alert("a", ttl = 10))
        clock += 10
        assertEquals(ConfirmResult.NOTHING_TO_CONFIRM, session.confirm("a"))
        assertTrue(phone.received.isEmpty())
    }

    @Test
    fun aPhoneOutOfReachLeavesTheButtonToPressAgain() {
        phone.push(session, relay(true))
        phone.push(session, alert("a"))
        phone.reachable = false
        assertEquals(ConfirmResult.LINK_DOWN, session.confirm("a"))
        assertTrue(lines().single().canConfirm)
        assertFalse(lines().single().confirmed)
        assertTrue(phone.received.isEmpty())
        phone.reachable = true
        assertEquals(ConfirmResult.SENT, session.confirm("a"))
        assertEquals(1, phone.received.size)
    }

    @Test
    fun switchingTheAlertsOffForgetsEverythingAndRefusesTheConfirmation() {
        phone.push(session, relay(true))
        phone.push(session, alert("a"))
        phone.push(session, relay(false))
        assertEquals(WatchScreen.Off, session.screen())
        assertEquals(ConfirmResult.NOTHING_TO_CONFIRM, session.confirm("a"))
    }

    @Test
    fun theSimulatedPhoneRecordsOnlyWhatReachedIt() {
        assertTrue(phone.send("/x", "one"))
        phone.reachable = false
        assertFalse(phone.send("/x", "two"))
        assertEquals(listOf("/x" to "one"), phone.received)
    }
}
