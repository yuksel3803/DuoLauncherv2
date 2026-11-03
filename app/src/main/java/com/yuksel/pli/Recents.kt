package com.yuksel.pli

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Apps opened lately (newest first) with the time they were last in front. Needs usage access. */
object RecentApps {
    fun allowed(context: Context): Boolean = runCatching {
        context.getSystemService(AppOpsManager::class.java)
            .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    suspend fun load(context: Context, launcher: Launcher, max: Int = 16): List<Pair<AppInfo, Long>> = withContext(Dispatchers.IO) {
        if (!allowed(context)) return@withContext emptyList()
        val usage = context.getSystemService(UsageStatsManager::class.java) ?: return@withContext emptyList()
        val now = System.currentTimeMillis()
        val homes = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.toSet()
        val last = HashMap<String, Long>()
        runCatching {
            val events = usage.queryEvents(now - 3 * 86_400_000L, now)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) last[e.packageName] = e.timeStamp
            }
        }
        val byPackage = launcher.visibleApps.filter { !it.work }.associateBy { it.packageName }
        last.entries.asSequence()
            .filter { it.key != context.packageName && it.key !in homes && it.key != "com.android.systemui" }
            .sortedByDescending { it.value }
            .mapNotNull { (pkg, time) -> byPackage[pkg]?.let { it to time } }
            .take(max).toList()
    }

    /** Frees the memory of apps in the background (like "Tout fermer"); the apps stay installed and reopen normally. */
    fun closeAll(context: Context, packages: List<String>) {
        val am = context.getSystemService(ActivityManager::class.java) ?: return
        packages.forEach { runCatching { am.killBackgroundProcesses(it) } }
    }
}

private fun ago(time: Long): String {
    val minutes = (System.currentTimeMillis() - time) / 60_000
    return when {
        minutes < 1 -> "à l’instant"
        minutes < 60 -> "il y a $minutes min"
        minutes < 24 * 60 -> "il y a ${minutes / 60} h"
        else -> "il y a ${minutes / (24 * 60)} j"
    }
}

/**
 * Fold Launcher Pro's own recent apps: big cards, newest first. Tap to open, "À côté" to open it in split screen
 * next to the previous app, × to forget it, "Tout fermer" to free their memory.
 */
@Composable
fun RecentsPanel(launcher: Launcher, modifier: Modifier, onOpen: (AppInfo, Rect?) -> Unit, onSplit: (AppInfo?, AppInfo) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var hidden by remember { mutableStateOf(emptySet<String>()) }
    val allowed = RecentApps.allowed(context)
    val list by produceState(emptyList<Pair<AppInfo, Long>>(), refresh, allowed) { value = RecentApps.load(context, launcher) }
    val shown = list.filter { it.first.id !in hidden }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IridescentText("Récents", 30.sp, Modifier.weight(1f), letterSpacing = (-.5).sp)
            if (shown.isNotEmpty()) PillButton("Tout fermer") {
                RecentApps.closeAll(context, shown.map { it.first.packageName })
                hidden = hidden + shown.map { it.first.id }
                android.widget.Toast.makeText(context, "Applis fermées, mémoire libérée", android.widget.Toast.LENGTH_SHORT).show()
            }
            Spacer(Modifier.padding(start = 8.dp))
            PillButton("Fermer", onClick = onClose)
        }
        if (!allowed) {
            HintCard("Applis récentes", "Pour afficher tes applis récentes, autorise « Accès aux données d’utilisation » pour Fold Launcher Pro.", "Autoriser") {
                runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            return@Column
        }
        if (shown.isEmpty()) Text("Aucune appli récente.", color = Holo.Muted, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
        LazyVerticalGrid(GridCells.Adaptive(170.dp), Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shown, key = { it.first.id }) { (app, time) ->
                var bounds by remember { mutableStateOf(Rect.Zero) }
                val previous = shown.firstOrNull { it.first.id != app.id }?.first
                Column(Modifier.fillMaxWidth().onGloballyPositioned { bounds = it.boundsInRoot() }.clip(RoundedCornerShape(26.dp))
                    .glass(RoundedCornerShape(26.dp), .06f).clickable { onOpen(app, bounds) }.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        AppIcon(app, 56.dp)
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(32.dp).clip(CircleShape).clickable {
                            RecentApps.closeAll(context, listOf(app.packageName)); hidden = hidden + app.id
                        }.semantics { contentDescription = "Retirer ${app.label}" }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, null, tint = Holo.Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(app.label, color = Holo.Ink, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                    Text(ago(time), color = Holo.Muted, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    PillButton(if (previous != null) "À côté de ${previous.label}" else "Ouvrir") {
                        if (previous != null) onSplit(previous, app) else onOpen(app, bounds)
                    }
                }
            }
        }
    }
}
