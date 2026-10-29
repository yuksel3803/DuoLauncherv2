package com.yuksel.pli

import android.app.WallpaperManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDateTime
import java.time.LocalTime

/** Palettes offered in the settings: key to French name. */
val PALETTES = listOf("holo" to "Holographique", "wallpaper" to "Fond d’écran", "aurore" to "Aurore", "ocean" to "Océan",
    "braise" to "Braise", "menthe" to "Menthe")

private val PRESETS = mapOf(
    "holo" to Holo.HOLO,
    "aurore" to Palette(Color(0xFFFFD3A5), Color(0xFFFF9EC7), Color(0xFFC9A7FF), Color(0xFFFFB070), Color(0xFFFF6FA8), Color(0xFFA27BFF)),
    "ocean" to Palette(Color(0xFF8FF0FF), Color(0xFF8FBEFF), Color(0xFF9CFFD9), Color(0xFF3FA9FF), Color(0xFF2E6BFF), Color(0xFF2FE0C0)),
    "braise" to Palette(Color(0xFFFFC27A), Color(0xFFFF9A8A), Color(0xFFFF8FC0), Color(0xFFFF8A3D), Color(0xFFFF4D4D), Color(0xFFD94DFF)),
    "menthe" to Palette(Color(0xFFB5FFCB), Color(0xFF8CF5E6), Color(0xFFD9FF9E), Color(0xFF3DDC97), Color(0xFF2EC4B6), Color(0xFFA6E22E)),
)

/** The palette for a settings key; the wallpaper choice falls back to holographic (used by the live wallpaper itself). */
fun paletteForKey(key: String): Palette = PRESETS[key] ?: Holo.HOLO

/** Colours of the phone's wallpaper, softened for text and kept vivid for the glows. Null when Android gives none. */
fun wallpaperPalette(context: Context): Palette? = runCatching {
    val colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
    val p = Color(colors.primaryColor.toArgb())
    val s = colors.secondaryColor?.let { Color(it.toArgb()) } ?: lerp(p, Color(0xFFC7A6FF), .5f)
    val t = colors.tertiaryColor?.let { Color(it.toArgb()) } ?: lerp(s, Color(0xFFFFB3D9), .5f)
    fun soft(c: Color) = lerp(c, Color.White, .5f)
    fun vivid(c: Color) = lerp(c, Color.White, .12f)
    Palette(soft(p), soft(s), soft(t), vivid(p), vivid(s), vivid(t))
}.getOrNull()

private val DAWN = listOf(Color(0xFFFFB38A), Color(0xFFFF8FB8), Color(0xFFC9A2FF))
private val NOON = listOf(Color(0xFF7CC8FF), Color(0xFF6FA0FF), Color(0xFF8FF0E0))
private val DUSK = listOf(Color(0xFFFF9F5A), Color(0xFFFF6F91), Color(0xFFB86BFF))
private val NIGHT = listOf(Color(0xFF5A6BFF), Color(0xFF8A5BFF), Color(0xFFC46BFF))

/** Glows for the hour, blended between the four moments of the day. */
private fun livingGlows(time: LocalTime): List<Color> {
    val h = time.hour + time.minute / 60f
    fun mix(a: List<Color>, b: List<Color>, f: Float) = a.zip(b) { x, y -> lerp(x, y, f.coerceIn(0f, 1f)) }
    return when {
        h < 5f -> NIGHT
        h < 7f -> mix(NIGHT, DAWN, (h - 5f) / 2f)
        h < 9f -> mix(DAWN, NOON, (h - 7f) / 2f)
        h < 17f -> NOON
        h < 19f -> mix(NOON, DUSK, (h - 17f) / 2f)
        h < 21f -> mix(DUSK, NIGHT, (h - 19f) / 2f)
        else -> NIGHT
    }
}

/**
 * Keeps [Holo.palette] and [Holo.ambience] in step with the settings, the wallpaper, the time of day
 * (sunrise and sunset from the weather when known) and the battery.
 */
@Composable
fun ThemeController(settings: PliSettings, today: TodayState) {
    val context = LocalContext.current
    var wallpaperTick by remember { mutableIntStateOf(0) }
    DisposableEffect(settings.palette) {
        val manager = WallpaperManager.getInstance(context)
        val listener = WallpaperManager.OnColorsChangedListener { _, which -> if (which and WallpaperManager.FLAG_SYSTEM != 0) wallpaperTick++ }
        if (settings.palette == "wallpaper") runCatching { manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper())) }
        onDispose { runCatching { manager.removeOnColorsChangedListener(listener) } }
    }
    val now = rememberMinute()
    val palette = remember(settings.palette, wallpaperTick) {
        if (settings.palette == "wallpaper") wallpaperPalette(context) ?: Holo.HOLO else PRESETS[settings.palette] ?: Holo.HOLO
    }
    val ambience = ambienceFor(settings, now, today.weather, today.battery)
    SideEffect {
        if (Holo.palette != palette) Holo.palette = palette
        if (Holo.ambience != ambience) Holo.ambience = ambience
    }
}

private fun ambienceFor(settings: PliSettings, now: LocalDateTime, weather: Weather?, battery: BatteryInfo?): Ambience {
    var base = Color(0xFF05060A)
    var glow = 1f
    if (settings.dayNight == "auto") {
        val rise = weather?.sunrise ?: LocalTime.of(7, 30)
        val set = weather?.sunset ?: LocalTime.of(20, 0)
        val t = now.toLocalTime()
        when {
            // Day: a lighter night-blue backdrop with stronger glows.
            !t.isBefore(rise) && t.isBefore(set) -> { base = Color(0xFF0C1324); glow = 1.35f }
            // Evening (until 1 h 30 after sunset): the classic look.
            !t.isBefore(set) && t.isBefore(set.plusMinutes(90)) && set.isBefore(LocalTime.of(22, 30)) -> Unit
            // Night: deep black, softer glows.
            else -> { base = Color(0xFF020203); glow = .62f }
        }
    }
    var glows: List<Color>? = null
    if (settings.livingHalos) {
        glows = livingGlows(now.toLocalTime())
        if (battery != null && !battery.charging) glow *= .45f + .55f * (battery.level.coerceIn(0, 100) / 100f)
    }
    return Ambience(base, glow, glows)
}
