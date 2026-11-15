// =============================================================================
// ARMOR-WATCH - src/test/kotlin/armor/watch/SimulatedPairTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The watch and the phone talking to each other, with a server behind the phone and faults on the link, all in memory (evidence: simulator). */
class SimulatedPairTest {
    private val pair = SimulatedPair()

    private fun ids() = (pair.watch.screen() as? WatchScreen.Alerts)?.lines?.map { it.alertId } ?: emptyList()

    private fun raise(alarmId: String, severity: String = "high") {
        pair.server.raise(alarmId)
        pair.phone.notice(alarmId, alarmId, severity, "Intrusion", "Radar 1 saw movement")
    }

    @Test
    fun nothingReachesTheWatchWhileTheOptionIsOff() {
        pair.server.raise("al-1")
        assertFalse(pair.phone.notice("al-1", "al-1", "high", "Intrusion", "Radar 1"))
        assertTrue(pair.toWatch.arrived.isEmpty())
        assertEquals(WatchScreen.Off, pair.watch.screen())
    }

    @Test
    fun anAlarmReachesTheWatchAndATapAcknowledgesItOnTheServerOnce() {
        pair.phone.setOption(true)
        raise("al-1")
        assertEquals(listOf("al-1"), ids())
        assertEquals(ConfirmResult.SENT, pair.watch.confirm("al-1"))
        assertEquals(listOf("al-1"), pair.server.acknowledged)
        assertEquals(listOf("al-1"), pair.phone.acknowledgedForWatch)
        // the phone then tells the watch the alert is over
        pair.phone.clear("al-1")
        assertEquals(WatchScreen.Quiet, pair.watch.screen())
        // a second tap finds nothing, and a repeated message from the watch is not acted on again
        assertEquals(ConfirmResult.NOTHING_TO_CONFIRM, pair.watch.confirm("al-1"))
        assertFalse(pair.phone.onWatchMessage(WatchProtocol.PATH_TO_PHONE, WatchProtocol.encode(AlertAck("al-1"))))
        assertEquals(1, pair.server.acknowledged.size)
    }

    @Test
    fun aDuplicatedConfirmationOnTheLinkAcknowledgesOnlyOnce() {
        pair.phone.setOption(true)
        raise("al-1")
        pair.toPhone.duplicateNext = true
        assertEquals(ConfirmResult.SENT, pair.watch.confirm("al-1"))
        assertEquals(2, pair.toPhone.arrived.size)
        assertEquals(listOf("al-1"), pair.server.acknowledged)
        assertEquals(listOf("unknown"), pair.phone.refusals)
    }

    @Test
    fun aPhoneOutOfRangeLeavesTheButtonToPressAgainAndTheAlarmOpen() {
        pair.phone.setOption(true)
        raise("al-1")
        pair.toPhone.up = false
        assertEquals(ConfirmResult.LINK_DOWN, pair.watch.confirm("al-1"))
        assertTrue(pair.server.isOpen("al-1"))
        assertTrue((pair.watch.screen() as WatchScreen.Alerts).lines.single().canConfirm)
        pair.toPhone.up = true
        assertEquals(ConfirmResult.SENT, pair.watch.confirm("al-1"))
        assertFalse(pair.server.isOpen("al-1"))
    }

    @Test
    fun aConfirmationLostOnTheWayLooksSentOnTheWatchAndNeverActsOnTheServer() {
        pair.phone.setOption(true)
        raise("al-1")
        pair.toPhone.dropNext = 1
        assertEquals(ConfirmResult.SENT, pair.watch.confirm("al-1"))
        assertTrue(pair.server.isOpen("al-1"), "the phone never heard of it")
        assertTrue((pair.watch.screen() as WatchScreen.Alerts).lines.single().confirmed)
        assertEquals(1, pair.toPhone.lost.size)
        // the phone has the alarm and clears the watch itself when it learns elsewhere that the alarm is closed
        pair.phone.clear("al-1")
        assertEquals(WatchScreen.Quiet, pair.watch.screen())
    }

    @Test
    fun anAlertThatWasHeldUpOnTheLinkIsRefusedWhenItArrivesAfterItsTimeToLive() {
        pair.phone.setOption(true)
        pair.toWatch.held = true
        raise("al-1")
        pair.advance(5)
        pair.toWatch.release()
        // the watch counts the time to live from the moment it receives the alert, so it shows it
        assertEquals(listOf("al-1"), ids())
        // but the phone counts from the moment it sent it: after two minutes the confirmation of a lesser alert is not accepted
        pair.advance(120)
        assertEquals(WatchScreen.Quiet, pair.watch.screen(), "the watch dropped it too")
        assertFalse(pair.phone.onWatchMessage(WatchProtocol.PATH_TO_PHONE, WatchProtocol.encode(AlertAck("al-1"))))
        assertEquals(listOf("expired"), pair.phone.refusals)
        assertTrue(pair.server.isOpen("al-1"))
    }

