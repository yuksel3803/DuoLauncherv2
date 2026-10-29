package com.yuksel.pli

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Looks for a newer Fold Launcher Pro on GitHub (releases named pli-N, N = build number) and installs it
 * after a tap. Android shows its own confirmation the first time; the layout and settings stay in place.
 */
object Updater {
    private const val RELEASES = "https://api.github.com/repos/yuksel3803/DuoLauncherv2/releases?per_page=20"
    private const val PREFS = "updates"
    /** Hours between automatic checks offered in the settings; 6 by default, to spare the battery. */
    val FREQUENCIES = listOf(1, 6, 24)
    const val CHANNEL = "updates"
    const val EXTRA_SHOW = "com.yuksel.pli.SHOW_UPDATE"

    /** Set when the update notification is tapped: the window opens even after "Plus tard". */
    var forceNext = false
    private const val LATER_FOR = 24 * 3_600_000L

    data class Release(val build: Int, val notes: String, val apkUrl: String, val size: Long)

    /** The newer build found, if any. */
    var available by mutableStateOf<Release?>(null); private set
    /** True while the update window is shown. */
    var dialog by mutableStateOf(false); private set
    var checking by mutableStateOf(false); private set
    /** Download progress 0..1 while the new version downloads, else null. */
    var progress by mutableStateOf<Float?>(null); private set
    /** A short message for the user (no update, error, permission needed…). */
    var message by mutableStateOf<String?>(null); private set

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun currentBuild(context: Context): Int = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
    }.getOrDefault(0)

    fun automatic(context: Context) = prefs(context).getBoolean("auto", true)

    /** True the first time only: the launcher then asks for the notification permission. */
    fun askNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) return false
        val p = prefs(context)
        if (p.getBoolean("askedNotifications", false)) return false
        p.edit().putBoolean("askedNotifications", true).apply()
        return true
    }
    fun setAutomatic(context: Context, on: Boolean) { prefs(context).edit().putBoolean("auto", on).apply() }

    fun everyHours(context: Context) = prefs(context).getInt("everyHours", 6).takeIf { it in FREQUENCIES } ?: 6

    fun setEveryHours(context: Context, hours: Int) {
        prefs(context).edit().putInt("everyHours", hours).apply()
        schedule(context, replace = true)
    }

    /**
     * Asks GitHub for the latest build. Automatic checks respect "Plus tard" (unless the notification was tapped);
     * a manual check (from the settings) always runs and always opens the window when there is something new.
     */
    suspend fun check(context: Context, manual: Boolean) {
        val forced = forceNext.also { forceNext = false }
        val p = prefs(context)
        val now = System.currentTimeMillis()
        if (!manual && !forced && (!automatic(context) || now - p.getLong("checkedAt", 0) < everyHours(context) * 3_600_000L) && available == null) return
        if (checking) return
        checking = true
        if (manual) message = null
        val release = withContext(Dispatchers.IO) { runCatching { latest(p) }.getOrNull() }
        checking = false
        if (release == null) { if (manual) message = "Impossible de joindre GitHub. Vérifie la connexion."; return }
        p.edit().putLong("checkedAt", now).apply()
        if (release.build <= currentBuild(context)) {
            available = null
            if (manual) message = "Tu as déjà la dernière version (build ${currentBuild(context)})."
            return
        }
        available = release
        val snoozed = p.getInt("laterBuild", 0) == release.build && now < p.getLong("laterUntil", 0)
        if (manual || forced || !snoozed) dialog = true
    }

    /** Background check (scheduled job): returns the newer build, if any. Blocking, call off the main thread. */
    fun checkInBackground(context: Context): Release? {
        if (!automatic(context)) return null
        val release = runCatching { latest(prefs(context)) }.getOrNull() ?: return null
        prefs(context).edit().putLong("checkedAt", System.currentTimeMillis()).apply()
        return release.takeIf { it.build > currentBuild(context) }
    }

    /** "Nouvelle version disponible", once per build. Tapping it opens the launcher on the update window. */
    fun notify(context: Context, release: Release) {
        val p = prefs(context)
        if (p.getInt("notifiedBuild", 0) >= release.build) return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(android.app.NotificationManager::class.java) ?: return
        manager.createNotificationChannel(android.app.NotificationChannel(CHANNEL, "Mises à jour", android.app.NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Nouvelle version de Fold Launcher Pro" })
        val open = PendingIntent.getActivity(context, 1, Intent(context, MainActivity::class.java).putExtra(EXTRA_SHOW, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val first = release.notes.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: "Touche pour voir les nouveautés"
        val notification = android.app.Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Fold Launcher Pro — build ${release.build} disponible")
            .setContentText(first)
            .setStyle(android.app.Notification.BigTextStyle().bigText(release.notes.ifBlank { first }))
            .setContentIntent(open).setAutoCancel(true).build()
        runCatching { manager.notify(4242, notification) }
        p.edit().putInt("notifiedBuild", release.build).apply()
    }

    /** Checks in the background every [everyHours] hours, even while another app is open (Android picks the best moment). */
    fun schedule(context: Context, replace: Boolean = false) {
        val jobs = context.getSystemService(android.app.job.JobScheduler::class.java) ?: return
        val period = everyHours(context) * 3_600_000L
        val pending = jobs.getPendingJob(JOB_ID)
        if (!replace && pending != null && pending.intervalMillis == period) return
        if (pending != null) jobs.cancel(JOB_ID)
        val job = android.app.job.JobInfo.Builder(JOB_ID, android.content.ComponentName(context, UpdateJob::class.java))
            .setPeriodic(period)
            .setRequiredNetworkType(android.app.job.JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .build()
        runCatching { jobs.schedule(job) }
    }

    private const val JOB_ID = 4242

    fun open() { if (available != null) { message = null; dialog = true } }

    fun later(context: Context) {
        available?.let { prefs(context).edit().putInt("laterBuild", it.build).putLong("laterUntil", System.currentTimeMillis() + LATER_FOR).apply() }
        dialog = false
        message = null
    }

    /** Asks GitHub with the last ETag: an unchanged list answers "304" (free, instant) and the saved copy is reused. */
    private fun latest(p: android.content.SharedPreferences): Release? {
        val connection = (URL(RELEASES).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "FoldLauncherPro")
            val cached = p.getString("body", null)
            p.getString("etag", null)?.takeIf { cached != null }?.let { setRequestProperty("If-None-Match", it) }
        }
        val text = if (connection.responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) p.getString("body", null) ?: return null
        else connection.inputStream.bufferedReader().use { it.readText() }.also { body ->
            p.edit().putString("body", body).putString("etag", connection.getHeaderField("ETag")).apply()
        }
        val array = JSONArray(text)
        var best: Release? = null
        for (i in 0 until array.length()) {
            val r = array.getJSONObject(i)
            if (r.optBoolean("draft")) continue
            val build = r.optString("tag_name").removePrefix("pli-").toIntOrNull() ?: continue
            val assets = r.optJSONArray("assets") ?: continue
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk") } ?: continue
            if (best == null || build > best.build) best = Release(build, r.optString("body").trim(), apk.getString("browser_download_url"), apk.optLong("size"))
        }
        return best
    }

    /** Downloads the new APK and hands it to Android's installer. */
    suspend fun install(context: Context) {
        val release = available ?: return
        if (progress != null) return
        if (!context.packageManager.canRequestPackageInstalls()) {
            message = "Autorise Fold Launcher Pro à installer des applis, puis appuie à nouveau sur « Mettre à jour »."
            runCatching {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return
        }
        message = null
        progress = 0f
        val file = withContext(Dispatchers.IO) { runCatching { download(context, release) }.getOrNull() }
        if (file == null) { progress = null; message = "Le téléchargement a échoué. Réessaie dans un instant."; return }
        message = "Installation…"
        val ok = withContext(Dispatchers.IO) { runCatching { commit(context, file) }.isSuccess }
        progress = null
        if (!ok) message = "Android a refusé l’installation."
    }

    private fun download(context: Context, release: Release): File {
        val dir = File(context.cacheDir, "update").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, "FoldLauncherPro-${release.build}.apk")
        val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 30_000; instanceFollowRedirects = true
            setRequestProperty("User-Agent", "FoldLauncherPro")
        }
        val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.size
        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                var lastShown = 0f
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    done += n
                    if (total > 0) {
                        val p = (done.toFloat() / total).coerceIn(0f, 1f)
                        if (p - lastShown > .01f || p == 1f) { lastShown = p; progress = p }
                    }
                }
            }
        }
        check(total <= 0 || file.length() == total) { "incomplete" }
        return file
    }

    private fun commit(context: Context, file: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(file.length())
            // Once Fold Launcher Pro installed itself, later updates can go through without an extra screen.
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("base.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out, 64 * 1024) }
                session.fsync(out)
            }
            val intent = Intent(context, UpdateReceiver::class.java)
            val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            session.commit(pending.intentSender)
        }
    }

    internal fun onResult(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    else intent.getParcelableExtra(Intent.EXTRA_INTENT)
                message = "Confirme l’installation dans la fenêtre d’Android."
                confirm?.let { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            }
            PackageInstaller.STATUS_SUCCESS -> { message = null; dialog = false; available = null }
            PackageInstaller.STATUS_FAILURE_ABORTED -> message = "Installation annulée."
            else -> message = "Installation impossible : " + (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "erreur inconnue")
        }
    }
}

