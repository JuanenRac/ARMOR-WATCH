// =============================================================================
// ARMOR-WATCH - wear/src/main/kotlin/es/electrohobby3d/armor/watch/DataLayer.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package es.electrohobby3d.armor.watch

import android.content.Context
import android.os.SystemClock
import armor.watch.PhoneLink
import armor.watch.WatchProtocol
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.TimeUnit

/** The way to the phone through the Data Layer: the confirmation goes as a message to the phone that is connected, nearest first. Never called on the main thread. */
class DataLayerPhoneLink(context: Context) : PhoneLink {
    private val app = context.applicationContext

    override fun send(path: String, text: String): Boolean = try {
        val nodes = Tasks.await(Wearable.getNodeClient(app).connectedNodes, TIMEOUT_S, TimeUnit.SECONDS)
        val node = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
        if (node == null) {
            false
        } else {
            Tasks.await(Wearable.getMessageClient(app).sendMessage(node.id, path, text.toByteArray(Charsets.UTF_8)), TIMEOUT_S, TimeUnit.SECONDS)
            true
        }
    } catch (_: Exception) {
        false   // no phone in reach, a timeout or the service of the Data Layer is not there: the person can press again
    }

    private companion object {
        const val TIMEOUT_S = 3L
    }
}

/** The one runtime of the process. */
object WatchRuntimeHolder {
    @Volatile
    private var instance: WatchRuntime? = null

    fun get(context: Context): WatchRuntime =
        instance ?: synchronized(this) {
            instance ?: WatchRuntime(DataLayerPhoneLink(context)) { SystemClock.elapsedRealtime() / 1000 }.also { instance = it }
        }
}

/** The messages of the phone arrive here, even when the screen is closed. Only the path of the alerts is listened to (the manifest says so and the session checks it again). */
class AlertListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WatchProtocol.PATH_TO_WATCH) return
        WatchRuntimeHolder.get(this).onPhoneMessage(event.path, String(event.data, Charsets.UTF_8))
    }
}
