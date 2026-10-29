package com.yuksel.pli

import android.content.Context
import org.json.JSONObject

data class PliSettings(
    val labels: Boolean = true,
    val phoneWallpaper: Boolean = false,
    val parallax: Boolean = true,
    val bubbles: Boolean = true,
    val dockSize: Int = 5,
    val innerDockSize: Int = 5,
    val coverColumns: Int = 0,
    val coverRows: Int = 0,
    val innerColumns: Int = 0,
    val innerRows: Int = 0,
    val discover: Boolean = true,
    val addNewApps: Boolean = true,
    val city: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val swipeDownNotifications: Boolean = true,
    val iconPack: String = "",
    val tintedIcons: Boolean = false,
    val doubleTap: String = "lock",
    val twoFingerUp: String = "none",
    val twoFingerDown: String = "notifications",
    val lockLayout: Boolean = false,
    val recentInDock: Int = 2,
    val iconShape: String = "circle",
    /** Right of the clock: "google", "finder", "pli" or "none". */
    val headerSearch: String = "google",
    /** "holo": iridescent burst + app growing from its icon; "system": Android's default. */
    val launchAnimation: String = "holo",
    val haptics: Boolean = true,
    /** Below 20 % (not charging): no moving halos, no launch effect. */
    val batterySaver: Boolean = true,
    val onboarded: Boolean = false,
    /** Folding or unfolding opens the other screen on the matching page. */
    val continuity: Boolean = true,
    /** Folding on a page the cover doesn't have shows its apps on the cover ("miroir"). */
    val coverMirror: Boolean = true,
    /** Suggestions row: "context" (car, headphones, meeting, bedtime) or "off". */
    val suggestions: String = "context",
    val newBadge: Boolean = true,
    /** Accent colours: "holo", "wallpaper", "aurore", "ocean", "braise", "menthe". */
    val palette: String = "holo",
    /** "auto": brighter by day, darker and warmer at night (sunrise/sunset); "off". */
    val dayNight: String = "auto",
    /** Halos follow the hour (dawn, noon, dusk, night) and fade with the battery. */
    val livingHalos: Boolean = false,
    /** Page change effect: "classic", "depth", "cube", "fade", "carousel". */
    val pageTransition: String = "classic",
    /** Half-folded in landscape: clock and music on top, controls below. */
    val tabletop: Boolean = true,
    /** Unread notifications as bubbles orbiting the icon instead of a number. */
    val orbitBadges: Boolean = false,
    /** Clock look: "fine", "bold", "analog" or "words". */
    val clockStyle: String = "fine",
    /** Rain, snow, storm or fog drawn lightly over the backdrop when the weather says so. */
    val liveWeather: Boolean = true,
    /** Inner screen: the dock slips away while pages scroll and comes back when they stop. */
    val autoHideDock: Boolean = false,
)

