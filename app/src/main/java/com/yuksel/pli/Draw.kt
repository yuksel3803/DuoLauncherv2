package com.yuksel.pli

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

/**
 * "Dessiner pour lancer": the user records a shape (a letter, a zigzag…) for an app, then draws it to open the app.
 * Shapes are compared after resampling to a fixed number of points and scaling (aspect kept, direction kept),
 * so a "W" and an "M" stay different.
 */
object Drawings {
    private const val N = 48
    private const val THRESHOLD = .78f

    data class Template(val app: String, val points: List<Offset>)

    var version by mutableStateOf(0); private set

    fun load(context: Context): List<Template> = runCatching {
        val a = JSONArray(context.getSharedPreferences("pli", Context.MODE_PRIVATE).getString("drawings", "[]"))
        (0 until a.length()).mapNotNull { a.optJSONObject(it) }.map { o ->
            val p = o.getJSONArray("p")
            Template(o.getString("app"), (0 until p.length() / 2).map { Offset(p.getDouble(it * 2).toFloat(), p.getDouble(it * 2 + 1).toFloat()) })
        }
    }.getOrDefault(emptyList())

    private fun save(context: Context, list: List<Template>) {
        val a = JSONArray()
        list.forEach { t -> a.put(JSONObject().put("app", t.app).put("p", JSONArray().apply { t.points.forEach { put(it.x.toDouble()); put(it.y.toDouble()) } })) }
        context.getSharedPreferences("pli", Context.MODE_PRIVATE).edit().putString("drawings", a.toString()).apply()
        version++
    }

    fun add(context: Context, app: String, stroke: List<Offset>): Boolean {
        val points = normalize(stroke) ?: return false
        save(context, load(context) + Template(app, points)); return true
    }

    fun remove(context: Context, index: Int) { save(context, load(context).filterIndexed { i, _ -> i != index }) }

    /** Resampled to [N] evenly spaced points, scaled so the longer side is 1, centred on 0. Null for a tap or a tiny scribble. */
    fun normalize(stroke: List<Offset>): List<Offset>? {
        if (stroke.size < 4) return null
        val length = stroke.zipWithNext { a, b -> (b - a).getDistance() }.sum()
        if (length < 40f) return null
        val step = length / (N - 1)
        val out = ArrayList<Offset>(N).apply { add(stroke.first()) }
        var carried = 0f
        var prev = stroke.first()
        var i = 1
        while (i < stroke.size && out.size < N) {
            val cur = stroke[i]
            val d = (cur - prev).getDistance()
            if (carried + d >= step && d > 0f) {
                val t = (step - carried) / d
                val q = prev + (cur - prev) * t
                out += q; prev = q; carried = 0f
            } else { carried += d; prev = cur; i++ }
        }
        while (out.size < N) out += stroke.last()
        val minX = out.minOf { it.x }; val maxX = out.maxOf { it.x }
        val minY = out.minOf { it.y }; val maxY = out.maxOf { it.y }
        val scale = maxOf(maxX - minX, maxY - minY).coerceAtLeast(1f)
        val cx = out.sumOf { it.x.toDouble() }.toFloat() / N; val cy = out.sumOf { it.y.toDouble() }.toFloat() / N
        return out.map { Offset((it.x - cx) / scale, (it.y - cy) / scale) }
    }

    private fun distance(a: List<Offset>, b: List<Offset>) = a.zip(b) { p, q -> hypot(p.x - q.x, p.y - q.y) }.average().toFloat()

    /** The app whose shape is closest to [stroke], if it is close enough. */
    fun recognize(context: Context, stroke: List<Offset>): String? {
        val points = normalize(stroke) ?: return null
        val best = load(context).map { it to 1f - distance(points, it.points) / .5f }.maxByOrNull { it.second } ?: return null
        return best.first.app.takeIf { best.second >= THRESHOLD }
    }
}

