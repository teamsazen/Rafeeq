package com.satechdev.rafeeqapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.style

/*
 * Debug-only tools (lib/ui/developer). Flutter's update dialog test comes
 * with in-app updates (phase 5); its event previews went with the event cards.
 */

private val danger = Color(0xFFFF5252)

@Composable
fun DeveloperPanelScreen() {
    val nav = LocalNavigator.current
    PushedScreen("Developer Panel") {
        item {
            SettingsCard(Glyph.Code, "Shared Preferences", "View and manage local keys") { nav.push(Route.DeveloperPrefs) }
        }
    }
}

/** Every stored key and value, with delete and clear all (developer_shared_prefs_screen.dart). */
@Composable
fun DeveloperPrefsScreen() {
    val p = palette
    var version by remember { mutableIntStateOf(0) }
    val entries = remember(version) { Settings.everything().entries.toList() }
    var confirm by remember { mutableStateOf<String?>(null) }
    val all = "\u0000all"
    PushedScreen("Shared Preferences") {
        item(key = "clear") {
            Row(
                Modifier.fillMaxWidth().pressable({ confirm = all }).clip(RoundedCornerShape(12.dp)).background(danger.copy(alpha = .12f)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("Clear all (${entries.size})", style(15.sp, danger, FontWeight.SemiBold, family = AppFonts.sans), Modifier.weight(1f))
            }
        }
        items(entries, key = { it.key }) { (key, value) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card).padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)) {
                Column(Modifier.weight(1f)) {
                    Txt(key, style(14.sp, p.title, FontWeight.SemiBold, family = AppFonts.sans).copy(textDirection = TextDirection.Ltr))
                    Spacer(Modifier.height(4.dp))
                    Txt(value.toString(), style(12.sp, p.subtitle, family = AppFonts.sans).copy(textDirection = TextDirection.Ltr), maxLines = 6)
                    Txt(value?.javaClass?.simpleName ?: "null", style(11.sp, p.accent, family = AppFonts.sans))
                }
                Box(Modifier.size(44.dp).pressable({ confirm = key }, scale = .85f), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.Close, danger, 20.dp)
                }
            }
        }
    }
    confirm?.let { target ->
        val everything = target == all
        InfoDialog(if (everything) "Clear All Preferences" else "Delete Preference", { confirm = null }) {
            Txt(
                if (everything) "Are you sure you want to delete all SharedPreferences? This will reset the app state and cannot be undone."
                else "Are you sure you want to delete '$target'?",
                style(14.sp, p.title, lineHeight = 1.5f, align = TextAlign.Center, family = AppFonts.sans),
                Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.weight(1f).pressable({ confirm = null }).clip(RoundedCornerShape(14.dp))
                        .border(1.dp, p.title.copy(alpha = .45f), RoundedCornerShape(14.dp)).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt("Cancel", style(15.sp, p.title, FontWeight.SemiBold, family = AppFonts.sans)) }
                Box(
                    Modifier.weight(1f).pressable({
                        if (everything) Settings.clearAll() else Settings.remove(target)
                        confirm = null
                        version++
                    }).clip(RoundedCornerShape(14.dp)).background(danger).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt(if (everything) "Delete All" else "Delete", style(15.sp, Color.White, FontWeight.Bold, family = AppFonts.sans)) }
            }
        }
    }
}
