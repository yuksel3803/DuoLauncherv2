package com.yuksel.pli

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Accent colours (text gradients, highlights) and the three background glows. */
data class Palette(val a: Color, val b: Color, val c: Color, val glowA: Color, val glowB: Color, val glowC: Color)

/** How the backdrop is lit: [base] colour, glow strength and colours (the living halos may override the palette's). */
data class Ambience(val base: Color = Color(0xFF05060A), val glow: Float = 1f, val glows: List<Color>? = null)

/** The holographic palette. Accents come from the chosen palette (holographic, wallpaper, presets) and can change live. */
object Holo {
    val HOLO = Palette(Color(0xFF9BE7FF), Color(0xFFC7A6FF), Color(0xFFFFB3D9), Color(0xFF6EBEFF), Color(0xFFC882FF), Color(0xFFFF8CC8))
    var palette by mutableStateOf(HOLO)
    var ambience by mutableStateOf(Ambience())

    val Base get() = ambience.base
    val Ink = Color(0xFFF4F2FF)
    val Soft = Color(0xFFE6E2FA)
    val Muted = Color(0xFFB9B4D6)
    val Cyan get() = palette.a
    val Violet get() = palette.b
    val Pink get() = palette.c
    val Mint = Color(0xFF9BFFD8)
    val Gold = Color(0xFFFFE59B)
    val Badge = Color(0xFFFF5FA8)
    val Panel = Color(0xFF0D0E16)
    val Iridescent get() = listOf(Cyan, Violet, Pink)
    val iridescentBrush get() = Brush.linearGradient(Iridescent)
    val edgeBrush = Brush.linearGradient(listOf(Color.White.copy(alpha = .30f), Color.White.copy(alpha = .08f), Color.White.copy(alpha = .18f)))
    val Font = FontFamily.SansSerif
}

/** Frosted glass surface: faint fill and a light edge. */
fun Modifier.glass(shape: Shape, fill: Float = .06f, edge: Boolean = true): Modifier =
    clip(shape).background(Color.White.copy(alpha = fill), shape)
        .then(if (edge) Modifier.border(1.dp, Holo.edgeBrush, shape) else Modifier)

/** A soft coloured halo behind an element. */
fun Modifier.halo(color: Color, radius: Dp = 28.dp): Modifier = drawWithCache {
    // The gradient is built once per size, not on every frame.
    val r = size.minDimension / 2f + radius.toPx()
    val middle = Offset(size.width / 2f, size.height / 2f)
    val brush = Brush.radialGradient(listOf(color, Color.Transparent), center = middle, radius = r)
    onDrawBehind { drawCircle(brush, radius = r, center = middle) }
}

@Composable
fun IridescentText(text: String, size: TextUnit, modifier: Modifier = Modifier, weight: FontWeight = FontWeight.ExtraLight,
    letterSpacing: TextUnit = (-1.5).sp) {
    Text(text, modifier, maxLines = 1, softWrap = false, style = TextStyle(brush = Brush.linearGradient(Holo.Iridescent),
        fontSize = size, fontWeight = weight, fontFamily = Holo.Font, letterSpacing = letterSpacing, lineHeight = size))
}

/** Sensor tilt, -1..1 on each axis, used to drift the halos (parallax). The sensor only runs while the launcher is on screen. */
@Composable
fun rememberTilt(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val active = enabled && rememberOnScreen()
    var tilt by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(active) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val y = ((event.values[1] - 6.5f) / 9.81f).coerceIn(-1f, 1f)
                if ((Offset(x, y) - tilt).getDistance() > .02f) tilt = Offset(x, y)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (active && sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        else if (!enabled) tilt = Offset.Zero
        onDispose { manager.unregisterListener(listener) }
    }
    return animateOffsetAsState(tilt, spring(stiffness = 40f), label = "tilt")
}

/**
 * The black holographic backdrop with three drifting colour halos. Over the phone wallpaper it only darkens.
 * The tilt is read while drawing, so the halos move without recomposing anything.
 */
@Composable
fun HoloBackground(phoneWallpaper: Boolean, parallax: Boolean, modifier: Modifier = Modifier.fillMaxSize()) {
    val tilt = rememberTilt(parallax)
    Canvas(modifier) {
        val ambience = Holo.ambience
        val palette = Holo.palette
        if (phoneWallpaper) drawRect(Color.Black.copy(alpha = .38f)) else drawRect(ambience.base)
        val w = size.width; val h = size.height
        val t = tilt.value
        val shift = Offset(t.x * w * .08f, t.y * h * .05f)
        val strength = (if (phoneWallpaper) .55f else 1f) * ambience.glow
        fun glow(center: Offset, radius: Float, color: Color, alpha: Float) =
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = (alpha * strength).coerceIn(0f, 1f)), Color.Transparent), center, radius), radius, center)
        val r = maxOf(w, h)
        val glows = ambience.glows ?: listOf(palette.glowA, palette.glowB, palette.glowC)
        glow(Offset(w * .05f, h * .06f) + shift, r * .42f, glows[0], .30f)
        glow(Offset(w * 1.0f, h * .48f) - shift * 1.4f, r * .46f, glows[1], .24f)
        glow(Offset(w * .18f, h * 1.02f) + shift * .7f, r * .40f, glows[2], .18f)
    }
}