    @Test
    fun theWatchThatStartsAgainIsOffUntilThePhoneSwitchesItOnAgain() {
        pair.phone.setOption(true)
        raise("al-1")
        pair.restartWatch()
        assertEquals(WatchScreen.Off, pair.watch.screen())
        assertEquals(ConfirmResult.NOTHING_TO_CONFIRM, pair.watch.confirm("al-1"))
        // an alert that arrives before the switch is dropped; the next notice of the phone sends the switch again
        raise("al-2")
        assertEquals(WatchScreen.Off, pair.watch.screen())
        pair.phone.setOption(true)
        raise("al-3")
        assertEquals(listOf("al-3"), ids())
    }

    @Test
    fun aBurstThatArrivesInTheWrongOrderStillKeepsTheMostSeriousFirstAndTheSwitchWins() {
        pair.phone.setOption(true)
        pair.toWatch.held = true
        raise("lesser", "warning")
        raise("worst", "critical")
        pair.toWatch.release(reversed = true)
        assertEquals(listOf("worst", "lesser"), ids())
        // the switch off that was sent last wins
        pair.toWatch.held = true
        pair.phone.setOption(false)
        pair.toWatch.release()
        assertEquals(WatchScreen.Off, pair.watch.screen())
    }

    @Test
    fun theWatchKeepsOnlyEightAlertsAndTheCriticalOnesStay() {
        pair.phone.setOption(true)
        raise("crit", "critical")
        for (i in 1..10) raise("lesser-$i")
        val shown = ids()
        assertEquals(WatchProtocol.PENDING_MAX, shown.size)
        assertEquals("crit", shown.first())
        assertFalse("lesser-1" in shown)
        assertTrue("lesser-10" in shown)
    }

    @Test
    fun switchingTheOptionOffClearsTheWatchAndTheConfirmationsStopBeingAccepted() {
        pair.phone.setOption(true)
        raise("al-1")
        pair.phone.setOption(false)
        assertEquals(WatchScreen.Off, pair.watch.screen())
        assertEquals(ConfirmResult.NOTHING_TO_CONFIRM, pair.watch.confirm("al-1"))
        assertFalse(pair.phone.onWatchMessage(WatchProtocol.PATH_TO_PHONE, WatchProtocol.encode(AlertAck("al-1"))))
        assertEquals(listOf("option off"), pair.phone.refusals)
        assertTrue(pair.server.isOpen("al-1"))
    }

    @Test
    fun aServerThatCannotBeReachedMakesTheConfirmationFailOnThePhoneNotOnTheWatch() {
        pair.phone.setOption(true)
        raise("al-1")
        pair.server.reachable = false
        assertEquals(ConfirmResult.SENT, pair.watch.confirm("al-1"))
        assertEquals(listOf("server"), pair.phone.refusals)
        assertTrue(pair.phone.acknowledgedForWatch.isEmpty())
        assertTrue(pair.server.isOpen("al-1"))
    }

    @Test
    fun theWatchIgnoresWhatTheContractForbidsEvenWhenThePhoneSendsIt() {
        pair.phone.setOption(true)
        assertTrue(pair.toWatch.send(WatchProtocol.PATH_TO_WATCH, """{"type":"alert","protocolVersion":1,"alertId":"a b","severity":"info","title":"t","text":"x","needsAck":false,"ttlSeconds":60}"""))
        assertTrue(pair.toWatch.send(WatchProtocol.PATH_TO_WATCH, """{"type":"alert","protocolVersion":1,"alertId":"a","severity":"info","title":"t","text":"x","needsAck":false,"ttlSeconds":4}"""))
        assertTrue(pair.toWatch.send("/armor/other", """{"type":"relay","protocolVersion":1,"enabled":false}"""))
        assertEquals(WatchScreen.Quiet, pair.watch.screen())
        assertIs<WatchScreen.Quiet>(pair.watch.screen())
    }

    @Test
    fun theNoticeOfTheLesserKindsTravelsAsInfoWithItsTextCutToTheLimits() {
        pair.phone.setOption(true)
        pair.server.raise("al-1")
        assertTrue(pair.phone.notice("n7", null, "warning", "T".repeat(100), "X".repeat(500)))
        val line = (pair.watch.screen() as WatchScreen.Alerts).lines.single()
        assertEquals(Severity.INFO, line.severity)
        assertEquals(WatchProtocol.TITLE_MAX, line.title.length)
        assertEquals(WatchProtocol.TEXT_MAX, line.text.length)
        assertFalse(line.canConfirm, "a notice with no alarm asks for no confirmation")
        assertNull((pair.watch.screen() as WatchScreen.Alerts).lines.firstOrNull { it.canConfirm })
    }

    @Test
    fun theChannelRecordsWhatWasLostAndWhatArrived() {
        val channel = SimulatedChannel { _, _ -> }
        channel.up = false
        assertFalse(channel.send("/x", "one"))
        channel.up = true
        channel.dropNext = 1
        assertTrue(channel.send("/x", "two"))
        assertTrue(channel.send("/x", "three"))
        assertEquals(listOf("/x" to "one", "/x" to "two"), channel.lost)
        assertEquals(listOf("/x" to "three"), channel.arrived)
    }
}
