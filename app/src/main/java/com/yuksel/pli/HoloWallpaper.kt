package com.yuksel.pli

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.sin

/**
 * "Fold Launcher Pro" live wallpaper, for the lock screen (and the home screen if wanted): the same black backdrop
 * and three iridescent halos as the launcher, in the palette chosen in its settings. The halos drift slowly while
 * the wallpaper is visible and stop as soon as it isn't; pages swiped on the home screen move them a little.
 */
class HoloWallpaper : WallpaperService() {
    override fun onCreateEngine(): Engine = HoloEngine()

    private inner class HoloEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val prefs = getSharedPreferences("pli", Context.MODE_PRIVATE)
        private var visible = false
        private var start = System.currentTimeMillis()
        private var pageOffset = .5f
        private var colors = glowColors()
        private val frame = Runnable { draw(); if (visible) scheduleNext() }

        private fun glowColors(): IntArray {
            val p = paletteForKey(prefs.getString("palette", "holo") ?: "holo")
            return intArrayOf(p.glowA.toArgb(), p.glowB.toArgb(), p.glowC.toArgb())
        }

        private fun scheduleNext() { handler.removeCallbacks(frame); handler.postDelayed(frame, 1000L / 24) }

        override fun onCreate(surfaceHolder: SurfaceHolder) { super.onCreate(surfaceHolder); prefs.registerOnSharedPreferenceChangeListener(this) }
        override fun onDestroy() { prefs.unregisterOnSharedPreferenceChangeListener(this); handler.removeCallbacks(frame); super.onDestroy() }

        override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) {
            if (key == "palette") { colors = glowColors(); draw() }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) { draw(); scheduleNext() } else handler.removeCallbacks(frame)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { super.onSurfaceChanged(holder, format, width, height); draw() }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int) {
            pageOffset = xOffset; if (!visible) return; draw()
        }

        private fun glow(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int, alpha: Float) {
            val c = (color and 0x00FFFFFF) or ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
            paint.shader = RadialGradient(x, y, radius, intArrayOf(c, c and 0x00FFFFFF), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, radius, paint)
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                val w = canvas.width.toFloat(); val h = canvas.height.toFloat()
                canvas.drawColor(0xFF05060A.toInt())
                val t = (System.currentTimeMillis() - start) / 1000.0
                // A slow orbit (about 40 s), plus a small shift with the home screen page.
                val dx = (sin(t * .16) * w * .05).toFloat() + (pageOffset - .5f) * w * .12f
                val dy = (cos(t * .13) * h * .04).toFloat()
                val r = maxOf(w, h)
                glow(canvas, w * .05f + dx, h * .06f + dy, r * .42f, colors[0], .30f)
                glow(canvas, w * 1.0f - dx * 1.4f, h * .48f - dy, r * .46f, colors[1], .24f)
                glow(canvas, w * .18f + dx * .7f, h * 1.02f + dy * .6f, r * .40f, colors[2], .18f)
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }
}
