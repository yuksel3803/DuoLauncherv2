package com.yuksel.pli

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val section = RoundedCornerShape(26.dp)

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title.uppercase(), color = Holo.Muted, fontSize = 11.sp, letterSpacing = 2.5.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
        Column(Modifier.fillMaxWidth().glass(section, .05f).padding(vertical = 6.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(title: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Holo.Ink, fontSize = 15.sp)
            if (detail != null) Text(detail, color = Holo.Muted, fontSize = 12.sp)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF120A22), checkedTrackColor = Holo.Violet,
            uncheckedThumbColor = Holo.Muted, uncheckedTrackColor = Color.White.copy(alpha = .08f), uncheckedBorderColor = Color.White.copy(alpha = .2f)))
    }
}

@Composable
private fun ActionRow(title: String, detail: String?, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Holo.Ink, fontSize = 15.sp)
            if (detail != null) Text(detail, color = Holo.Muted, fontSize = 12.sp)
        }
        Text(action, color = Holo.Violet, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** A row of round choices; 0 shows as "Auto". */
@Composable
private fun ChoiceRow(title: String, detail: String?, options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, color = Holo.Ink, fontSize = 15.sp)
        if (detail != null) Text(detail, color = Holo.Muted, fontSize = 12.sp)
        Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { n ->
                val on = selected == n
                Box(Modifier.height(38.dp).widthIn(min = 38.dp).clip(RoundedCornerShape(50))
                    .then(if (on) Modifier.background(Brush.linearGradient(Holo.Iridescent), RoundedCornerShape(50)) else Modifier.glass(RoundedCornerShape(50)))
                    .clickable { onSelect(n) }.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text(if (n == 0) "Auto" else "$n", color = if (on) Color(0xFF120A22) else Holo.Soft, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private enum class SettingsCategory(val label: String, val icon: ImageVector, val subs: List<Pair<String, ImageVector>>) {
    THEMES("Thèmes", Icons.Rounded.Palette, listOf("Fond" to Icons.Rounded.Wallpaper, "Couleurs" to Icons.Rounded.ColorLens, "Icônes" to Icons.Rounded.Category, "Forme" to Icons.Rounded.Interests, "Animations" to Icons.Rounded.AutoAwesome)),
    HOME("Accueil", Icons.Rounded.PhoneAndroid, listOf("Grille" to Icons.Rounded.GridView, "Dock" to Icons.Rounded.Dock,
        "Horloge" to Icons.Rounded.Schedule, "Pages" to Icons.Rounded.ViewCarousel, "Suggestions" to Icons.Rounded.AutoAwesome,
        "Pliage" to Icons.Rounded.Devices)),
    APPS("Applis", Icons.Rounded.Apps, listOf("Masquées" to Icons.Rounded.VisibilityOff, "Coffre" to Icons.Rounded.Lock, "Ajout" to Icons.Rounded.AddToHomeScreen)),
    WIDGETS("Widgets", Icons.Rounded.Widgets, listOf("Météo" to Icons.Rounded.WbSunny, "Notifications" to Icons.Rounded.Notifications)),
    GESTURES("Gestes", Icons.Rounded.Swipe, listOf("Gestes" to Icons.Rounded.TouchApp, "Dessins" to Icons.Rounded.Gesture)),
    GENERAL("Paramètres", Icons.Rounded.Settings, listOf("Général" to Icons.Rounded.Tune, "Sauvegarde" to Icons.Rounded.Backup, "Mises à jour" to Icons.Rounded.SystemUpdate)),
}

/** One entry of the side rails: a round icon with its name; the chosen one glows. */
@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(52.dp).then(if (selected) Modifier.halo(Holo.Violet.copy(alpha = .3f), 8.dp)
            .background(Brush.linearGradient(Holo.Iridescent.map { it.copy(alpha = .35f) }), CircleShape)
            .border(1.dp, Holo.edgeBrush, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (selected) Holo.Ink else Holo.Soft, modifier = Modifier.size(26.dp))
        }
        Text(label, color = if (selected) Holo.Ink else Holo.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Width ÷ height of each screen, measured when it was last shown, so the preview has the real proportions. */
object ScreenShapes {
    var cover = .62f
    var inner = 1.2f
}

/** Words that find each settings page from the search field. */
private val SETTINGS_INDEX: Map<String, String> = mapOf(
    "Fond" to "fond holographique halos animes fond d ecran wallpaper parallaxe arriere-plan meteo vivante pluie neige verrouillage anime",
    "Couleurs" to "couleurs palette aurore ocean braise menthe fond d ecran jour nuit ambiance halos vivants theme",
    "Icônes" to "pack d icones teintees noms des applis etiquettes labels",
    "Forme" to "forme des icones rond carre squircle goutte feuille cadre de verre bulles",
    "Animations" to "ouverture des applis animation holographique systeme changement de page cube profondeur fondu carrousel effet",
    "Grille" to "grille colonnes lignes ecran externe interne taille",
    "Dock" to "dock taille applis recentes cacher masquer automatique",
    "Horloge" to "horloge recherche google finder bouton style analogique aiguilles lettres grasse",
    "Pages" to "pages verrouiller disposition actus nouvelles applis",
    "Suggestions" to "suggestions contexte voiture casque pastille nouveau nouvelles applis",
    "Pliage" to "pliage continuite miroir chevalet plier deplier flex charniere",
    "Masquées" to "applis masquees cachees afficher",
    "Coffre" to "coffre secret cacher empreinte prive verrou",
    "Ajout" to "ajout nouvelles applis installation",
    "Météo" to "meteo ville position temperature",
    "Notifications" to "notifications pastilles badges orbite musique acces",
    "Gestes" to "gestes double appui deux doigts glisser verrouiller notifications guide",
    "Dessins" to "dessins dessiner lancer formes lettres",
    "Général" to "general launcher par defaut accueil vibrations haptique economie batterie guide presentation",
    "Sauvegarde" to "sauvegarde exporter importer disposition fichier",
    "Mises à jour" to "mises a jour version build verifier journal update",
)

/** A small live copy of the home screen (cover or inner): real time, palette and layout, following the settings as they change. */
@Composable
private fun HomePreview(launcher: Launcher, inner: Boolean, modifier: Modifier) {
    val s = launcher.settings
    val now = rememberMinute()
    val shape = RoundedCornerShape(if (inner) 18.dp else 22.dp)
    BoxWithConstraints(modifier.aspectRatio(if (inner) ScreenShapes.inner else ScreenShapes.cover).clip(shape).border(1.dp, Holo.edgeBrush, shape)) {
        if (s.phoneWallpaper) Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2B4C6B), Color(0xFFB0573A)))))
        HoloBackground(s.phoneWallpaper, false)
        val screen = if (inner) Screen.INNER else Screen.COVER
        val pad = 8.dp
        // The real grid of the first page: same cells, empty cells kept, big folders with their size.
        val grid = launcher.layout.grid(screen)
        val columns = grid?.columns ?: (if (inner) s.innerColumns else s.coverColumns).takeIf { it > 0 } ?: 4
        val rows = grid?.rows ?: (if (inner) s.innerRows else s.coverRows).takeIf { it > 0 } ?: 5
        val slots = launcher.visibleHome(screen).take(columns * rows)
        val dock = launcher.visibleDock(screen).mapNotNull { launcher.byId[it] }
        val clock = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        val home: @Composable (Modifier) -> Unit = { m ->
            Column(m) {
                Row(verticalAlignment = Alignment.Bottom) {
                    IridescentText(clock, if (inner) 18.sp else 22.sp, Modifier.weight(1f), letterSpacing = (-.5).sp)
                    if (s.headerSearch != "none") Box(Modifier.padding(bottom = 3.dp).size(16.dp).glass(CircleShape, .1f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Search, null, tint = Holo.Ink, modifier = Modifier.size(9.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                    val cw = maxWidth / columns
                    val ch = maxHeight / rows
                    val icon = minOf(cw * .62f, ch * (if (s.labels) .6f else .75f), 30.dp)
                    slots.forEachIndexed { index, id ->
                        if (id.isEmpty() || id == HomeLayout.COVERED) return@forEachIndexed
                        val row = index / columns; val col = index % columns
                        val folder = launcher.layout.folders[id]
                        val w = if (folder?.big == true) (if (folder.w <= 0) columns - col else folder.w.coerceIn(1, columns - col)) else 1
                        val h = if (folder?.big == true) folder.h.coerceIn(1, rows - row) else 1
                        Box(Modifier.offset(x = cw * col, y = ch * row).size(cw * w, ch * h), contentAlignment = Alignment.TopCenter) {
                            if (folder != null) {
                                val inside = launcher.folderApps(id)
                                val big = folder.big
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.padding(top = 2.dp).then(if (big) Modifier.size(cw * w - 4.dp, ch * h - 4.dp) else Modifier.size(icon))
                                        .glass(RoundedCornerShape(if (big) 10.dp else 8.dp), .1f).padding(2.dp)) {
                                        val per = if (big) maxOf(2, w * 2) else 2
                                        val tiny = if (big) minOf(icon * .8f, (cw * w - 8.dp) / per) else (icon - 4.dp) / 2
                                        Column {
                                            inside.take(if (big) per * maxOf(2, h * 2) else 4).chunked(per).forEach { line ->
                                                Row { line.forEach { AppIcon(it, tiny, showBadge = false) } }
                                            }
                                        }
                                    }
                                    if (s.labels && !big) Text(folder.title.ifBlank { "Dossier" }, color = Holo.Soft, fontSize = 5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            } else launcher.byId[id]?.let { app ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    AppIcon(app, icon, Modifier.padding(top = 2.dp), showBadge = false)
                                    if (s.labels) Text(app.label, color = Holo.Soft, fontSize = 5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        val dockIcon = if (inner) 14.dp else 18.dp
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (inner) Row(Modifier.weight(1f)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    IridescentText("Aujourd’hui", 11.sp, letterSpacing = 0.sp)
                    launcher.layout.today.take(4).forEach { item ->
                        Box(Modifier.fillMaxWidth().height(if (item.kind == TodayItem.WEATHER) 30.dp else 22.dp).glass(RoundedCornerShape(8.dp), .06f)
                            .padding(horizontal = 6.dp), contentAlignment = Alignment.CenterStart) {
                            Text(previewTitle(item.kind), color = Holo.Muted, fontSize = 5.sp, letterSpacing = 1.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.width(pad))
                home(Modifier.weight(1f).fillMaxHeight())
            } else home(Modifier.weight(1f))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(50), .08f).padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                dock.take(if (inner) s.innerDockSize else s.dockSize).forEach { AppIcon(it, dockIcon, showBadge = false) }
            }
        }
    }
}

private fun previewTitle(kind: String) = when (kind) {
    TodayItem.WEATHER -> "MÉTÉO"; TodayItem.AGENDA -> "AGENDA"; TodayItem.MEDIA -> "MUSIQUE"; TodayItem.BATTERY -> "BATTERIE"
    TodayItem.NOTE -> "NOTE"; TodayItem.TOGGLES -> "RACCOURCIS"; TodayItem.TASKS -> "RAPPELS"; TodayItem.SCREEN_TIME -> "TEMPS D’ÉCRAN"
    TodayItem.COUNTDOWN -> "COMPTE À REBOURS"; TodayItem.MEMORY -> "SOUVENIR"; TodayItem.TRIP -> "TRAJET"; else -> "WIDGET"
}

@Composable
fun SettingsPanel(
    launcher: Launcher, isDefaultHome: Boolean, badgesAllowed: Boolean, modifier: Modifier,
    onClose: () -> Unit, onMakeDefault: () -> Unit, onNotificationAccess: () -> Unit, onLocation: () -> Unit,
    onWeatherChanged: () -> Unit,
    onExport: () -> Unit = {}, onImport: () -> Unit = {}, accessibilityOn: Boolean = false, onAccessibility: () -> Unit = {},
    onGuide: () -> Unit = {}, onOnboarding: () -> Unit = {}, startOn: String? = null,
) {
    var picking by remember { mutableStateOf<String?>(null) }
    var pickFor by remember { mutableStateOf<((AppInfo) -> Unit)?>(null) }
    pickFor?.let { callback ->
        AppPick(launcher, modifier, onClose = { pickFor = null }) { app -> callback(app); pickFor = null }
        return
    }
    picking?.let { which ->
        AppPick(launcher, modifier, onClose = { picking = null }) { app ->
            launcher.updateSettings { when (which) {
                "doubleTap" -> it.copy(doubleTap = "app:${app.id}")
                "twoFingerUp" -> it.copy(twoFingerUp = "app:${app.id}")
                else -> it.copy(twoFingerDown = "app:${app.id}")
            } }
            picking = null
        }
        return
    }
    var category by rememberSaveable { mutableStateOf(if (isDefaultHome) SettingsCategory.THEMES else SettingsCategory.GENERAL) }
    var sub by rememberSaveable { mutableStateOf(category.subs.first().first) }
    var query by rememberSaveable { mutableStateOf("") }
    var previewInner by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    fun go(c: SettingsCategory, target: String) {
        val changed = c != category
        category = c; sub = target; query = ""
        val index = c.subs.indexOfFirst { it.first == target }.coerceAtLeast(0)
        scope.launch { if (changed) list.scrollToItem(index) else list.animateScrollToItem(index) }
    }
    // Opened from elsewhere on a given page (e.g. "Mes dessins" from the drawing pad).
    LaunchedEffect(startOn) {
        val target = startOn ?: return@LaunchedEffect
        SettingsCategory.entries.firstOrNull { c -> c.subs.any { it.first == target } }?.let { go(it, target) }
    }
    // The page under the title follows the scroll.
    LaunchedEffect(list, category) {
        snapshotFlow { list.firstVisibleItemIndex }.collect { i -> category.subs.getOrNull(i)?.let { sub = it.first } }
    }
    val chip: @Composable (String, Boolean, () -> Unit) -> Unit = { label, on, click ->
        Box(Modifier.height(36.dp).clip(RoundedCornerShape(50))
            .then(if (on) Modifier.background(Brush.linearGradient(Holo.Iridescent), RoundedCornerShape(50)) else Modifier.glass(RoundedCornerShape(50), .05f))
            .clickable(onClick = click).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (on) Color(0xFF120A22) else Holo.Soft, fontSize = 13.sp, maxLines = 1)
        }
    }
    // Every page of the category in one scroll; the chips jump to a page.
    val options: @Composable (Modifier) -> Unit = { m ->
        Column(m.clip(RoundedCornerShape(30.dp)).background(Holo.Panel).border(1.dp, Holo.edgeBrush, RoundedCornerShape(30.dp))) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(category.icon, null, tint = Holo.Violet, modifier = Modifier.size(24.dp))
                Text(category.label, color = Holo.Ink, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                category.subs.forEach { (name, _) -> chip(name, name == sub) { go(category, name) } }
            }
            LazyColumn(Modifier.fillMaxSize(), list, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 260.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)) {
                items(category.subs, key = { category.name + it.first }) { (name, icon) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(30.dp).background(Brush.linearGradient(Holo.Iridescent.map { it.copy(alpha = .28f) }), CircleShape),
                                contentAlignment = Alignment.Center) { Icon(icon, null, tint = Holo.Ink, modifier = Modifier.size(17.dp)) }
                            Text(name, color = Holo.Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 10.dp))
                        }
                        SettingsOptions(category, name, launcher, isDefaultHome, badgesAllowed, onMakeDefault, onNotificationAccess, onLocation,
                            onWeatherChanged, onExport, onImport, accessibilityOn, onAccessibility, onPick = { picking = it },
                            onGuide = onGuide, onOnboarding = onOnboarding, onPickApp = { pickFor = it })
                    }
                }
            }
        }
    }
    val categories: @Composable (Modifier) -> Unit = { m ->
        Column(m.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SettingsCategory.entries.forEach { c -> RailItem(c.icon, c.label, c == category) { go(c, c.subs.first().first) } }
        }
    }
    val preview: @Composable (Modifier) -> Unit = { m ->
        Column(m, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chip("Externe", !previewInner) { previewInner = false }
                chip("Interne", previewInner) { previewInner = true }
            }
            Spacer(Modifier.height(10.dp))
            HomePreview(launcher, previewInner, Modifier.widthIn(max = if (previewInner) 300.dp else 170.dp).fillMaxWidth())
            Text("Aperçu en direct", color = Holo.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
    val results = remember(query) {
        val q = query.trim().normalizedForSearch()
        if (q.length < 2) emptyList() else SettingsCategory.entries.flatMap { c -> c.subs.map { c to it } }.filter { (_, sub) ->
            sub.first.normalizedForSearch().contains(q) || (SETTINGS_INDEX[sub.first] ?: "").contains(q)
        }
    }
    BoxWithConstraints(modifier) {
        val wide = maxWidth > 700.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (wide) IridescentText("Réglages", 28.sp, Modifier.padding(end = 16.dp), letterSpacing = (-.5).sp)
                GlassField(query, { query = it }, "Rechercher un réglage", null, Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
                PillButton("Fermer", onClick = onClose)
            }
            if (query.trim().length >= 2) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (results.isEmpty()) Text("Aucun réglage ne correspond.", color = Holo.Muted, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                    results.forEach { (c, entry) ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).glass(RoundedCornerShape(20.dp), .05f)
                            .clickable { go(c, entry.first) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(entry.second, null, tint = Holo.Violet, modifier = Modifier.size(22.dp))
                            Column(Modifier.padding(start = 14.dp)) {
                                Text(entry.first, color = Holo.Ink, fontSize = 15.sp)
                                Text(c.label, color = Holo.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else if (wide) Row(Modifier.fillMaxSize()) {
                categories(Modifier.width(92.dp).fillMaxHeight())
                Spacer(Modifier.width(8.dp))
                preview(Modifier.width(if (previewInner) 300.dp else 170.dp).padding(top = 4.dp))
                Spacer(Modifier.width(16.dp))
                options(Modifier.weight(1f).fillMaxHeight())
            } else Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingsCategory.entries.forEach { c -> chip(c.label, c == category) { go(c, c.subs.first().first) } }
                }
                options(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

/** The options of one sub-section. */
@Composable
private fun SettingsOptions(
    category: SettingsCategory, sub: String, launcher: Launcher, isDefaultHome: Boolean, badgesAllowed: Boolean,
    onMakeDefault: () -> Unit, onNotificationAccess: () -> Unit, onLocation: () -> Unit, onWeatherChanged: () -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, accessibilityOn: Boolean, onAccessibility: () -> Unit, onPick: (String) -> Unit,
    onGuide: () -> Unit = {}, onOnboarding: () -> Unit = {}, onPickApp: ((AppInfo) -> Unit) -> Unit = {},
) {
    val s = launcher.settings
    val context = androidx.compose.ui.platform.LocalContext.current
    when (category to sub) {
        SettingsCategory.THEMES to "Fond" -> Section("Arrière-plan") {
            ToggleRow("Fond holographique", if (s.phoneWallpaper) "Ton fond d’écran du téléphone est utilisé" else "Noir profond avec halos de couleur",
                !s.phoneWallpaper) { v -> launcher.updateSettings { it.copy(phoneWallpaper = !v) } }
            ToggleRow("Halos animés", "Les couleurs bougent quand tu inclines le téléphone", s.parallax) { v -> launcher.updateSettings { it.copy(parallax = v) } }
            ActionRow("Fond d’écran animé du téléphone", "Les halos holographiques sur l’écran de verrouillage (et l’accueil si tu veux), dans ta palette", "Appliquer") {
                runCatching {
                    context.startActivity(android.content.Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                        .putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, android.content.ComponentName(context, HoloWallpaper::class.java))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { android.widget.Toast.makeText(context, "Choisis « Fold Launcher Pro » dans les fonds d’écran animés", android.widget.Toast.LENGTH_LONG).show() }
            }
            ToggleRow("Météo vivante", "Pluie, neige, orage ou brouillard dessinés légèrement sur le fond quand la météo l’annonce", s.liveWeather) { v ->
                launcher.updateSettings { it.copy(liveWeather = v) } }
            ActionRow("Changer le fond d’écran du téléphone", "Utilisé quand le fond holographique est coupé", "Ouvrir") {
                runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        SettingsCategory.THEMES to "Couleurs" -> {
            Section("Palette") {
                Text("Les dégradés, les halos et les reflets prennent ces couleurs.", color = Holo.Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Row(Modifier.padding(12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PALETTES.forEach { (key, name) ->
                        val on = s.palette == key
                        val sample = if (key == "wallpaper") remember { wallpaperPalette(context) } else null
                        val colors = when (key) { "wallpaper" -> sample?.let { listOf(it.a, it.b, it.c) } ?: listOf(Holo.Muted, Holo.Soft, Holo.Muted)
                            "aurore" -> listOf(Color(0xFFFFD3A5), Color(0xFFFF9EC7), Color(0xFFC9A7FF))
                            "ocean" -> listOf(Color(0xFF8FF0FF), Color(0xFF8FBEFF), Color(0xFF9CFFD9))
                            "braise" -> listOf(Color(0xFFFFC27A), Color(0xFFFF9A8A), Color(0xFFFF8FC0))
                            "menthe" -> listOf(Color(0xFFB5FFCB), Color(0xFF8CF5E6), Color(0xFFD9FF9E))
                            else -> listOf(Color(0xFF9BE7FF), Color(0xFFC7A6FF), Color(0xFFFFB3D9)) }
                        Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable { launcher.updateSettings { it.copy(palette = key) } }
                            .then(if (on) Modifier.glass(RoundedCornerShape(16.dp), .12f) else Modifier).padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(Brush.linearGradient(colors), CircleShape)
                                .border(if (on) 2.dp else 1.dp, if (on) Holo.Ink else Color.White.copy(alpha = .2f), CircleShape))
                            Text(name, color = if (on) Holo.Violet else Holo.Soft, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
            Section("Jour et nuit") {
                ToggleRow("Ambiance automatique", "Plus lumineux le jour, noir profond la nuit (selon le lever et le coucher du soleil)",
                    s.dayNight == "auto") { v -> launcher.updateSettings { it.copy(dayNight = if (v) "auto" else "off") } }
                ToggleRow("Halos vivants", "Les halos suivent l’heure (aube rose, midi bleu, crépuscule orangé) et pâlissent quand la batterie baisse",
                    s.livingHalos) { v -> launcher.updateSettings { it.copy(livingHalos = v) } }
            }
        }
        SettingsCategory.THEMES to "Icônes" -> {
            val packs = remember { IconPacks.installed(context) }
            Section("Pack d’icônes") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(if (packs.isEmpty()) "Installe un pack d’icônes depuis le Play Store (format Nova / ADW)" else "Les applis absentes du pack gardent leur icône",
                        color = Holo.Muted, fontSize = 12.sp)
                    Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (listOf(IconPackInfo("", "Aucun")) + packs).forEach { p ->
                            PillButton(p.label, highlighted = s.iconPack == p.packageName) { launcher.updateSettings { it.copy(iconPack = p.packageName) } }
                        }
                    }
                }
                ToggleRow("Icônes teintées", "Monochromes aux couleurs holographiques", s.tintedIcons) { v -> launcher.updateSettings { it.copy(tintedIcons = v) } }
                ToggleRow("Noms des applis", null, s.labels) { v -> launcher.updateSettings { it.copy(labels = v) } }
            }
        }
        SettingsCategory.THEMES to "Forme" -> Section("Forme des icônes") {
            ToggleRow("Cadre de verre", "Sinon, l’icône seule dans la forme choisie", s.bubbles) { v -> launcher.updateSettings { it.copy(bubbles = v) } }
            val sample = launcher.visibleApps.firstOrNull()
            Row(Modifier.padding(12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ICON_SHAPES.forEach { (key, name) ->
                    val on = s.iconShape == key
                    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable { launcher.updateSettings { it.copy(iconShape = key) } }
                        .then(if (on) Modifier.glass(RoundedCornerShape(16.dp), .12f) else Modifier).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.runtime.CompositionLocalProvider(LocalIconShape provides key) {
                            if (sample != null) AppIcon(sample, 44.dp, showBadge = false)
                        }
                        Text(name, color = if (on) Holo.Violet else Holo.Soft, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        SettingsCategory.THEMES to "Animations" -> Section("Ouverture des applis") {
            val choices = listOf("holo" to "Holographique", "system" to "Système")
            Text("Holographique : un anneau irisé jaillit de l’icône et l’appli grandit depuis sa bulle.", color = Holo.Muted, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { (key, name) -> PillButton(name, highlighted = s.launchAnimation == key) { launcher.updateSettings { it.copy(launchAnimation = key) } } }
            }
            Text("CHANGEMENT DE PAGE", color = Holo.Muted, fontSize = 11.sp, letterSpacing = 2.5.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PAGE_TRANSITIONS.forEach { (key, name) -> PillButton(name, highlighted = s.pageTransition == key) { launcher.updateSettings { it.copy(pageTransition = key) } } }
            }
        }
        SettingsCategory.HOME to "Grille" -> {
            Section("Écran externe") {
                ChoiceRow("Colonnes", null, listOf(0, 3, 4, 5, 6), s.coverColumns) { v -> launcher.updateSettings { it.copy(coverColumns = v) } }
                ChoiceRow("Lignes", null, listOf(0, 4, 5, 6, 7, 8), s.coverRows) { v -> launcher.updateSettings { it.copy(coverRows = v) } }
            }
            Section("Écran interne") {
                ChoiceRow("Colonnes", null, listOf(0, 3, 4, 5, 6, 7, 8), s.innerColumns) { v -> launcher.updateSettings { it.copy(innerColumns = v) } }
                ChoiceRow("Lignes", null, listOf(0, 4, 5, 6, 7, 8, 9), s.innerRows) { v -> launcher.updateSettings { it.copy(innerRows = v) } }
            }
        }
        SettingsCategory.HOME to "Dock" -> Section("Dock") {
            ChoiceRow("Écran externe", "Les mêmes en premier sur l’écran interne", (3..6).toList(), s.dockSize) { v ->
                launcher.updateSettings { it.copy(dockSize = v, innerDockSize = maxOf(v, it.innerDockSize)) } }
            ChoiceRow("Écran interne", "Jusqu’à 10 ; celles en plus n’apparaissent que dépliées", (s.dockSize..10).toList(), s.innerDockSize.coerceAtLeast(s.dockSize)) { v ->
                launcher.updateSettings { it.copy(innerDockSize = v) } }
            ChoiceRow("Applis récentes au bout du dock (interne)", null, listOf(0, 1, 2, 3), s.recentInDock) { v -> launcher.updateSettings { it.copy(recentInDock = v) } }
            ToggleRow("Dock qui se cache (interne)", "Il glisse vers le bas pendant que tu changes de page et revient dès que tu t’arrêtes", s.autoHideDock) { v ->
                launcher.updateSettings { it.copy(autoHideDock = v) } }
        }
        SettingsCategory.HOME to "Horloge" -> { Section("Style de l’horloge") {
            Row(Modifier.padding(16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CLOCK_STYLES.forEach { (key, name) -> PillButton(name, highlighted = s.clockStyle == key) { launcher.updateSettings { it.copy(clockStyle = key) } } }
            }
            Text(when (s.clockStyle) { "words" -> "Exemple : « ${timeInWords(java.time.LocalDateTime.now())} »"; "analog" -> "Un cadran holographique à aiguilles"
                "bold" -> "Chiffres épais"; else -> "Chiffres fins irisés" }, color = Holo.Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
        }
        Section("À droite de l’horloge") {
            val choices = listOf("google" to "Recherche Google", "finder" to "Finder Samsung", "pli" to "Recherche Fold Launcher Pro", "none" to "Rien")
            Row(Modifier.padding(16.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { (key, name) -> PillButton(name, highlighted = s.headerSearch == key) { launcher.updateSettings { it.copy(headerSearch = key) } } }
            }
        }
        }
        SettingsCategory.HOME to "Pages" -> Section("Pages d’accueil") {
            ToggleRow("Verrouiller la disposition", "Empêche de déplacer ou retirer des icônes par erreur", s.lockLayout) { v -> launcher.updateSettings { it.copy(lockLayout = v) } }
            ToggleRow("Page Actus", "Tout à gauche : Google Actualités, Le Monde, franceinfo", s.discover) { v -> launcher.updateSettings { it.copy(discover = v) } }
            ToggleRow("Ajouter les nouvelles applis", "Une appli installée apparaît sur les deux écrans", s.addNewApps) { v -> launcher.updateSettings { it.copy(addNewApps = v) } }
        }
        SettingsCategory.HOME to "Suggestions" -> {
            Section("Suggestions sous l’horloge") {
                Text("Une rangée d’applis qui change selon le moment : en voiture, casque branché, rendez-vous proche, avant de dormir…",
                    color = Holo.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("context" to "En situation", "off" to "Jamais").forEach { (key, name) ->
                        PillButton(name, highlighted = s.suggestions == key) { launcher.updateSettings { it.copy(suggestions = key) } }
                    }
                }
                Text("Rien ne s’affiche en dehors de ces situations. La voiture est reconnue par son Bluetooth (autorisation « Appareils à proximité »).",
                    color = Holo.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Section("Nouvelles applis") {
                ToggleRow("Pastille « Nouveau »", "Une perle verte sur les applis installées cette semaine, jusqu’à leur première ouverture", s.newBadge) { v ->
                    launcher.updateSettings { it.copy(newBadge = v) } }
            }
        }
        SettingsCategory.HOME to "Pliage" -> Section("Plier et déplier") {
            ToggleRow("Continuité", "En dépliant ou en repliant, l’autre écran s’ouvre sur la page qui contient les mêmes applis", s.continuity) { v ->
                launcher.updateSettings { it.copy(continuity = v) } }
            ToggleRow("Écran externe miroir", "Replié sur une page que l’écran externe n’a pas : ses applis s’affichent quand même, le temps d’en ouvrir une",
                s.coverMirror) { v -> launcher.updateSettings { it.copy(coverMirror = v) } }
            ToggleRow("Mode chevalet", "À moitié plié et tourné (comme un petit ordinateur) : horloge et musique en haut, commandes en bas",
                s.tabletop) { v -> launcher.updateSettings { it.copy(tabletop = v) } }
        }
        SettingsCategory.APPS to "Masquées" -> Section("Applis masquées") {
            val hidden = launcher.layout.hidden.toList()
            if (hidden.isEmpty()) Text("Aucune. Maintiens une appli puis « Masquer l’appli ».", color = Holo.Muted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            hidden.forEach { id ->
                val app = launcher.byId[id]
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (app != null) AppIcon(app, 36.dp, showBadge = false)
                    Text(app?.label ?: id.substringBefore('/'), color = Holo.Ink, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    PillButton("Afficher") { launcher.unhide(id) }
                }
            }
        }
        SettingsCategory.APPS to "Coffre" -> Section("Coffre secret") {
            Text("Les applis du coffre disparaissent de l’accueil, du dock, de la liste et de la recherche. On l’ouvre avec un geste choisi dans Gestes (« Coffre secret »), puis l’empreinte ou le code du téléphone.",
                color = Holo.Soft, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            Text("Pour y mettre une appli : appui long sur l’appli → « Mettre au coffre secret », ou « Ajouter » dans le coffre. Il contient ${launcher.vault.size} appli" + if (launcher.vault.size > 1) "s." else ".",
                color = Holo.Muted, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp))
        }
        SettingsCategory.APPS to "Ajout" -> Section("Nouvelles applis") {
            ToggleRow("Ajouter à l’accueil à l’installation", "Sur les deux écrans, dans la première case libre", s.addNewApps) { v -> launcher.updateSettings { it.copy(addNewApps = v) } }
            Text("Pour en ajouter plusieurs : appui long sur une case vide → « Ajouter des applis ».", color = Holo.Muted, fontSize = 12.sp,
                modifier = Modifier.padding(16.dp))
        }
        SettingsCategory.WIDGETS to "Météo" -> WeatherSettings(launcher, onLocation, onWeatherChanged)
        SettingsCategory.WIDGETS to "Notifications" -> Section("Notifications") {
            ActionRow("Pastilles, aperçus et musique en cours", if (badgesAllowed) "Accès aux notifications activé" else "Demande l’accès aux notifications",
                if (badgesAllowed) "Ouvrir" else "Activer", onNotificationAccess)
            ToggleRow("Notifications en orbite", "Les messages non lus tournent autour de l’icône en petites bulles ; plus il y en a, plus elles vont vite",
                s.orbitBadges) { v -> launcher.updateSettings { it.copy(orbitBadges = v) } }
        }
        SettingsCategory.GESTURES to "Dessins" -> Section("Dessiner pour lancer") {
            Column(Modifier.padding(10.dp)) { DrawingsSettings(launcher, onPickApp) }
        }
        SettingsCategory.GESTURES to "Gestes" -> Section("Gestes sur l’accueil") {
            ActionRow("Guide des gestes", "Tous les gestes de Fold Launcher Pro expliqués", "Voir", onGuide)
            ToggleRow("Glisser vers le bas en haut de l’écran", "Ouvre les notifications (ailleurs : la recherche)", s.swipeDownNotifications) { v ->
                launcher.updateSettings { it.copy(swipeDownNotifications = v) } }
            GestureRow("Double appui sur un espace vide", s.doubleTap, launcher, onPick = { onPick("doubleTap") }) { v -> launcher.updateSettings { it.copy(doubleTap = v) } }
            GestureRow("Glisser à deux doigts vers le haut", s.twoFingerUp, launcher, onPick = { onPick("twoFingerUp") }) { v -> launcher.updateSettings { it.copy(twoFingerUp = v) } }
            GestureRow("Glisser à deux doigts vers le bas", s.twoFingerDown, launcher, onPick = { onPick("twoFingerDown") }) { v -> launcher.updateSettings { it.copy(twoFingerDown = v) } }
            if (listOf(s.doubleTap, s.twoFingerUp, s.twoFingerDown).contains("lock") && !accessibilityOn)
                ActionRow("Verrouiller demande une autorisation", "Active « Fold Launcher Pro – verrouillage par geste » dans Accessibilité", "Activer", onAccessibility)
        }
        SettingsCategory.GENERAL to "Général" -> Section("Fold Launcher Pro") {
            ToggleRow("Vibrations", "Petit retour haptique sur les appuis longs et les déplacements", s.haptics) { v -> launcher.updateSettings { it.copy(haptics = v) } }
            ToggleRow("Économie de batterie", "Sous 20 % (hors charge) : halos figés et ouverture des applis simple", s.batterySaver) { v ->
                launcher.updateSettings { it.copy(batterySaver = v) } }
            ActionRow("Guide des gestes", "Tous les gestes de Fold Launcher Pro expliqués", "Voir", onGuide)
            ActionRow("Revoir la présentation", "Les étapes du premier lancement", "Ouvrir", onOnboarding)
            Text("Fold Launcher Pro · launcher holographique pour Galaxy Z Fold", color = Holo.Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        SettingsCategory.GENERAL to "Sauvegarde" -> Section("Sauvegarde") {
            ActionRow("Exporter ma disposition", "Pages, dossiers, dock, widgets Fold Launcher Pro et réglages dans un fichier", "Exporter", onExport)
            ActionRow("Importer une disposition", "Remplace la disposition actuelle", "Importer", onImport)
        }
        SettingsCategory.GENERAL to "Mises à jour" -> UpdateSettings()
        else -> Unit
    }
}

@Composable
private fun UpdateSettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var auto by remember { mutableStateOf(Updater.automatic(context)) }
    Section("Mises à jour") {
        Text("Version installée : build ${Updater.currentBuild(context)}", color = Holo.Soft, fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        ToggleRow("Vérifier automatiquement", "Une fenêtre (ou une notification) s’affiche quand une nouvelle version sort", auto) { v -> auto = v; Updater.setAutomatic(context, v) }
        if (auto) {
            var every by remember { mutableStateOf(Updater.everyHours(context)) }
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text("Fréquence", color = Holo.Ink, fontSize = 15.sp)
                Text("Moins souvent = moins de batterie ; « Vérifier » ci-dessous marche à tout moment", color = Holo.Muted, fontSize = 12.sp)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Updater.FREQUENCIES.forEach { h ->
                        PillButton(if (h == 24) "Une fois par jour" else "Toutes les $h h", highlighted = every == h) { every = h; Updater.setEveryHours(context, h) }
                    }
                }
            }
        }
        val found = Updater.available
        if (found != null) ActionRow("Build ${found.build} disponible", "Voir les nouveautés et installer", "Installer") { Updater.open() }
        else ActionRow("Rechercher une mise à jour", if (Updater.checking) "Recherche…" else Updater.message, "Vérifier") {
            scope.launch { Updater.check(context, manual = true) }
        }
        ActionRow("Journal des versions", "Tout ce qui a changé, build par build, sur GitHub", "Ouvrir") {
            runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/yuksel3803/DuoLauncherv2/blob/pli/CHANGELOG.md")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}

@Composable
internal fun WeatherSettings(launcher: Launcher, onLocation: () -> Unit, onWeatherChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var city by remember { mutableStateOf(launcher.settings.city) }
    var cityStatus by remember { mutableStateOf<String?>(null) }
    fun validate() = scope.launch {
        cityStatus = "Recherche…"
        val found = findCity(city)
        if (found == null) cityStatus = "Ville introuvable ou pas de connexion"
        else {
            launcher.updateSettings { it.copy(city = found.first, latitude = found.second, longitude = found.third) }
            city = found.first; cityStatus = "Météo de ${found.first}"; onWeatherChanged()
        }
    }
    Section("Ville de la météo") {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassField(city, { city = it }, "Ville (ex. Paris)", null, Modifier.fillMaxWidth(), onDone = { validate() })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton("Valider la ville", true) { validate() }
                PillButton("Ma position") {
                    launcher.updateSettings { it.copy(city = "", latitude = null, longitude = null) }
                    city = ""; cityStatus = "Position du téléphone utilisée"; onLocation()
                }
            }
            cityStatus?.let { Text(it, color = Holo.Muted, fontSize = 12.sp) }
        }
    }
}

private val GESTURE_ACTIONS = listOf("none" to "Rien", "lock" to "Verrouiller", "notifications" to "Notifications",
    "search" to "Recherche", "drawer" to "Toutes les applis", "draw" to "Dessiner pour lancer", "vault" to "Coffre secret")

@Composable
private fun GestureRow(title: String, value: String, launcher: Launcher, onPick: () -> Unit, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(title, color = Holo.Ink, fontSize = 15.sp)
        Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val app = if (value.startsWith("app:")) launcher.byId[value.removePrefix("app:")] else null
            (GESTURE_ACTIONS + ("app" to (app?.label ?: "Ouvrir une appli…"))).forEach { (key, label) ->
                val on = value == key || (key == "app" && value.startsWith("app:"))
                Box(Modifier.height(38.dp).clip(RoundedCornerShape(50))
                    .then(if (on) Modifier.background(Brush.linearGradient(Holo.Iridescent), RoundedCornerShape(50)) else Modifier.glass(RoundedCornerShape(50)))
                    .clickable { if (key == "app") onPick() else onSelect(key) }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center) {
                    Text(label, color = if (on) Color(0xFF120A22) else Holo.Soft, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun AppPick(launcher: Launcher, modifier: Modifier, onClose: () -> Unit, onPick: (AppInfo) -> Unit) {
    var filter by remember { mutableStateOf("") }
    val apps = launcher.visibleApps
    val shown = remember(filter, apps) { if (filter.isBlank()) apps else matchApps(apps, filter) }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IridescentText("Choisir une appli", 26.sp, Modifier.weight(1f), letterSpacing = 0.sp)
            PillButton("Retour", onClick = onClose)
        }
        GlassField(filter, { filter = it }, "Filtrer", null, Modifier.fillMaxWidth())
        Column(Modifier.padding(top = 10.dp).verticalScroll(rememberScrollState())) {
            shown.forEach { app ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(18.dp)).clickable { onPick(app) }
                    .padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app, 40.dp, showBadge = false)
                    Text(app.label, color = Holo.Ink, fontSize = 15.sp, modifier = Modifier.padding(start = 14.dp))
                }
            }
        }
    }
}
