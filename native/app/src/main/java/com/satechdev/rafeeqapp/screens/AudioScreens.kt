package com.satechdev.rafeeqapp.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.audio.PlaybackScope
import com.satechdev.rafeeqapp.data.audio.QuranAudio
import com.satechdev.rafeeqapp.data.audio.Reciter
import com.satechdev.rafeeqapp.data.quran.Editions
import com.satechdev.rafeeqapp.data.quran.SurahNames
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.QuranArabic
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style

/** The ayah being recited, large, with its translation (ayah_focus_screen.dart). */
@Composable
fun AyahFocusScreen() {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val p = palette
    val state = QuranAudio.state
    val view = Settings.quranView
    val urdu = Settings.language.rtl
    val ayah = state.currentAyah
    val juz = state.scopeNumber.takeIf { state.scope == PlaybackScope.Juz }
    val title = when {
        juz != null -> playingTitle(state)
        state.surahNumber == null || state.surahName.isEmpty() -> tr(R.string.quran)
        else -> "${localNumber(state.surahNumber)}. ${if (urdu) SurahNames.urdu[state.surahNumber] else state.surahName}"
    }
    val subtitle = when {
        ayah == null || state.ayahs.isEmpty() -> tr(R.string.noAyahPlaying)
        juz != null -> "${localNumber(state.currentIndex + 1)}/${localNumber(state.ayahs.size)}"
        else -> "${localNumber(ayah.numberInSurah)}/${localNumber(state.ayahs.size)}"
    }

    Column(Modifier.fillMaxSize().background(p.card)) {
        Column(Modifier.fillMaxWidth().background(p.background).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                val mirror = if (urdu) -1f else 1f
                Box(Modifier.size(56.dp).pressable(nav::pop, scale = .9f), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.ArrowBack, p.title, 24.dp, Modifier.graphicsLayer { scaleX = mirror })
                }
                val pick = state.scopeNumber?.let { { nav.push(Route.QuranSectionPicker(juz = juz != null, audio = true)) } }
                Column(Modifier.weight(1f).pressable(pick).padding(vertical = 4.dp)) {
                    Txt(title, style(17.sp, p.title, FontWeight.Bold), maxLines = 1)
                    Txt(subtitle, style(12.sp, p.subtitle), maxLines = 1)
                }
                Box(Modifier.size(48.dp).pressable(if (state.ayahs.isEmpty()) null else ({ nav.push(Route.QuranAyahPicker(audio = true)) }), scale = .9f), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.NumberedList, p.title, 24.dp)
                }
                Box(Modifier.size(48.dp).pressable({ nav.push(Route.QuranSettings) }, scale = .9f), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.Tune, p.title, 24.dp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            if (ayah == null) {
                Txt(tr(R.string.noQuranAudioPlaying), style(15.sp, p.subtitle), Modifier.align(Alignment.Center))
            } else {
                val edition = view.translationEditionId
                val key = "${ayah.surahNumber}/$edition"
                if (view.showTranslation) {
                    LaunchedEffect(key) {
                        if (TranslationCache.texts[key] == null) {
                            runCatching { Editions.surah(context, ayah.surahNumber, edition) }
                                .onSuccess { TranslationCache.texts[key] = it }
                                .onFailure { TranslationCache.failed[key] = true }
                        }
                    }
                }
                val still = reducedMotion()
                // Each ayah rises in as the recitation reaches it.
                AnimatedContent(
                    ayah,
                    transitionSpec = {
                        if (still) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (slideInVertically(tween(320)) { it / 8 } + fadeIn(tween(320))) togetherWith (slideOutVertically(tween(200)) { -it / 8 } + fadeOut(tween(200)))
                    },
                    contentKey = { it.number },
                    label = "focus",
                ) { shown ->
                    Column(
                        Modifier.widthIn(max = 760.dp).fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                    ) {
                        QuranArabic(shown, (view.arabicFontSize + 8).sp, p.title, Modifier.fillMaxWidth())
                        if (view.showTranslation) {
                            TranslationCache.texts["${shown.surahNumber}/$edition"]?.get(shown.number)?.let { value ->
                                val language = edition.substringBefore('.').lowercase()
                                val rtl = language in rtlLanguages
                                Spacer(Modifier.height(12.dp))
                                Txt(
                                    value,
                                    style(
                                        (view.translationFontSize + 4).sp, p.subtitle,
                                        family = if (language == "ur") AppFonts.urdu else AppFonts.sans,
                                        lineHeight = 1.55f,
                                        align = if (rtl) TextAlign.Right else TextAlign.Left,
                                    ).copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
                                    Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.background(p.card).navigationBarsPadding()) { MiniPlayerSlot(PlayerPlace.AyahFocus) }
    }
}

/** The ayah reciters; choosing one carries on from the same ayah (choose_recitor_screen.dart). */
@Composable
fun ChooseReciterScreen() {
    val context = LocalContext.current
    val urdu = Settings.language.rtl
    val selected = QuranAudio.selectedReciter
    PushedScreen(tr(R.string.chooseReciter)) {
        items(Reciter.entries.size) { i ->
            val reciter = Reciter.entries[i]
            PickerTile(if (urdu) reciter.nativeName else reciter.englishName, selected = reciter == selected) {
                playAudio(context) { QuranAudio.chooseReciter(context, reciter) }
            }
        }
    }
}
