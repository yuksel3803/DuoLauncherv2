package com.yuksel.pli

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The secret vault, opened after the fingerprint: its apps, adding more, taking one out (long press). */
@Composable
fun VaultPanel(launcher: Launcher, modifier: Modifier, onOpen: (AppInfo, Rect) -> Unit, onClose: () -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<AppInfo?>(null) }
    if (adding) {
        AppPick(launcher, modifier, onClose = { adding = false }) { app -> launcher.toVault(app); adding = false }
        return
    }
    val apps = launcher.vault.mapNotNull(launcher.byId::get).sortedBy { it.label.lowercase() }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                IridescentText("Coffre secret", 30.sp, letterSpacing = (-.5).sp)
                Text("Visible seulement ici · appui long pour sortir une appli", color = Holo.Muted, fontSize = 12.sp)
            }
            PillButton("Ajouter", highlighted = true) { adding = true }
            Spacer(Modifier.padding(start = 8.dp))
            PillButton("Fermer", onClick = onClose)
        }
        if (apps.isEmpty()) Text("Le coffre est vide. « Ajouter », ou appui long sur une appli de l’accueil → « Mettre au coffre secret ».",
            color = Holo.Soft, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
        LazyVerticalGrid(GridCells.Adaptive(86.dp), Modifier.weight(1f).fillMaxWidth()) {
            items(apps, key = { it.id }) { app -> AppChoice(app, 56.dp, { a, r -> onOpen(a, r); onClose() }, { a, _ -> selected = a }) }
        }
        selected?.let { app ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(64.dp).glass(RoundedCornerShape(32.dp), .07f).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppIcon(app, 36.dp, showBadge = false)
                Text(app.label, color = Holo.Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                PillButton("Sortir du coffre", highlighted = true) { launcher.fromVault(app.id); selected = null }
                PillButton("Annuler") { selected = null }
            }
        }
        Box(Modifier.height(4.dp))
    }
}
