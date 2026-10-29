package com.yuksel.pli

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/** A fixed pseudo-random number in 0..1 for particle [i] and channel [k]: no allocation per frame. */
private fun rnd(i: Int, k: Int): Float {
    var x = i * 374761393 + k * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFFFF) / 16777216f
}

/**
 * Light weather over the backdrop: falling streaks for rain (more and faster in a storm, with a rare flash),
 * drifting flakes for snow, slow haze for fog. Time is read while drawing, so nothing recomposes.
 */
@Composable
fun WeatherEffect(sky: Sky?, modifier: Modifier = Modifier.fillMaxSize()) {
    val kind = when (sky) { Sky.DRIZZLE, Sky.RAIN -> "rain"; Sky.STORM -> "storm"; Sky.SNOW -> "snow"; Sky.FOG -> "fog"; else -> null } ?: return
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(kind) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time.floatValue = (it - start) / 1e9f }
    }
    val drizzle = sky == Sky.DRIZZLE
    Canvas(modifier) {
        val t = time.floatValue
        val w = size.width; val h = size.height
        when (kind) {
            "rain", "storm" -> {
                val count = when { kind == "storm" -> 90; drizzle -> 35; else -> 60 }
                val speed = if (kind == "storm") 1.5f else 1f
                for (i in 0 until count) {
                    val len = (14f + 22f * rnd(i, 1)).dp.toPx()
                    val v = (.55f + .45f * rnd(i, 2)) * speed * h
                    val y = ((rnd(i, 3) * (h + len) + v * t) % (h + len)) - len
                    val x = (rnd(i, 4) * (w + 60f) + y * .12f) % (w + 60f) - 30f
                    drawLine(Brush.linearGradient(listOf(Color.Transparent, Holo.Cyan.copy(alpha = .16f + .12f * rnd(i, 5))), Offset(x, y), Offset(x + len * .12f, y + len)),
                        Offset(x, y), Offset(x + len * .12f, y + len), 1.4.dp.toPx(), StrokeCap.Round)
                }
                if (kind == "storm") {
                    // A short, soft flash every ~9 s.
                    val phase = t % 9f
                    if (phase < .16f) drawRect(Color.White.copy(alpha = .07f * (1f - phase / .16f)))
                }
            }
            "snow" -> for (i in 0 until 55) {
                val r = (1.6f + 2.6f * rnd(i, 1)).dp.toPx()
                val v = (.05f + .07f * rnd(i, 2)) * h
                val y = ((rnd(i, 3) * (h + 20f) + v * t) % (h + 20f)) - 10f
                val x = rnd(i, 4) * w + sin(t * (.6f + rnd(i, 5)) + i) * 14.dp.toPx()
                drawCircle(Color.White.copy(alpha = .35f + .3f * rnd(i, 6)), r, Offset(x, y))
            }
            "fog" -> for (i in 0 until 3) {
                val y = h * (.25f + .3f * i)
                val x = ((rnd(i, 1) * w + t * (12f + 8f * i).dp.toPx()) % (w * 1.6f)) - w * .3f
                val rad = w * .6f
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = .06f), Color.Transparent), Offset(x, y), rad), rad, Offset(x, y))
            }
        }
    }
}
