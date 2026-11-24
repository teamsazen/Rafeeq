package com.satechdev.rafeeqapp.screens

import com.satechdev.rafeeqapp.data.audio.QuranAudio
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.LocalStrings
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.Toast
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.quran.Edition
import com.satechdev.rafeeqapp.data.quran.Editions
import com.satechdev.rafeeqapp.data.quran.QuranStore
import com.satechdev.rafeeqapp.data.quran.QuranViewSettings
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.AppSlider
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.QuranArabic
import com.satechdev.rafeeqapp.ui.style

private val redAccent = Color(0xFFFF5252)

/** Editions from alquran.cloud, loaded once per run (Flutter's editionsProvider). */
private object EditionList {
    var editions by mutableStateOf<List<Edition>?>(null)
    var failed by mutableStateOf(false)
}

@Composable
private fun rememberEditions(): List<Edition>? {
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        if (EditionList.editions == null) {
            EditionList.failed = false
            runCatching { Editions.list() }.onSuccess { EditionList.editions = it }.onFailure { EditionList.failed = true }
        }
    }
    return EditionList.editions
}

/** Font sizes, translation and reset, as lib/ui/quran/quran_view_settings.dart. */
@Composable
fun QuranSettingsScreen() {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current
    val settings = Settings.quranView
    val editions = rememberEditions()
    val urdu = Settings.language.rtl
    var askReset by remember { mutableStateOf(false) }
    val editionName = when {
        editions != null -> editions.firstOrNull { it.identifier == settings.translationEditionId }?.let { if (urdu) it.name else it.englishName } ?: settings.translationEditionId
        EditionList.failed -> settings.translationEditionId
        else -> "Loading..."
    }
    fun change(update: QuranViewSettings.() -> QuranViewSettings) = Settings.setQuranView(Settings.quranView.update())

    PushedScreen(tr(R.string.quranViewSettings)) {
        item(key = "arabic") {
            SettingsCard(Glyph.FormatSize, tr(R.string.arabicFontSize), settings.arabicFontSize.toInt().toString(), supporting = {
                AyahPreview(showArabic = true)
                Spacer(Modifier.height(8.dp))
                AppSlider(settings.arabicFontSize, 20f, 40f, { v -> change { copy(arabicFontSize = v) } })
            })
        }
        item(key = "show-translation") {
            SettingsCard(Glyph.TranslateRounded, tr(R.string.showTranslation), tr(R.string.toggleTranslationVisibility), switch = settings.showTranslation) {
                change { copy(showTranslation = !showTranslation) }
            }
        }
        if (settings.showTranslation) {
            item(key = "translation-size") {
                SettingsCard(Glyph.TextFields, tr(R.string.translationFontSize), settings.translationFontSize.toInt().toString(), supporting = {
                    AyahPreview(showArabic = false)
                    Spacer(Modifier.height(8.dp))
                    AppSlider(settings.translationFontSize, 12f, 36f, { v -> change { copy(translationFontSize = v) } })
                })
            }
            item(key = "edition") {
                SettingsCard(Glyph.MenuBook, tr(R.string.translationEdition), editionName) { nav.push(Route.TranslationPicker()) }
            }
        }
        item(key = "reciter") {
            val reciter = QuranAudio.selectedReciter
            SettingsCard(Glyph.RecordVoice, tr(R.string.changeReciter), if (Settings.language.rtl) reciter.nativeName else reciter.englishName) { nav.push(Route.ChooseReciter) }
        }
        item(key = "reset") {
            SettingsCard(Glyph.RestartAlt, tr(R.string.resetToDefault), tr(R.string.resetToDefaultSubtitle), foreground = redAccent) { askReset = true }
        }
    }
    if (askReset) {
        val p = palette
        InfoDialog(tr(R.string.resetSettingsTitle), { askReset = false }) {
            Txt(tr(R.string.resetSettingsMessage), style(14.sp, p.title, lineHeight = 1.5f, align = TextAlign.Center), Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.weight(1f).pressable({ askReset = false }).clip(RoundedCornerShape(14.dp))
                        .border(1.dp, p.title.copy(alpha = .45f), RoundedCornerShape(14.dp)).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt(tr(R.string.cancel), style(15.sp, p.title, FontWeight.SemiBold)) }
                Box(
                    Modifier.weight(1f).pressable({
                        Settings.setQuranView(QuranViewSettings())
                        askReset = false
                        Toast.show(strings.getString(R.string.settingsResetSuccess))
                    }).clip(RoundedCornerShape(14.dp)).background(redAccent).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Txt(tr(R.string.reset), style(15.sp, Color.White, FontWeight.Bold)) }
            }
        }
    }
}

