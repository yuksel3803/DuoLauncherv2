package com.yuksel.pli

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.os.Process
import android.os.UserManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs

/**
 * The dock inside apps: a slim holographic handle on the right edge of the screen. Tap it and the dock apps slide out;
 * tap one to open it, hold one to open it next to the current app (split screen). Hidden while the home screen is shown.
 * Needs "Afficher par-dessus les autres applis".
 */
class FloatingDock : Service() {
    private lateinit var windows: WindowManager
    private var handle: View? = null
    private var panel: View? = null
    private val dp get() = resources.displayMetrics.density

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windows = getSystemService(WindowManager::class.java)
        instance = this
        showHandle()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { refreshVisibility(); return START_STICKY }

    override fun onDestroy() {
        instance = null
        removePanel()
        handle?.let { runCatching { windows.removeView(it) } }
        handle = null
        super.onDestroy()
    }

    private fun overlayParams(width: Int, height: Int, extraFlags: Int = 0) = WindowManager.LayoutParams(
        width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or extraFlags,
        PixelFormat.TRANSLUCENT)

    private fun prefs() = getSharedPreferences("floating_dock", Context.MODE_PRIVATE)

    @SuppressLint("ClickableViewAccessibility")
    private fun showHandle() {
        val palette = paletteForKey(getSharedPreferences("pli", Context.MODE_PRIVATE).getString("palette", "holo") ?: "holo")
        val view = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(palette.a.toArgb(), palette.b.toArgb(), palette.c.toArgb()))
                .apply { cornerRadius = 6 * dp; alpha = 190 }
            contentDescription = "Dock Fold Launcher Pro"
        }
        val params = overlayParams((7 * dp).toInt(), (88 * dp).toInt()).apply {
            gravity = Gravity.END or Gravity.TOP
            y = prefs().getInt("y", (resources.displayMetrics.heightPixels * .38f).toInt())
        }
        // The touch area is wider than the visible pill: easier to catch at the edge.
        val touch = LinearLayout(this).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding((14 * dp).toInt(), 0, (3 * dp).toInt(), 0)
            addView(view, LinearLayout.LayoutParams((7 * dp).toInt(), (88 * dp).toInt()))
        }
        params.width = (24 * dp).toInt()
        var downY = 0f; var startY = 0; var moved = false
        touch.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; startY = params.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dy = e.rawY - downY
                    if (abs(dy) > 8 * dp) moved = true
                    if (moved) { params.y = (startY + dy).toInt().coerceAtLeast(0); runCatching { windows.updateViewLayout(touch, params) } }
                }
                MotionEvent.ACTION_UP -> if (moved) prefs().edit().putInt("y", params.y).apply() else showPanel()
            }
            true
        }
        runCatching { windows.addView(touch, params) }.onSuccess { handle = touch }
        refreshVisibility()
    }

    /** The dock apps: the inner dock (up to 10), without hidden or vault apps. */
    private fun dockApps(): List<Triple<String, ComponentName, android.os.UserHandle>> {
        val store = Store(this)
        val layout = store.loadLayout()
        val users = getSystemService(UserManager::class.java)
        val concealed = layout.hidden + store.vault
        return layout.dockOf(Screen.INNER).filter { it !in concealed }.mapNotNull { id ->
            val component = ComponentName.unflattenFromString(id.substringBefore('#')) ?: return@mapNotNull null
            val user = id.substringAfter('#', "").toLongOrNull()?.let { users.getUserForSerialNumber(it) } ?: Process.myUserHandle()
            Triple(id, component, user)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showPanel() {
        if (panel != null) { removePanel(); return }
        val apps = getSystemService(LauncherApps::class.java)
        val density = resources.displayMetrics.densityDpi
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        for ((_, component, user) in dockApps()) {
            val info = runCatching {
                apps.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component), user)
            }.getOrNull() ?: continue
            val icon = ImageView(this).apply {
                setImageDrawable(runCatching { info.getBadgedIcon(density) }.getOrNull())
                contentDescription = info.label
                val pad = (6 * dp).toInt(); setPadding(pad, pad, pad, pad)
                setOnClickListener { open(component, user, adjacent = false); removePanel() }
                setOnLongClickListener { open(component, user, adjacent = true); removePanel(); true }
            }
            column.addView(icon, LinearLayout.LayoutParams((58 * dp).toInt(), (58 * dp).toInt()))
        }
        if (column.childCount == 0) column.addView(TextView(this).apply {
            text = "Dock vide"; setTextColor(0xFFB9B4D6.toInt()); textSize = 12f
        })
        column.addView(TextView(this).apply {
            text = "Appui long :\nà côté"; setTextColor(0xFFB9B4D6.toInt()); textSize = 10f; gravity = Gravity.CENTER
            setPadding(0, (6 * dp).toInt(), 0, 0)
        })
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            val pad = (8 * dp).toInt(); setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                cornerRadius = 30 * dp; setColor(0xF00D0E16.toInt()); setStroke((1 * dp).toInt(), 0x40FFFFFF)
            }
            addView(column)
            // A touch outside the panel closes it.
            setOnTouchListener { _, e -> if (e.actionMasked == MotionEvent.ACTION_OUTSIDE) { removePanel(); true } else false }
        }
        val params = overlayParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = (12 * dp).toInt()
        }
        val max = (resources.displayMetrics.heightPixels * .8f).toInt()
        scroll.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        if (scroll.measuredHeight > max) params.height = max
        runCatching { windows.addView(scroll, params) }.onSuccess { panel = scroll }
    }

    private fun removePanel() { panel?.let { runCatching { windows.removeView(it) } }; panel = null }

    private fun open(component: ComponentName, user: android.os.UserHandle, adjacent: Boolean) {
        if (!adjacent) {
            val ok = runCatching { getSystemService(LauncherApps::class.java).startMainActivity(component, user, null, null) }.isSuccess
            if (!ok) Toast.makeText(this, "Impossible d’ouvrir cette appli", Toast.LENGTH_SHORT).show()
            return
        }
        // Split screen: the app in front is known through the accessibility service (package name only).
        val front = PliAccessibility.frontPackage
        if (!PliAccessibility.enabled(this) || front == null) {
            Toast.makeText(this, "Pour l’écran partagé, active « Fold Launcher Pro – gestes et écran partagé » dans Accessibilité, puis rouvre ton appli",
                Toast.LENGTH_LONG).show()
            runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
        if (front == component.packageName) { Toast.makeText(this, "Cette appli est déjà ouverte", Toast.LENGTH_SHORT).show(); return }
        SplitActivity.start(this, front, component, user)
    }

    private fun refreshVisibility() {
        handle?.visibility = if (homeVisible) View.GONE else View.VISIBLE
        if (homeVisible) removePanel()
    }

    companion object {
        private var instance: FloatingDock? = null
        /** True while Fold Launcher Pro itself is on screen: the handle hides. */
        var homeVisible = false
            set(value) { field = value; instance?.refreshVisibility() }

        fun canShow(context: Context) = Settings.canDrawOverlays(context)

        /** Starts or stops the service to match the setting and the permission. Call while the launcher is in front. */
        fun sync(context: Context, enabled: Boolean) {
            val intent = Intent(context, FloatingDock::class.java)
            if (enabled && canShow(context)) runCatching { context.startService(intent) }
            else if (instance != null) runCatching { context.stopService(intent) }
        }

        fun requestPermission(context: Context) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
