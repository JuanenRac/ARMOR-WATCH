// =============================================================================
// ARMOR-WATCH - wear/src/main/kotlin/es/electrohobby3d/armor/watch/MainActivity.kt
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D)
// GPL-3.0-or-later - see LICENSE
// =============================================================================
package es.electrohobby3d.armor.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import armor.watch.AlertLine
import armor.watch.ConfirmResult
import armor.watch.Severity
import armor.watch.WatchScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = WatchRuntimeHolder.get(this)
        setContent { WatchApp(runtime) }
    }
}

private val RED = Color(0xFFEE6B80)
private val AMBER = Color(0xFFF3BA55)
private val CYAN = Color(0xFF38D4E6)

private fun colourOf(severity: Severity): Color = when (severity) {
    Severity.CRITICAL -> RED
    Severity.WARNING -> AMBER
    Severity.INFO -> CYAN
}

@Composable
fun WatchApp(runtime: WatchRuntime) {
    // an alert that expires leaves the screen without any message, so the screen looks at the clock once a second
    LaunchedEffect(runtime) {
        while (true) {
            delay(1000)
            runtime.refresh()
        }
    }
    val screen by runtime.screen.collectAsState()
    MaterialTheme {
        when (val current = screen) {
            WatchScreen.Off -> Message(stringResource(R.string.alerts_off))
            WatchScreen.Quiet -> Message(stringResource(R.string.no_alerts))
            is WatchScreen.Alerts -> AlertList(current.lines, runtime)
        }
    }
}

@Composable
private fun Message(text: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AlertList(lines: List<AlertLine>, runtime: WatchRuntime) {
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<ConfirmResult?>(null) }
    ScalingLazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        items(lines, key = { it.alertId }) { line ->
            Column(Modifier.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(line.title, color = colourOf(line.severity), textAlign = TextAlign.Center)
                Text(line.text, textAlign = TextAlign.Center)
                if (line.confirmed) {
                    Text(stringResource(R.string.confirmed), color = CYAN)
                } else if (line.canConfirm) {
                    Button(onClick = {
                        // the Data Layer is waited for: never on the main thread
                        scope.launch { notice = withContext(Dispatchers.IO) { runtime.confirm(line.alertId) } }
                    }) { Text(stringResource(R.string.confirm)) }
                }
            }
        }
        if (notice == ConfirmResult.LINK_DOWN) {
            item { Text(stringResource(R.string.phone_out_of_reach), color = AMBER, textAlign = TextAlign.Center) }
        }
    }
}
