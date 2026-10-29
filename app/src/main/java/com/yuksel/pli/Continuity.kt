package com.yuksel.pli

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** What was on screen just before folding or unfolding. */
class Continuity {
    var screen: Screen? = null
    var news = false
    /** True when the first home page was shown (the usual resting page: folding there shows no mirror). */
    var firstPage = true
    /** Apps and folders of the page that was shown (folder contents included). */
    var ids: List<String> = emptyList()
    var stamp = 0L

    data class Snapshot(val screen: Screen?, val news: Boolean, val firstPage: Boolean, val ids: List<String>, val stamp: Long)
    fun snapshot() = Snapshot(screen, news, firstPage, ids, stamp)
}

private fun pageIds(launcher: Launcher, items: List<String>, page: Int, capacity: Int): List<String> {
    val out = ArrayList<String>()
    for (i in page * capacity until minOf(items.size, (page + 1) * capacity)) {
        val id = items[i]
        if (id.isEmpty() || id == HomeLayout.COVERED) continue
        out += id
        if (HomeLayout.isFolder(id)) launcher.layout.folders[id]?.apps?.let(out::addAll)
    }
    return out
}

/**
 * Records the page shown on this screen, and when this screen has just appeared after a fold or unfold,
 * turns to the page sharing the most apps with the one left on the other screen. On the cover, when no page
 * matches, [onMirror] receives the apps left on the inner screen so they can be shown anyway.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinuityEffect(screen: Screen, launcher: Launcher, continuity: Continuity, pager: PagerState, lead: Int,
    items: List<String>, capacity: Int, pages: Int, onMirror: (List<String>) -> Unit) {
    val previous = remember { continuity.snapshot() }
    LaunchedEffect(Unit) {
        val s = launcher.settings
        if (!s.continuity || previous.screen == null || previous.screen == screen) return@LaunchedEffect
        if (System.currentTimeMillis() - previous.stamp > 10 * 60_000L) return@LaunchedEffect
        if (previous.news) { if (lead > 0) pager.scrollToPage(0); return@LaunchedEffect }
        val wanted = previous.ids.toSet()
        if (wanted.isEmpty()) return@LaunchedEffect
        val scores = (0 until pages).map { p -> pageIds(launcher, items, p, capacity).count { it in wanted } }
        val best = scores.indices.maxByOrNull { scores[it] } ?: 0
        val score = scores.getOrElse(best) { 0 }
        val apps = previous.ids.filterNot { HomeLayout.isFolder(it) }
        if (screen == Screen.COVER && s.coverMirror && !previous.firstPage && score * 2 < apps.size.coerceAtLeast(1)) {
            onMirror(apps)
            return@LaunchedEffect
        }
        if (score == 0) return@LaunchedEffect
        val target = lead + best
        withTimeoutOrNull(1500) { snapshotFlow { pager.pageCount }.first { it > target } }
        if (target < pager.pageCount && target != pager.currentPage) pager.scrollToPage(target)
    }
    LaunchedEffect(pager, items, capacity) {
        snapshotFlow { pager.currentPage }.collect { current ->
            continuity.screen = screen
            continuity.news = current < lead
            continuity.firstPage = current <= lead
            continuity.ids = if (current < lead) emptyList() else pageIds(launcher, items, current - lead, capacity)
            continuity.stamp = System.currentTimeMillis()
        }
    }
}

/** On the cover after folding: the apps that were on the inner screen's page, until one is opened or the sheet is closed. */
@Composable
fun MirrorSheet(apps: List<AppInfo>, onOpen: (AppInfo, Rect) -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f))
        .clickable(remember { MutableInteractionSource() }, null, onClick = onClose), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.windowInsetsPadding(WindowInsets.systemBars).padding(12.dp).fillMaxWidth()
            .halo(Holo.Violet.copy(alpha = .16f), 20.dp).background(Holo.Panel, RoundedCornerShape(30.dp)).glass(RoundedCornerShape(30.dp), .05f)
            .clickable(remember { MutableInteractionSource() }, null) {}.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("DEPUIS L’ÉCRAN INTERNE", color = Holo.Muted, fontSize = 11.sp, letterSpacing = 2.5.sp)
                    IridescentText("Tu étais ici", 26.sp, letterSpacing = (-.5).sp)
                }
                PillButton("Fermer", onClick = onClose)
            }
            Spacer(Modifier.padding(top = 10.dp))
            apps.take(12).chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { app -> Box(Modifier.weight(1f)) { AppChoice(app, 52.dp, { a, r -> onOpen(a, r) }, { _, _ -> }) } }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
