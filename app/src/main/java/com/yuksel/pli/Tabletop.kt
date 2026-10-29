package com.yuksel.pli

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * True while the phone stands half-folded (hinge between about 60° and 150°). Read from the hinge sensor,
 * only while [enabled] (setting on, inner screen showing, launcher on screen).
 */
@Composable
fun rememberHalfFolded(enabled: Boolean): Boolean {
    val context = LocalContext.current
    var half by remember { mutableStateOf(false) }
    DisposableEffect(enabled) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val angle = event.values.firstOrNull() ?: return
                // A little hysteresis so it doesn't flicker at the edges.
                half = if (half) angle in 50f..160f else angle in 60f..150f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (enabled && sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL) else half = false
        onDispose { manager.unregisterListener(listener) }
    }
    return half && enabled
}

/**
 * Tabletop mode: the phone stands like a small laptop. Top half: clock, date and what's playing;
 * bottom half (on the table): big music controls, the next event and the dock apps.
 */
@Composable
fun TabletopHome(launcher: Launcher, today: TodayState, onOpenApp: (AppInfo, Rect) -> Unit, onOpenPackage: (String) -> Unit) {
    val now = rememberMinute()
    val playing by MediaWatcher.nowPlaying.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE)).uppercase(Locale.FRANCE), color = Holo.Muted,
                fontSize = 13.sp, letterSpacing = 3.sp)
            IridescentText(now.format(DateTimeFormatter.ofPattern("HH:mm")), 120.sp, letterSpacing = (-4).sp)
            val p = playing
            if (p != null) Text(p.title + if (p.artist.isNotBlank()) " · ${p.artist}" else "", color = Holo.Soft, fontSize = 16.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clickable { onOpenPackage(p.packageName) })
            else today.weather?.let { w -> Text("${w.temperature}° · ${w.label} · ${w.place}", color = Holo.Soft, fontSize = 16.sp) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(Holo.Iridescent.map { it.copy(alpha = .4f) })))
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp, vertical = 18.dp), verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (playing != null) Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                TableButton(Icons.Rounded.SkipPrevious, "Précédent", 64) { MediaWatcher.previous() }
                TableButton(if (playing?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Lecture / pause", 88, true) { MediaWatcher.toggle() }
                TableButton(Icons.Rounded.SkipNext, "Suivant", 64) { MediaWatcher.next() }
            } else today.events.firstOrNull()?.let { e ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PROCHAIN", color = Holo.Muted, fontSize = 11.sp, letterSpacing = 2.5.sp)
                    Text(e.title, color = Holo.Ink, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (e.allDay) "Toute la journée" else e.start.format(DateTimeFormatter.ofPattern("HH:mm")), color = Holo.Soft, fontSize = 15.sp)
                }
            }
            val dock = launcher.visibleDock(Screen.INNER).mapNotNull(launcher.byId::get).take(7)
            if (dock.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                dock.forEach { app -> Box(Modifier.width(78.dp)) { AppChoice(app, 52.dp, onOpenApp, null) } }
            }
            Text("Déplie complètement pour revenir à l’accueil", color = Holo.Muted.copy(alpha = .7f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun TableButton(icon: ImageVector, label: String, size: Int, main: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(size.dp).clip(CircleShape)
        .then(if (main) Modifier.background(Brush.linearGradient(Holo.Iridescent), CircleShape) else Modifier.glass(CircleShape, .08f))
        .clickable(onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (main) Color(0xFF120A22) else Holo.Ink, modifier = Modifier.size((size * .45f).dp))
    }
}
