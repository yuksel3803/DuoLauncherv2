package com.yuksel.pli

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val cardShape = RoundedCornerShape(28.dp)
private val FR = Locale.FRANCE

// ---------- Compte à rebours ----------

private data class Countdown(val title: String, val date: LocalDate)

private object CountdownStore {
    fun load(context: Context): Countdown? = runCatching {
        val o = JSONObject(context.getSharedPreferences("pli", Context.MODE_PRIVATE).getString("countdown", null) ?: return null)
        Countdown(o.getString("title"), LocalDate.parse(o.getString("date")))
    }.getOrNull()

    fun save(context: Context, c: Countdown?) {
        context.getSharedPreferences("pli", Context.MODE_PRIVATE).edit()
            .putString("countdown", c?.let { JSONObject().put("title", it.title).put("date", it.date.toString()).toString() }).apply()
    }
}

/** Days left before an event the user names (holidays, birthday…). Tap to change it. */
@Composable
fun CountdownCard() {
    val context = LocalContext.current
    var countdown by remember { mutableStateOf(CountdownStore.load(context)) }
    var editing by remember { mutableStateOf(countdown == null) }
    var title by remember { mutableStateOf(countdown?.title ?: "") }
    var date by remember { mutableStateOf(countdown?.date ?: LocalDate.now().plusDays(30)) }
    val today = rememberMinute().toLocalDate()
    Column(Modifier.fillMaxWidth().glass(cardShape, .05f).clickable(enabled = !editing) { editing = true }.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardLabel("Compte à rebours")
        val c = countdown
        if (editing || c == null) {
            Row(Modifier.fillMaxWidth().height(46.dp).glass(RoundedCornerShape(50), .06f).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (title.isEmpty()) Text("Vacances, anniversaire…", color = Holo.Muted, fontSize = 14.sp)
                    BasicTextField(title, { title = it.take(40) }, Modifier.fillMaxWidth(), singleLine = true,
                        textStyle = TextStyle(color = Holo.Ink, fontSize = 14.sp), cursorBrush = SolidColor(Holo.Violet),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = {}))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", FR))) {
                    DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }
                PillButton("Enregistrer", highlighted = true) {
                    val next = Countdown(title.trim().ifEmpty { "Le grand jour" }, date)
                    CountdownStore.save(context, next); countdown = next; editing = false
                }
                if (c != null) PillButton("Annuler") { editing = false }
            }
        } else {
            val days = ChronoUnit.DAYS.between(today, c.date)
            Row(verticalAlignment = Alignment.Bottom) {
                IridescentText(when { days > 0 -> "J-$days"; days == 0L -> "Jour J"; else -> "J+${-days}" }, 46.sp, letterSpacing = (-1).sp)
                Text(c.date.format(DateTimeFormatter.ofPattern("EEE d MMM", FR)), color = Holo.Muted, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
            }
            Text(when {
                days > 1 -> "$days jours avant ${c.title}"
                days == 1L -> "${c.title}, c’est demain !"
                days == 0L -> "${c.title}, c’est aujourd’hui !"
                else -> "${c.title}, c’était il y a ${-days} jour" + if (-days > 1) "s" else ""
            }, color = Holo.Soft, fontSize = 14.sp)
        }
    }
}

// ---------- Souvenir du jour ----------

private data class Memory(val uri: Uri, val years: Int, val image: ImageBitmap, val date: LocalDate)

/** A photo taken on this day in a past year (or within 3 days of it). Read once a day. */
private suspend fun findMemory(context: Context, day: LocalDate): Memory? = withContext(Dispatchers.IO) {
    runCatching {
        val zone = ZoneId.systemDefault()
        for (spread in listOf(0L, 3L)) {
            val candidates = ArrayList<Pair<Long, Long>>()
            for (years in 1..12) {
                val center = day.minusYears(years.toLong())
                val from = center.minusDays(spread).atStartOfDay(zone).toInstant().toEpochMilli()
                val to = center.plusDays(spread + 1).atStartOfDay(zone).toInstant().toEpochMilli()
                context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN),
                    "${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?",
                    arrayOf(from.toString(), to.toString()), null)?.use { c ->
                    while (c.moveToNext() && candidates.size < 200) candidates += c.getLong(0) to c.getLong(1)
                }
            }
            if (candidates.isEmpty()) continue
            // The same photo all day, a different one tomorrow.
            val (id, taken) = candidates[(day.toEpochDay() % candidates.size).toInt()]
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val bitmap = context.contentResolver.loadThumbnail(uri, Size(720, 720), null)
            val takenDate = java.time.Instant.ofEpochMilli(taken).atZone(zone).toLocalDate()
            return@withContext Memory(uri, day.year - takenDate.year, bitmap.asImageBitmap(), takenDate)
        }
        null
    }.getOrNull()
}

@Composable
fun MemoryCard(onPermission: (String) -> Unit) {
    val context = LocalContext.current
    val allowed = context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    if (!allowed) {
        HintCard("Souvenir du jour", "Une photo prise à cette date les années précédentes. Autorise l’accès aux photos pour la voir.",
            "Autoriser") { onPermission(Manifest.permission.READ_MEDIA_IMAGES) }
        return
    }
    val day = rememberMinute().toLocalDate()
    val memory by produceState<Memory?>(null, day) { value = findMemory(context, day) }
    val m = memory
    if (m == null) {
        HintCard("Souvenir du jour", "Pas encore de photo prise à cette date les années précédentes.", null, null)
        return
    }
    Box(Modifier.fillMaxWidth().height(220.dp).clip(cardShape).clickable {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, m.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }) {
        Image(m.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .72f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text("SOUVENIR", color = Holo.Soft, fontSize = 11.sp, letterSpacing = 2.5.sp)
            IridescentText(if (m.years <= 1) "Il y a 1 an" else "Il y a ${m.years} ans", 28.sp, letterSpacing = (-.5).sp)
            Text(m.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", FR)), color = Holo.Soft, fontSize = 12.sp)
        }
    }
}

// ---------- Prochain trajet ----------

fun openDirections(context: Context, place: String) {
    val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(place))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(nav) }.onFailure {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(place))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

/** The next calendar event that has a place, with a button for directions. */
@Composable
fun TripCard(today: TodayState, onPermission: (String) -> Unit) {
    val context = LocalContext.current
    if (!today.calendarAllowed) {
        HintCard("Prochain trajet", "Le prochain rendez-vous avec une adresse, et l’itinéraire en un appui.", "Autoriser l’agenda") {
            onPermission(Manifest.permission.READ_CALENDAR)
        }
        return
    }
    val now = rememberMinute()
    val next = today.events.firstOrNull { it.location.isNotBlank() && !it.allDay && it.start.isAfter(now.minusMinutes(30)) }
    if (next == null) {
        HintCard("Prochain trajet", "Aucun rendez-vous avec une adresse dans les prochaines 24 heures.", null, null)
        return
    }
    val minutes = Duration.between(now, next.start).toMinutes()
    Column(Modifier.fillMaxWidth().glass(cardShape, .05f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CardLabel("Prochain trajet")
        Text(next.title, color = Holo.Ink, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(next.location, color = Holo.Soft, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(when {
                minutes <= 0 -> "Maintenant"
                minutes < 60 -> "Dans $minutes min · ${next.start.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                else -> "À ${next.start.format(DateTimeFormatter.ofPattern("HH:mm"))}" +
                    if (next.start.toLocalDate() != LocalDateTime.now().toLocalDate()) " demain" else ""
            }, color = Holo.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            PillButton("Itinéraire", highlighted = true) { openDirections(context, next.location) }
        }
    }
}