/** A surface to draw one stroke on; [onStroke] gets the finished stroke. The last stroke stays visible until [clearKey] changes. */
@Composable
fun StrokeCanvas(modifier: Modifier, clearKey: Any?, onStroke: (List<Offset>) -> Unit) {
    val points = remember { mutableStateListOf<Offset>() }
    LaunchedEffect(clearKey) { points.clear() }
    Canvas(modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { points.clear(); points += it },
            onDrag = { change, _ -> points += change.position },
            onDragEnd = { onStroke(points.toList()) },
            onDragCancel = { points.clear() })
    }) {
        if (points.size > 1) {
            val path = Path().apply { moveTo(points[0].x, points[0].y); points.drop(1).forEach { lineTo(it.x, it.y) } }
            drawPath(path, Brush.linearGradient(Holo.Iridescent), style = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round), alpha = .25f)
            drawPath(path, Brush.linearGradient(Holo.Iridescent), style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** A small preview of a saved shape. */
@Composable
fun ShapePreview(points: List<Offset>, modifier: Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val s = size.minDimension * .8f
        val c = Offset(size.width / 2, size.height / 2)
        val path = Path().apply { points.forEachIndexed { i, p -> val q = c + p * s; if (i == 0) moveTo(q.x, q.y) else lineTo(q.x, q.y) } }
        drawPath(path, Brush.linearGradient(Holo.Iridescent), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Full-screen pad: draw a shape, the matching app opens. */
@Composable
fun DrawPad(launcher: Launcher, modifier: Modifier, onOpen: (AppInfo, Rect?) -> Unit, onSettings: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf<AppInfo?>(null) }
    var clear by remember { mutableStateOf(0) }
    val count = remember(Drawings.version) { Drawings.load(context).size }
    LaunchedEffect(found) { found?.let { delay(260); onOpen(it, null); onClose() } }
    LaunchedEffect(message) { if (message != null) { delay(1400); message = null; clear++ } }
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                IridescentText("Dessine", 30.sp, letterSpacing = (-.5).sp)
                Text(if (count == 0) "Aucun dessin enregistré pour l’instant" else "$count dessin" + (if (count > 1) "s" else "") + " enregistré" + (if (count > 1) "s" else ""),
                    color = Holo.Muted, fontSize = 13.sp)
            }
            PillButton("Mes dessins", onClick = onSettings)
            Box(Modifier.padding(start = 8.dp).size(40.dp).clip(CircleShape).glass(CircleShape).clickable(onClick = onClose)
                .semantics { contentDescription = "Fermer" }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, null, tint = Holo.Ink, modifier = Modifier.size(18.dp))
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp).glass(RoundedCornerShape(32.dp), .03f), contentAlignment = Alignment.Center) {
            val f = found
            when {
                f != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppIcon(f, 84.dp, showBadge = false)
                    Text(f.label, color = Holo.Ink, fontSize = 16.sp, modifier = Modifier.padding(top = 10.dp))
                }
                count == 0 -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)) {
                    Text("Enregistre d’abord un dessin par appli (un W pour WhatsApp, un M pour Maps…).", color = Holo.Soft, fontSize = 15.sp)
                    PillButton("Enregistrer un dessin", highlighted = true, onClick = onSettings)
                }
                else -> {
                    StrokeCanvas(Modifier.fillMaxSize(), clear) { stroke ->
                        val id = Drawings.recognize(context, stroke)
                        val app = id?.let { launcher.byId[it] }
                        if (app != null) found = app else message = "Pas reconnu, réessaie"
                    }
                    message?.let { Text(it, color = Holo.Pink, fontSize = 15.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp)) }
                    if (message == null) Text("Trace ta forme d’un seul geste", color = Holo.Muted.copy(alpha = .6f), fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
                }
            }
        }
    }
}

/** Settings page: the saved shapes, and recording a new one (draw, then choose the app). */
@Composable
fun DrawingsSettings(launcher: Launcher, onPickApp: ((AppInfo) -> Unit) -> Unit) {
    val context = LocalContext.current
    val list = remember(Drawings.version) { Drawings.load(context) }
    var recording by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<List<Offset>?>(null) }
    var clear by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val st = launcher.settings
        val gesture = when ("draw") { st.twoFingerUp -> "Glisse à deux doigts vers le haut"; st.twoFingerDown -> "Glisse à deux doigts vers le bas"
            st.doubleTap -> "Touche deux fois l’accueil"; else -> null }
        Text(if (gesture != null) "$gesture pour ouvrir le tableau, puis trace la forme d’une appli." else
            "Choisis « Dessiner pour lancer » pour un geste dans Gestes (le glisser à deux doigts vers le haut est pris automatiquement s’il est libre).",
            color = Holo.Soft, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
        list.forEachIndexed { i, t ->
            val app = launcher.byId[t.app]
            Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp), .05f).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ShapePreview(t.points, Modifier.size(44.dp))
                if (app != null) AppIcon(app, 36.dp, Modifier.padding(start = 12.dp), showBadge = false)
                Text(app?.label ?: "Appli désinstallée", color = Holo.Ink, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Box(Modifier.size(36.dp).clip(CircleShape).clickable { Drawings.remove(context, i) }.semantics { contentDescription = "Supprimer" },
                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, null, tint = Holo.Muted, modifier = Modifier.size(16.dp)) }
            }
        }
        if (recording) {
            Box(Modifier.fillMaxWidth().height(260.dp).glass(RoundedCornerShape(26.dp), .04f)) {
                StrokeCanvas(Modifier.fillMaxSize(), clear) { stroke -> if (Drawings.normalize(stroke) != null) pending = stroke }
                Text(if (pending == null) "Dessine la forme d’un seul geste" else "Pour quelle appli ?", color = Holo.Muted, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pending != null) PillButton("Choisir l’appli", highlighted = true) {
                    val stroke = pending ?: return@PillButton
                    onPickApp { app ->
                        Drawings.add(context, app.id, stroke); pending = null; recording = false; clear++
                        // First drawing: give it a gesture if none opens the pad yet.
                        val st = launcher.settings
                        if ("draw" !in listOf(st.doubleTap, st.twoFingerUp, st.twoFingerDown) && st.twoFingerUp == "none")
                            launcher.updateSettings { it.copy(twoFingerUp = "draw") }
                    }
                }
                if (pending != null) PillButton("Recommencer") { pending = null; clear++ }
                PillButton("Annuler") { pending = null; recording = false; clear++ }
            }
        } else PillButton("Ajouter un dessin", highlighted = true) { recording = true }
    }
}