/** Al-Fatiha's first ayah at the chosen size, in Arabic or the chosen translation. */
@Composable
private fun AyahPreview(showArabic: Boolean) {
    val p = palette
    val context = LocalContext.current
    val settings = Settings.quranView
    var quran by remember { mutableStateOf(QuranStore.cached) }
    LaunchedEffect(Unit) { if (quran == null) quran = QuranStore.get(context) }
    var translation by remember(settings.translationEditionId) { mutableStateOf<String?>(null) }
    if (!showArabic) {
        LaunchedEffect(settings.translationEditionId) {
            translation = runCatching { Editions.surah(context, 1, settings.translationEditionId)[1] }.getOrNull()
        }
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val ayah = quran?.ayahs?.firstOrNull()
        when {
            ayah == null -> Box(Modifier.fillMaxWidth().height(86.dp), contentAlignment = Alignment.Center) { Spinner(p.accent) }
            showArabic -> QuranArabic(ayah, settings.arabicFontSize.sp, p.title, Modifier.fillMaxWidth(), maxLines = 2)
            translation == null -> Txt("Loading translation…", style(12.sp, p.subtitle, family = AppFonts.sans))
            else -> {
                val urduEdition = settings.translationEditionId.substringBefore('.').lowercase() == "ur"
                Txt(
                    translation!!,
                    style(
                        settings.translationFontSize.sp, p.subtitle,
                        family = if (urduEdition) AppFonts.urdu else AppFonts.sans,
                        lineHeight = 1.35f,
                        align = if (urduEdition) TextAlign.Right else TextAlign.Left,
                    ).copy(textDirection = if (urduEdition) TextDirection.Rtl else TextDirection.Ltr),
                    Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        }
    }
}

private val languageNames = mapOf(
    "en" to "English", "ar" to "Arabic", "ur" to "Urdu", "id" to "Indonesian", "tr" to "Turkish", "fr" to "French",
    "de" to "German", "es" to "Spanish", "it" to "Italian", "pt" to "Portuguese", "ru" to "Russian", "zh" to "Chinese",
    "ja" to "Japanese", "ko" to "Korean", "hi" to "Hindi", "bn" to "Bengali", "ms" to "Malay", "fa" to "Persian",
    "uz" to "Uzbek", "sq" to "Albanian", "az" to "Azerbaijani", "bs" to "Bosnian", "dv" to "Divehi", "ha" to "Hausa",
    "kk" to "Kazakh", "ku" to "Kurdish", "ml" to "Malayalam", "nl" to "Dutch", "pl" to "Polish", "ro" to "Romanian",
    "sd" to "Sindhi", "so" to "Somali", "sw" to "Swahili", "ta" to "Tamil", "tg" to "Tajik", "th" to "Thai",
    "tt" to "Tatar", "ug" to "Uyghur", "ps" to "Pashto",
)

private fun languageName(code: String) = languageNames[code.lowercase()] ?: code.uppercase()

/** Translations grouped by language, English and Urdu first (TranslationSelectionPage). */
@Composable
fun TranslationPickerScreen(forPicture: Boolean = false) {
    val nav = LocalNavigator.current
    val p = palette
    val editions = rememberEditions()
    val search = rememberTextFieldState()
    val urdu = Settings.language.rtl
    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(tr(R.string.selectTranslation), onBack = nav::pop)
        SearchField(search, tr(R.string.searchTranslations))
        when {
            editions == null && EditionList.failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Txt(tr(R.string.noResultsFound), style(14.sp, p.subtitle, family = AppFonts.sans))
            }
            editions == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) }
            else -> {
                val q = search.text.toString().lowercase()
                val groups = editions
                    .filter { q.isEmpty() || it.name.lowercase().contains(q) || it.englishName.lowercase().contains(q) || it.identifier.lowercase().contains(q) || languageName(it.language).lowercase().contains(q) }
                    .groupBy { it.language.lowercase() }
                val languages = groups.keys.sortedWith(compareBy({ mapOf("en" to 0, "ur" to 1)[it] ?: 2 }, { languageName(it) }))
                if (languages.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Txt(tr(R.string.noResultsFound), style(14.sp, p.subtitle, family = AppFonts.sans)) }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        LazyColumn(Modifier.widthIn(max = 760.dp).fillMaxSize(), contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = ScreenBottomPadding)) {
                            languages.forEach { code ->
                                val list = groups.getValue(code)
                                item(key = "lang-$code") {
                                    Row(Modifier.padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Txt(languageName(code), style(14.sp, p.accent, FontWeight.SemiBold, family = if (code == "ur") AppFonts.urdu else AppFonts.sans))
                                        Spacer(Modifier.width(8.dp))
                                        Txt("(${list.size})", style(12.sp, p.subtitle, family = AppFonts.sans))
                                    }
                                }
                                items(list, key = { it.identifier }) { edition ->
                                    TranslationRow(
                                        if (urdu) edition.name else edition.englishName,
                                        urduFont = edition.language.lowercase() == "ur",
                                        selected = (if (forPicture) PicturePick.current.third else Settings.quranView.translationEditionId) == edition.identifier,
                                    ) {
                                        if (forPicture) PicturePick.edition = edition.identifier
                                        else Settings.setQuranView(Settings.quranView.copy(translationEditionId = edition.identifier))
                                        nav.pop()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationRow(title: String, urduFont: Boolean, selected: Boolean, onClick: () -> Unit) {
    val p = palette
    Row(Modifier.fillMaxWidth().height(56.dp).pressable(onClick).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(title, style(16.sp, p.title, FontWeight.SemiBold, family = if (urduFont) AppFonts.urdu else AppFonts.sans), Modifier.weight(1f), maxLines = 1)
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(24.dp).height(24.dp).background(p.accent, CircleShape), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.CheckMark, p.checkForeground, 15.dp)
            }
        }
    }
}
