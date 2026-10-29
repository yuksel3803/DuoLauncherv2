package com.yuksel.pli

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/** Clock styles offered in the settings: key to French name. */
val CLOCK_STYLES = listOf("fine" to "Fine", "bold" to "Grasse", "analog" to "Aiguilles", "words" to "En lettres")

val LocalClockStyle = staticCompositionLocalOf { "fine" }

private val UNITS = listOf("zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", "onze", "douze",
    "treize", "quatorze", "quinze", "seize")
private val TENS = mapOf(2 to "vingt", 3 to "trente", 4 to "quarante", 5 to "cinquante")

/** 0..59 in French words; [feminine] gives "une" (une heure, vingt et une). */
fun frenchNumber(n: Int, feminine: Boolean = false): String {
    fun unit(u: Int) = if (u == 1 && feminine) "une" else UNITS[u]
    return when {
        n <= 16 -> unit(n)
        n < 20 -> "dix-" + UNITS[n - 10]
        else -> {
            val t = TENS[n / 10] ?: return n.toString(); val u = n % 10
            when (u) { 0 -> t; 1 -> "$t et ${unit(1)}"; else -> "$t-${UNITS[u]}" }
        }
    }
}

/** "quatorze heures douze", "midi et quart" style kept simple and exact: "midi vingt", "minuit cinq". */
fun timeInWords(now: LocalDateTime): String {
    val h = now.hour; val m = now.minute
    val hour = when (h) { 0 -> "minuit"; 12 -> "midi"; 1 -> "une heure"; else -> frenchNumber(h, feminine = true) + " heures" }
    return if (m == 0) hour else hour + " " + frenchNumber(m, feminine = true)
}

/** The big clock in the chosen style. */
@Composable
fun ClockFace(now: LocalDateTime, pattern: String, size: TextUnit, style: String, modifier: Modifier = Modifier) {
    when (style) {
        "bold" -> IridescentText(now.format(DateTimeFormatter.ofPattern(pattern)), size, modifier, weight = FontWeight.Bold, letterSpacing = (-3).sp)
        "words" -> Text(timeInWords(now), modifier.widthIn(max = (size.value * 4.6f).dp).padding(vertical = 6.dp), maxLines = 2,
            style = TextStyle(brush = Brush.linearGradient(Holo.Iridescent), fontSize = size * .44f, lineHeight = size * .5f,
                fontWeight = FontWeight.Light, fontFamily = Holo.Font, letterSpacing = (-.5).sp))
        "analog" -> AnalogClock(now, modifier.padding(vertical = 6.dp).size((size.value * 1.15f).dp))
        else -> IridescentText(now.format(DateTimeFormatter.ofPattern(pattern)), size, modifier)
    }
}

/** A holographic dial: iridescent ring, twelve marks, hour and minute hands. */
@Composable
private fun AnalogClock(now: LocalDateTime, modifier: Modifier) {
    val colors = Holo.Iridescent
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2f - 4.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(colors[1].copy(alpha = .18f), Color.Transparent), c, r * 1.1f), r * 1.1f, c)
        drawCircle(Brush.sweepGradient(colors + colors.first(), c), r, c, style = Stroke(2.5.dp.toPx()))
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90)
            val outer = Offset(c.x + r * .9f * cos(a).toFloat(), c.y + r * .9f * sin(a).toFloat())
            val inner = Offset(c.x + r * (if (i % 3 == 0) .74f else .8f) * cos(a).toFloat(), c.y + r * (if (i % 3 == 0) .74f else .8f) * sin(a).toFloat())
            drawLine(Holo.Soft.copy(alpha = if (i % 3 == 0) .9f else .45f), inner, outer, (if (i % 3 == 0) 3f else 1.6f).dp.toPx(), StrokeCap.Round)
        }
        fun hand(angle: Double, length: Float, width: Float, color: Brush) {
            val a = Math.toRadians(angle - 90)
            drawLine(color, c, Offset(c.x + length * cos(a).toFloat(), c.y + length * sin(a).toFloat()), width, StrokeCap.Round)
        }
        val minute = now.minute + now.second / 60.0
        val hour = (now.hour % 12) + minute / 60.0
        hand(hour * 30, r * .5f, 5.dp.toPx(), Brush.linearGradient(listOf(colors[0], colors[1])))
        hand(minute * 6, r * .74f, 3.dp.toPx(), Brush.linearGradient(listOf(colors[1], colors[2])))
        drawCircle(Holo.Ink, 4.dp.toPx(), c)
    }
}
