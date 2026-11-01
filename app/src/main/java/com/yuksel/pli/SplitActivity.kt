package com.yuksel.pli

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.widget.Toast

/**
 * A blink-long stepping stone for split screen. Android opens an app "next to" another only when asked by a real
 * window in front, so this window: 1) opens the chosen app beside itself, 2) reopens the app you were using in its
 * own half, 3) closes. Result: your app on one side, the chosen one on the other.
 */
class SplitActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Opaque (a see-through window can't anchor a split): the dark holographic base, shown for a blink.
        window.decorView.setBackgroundColor(0xFF05060A.toInt())
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        if (started) return
        started = true
        val front = intent.getStringExtra(EXTRA_FRONT)
        @Suppress("DEPRECATION") val second = intent.getParcelableExtra<ComponentName>(EXTRA_SECOND)
        @Suppress("DEPRECATION") val user = intent.getParcelableExtra<UserHandle>(EXTRA_USER) ?: Process.myUserHandle()
        if (second == null) { finish(); return }
        val ok = runCatching {
            if (user == Process.myUserHandle()) startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(second)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
            else getSystemService(LauncherApps::class.java).startMainActivity(second, user, null, null)
        }.isSuccess
        if (!ok) { Toast.makeText(this, "Cette appli ne s’ouvre pas en écran partagé", Toast.LENGTH_SHORT).show(); finish(); return }
        // Then the app you were using takes this window's half.
        handler.postDelayed({
            front?.let { pkg -> packageManager.getLaunchIntentForPackage(pkg)?.let { i -> runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } }
            handler.postDelayed({ finish(); overridePendingTransition(0, 0) }, 250)
        }, 450)
    }

    override fun onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy() }

    companion object {
        private const val EXTRA_FRONT = "front"
        private const val EXTRA_SECOND = "second"
        private const val EXTRA_USER = "user"

        fun start(context: Context, frontPackage: String?, second: ComponentName, user: UserHandle) {
            runCatching {
                context.startActivity(Intent(context, SplitActivity::class.java)
                    .putExtra(EXTRA_FRONT, frontPackage).putExtra(EXTRA_SECOND, second).putExtra(EXTRA_USER, user)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
            }
        }
    }
}