/** True: real app icons sit in glass frames. False: just the icon. */
val LocalBubbles = staticCompositionLocalOf { true }
/** The icon shape chosen in the settings (see [ICON_SHAPES]). */
val LocalIconShape = staticCompositionLocalOf { "circle" }
val LocalBadges = staticCompositionLocalOf<Map<String, Int>> { emptyMap() }
/** Apps installed recently and never opened since: they get a small "new" dot. */
val LocalNewApps = staticCompositionLocalOf<Set<String>> { emptySet() }
/** True: unread notifications orbit the icon as bubbles instead of a number. */
val LocalOrbitBadges = staticCompositionLocalOf { false }
/** True: moving effects are allowed (off with the battery saver). */
val LocalMotion = staticCompositionLocalOf { true }

/** Icon shapes offered in the settings: key to French name. */
val ICON_SHAPES = listOf("circle" to "Rond", "squircle" to "Squircle", "rounded" to "Arrondi", "square" to "Carré",
    "teardrop" to "Goutte", "leaf" to "Feuille", "original" to "D’origine")

fun iconShape(name: String): Shape = when (name) {
    "squircle" -> RoundedCornerShape(36)
    "rounded" -> RoundedCornerShape(22)
    "square" -> RoundedCornerShape(6)
    "teardrop" -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 12, bottomStartPercent = 50)
    "leaf" -> RoundedCornerShape(topStartPercent = 50, topEndPercent = 8, bottomEndPercent = 50, bottomStartPercent = 8)
    "original" -> RoundedCornerShape(0)
    else -> CircleShape
}

@Composable
fun AppIcon(app: AppInfo, size: Dp, modifier: Modifier = Modifier, showBadge: Boolean = true) {
    val bubbles = LocalBubbles.current
    val shapeName = LocalIconShape.current
    val shape = remember(shapeName) { iconShape(shapeName) }
    val count = if (showBadge) LocalBadges.current[app.packageName] ?: 0 else 0
    Box(modifier.size(size)) {
        if (bubbles) {
            Box(Modifier.fillMaxSize().halo(Holo.Violet.copy(alpha = .16f), size * .2f)
                .glass(if (shapeName == "original") RoundedCornerShape(26) else shape, .06f), contentAlignment = Alignment.Center) {
                Image(app.icon, null, Modifier.size(size * .78f).then(if (shapeName == "original") Modifier else Modifier.clip(shape)))
            }
        } else Image(app.icon, null, Modifier.fillMaxSize().then(if (shapeName == "original") Modifier else Modifier.clip(shape)))
        if (count > 0) {
            if (LocalOrbitBadges.current) OrbitBadge(count, Modifier.fillMaxSize())
            else BadgeDot(count, Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp))
        }
        if (showBadge && app.id in LocalNewApps.current) NewDot(Modifier.align(Alignment.TopStart).offset(x = (-2).dp, y = (-2).dp))
    }
}

/** The "new app" mark: a small mint pearl. */
@Composable
fun NewDot(modifier: Modifier = Modifier) {
    Box(modifier.halo(Holo.Mint.copy(alpha = .6f), 5.dp).size(11.dp).background(Holo.Mint, CircleShape)
        .border(1.5.dp, Holo.Base, CircleShape).semantics { contentDescription = "Nouvelle appli" })
}

/**
 * Unread notifications as bubbles circling the icon: one bubble per message (up to 6), and the more there are,
 * the faster they turn. The angle is read while drawing, so nothing recomposes as they move.
 */
@Composable
fun OrbitBadge(count: Int, modifier: Modifier = Modifier) {
    val bubbles = count.coerceIn(1, 6)
    val motion = LocalMotion.current
    val turn = if (motion) {
        val transition = rememberInfiniteTransition(label = "orbit")
        transition.animateFloat(0f, 360f, infiniteRepeatable(tween((9000 - bubbles * 900).coerceAtLeast(3000), easing = LinearEasing)), label = "turn")
    } else remember { mutableStateOf(0f) }
    val colors = Holo.Iridescent + Holo.Badge
    Box(modifier.semantics { contentDescription = "$count notification" + if (count > 1) "s" else "" }.drawBehind {
        val ring = size.minDimension / 2f + 3.dp.toPx()
        val r = (if (bubbles > 3) 3.6f else 4.4f).dp.toPx()
        for (i in 0 until bubbles) {
            val angle = Math.toRadians((turn.value + i * 360f / bubbles - 90f).toDouble())
            val c = Offset(center.x + ring * kotlin.math.cos(angle).toFloat(), center.y + ring * kotlin.math.sin(angle).toFloat())
            val color = colors[i % colors.size]
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .55f), Color.Transparent), c, r * 2.6f), r * 2.6f, c)
            drawCircle(color, r, c)
        }
    })
}

@Composable
fun BadgeDot(count: Int, modifier: Modifier = Modifier) {
    Box(modifier.halo(Holo.Badge.copy(alpha = .55f), 6.dp).defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
        .background(Holo.Badge, CircleShape).padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
        Text(if (count > 99) "99+" else count.toString(), color = Color(0xFF1A0512), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
