package com.satechdev.rafeeqapp.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.LocalThemeReveal
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.AppLanguage
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.ThemeMode
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.style

/** Light, system or dark. Choosing one grows the new theme out from the tap. */
@Composable
fun ThemeScreen() {
    val switchTheme = LocalThemeReveal.current
    PushedScreen(tr(R.string.theme)) {
        // The Flutter app's order.
        listOf(ThemeMode.Light, ThemeMode.System, ThemeMode.Dark).forEach { mode ->
            item(key = mode.stored) {
                PickerTile(themeLabel(mode), selected = Settings.themeMode == mode) { switchTheme(mode) }
            }
        }
    }
}

@Composable
fun LanguageScreen() {
    PushedScreen(tr(R.string.language)) {
        AppLanguage.entries.forEach { language ->
            item(key = language.code) {
                PickerTile(languageLabel(language), selected = Settings.language == language) {
                    Settings.setLanguage(language)
                }
            }
        }
    }
}

/** Stands in for a screen that is still Flutter only, until its phase. */
@Composable
fun PlaceholderScreen(@StringRes title: Int) {
    val nav = LocalNavigator.current
    Column(Modifier.fillMaxSize().background(palette.background)) {
        AppBar(tr(title), onBack = nav::pop)
        ComingSoon()
    }
}

@Composable
fun ComingSoon(modifier: Modifier = Modifier) {
    val p = palette
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Txt(tr(R.string.nativeComingTitle), style(18.sp, p.title, FontWeight.SemiBold, align = TextAlign.Center))
            Spacer(Modifier.height(8.dp))
            Txt(tr(R.string.nativeComingBody), style(14.sp, p.subtitle, lineHeight = 1.4f, align = TextAlign.Center))
        }
    }
}