/** Saves the layout, the settings and the quick note in private preferences. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("pli", Context.MODE_PRIVATE)

    fun hasLayout() = prefs.contains("layout")

    fun loadLayout(): HomeLayout = prefs.getString("layout", null)
        ?.let { runCatching { HomeLayout.fromJson(JSONObject(it)) }.getOrNull() } ?: HomeLayout()

    fun saveLayout(layout: HomeLayout) { prefs.edit().putString("layout", layout.toJson().toString()).apply() }

    fun loadSettings() = PliSettings(
        labels = prefs.getBoolean("labels", true),
        phoneWallpaper = prefs.getBoolean("phoneWallpaper", false),
        parallax = prefs.getBoolean("parallax", true),
        bubbles = prefs.getBoolean("bubbles", true),
        dockSize = prefs.getInt("dockSize", 5),
        innerDockSize = prefs.getInt("innerDockSize", prefs.getInt("dockSize", 5)),
        coverColumns = prefs.getInt("coverColumns", 0),
        coverRows = prefs.getInt("coverRows", 0),
        innerColumns = prefs.getInt("innerColumns", 0),
        innerRows = prefs.getInt("innerRows", 0),
        discover = prefs.getBoolean("discover", true),
        addNewApps = prefs.getBoolean("addNewApps", true),
        city = prefs.getString("city", "") ?: "",
        latitude = prefs.getString("lat", null)?.toDoubleOrNull(),
        longitude = prefs.getString("lon", null)?.toDoubleOrNull(),
        swipeDownNotifications = prefs.getBoolean("swipeDownNotifications", true),
        iconPack = prefs.getString("iconPack", "") ?: "",
        tintedIcons = prefs.getBoolean("tintedIcons", false),
        doubleTap = prefs.getString("doubleTap", "lock") ?: "lock",
        twoFingerUp = prefs.getString("twoFingerUp", "none") ?: "none",
        twoFingerDown = prefs.getString("twoFingerDown", "notifications") ?: "notifications",
        lockLayout = prefs.getBoolean("lockLayout", false),
        recentInDock = prefs.getInt("recentInDock", 2),
        iconShape = prefs.getString("iconShape", "circle") ?: "circle",
        headerSearch = prefs.getString("headerSearch", "google") ?: "google",
        launchAnimation = prefs.getString("launchAnimation", "holo") ?: "holo",
        haptics = prefs.getBoolean("haptics", true),
        batterySaver = prefs.getBoolean("batterySaver", true),
        onboarded = prefs.getBoolean("onboarded", false),
        continuity = prefs.getBoolean("continuity", true),
        coverMirror = prefs.getBoolean("coverMirror", true),
        // The "usual apps at this hour" row was removed: an old "always" now means "context".
        suggestions = (prefs.getString("suggestions", "context") ?: "context").let { if (it == "always") "context" else it },
        newBadge = prefs.getBoolean("newBadge", true),
        palette = prefs.getString("palette", "holo") ?: "holo",
        dayNight = prefs.getString("dayNight", "auto") ?: "auto",
        livingHalos = prefs.getBoolean("livingHalos", false),
        pageTransition = prefs.getString("pageTransition", "classic") ?: "classic",
        tabletop = prefs.getBoolean("tabletop", true),
        orbitBadges = prefs.getBoolean("orbitBadges", false),
        clockStyle = prefs.getString("clockStyle", "fine") ?: "fine",
        liveWeather = prefs.getBoolean("liveWeather", true),
        autoHideDock = prefs.getBoolean("autoHideDock", false),
    )

    fun saveSettings(s: PliSettings) {
        prefs.edit().putBoolean("labels", s.labels).putBoolean("phoneWallpaper", s.phoneWallpaper)
            .putBoolean("parallax", s.parallax).putBoolean("bubbles", s.bubbles).putInt("dockSize", s.dockSize)
            .putInt("innerDockSize", s.innerDockSize).putInt("coverColumns", s.coverColumns).putInt("coverRows", s.coverRows)
            .putInt("innerColumns", s.innerColumns).putInt("innerRows", s.innerRows).putBoolean("discover", s.discover)
            .putBoolean("addNewApps", s.addNewApps).putString("city", s.city)
            .putString("lat", s.latitude?.toString()).putString("lon", s.longitude?.toString())
            .putBoolean("swipeDownNotifications", s.swipeDownNotifications)
            .putString("iconPack", s.iconPack).putBoolean("tintedIcons", s.tintedIcons).putString("doubleTap", s.doubleTap)
            .putString("twoFingerUp", s.twoFingerUp).putString("twoFingerDown", s.twoFingerDown)
            .putBoolean("lockLayout", s.lockLayout).putInt("recentInDock", s.recentInDock)
            .putString("iconShape", s.iconShape).putString("headerSearch", s.headerSearch)
            .putString("launchAnimation", s.launchAnimation).putBoolean("haptics", s.haptics)
            .putBoolean("batterySaver", s.batterySaver).putBoolean("onboarded", s.onboarded)
            .putBoolean("continuity", s.continuity).putBoolean("coverMirror", s.coverMirror).putString("suggestions", s.suggestions)
            .putBoolean("newBadge", s.newBadge).putString("palette", s.palette).putString("dayNight", s.dayNight)
            .putBoolean("livingHalos", s.livingHalos).putString("pageTransition", s.pageTransition)
            .putBoolean("tabletop", s.tabletop).putBoolean("orbitBadges", s.orbitBadges)
            .putString("clockStyle", s.clockStyle).putBoolean("liveWeather", s.liveWeather).putBoolean("autoHideDock", s.autoHideDock).apply()
    }

    /** Everything Pli saved (layout, settings, note) as one JSON document. Android widgets are left out: they belong to this phone. */
    fun export(): JSONObject {
        val layout = loadLayout()
        return JSONObject().put("app", "Pli").put("version", 1)
            .put("layout", layout.copy(today = layout.today.filterNot { it.isAndroid }).toJson())
            .put("settings", JSONObject().apply {
                prefs.all.forEach { (key, value) -> if (key != "layout" && key != "note" && key != "vault" && value != null) put(key, value) }
            })
            .put("note", note)
    }

    /** Restores a document from [export]. Returns false when it is not a Pli backup. */
    fun import(json: JSONObject, keepAndroidWidgets: List<TodayItem>): Boolean {
        if (json.optString("app") != "Pli") return false
        val layout = HomeLayout.fromJson(json.optJSONObject("layout") ?: return false)
        val settings = json.optJSONObject("settings") ?: JSONObject()
        val edit = prefs.edit()
        settings.keys().forEach { key ->
            when (val v = settings.get(key)) {
                is Boolean -> edit.putBoolean(key, v)
                is Int -> edit.putInt(key, v)
                is Long -> edit.putInt(key, v.toInt())
                is Double -> edit.putString(key, v.toString())
                is String -> edit.putString(key, v)
            }
        }
        edit.putString("layout", layout.copy(today = layout.today.filterNot { it.isAndroid } + keepAndroidWidgets).toJson().toString())
        edit.putString("note", json.optString("note", note))
        edit.apply()
        return true
    }

    /** Apps in the secret vault: hidden everywhere, opened only after the fingerprint. */
    var vault: Set<String>
        get() = prefs.getStringSet("vault", emptySet())?.toSet() ?: emptySet()
        set(value) { prefs.edit().putStringSet("vault", value).apply() }

    var note: String
        get() = prefs.getString("note", "") ?: ""
        set(value) { prefs.edit().putString("note", value).apply() }
}