/** The background check: GitHub → notification when a newer build is out. */
class UpdateJob : android.app.job.JobService() {
    override fun onStartJob(params: android.app.job.JobParameters?): Boolean {
        Thread {
            Updater.checkInBackground(applicationContext)?.let { Updater.notify(applicationContext, it) }
            jobFinished(params, false)
        }.start()
        return true
    }
    override fun onStopJob(params: android.app.job.JobParameters?) = true
}

/** Receives the installer's answer (confirmation needed, done, failed). */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Updater.onResult(context, intent)
}

/** The holographic "new version" window. */
@Composable
fun UpdateDialog(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val release = Updater.available
    AnimatedVisibility(Updater.dialog && release != null, modifier, enter = fadeIn(), exit = fadeOut()) {
        val r = release ?: return@AnimatedVisibility
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f))
            .clickable(remember { MutableInteractionSource() }, null) { if (Updater.progress == null) Updater.later(context) },
            contentAlignment = Alignment.Center) {
            Column(Modifier.padding(24.dp).widthIn(max = 440.dp).fillMaxWidth().halo(Holo.Violet.copy(alpha = .18f), 30.dp)
                .background(Holo.Panel, RoundedCornerShape(30.dp)).glass(RoundedCornerShape(30.dp), .05f)
                .clickable(remember { MutableInteractionSource() }, null) {}.padding(24.dp)) {
                Text("NOUVELLE VERSION", color = Holo.Muted, fontSize = 11.sp, letterSpacing = 2.5.sp)
                IridescentText("Build ${r.build}", 40.sp, Modifier.padding(top = 6.dp), letterSpacing = (-1).sp)
                Text("Tu as la build ${Updater.currentBuild(context)}. Ta disposition et tes réglages sont conservés.",
                    color = Holo.Soft, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                if (r.notes.isNotBlank()) {
                    Text("NOUVEAUTÉS", color = Holo.Muted, fontSize = 11.sp, letterSpacing = 2.5.sp, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
                    Column(Modifier.fillMaxWidth().heightIn(max = 260.dp).glass(RoundedCornerShape(18.dp), .04f).verticalScroll(rememberScrollState()).padding(14.dp)) {
                        Text(r.notes.replace("**", "").replace(Regex("(?m)^[-*] "), "• "), color = Holo.Ink, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
                Updater.progress?.let { p ->
                    Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = .08f))) {
                        Box(Modifier.fillMaxWidth(p.coerceAtLeast(.02f)).height(6.dp).background(Brush.horizontalGradient(Holo.Iridescent)))
                    }
                    Text("Téléchargement… ${(p * 100).toInt()} %", color = Holo.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Updater.message?.let { Text(it, color = Holo.Pink, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)) }
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val busy = Updater.progress != null
                    Box(Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(25.dp)).glass(RoundedCornerShape(25.dp))
                        .clickable(enabled = !busy) { Updater.later(context) }, contentAlignment = Alignment.Center) {
                        Text("Plus tard", color = if (busy) Holo.Muted else Holo.Soft, fontSize = 15.sp)
                    }
                    Box(Modifier.weight(1.3f).height(50.dp).clip(RoundedCornerShape(25.dp)).background(Brush.linearGradient(Holo.Iridescent))
                        .clickable(enabled = !busy) { scope.launch { Updater.install(context) } }, contentAlignment = Alignment.Center) {
                        Text(if (busy) "Téléchargement…" else "Mettre à jour", color = Color(0xFF120A22), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
