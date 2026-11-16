// =============================================================================
// ARMOR-WATCH - src/test/kotlin/armor/watch/NodeTargetsTest.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package armor.watch

import kotlin.test.Test
import kotlin.test.assertEquals

/** Which nodes a message goes to: the ones that declare the capability of the other app, else the connected ones. */
class NodeTargetsTest {
    @Test
    fun `the nodes that declare the capability win over the connected ones`() {
        assertEquals(listOf("phone"), WatchProtocol.targets(listOf("phone"), listOf("phone", "tablet", "car")))
    }

    @Test
    fun `without a node that declares it the connected nodes are used`() {
        assertEquals(listOf("tablet", "car"), WatchProtocol.targets(emptyList<String>(), listOf("tablet", "car")))
    }

    @Test
    fun `with nobody there is nobody to send to and the capability names are fixed`() {
        assertEquals(emptyList<String>(), WatchProtocol.targets(emptyList<String>(), emptyList()))
        assertEquals("armor_phone_alerts", WatchProtocol.CAPABILITY_PHONE)
        assertEquals("armor_watch_alerts", WatchProtocol.CAPABILITY_WATCH)
    }
}
