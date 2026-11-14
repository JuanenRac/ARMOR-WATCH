// =============================================================================
// ARMOR-WATCH - wear/src/main/kotlin/es/electrohobby3d/armor/watch/WatchRuntime.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package es.electrohobby3d.armor.watch

import armor.watch.ConfirmResult
import armor.watch.PhoneLink
import armor.watch.WatchScreen
import armor.watch.WatchSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the Android parts share: one [WatchSession] behind a lock (the service of the Data Layer and the screen are on different threads) and the screen model as a flow that the
 * Compose screen collects. No Android class is used here, so it is tested on a computer.
 */
class WatchRuntime(link: PhoneLink, now: () -> Long) {
    private val lock = Any()
    private val session = WatchSession(link, now)
    private val state = MutableStateFlow<WatchScreen>(WatchScreen.Off)

    /** What the screen draws now. */
    val screen: StateFlow<WatchScreen> = state.asStateFlow()

    /** A text from the phone on [path]; true when the screen changed. */
    fun onPhoneMessage(path: String, raw: String): Boolean = synchronized(lock) {
        val changed = session.onPhoneMessage(path, raw)
        state.value = session.screen()
        changed
    }

    /** Looks again at the clock: an alert that expired leaves the screen. */
    fun refresh() = synchronized(lock) { state.value = session.screen() }

    /** The person pressed the confirm button of [alertId]. It waits for the Data Layer, so it is not called on the main thread. */
    fun confirm(alertId: String): ConfirmResult = synchronized(lock) {
        val result = session.confirm(alertId)
        state.value = session.screen()
        result
    }
}
