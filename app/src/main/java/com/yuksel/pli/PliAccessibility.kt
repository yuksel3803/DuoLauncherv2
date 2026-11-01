package com.yuksel.pli

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Used for two things a launcher can't do otherwise: locking the screen from a home gesture, and putting the current app
 * into split screen so the dock (in apps) can open another app next to it. It never reads the screen's content.
 */
class PliAccessibility : AccessibilityService() {
    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        @Volatile private var instance: PliAccessibility? = null

        fun lock(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true

        /** Docks the app on screen into split screen (Android's own split action). False when the service is off. */
        fun splitScreen(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN) == true

        fun enabled(context: Context): Boolean {
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = ComponentName(context, PliAccessibility::class.java)
            return list.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}
