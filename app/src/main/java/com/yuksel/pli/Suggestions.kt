package com.yuksel.pli

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.LocalDateTime

/** What the phone is doing right now, and the apps that go with it. */
data class Suggestion(val reason: String, val apps: List<AppInfo>, val contextual: Boolean)

private fun headphones(context: Context): Boolean = runCatching {
    context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
        it.type in setOf(AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_BLE_HEADSET)
    }
}.getOrDefault(false)

/** Name of a connected car (Bluetooth car audio or hands-free kit), if any. */
@SuppressLint("MissingPermission")
private fun car(context: Context): String? {
    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return null
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
    return runCatching {
        adapter.bondedDevices.orEmpty().firstOrNull { device ->
            val connected = runCatching { BluetoothDevice::class.java.getMethod("isConnected").invoke(device) as Boolean }.getOrDefault(false)
            connected && device.bluetoothClass?.deviceClass in setOf(BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO, BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE)
        }?.let { it.alias ?: it.name ?: "Voiture" }
    }.getOrNull()
}

private fun clockPackage(context: Context): String? = runCatching {
    context.packageManager.resolveActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS), 0)?.activityInfo?.packageName
}.getOrNull()

/**
 * Picks the row to show: in the car, maps and music; headphones on, music and videos; a meeting with an address soon,
 * maps; late evening while charging, the alarm clock; otherwise (mode "always") the apps usually opened at this hour.
 */
fun suggestionFor(context: Context, launcher: Launcher, today: TodayState, playing: String?, exclude: Set<String>, mode: String): Suggestion? {
    if (mode == "off") return null
    val usual = UsageLog.suggestions(context, launcher.byId, 60)
    val rank = usual.withIndex().associate { (i, app) -> app.id to i }
    val visible = launcher.visibleApps.filter { it.id !in exclude && !it.work }
    fun pick(vararg categories: Int, first: String? = null): List<AppInfo> {
        val head = first?.let { pkg -> visible.firstOrNull { it.packageName == pkg } }
        val inCategory = visible.filter { it.category in categories }.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
        return (listOfNotNull(head) + inCategory).distinctBy { it.id }.take(5)
    }
    car(context)?.let { name ->
        val apps = (pick(ApplicationInfo.CATEGORY_MAPS) + pick(ApplicationInfo.CATEGORY_AUDIO, first = playing)).distinctBy { it.id }.take(5)
        if (apps.isNotEmpty()) return Suggestion("En voiture · $name", apps, true)
    }
    if (headphones(context)) {
        val apps = pick(ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, first = playing)
        if (apps.isNotEmpty()) return Suggestion("Casque connecté", apps, true)
    }
    val now = LocalDateTime.now()
    today.events.firstOrNull { it.location.isNotBlank() && !it.allDay && Duration.between(now, it.start).toMinutes() in 0..60 }?.let { e ->
        val apps = pick(ApplicationInfo.CATEGORY_MAPS)
        if (apps.isNotEmpty()) return Suggestion("${e.title} à ${e.start.toLocalTime().toString().take(5)}", apps, true)
    }
    val battery = today.battery
    if (battery != null && battery.charging && (now.hour >= 21 || now.hour < 4)) {
        val apps = (pick(-2, first = clockPackage(context)) + usual.filter { it.id !in exclude }).distinctBy { it.id }.take(5)
        if (apps.isNotEmpty()) return Suggestion("Avant de dormir", apps, true)
    }
    if (mode != "always") return null
    val apps = usual.filter { it.id !in exclude && it.id !in launcher.layout.hidden }.take(5)
    return if (apps.size >= 2) Suggestion(when (now.hour) { in 5..10 -> "Ce matin"; in 11..13 -> "Ce midi"; in 14..17 -> "Cet après-midi"; in 18..21 -> "Ce soir"; else -> "Cette nuit" }, apps, false)
    else null
}

/** The suggestion row, refreshed every minute and as soon as headphones or a car connect. */
@Composable
fun rememberSuggestion(launcher: Launcher, today: TodayState, exclude: Set<String>): Suggestion? {
    val context = LocalContext.current
    var audioTick by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val audio = context.getSystemService(AudioManager::class.java)
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) { audioTick++ }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) { audioTick++ }
        }
        runCatching { audio.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper())) }
        onDispose { runCatching { audio.unregisterAudioDeviceCallback(callback) } }
    }
    val playing by MediaWatcher.nowPlaying.collectAsState()
    val minute = rememberMinute()
    val mode = launcher.settings.suggestions
    return remember(mode, minute, audioTick, playing?.packageName, launcher.byId, launcher.lastLaunch, today.events, today.battery?.charging, exclude) {
        suggestionFor(context, launcher, today, playing?.packageName, exclude, mode)
    }
}

@Composable
fun SuggestionRow(suggestion: Suggestion, iconSize: Dp, onOpen: (AppInfo, Rect) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text((if (suggestion.contextual) "✦ " else "") + suggestion.reason, color = if (suggestion.contextual) Holo.Violet else Holo.Muted,
            fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestion.apps.forEach { app ->
                var bounds by remember { mutableStateOf(Rect.Zero) }
                Box(Modifier.size(iconSize).onGloballyPositioned { bounds = it.boundsInRoot() }.clip(CircleShape)
                    .clickable { onOpen(app, bounds) }.semantics { contentDescription = app.label }) {
                    AppIcon(app, iconSize, showBadge = false)
                }
            }
        }
    }
}
