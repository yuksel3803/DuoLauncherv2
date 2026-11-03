package com.yuksel.pli

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Gérer les pages": every home page of a screen in miniature. Move a page left or right, delete it (even with icons:
 * they go back to the app list), add one. Changes can be taken back with "Annuler".
 */
@Composable
fun PagesManager(launcher: Launcher, startScreen: Screen, modifier: Modifier, onClose: () -> Unit) {
    var screen by remember { mutableStateOf(startScreen) }
    var confirm by remember { mutableStateOf<Int?>(null) }
    val layout = launcher.layout
    val grid = layout.grid(screen) ?: Grid(4, 5)
    val capacity = grid.capacity
    val pages = LayoutEdits.pageCount(layout, screen, capacity)
    val slots = launcher.visibleHome(screen)
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IridescentText("Pages", 30.sp, Modifier.weight(1f), letterSpacing = (-.5).sp)
            PillButton("Fermer", onClick = onClose)
        }
        Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton("Écran externe", highlighted = screen == Screen.COVER) { screen = Screen.COVER; confirm = null }
            PillButton("Écran interne", highlighted = screen == Screen.INNER) { screen = Screen.INNER; confirm = null }
            Text("$pages page" + if (pages > 1) "s" else "", color = Holo.Muted, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp))
        }
        LazyVerticalGrid(GridCells.Adaptive(if (screen == Screen.INNER) 220.dp else 150.dp), Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items((0 until pages).toList(), key = { "${screen.name}-$it" }) { page ->
                val pageSlots = (page * capacity until (page + 1) * capacity).map { slots.getOrElse(it) { "" } }
                val count = pageSlots.count { it.isNotEmpty() && it != HomeLayout.COVERED }
                Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp), .05f).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Page ${page + 1}", color = Holo.Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(if (count == 0) "vide" else "$count", color = Holo.Muted, fontSize = 12.sp)
                    }
                    PageThumbnail(launcher, screen, pageSlots, grid)
                    if (confirm == page) {
                        Text(if (count == 0) "Supprimer cette page ?" else "Supprimer la page et retirer ses $count icône" + (if (count > 1) "s" else "") +
                            " de l’accueil ? Les applis restent dans la liste.", color = Holo.Soft, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PillButton("Supprimer", highlighted = true) {
                                launcher.edit("Page ${page + 1} supprimée") { LayoutEdits.removePage(it, screen, page, capacity) }; confirm = null
                            }
                            PillButton("Annuler") { confirm = null }
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PageAction(Icons.Rounded.ChevronLeft, "Vers la gauche", page > 0) {
                            launcher.edit("Page déplacée") { LayoutEdits.movePage(it, screen, page, page - 1, capacity) }
                        }
                        PageAction(Icons.Rounded.ChevronRight, "Vers la droite", page < pages - 1) {
                            launcher.edit("Page déplacée") { LayoutEdits.movePage(it, screen, page, page + 1, capacity) }
                        }
                        Spacer(Modifier.weight(1f))
                        PageAction(Icons.Rounded.Delete, "Supprimer la page", pages > 1, danger = true) {
                            if (count == 0) launcher.edit("Page supprimée") { LayoutEdits.removePage(it, screen, page, capacity) } else confirm = page
                        }
                    }
                }
            }
            item(key = "add-${screen.name}") {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).glass(RoundedCornerShape(24.dp), .03f)
                    .clickable { launcher.update { LayoutEdits.withPages(it, screen, pages + 1) } }.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nouvelle page", color = Holo.Ink, fontSize = 15.sp)
                    Box(Modifier.fillMaxWidth().aspectRatio(if (screen == Screen.INNER) .9f else ScreenShapes.cover * 1.15f), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(56.dp).background(Brush.linearGradient(Holo.Iridescent), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, null, tint = Color(0xFF120A22), modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

/** A page in miniature: the icons in their real cells. */
@Composable
private fun PageThumbnail(launcher: Launcher, screen: Screen, slots: List<String>, grid: Grid) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(if (screen == Screen.INNER) .9f else ScreenShapes.cover * 1.15f)
        .clip(RoundedCornerShape(16.dp)).background(Holo.Base.copy(alpha = .6f)).padding(6.dp)) {
        val cw = maxWidth / grid.columns
        val ch = maxHeight / grid.rows
        val icon = minOf(cw, ch) * .7f
        slots.forEachIndexed { i, id ->
            if (id.isEmpty() || id == HomeLayout.COVERED) return@forEachIndexed
            val app = if (HomeLayout.isFolder(id)) launcher.folderApps(id).firstOrNull() else launcher.byId[id]
            app ?: return@forEachIndexed
            Box(Modifier.offset(x = cw * (i % grid.columns), y = ch * (i / grid.columns)).size(cw, ch), contentAlignment = Alignment.Center) {
                AppIcon(app, icon, showBadge = false)
            }
        }
        if (slots.none { it.isNotEmpty() && it != HomeLayout.COVERED }) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Page vide", color = Holo.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PageAction(icon: ImageVector, label: String, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).glass(CircleShape, .08f).clickable(enabled = enabled, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = when { !enabled -> Holo.Muted.copy(alpha = .3f); danger -> Holo.Pink; else -> Holo.Ink }, modifier = Modifier.size(20.dp))
    }
}
