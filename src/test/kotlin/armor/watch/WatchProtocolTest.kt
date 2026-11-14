// =============================================================================
// ARMOR-WATCH - src/test/kotlin/armor/watch/WatchProtocolTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The Kotlin code against docs/watch_alert_contract.json: the same accepted and rejected examples tools/watch_contract.py runs. */
class WatchProtocolTest {
    private val contract = JSONObject(File("docs/watch_alert_contract.json").readText(Charsets.UTF_8))

    /** An example may write a long text as {"repeat": "x", "count": 161}; this gives the example as the text that travels. */
    private fun expand(value: Any?): Any? = when (value) {
        is JSONObject -> {
            if (value.length() == 2 && value.has("repeat") && value.has("count")) {
                value.getString("repeat").repeat(value.getInt("count"))
            } else {
                JSONObject().also { out -> value.keys().forEach { key -> out.put(key, expand(value.get(key))) } }
            }
        }
        is JSONArray -> JSONArray().also { out -> for (i in 0 until value.length()) out.put(expand(value.get(i))) }
        else -> value
    }

    @Test
    fun everyValidExampleIsAccepted() {
        val valid = contract.getJSONArray("valid")
        assertTrue(valid.length() >= 7)
        for (i in 0 until valid.length()) {
            val raw = (expand(valid.get(i)) as JSONObject).toString()
            assertNotNull(WatchProtocol.parse(raw), "valid[$i] was refused: $raw")
        }
    }

    @Test
    fun everyInvalidExampleIsRefused() {
        val invalid = contract.getJSONArray("invalid")
        assertTrue(invalid.length() >= 20)
        for (i in 0 until invalid.length()) {
            val example = expand(invalid.get(i)) as JSONObject
            val why = example.remove("why")
            assertNull(WatchProtocol.parse(example.toString()), "invalid[$i] was accepted: $why")
        }
    }

    @Test
    fun theLimitsOfTheCodeAreTheLimitsOfTheContract() {
        val limits = contract.getJSONObject("limits")
        assertEquals(limits.getInt("rawMax"), WatchProtocol.RAW_MAX)
        assertEquals(limits.getInt("titleMax"), WatchProtocol.TITLE_MAX)
        assertEquals(limits.getInt("textMax"), WatchProtocol.TEXT_MAX)
        assertEquals(limits.getInt("pendingMax"), WatchProtocol.PENDING_MAX)
        assertEquals(limits.getJSONArray("ttlSeconds").getInt(0), WatchProtocol.TTL_MIN_SECONDS)
        assertEquals(limits.getJSONArray("ttlSeconds").getInt(1), WatchProtocol.TTL_MAX_SECONDS)
        assertEquals(contract.getInt("protocolVersion"), WatchProtocol.VERSION)
        assertEquals(contract.getJSONObject("paths").getString("toWatch"), WatchProtocol.PATH_TO_WATCH)
        assertEquals(contract.getJSONObject("paths").getString("toPhone"), WatchProtocol.PATH_TO_PHONE)
        val severities = contract.getJSONArray("severities")
        assertEquals((0 until severities.length()).map { severities.getString(it) }, Severity.entries.map { it.wire })
    }

    @Test
    fun anAlertIsReadWithAllItsFields() {
        val alert = WatchProtocol.parse(
            """{"type":"alert","protocolVersion":1,"alertId":"al-1","severity":"critical","title":"Door","text":"Opened","needsAck":true,"ttlSeconds":60}""",
        )
        assertEquals(Alert("al-1", Severity.CRITICAL, "Door", "Opened", true, 60), alert)
    }

    @Test
    fun garbageNeverThrows() {
        for (raw in listOf("", "{", "[]", "null", "42", "\"text\"", "{\"type\":null}", "{\"type\":1,\"protocolVersion\":1}")) {
            assertNull(WatchProtocol.parse(raw), raw)
        }
    }

    @Test
    fun theAcknowledgementIsWhatThePhoneExpects() {
        val raw = WatchProtocol.encode(AlertAck("al-1"))
        assertEquals(AlertAck("al-1"), WatchProtocol.parse(raw))
        val obj = JSONObject(raw)
        assertEquals(setOf("type", "protocolVersion", "alertId"), obj.keySet())
    }
}
