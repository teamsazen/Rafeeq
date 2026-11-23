package com.satechdev.rafeeqapp.screens

import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.satechdev.rafeeqapp.data.quran.Ayah
import com.satechdev.rafeeqapp.data.quran.Editions
import com.satechdev.rafeeqapp.data.quran.JuzNames
import com.satechdev.rafeeqapp.data.quran.QuranStore
import com.satechdev.rafeeqapp.data.quran.QuranText
import com.satechdev.rafeeqapp.data.quran.Surah
import com.satechdev.rafeeqapp.data.quran.SurahNames
import com.satechdev.rafeeqapp.data.quran.withoutBismillah
import com.satechdev.rafeeqapp.hero
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.SceneArt
import com.satechdev.rafeeqapp.ui.SceneArtwork
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.quranStyle
import com.satechdev.rafeeqapp.ui.style

/** A surah or a juz. */
sealed interface Section {
    data class OfSurah(val number: Int) : Section
    data class OfJuz(val number: Int) : Section
}

/**
 * What the open reader shows. The section and ayah pickers change it and go
 * back, as the Flutter pickers return their choice.
 */
object ReaderState {
    var section by mutableStateOf<Section>(Section.OfSurah(1))
    /** A global ayah number to scroll to, consumed by the reader. */
    var jumpTo by mutableStateOf<Int?>(null)
}

/** Flutter Prefs.isQuranAutoScrollEnabled; on unless turned off. */
private const val AUTO_SCROLL = "isQuranAutoScrollEnabled"

internal val rtlLanguages = setOf("ar", "ur", "fa", "he", "ps", "sd", "ug", "dv", "ku", "yi")

private sealed interface ReaderItem {
    data class Header(val surah: Surah) : ReaderItem
    data class Verse(val ayah: Ayah) : ReaderItem
}

/** The Quran reader, as lib/ui/quran/quran_details_screen.dart. */
@Composable
fun ReaderScreen(route: Route.QuranReader) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val p = palette
    var quran by remember { mutableStateOf(QuranStore.cached) }
    LaunchedEffect(Unit) { if (quran == null) quran = QuranStore.get(context) }
    // Opening a route sets the section and the ayah to show first.
    remember(route) {
        ReaderState.section = route.juz?.let { Section.OfJuz(it) } ?: Section.OfSurah(route.surah ?: 1)
        ReaderState.jumpTo = null
    }
    val text = quran
    val section = ReaderState.section
    val items = remember(text, section) { text?.let { readerItems(it, section) } ?: emptyList() }
    val list = rememberLazyListState()

    LaunchedEffect(text, route) {
        val q = text ?: return@LaunchedEffect
        val surah = route.surah
        if (route.ayah != null && surah != null) ReaderState.jumpTo = q.surah(surah).ayahs.firstOrNull { it.numberInSurah == route.ayah }?.number
    }
    LaunchedEffect(items, ReaderState.jumpTo) {
        val target = ReaderState.jumpTo ?: return@LaunchedEffect
        val index = items.indexOfFirst { it is ReaderItem.Verse && it.ayah.number == target }
        if (index >= 0) list.animateScrollToItem(index)
        ReaderState.jumpTo = null
    }
    LaunchedEffect(section) { if (ReaderState.jumpTo == null) list.scrollToItem(0) }

    val audio = QuranAudio.state
    // The reader follows the recitation into the next surah or juz...
    var lastPlaying by remember { mutableStateOf(audio.scope to audio.scopeNumber) }
    LaunchedEffect(audio.scope, audio.scopeNumber) {
        val playing = audio.scope to audio.scopeNumber
        if (playing == lastPlaying) return@LaunchedEffect
        lastPlaying = playing
        val number = audio.scopeNumber ?: return@LaunchedEffect
        val next = if (audio.scope == PlaybackScope.Juz) Section.OfJuz(number) else Section.OfSurah(number)
        if (next != ReaderState.section) {
            ReaderState.jumpTo = audio.currentAyah?.number
            ReaderState.section = next
        }
    }
    // ...and keeps the ayah being recited in view (Flutter's isQuranAutoScrollEnabled).
    val reciting = audio.currentAyah?.number
    LaunchedEffect(reciting) {
        if (reciting == null || !Settings.bool(AUTO_SCROLL, true)) return@LaunchedEffect
        val index = items.indexOfFirst { it is ReaderItem.Verse && it.ayah.number == reciting }
        if (index >= 0 && list.layoutInfo.visibleItemsInfo.none { it.index == index && it.offset >= 0 && it.offset + it.size <= list.layoutInfo.viewportEndOffset }) {
            list.animateScrollToItem(index, -(list.layoutInfo.viewportSize.height * .12f).toInt())
        }
    }

    Column(Modifier.fillMaxSize().background(p.background)) {
        ReaderAppBar(text, section, onBack = nav::pop)
        if (text == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) }
            return@Column
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 760.dp).fillMaxSize(),
                state = list,
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
            ) {
                itemsIndexed(items, key = { _, item -> if (item is ReaderItem.Header) "h${item.surah.number}" else "a${(item as ReaderItem.Verse).ayah.number}" }) { _, item ->
                    when (item) {
                        is ReaderItem.Header -> SurahHeader(item.surah) {
                            items.firstNotNullOfOrNull { (it as? ReaderItem.Verse)?.ayah?.takeIf { a -> a.surahNumber == item.surah.number } }
                                ?.let { playFrom(context, section, it) }
                        }
                        is ReaderItem.Verse -> AyahCard(item.ayah) { playFrom(context, section, item.ayah) }
                    }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(if (audio.isVisible) MiniPlayerHeight else 0.dp)) }
            }
        }
    }
    // The player sits under the reader rather than over the last ayah.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.background(if (audio.isVisible) p.card else Color.Transparent).navigationBarsPadding()) { MiniPlayerSlot(PlayerPlace.Reader) }
    }
}

private fun readerItems(quran: QuranText, section: Section): List<ReaderItem> = when (section) {
    is Section.OfSurah -> quran.surah(section.number).let { s -> listOf(ReaderItem.Header(s)) + s.ayahs.map { ReaderItem.Verse(it) } }
    is Section.OfJuz -> buildList {
        var previous: Int? = null
        for (ayah in quran.juz(section.number)) {
            if (ayah.surahNumber != previous) {
                add(ReaderItem.Header(quran.surah(ayah.surahNumber)))
                previous = ayah.surahNumber
            }
            add(ReaderItem.Verse(ayah))
        }
    }
}

/** Title (tap to choose another surah or juz), the juz as subtitle, and the ayah and settings actions. */
@Composable
private fun ReaderAppBar(quran: QuranText?, section: Section, onBack: () -> Unit) {
    val nav = LocalNavigator.current
    val p = palette
    val urdu = Settings.language.rtl
    val title = when (section) {
        is Section.OfSurah -> "${localNumber(section.number)}. " + if (urdu) SurahNames.urdu[section.number] else quran?.surah(section.number)?.englishName.orEmpty()
        is Section.OfJuz -> if (urdu) "${tr(R.string.juz)} ${localNumber(section.number)} • ${JuzNames.urdu[section.number - 1]}" else "Juz ${section.number}"
    }
    val subtitle = when (section) {
        is Section.OfSurah -> quran?.surah(section.number)?.ayahs?.firstOrNull()?.let { "${tr(R.string.juz)} ${localNumber(it.juz)}" } ?: tr(R.string.quran)
        is Section.OfJuz -> tr(R.string.quran)
    }
    Column(Modifier.fillMaxWidth().background(p.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            val mirror = if (urdu) -1f else 1f
            Box(Modifier.size(56.dp).pressable(onBack, scale = .9f), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.ArrowBack, p.title, 24.dp, Modifier.graphicsLayer { scaleX = mirror })
            }
            Column(
                Modifier.weight(1f).pressable({ nav.push(Route.QuranSectionPicker(juz = section is Section.OfJuz)) }).padding(vertical = 4.dp)
                    .then(if (section is Section.OfJuz) Modifier.hero("juz-${section.number}") else Modifier),
            ) {
                Txt(title, style(17.sp, p.title, FontWeight.Bold), maxLines = 1)
                Txt(subtitle, style(12.sp, p.subtitle), maxLines = 1)
            }
            Box(Modifier.size(48.dp).pressable({ nav.push(Route.QuranAyahPicker()) }, scale = .9f), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.NumberedList, p.title, 24.dp)
            }
            Box(Modifier.size(48.dp).pressable({ nav.push(Route.QuranSettings) }, scale = .9f), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.Tune, p.title, 24.dp)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}

/** Makkah or Madinah, the meaning and the name (the surah name lands here from the list), and Play. */
@Composable
private fun SurahHeader(surah: Surah, onPlay: () -> Unit) {
    val p = palette
    val urdu = Settings.language.rtl
    val makki = surah.revelationType.lowercase().contains("meccan")
    Column(Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            SceneArtwork(if (makki) SceneArt.Makkah else SceneArt.Madinah, Modifier.height(150.dp))
        }
        Spacer(Modifier.height(12.dp))
        Txt(if (urdu) SurahNames.urduMeanings[surah.number] else surah.englishNameTranslation, style(14.sp, p.subtitle, align = TextAlign.Center))
        Spacer(Modifier.height(2.dp))
        Txt(
            if (urdu) SurahNames.urdu[surah.number] else surah.englishName,
            style(24.sp, p.title, FontWeight.Bold, align = TextAlign.Center),
            Modifier.hero("surah-${surah.number}"),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.pressable(onPlay, scale = .95f).clip(RoundedCornerShape(50)).background(p.card).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(Glyph.PlayCircle, p.title, 22.dp)
            Spacer(Modifier.width(8.dp))
            Txt(tr(R.string.playAudio), style(14.sp, p.title, FontWeight.SemiBold))
        }
    }
}

/** Translations per surah, loaded once per edition and kept while the app runs. */
internal object TranslationCache {
    val texts = mutableStateMapOf<String, Map<Int, String>>()
    val failed = mutableStateMapOf<String, Boolean>()
}

@Composable
private fun AyahCard(ayah: Ayah, onPlay: () -> Unit) {
    val p = palette
    val context = LocalContext.current
    val view = Settings.quranView
    val urdu = Settings.language.rtl
    val bookmarked = Settings.isBookmarked(ayah.surahNumber, ayah.numberInSurah)
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
    val audio = QuranAudio.state
    // The ayah being recited is lit, and tapping it pauses.
    val current = audio.currentAyah?.number == ayah.number
    val pausable = current && audio.isPlaying
    val tap = if (pausable) QuranAudio::togglePlayPause else onPlay
    val card by animateColorAsState(if (current) p.selectedCard else p.card, tween(220, easing = AppMotion.easeOut), label = "ayah")
    val ink by animateColorAsState(if (current) p.background else p.title, tween(220, easing = AppMotion.easeOut), label = "ink")
    val translationInk by animateColorAsState(if (current) p.card else p.title, tween(220, easing = AppMotion.easeOut), label = "translationInk")
    val reference = if (urdu) "القرآن ${localNumber(ayah.surahNumber)}:${localNumber(ayah.numberInSurah)}" else "${ayah.surahNumber}:${ayah.numberInSurah}"
    val arabic = ayah.withoutBismillah()

    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(12.dp)).background(card).pressable(tap, scale = .99f)) {
        Column(Modifier.padding(12.dp)) {
            Txt(reference, style(15.sp, ink, FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Txt(
                arabic,
                quranStyle(view.arabicFontSize.sp, ink, TextAlign.Right).copy(textDirection = TextDirection.Rtl),
                Modifier.fillMaxWidth(),
            )
            if (view.showTranslation) {
                val texts = TranslationCache.texts[key]
                when {
                    texts != null -> texts[ayah.number]?.takeIf { it.isNotEmpty() }?.let { value ->
                        val language = edition.substringBefore('.').lowercase()
                        val rtl = language in rtlLanguages
                        Spacer(Modifier.height(8.dp))
                        Txt(
                            value,
                            style(
                                view.translationFontSize.sp, translationInk,
                                family = if (language == "ur") AppFonts.urdu else AppFonts.sans,
                                lineHeight = 1.55f,
                                align = if (rtl) TextAlign.Right else TextAlign.Left,
                            ).copy(textDirection = if (rtl) TextDirection.Rtl else TextDirection.Ltr),
                            Modifier.fillMaxWidth(),
                        )
                    }
                    TranslationCache.failed[key] != true -> {
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(2.dp).background(p.card.copy(alpha = .25f)))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.card.copy(alpha = .25f)))
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).pressable(tap, scale = .85f), contentAlignment = Alignment.Center) {
                AppIcon(if (pausable) Glyph.CupertinoPause else Glyph.CupertinoPlay, ink, 22.dp)
            }
            BookmarkButton(bookmarked, ink) { Settings.toggleBookmark(ayah.surahNumber, ayah.numberInSurah) }
            val nav = LocalNavigator.current
            Box(Modifier.size(48.dp).pressable({ nav.push(Route.Placeholder(R.string.shareAyah)) }, scale = .85f), contentAlignment = Alignment.Center) {
                AppIcon(Glyph.CupertinoShare, ink, 22.dp)
            }
            Spacer(Modifier.weight(1f))
            if (current) {
                AnimatedContent(tr(if (audio.isPlaying) R.string.playing else R.string.paused), transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) }, label = "playing") {
                    Txt(it, style(12.sp, p.card, FontWeight.SemiBold), Modifier.padding(end = 10.dp))
                }
            }
        }
    }
}

/** Fills with a pop when an ayah is bookmarked (the favourite pop from 99 Names). */
@Composable
private fun BookmarkButton(bookmarked: Boolean, tint: Color, onClick: () -> Unit) {
    val p = palette
    val pop = remember { Animatable(1f) }
    var last by remember { mutableStateOf(bookmarked) }
    LaunchedEffect(bookmarked) {
        if (bookmarked && !last) {
            pop.snapTo(.6f)
            pop.animateTo(1f, spring(dampingRatio = .4f, stiffness = 500f))
        }
        last = bookmarked
    }
    Box(Modifier.size(48.dp).pressable(onClick, scale = .85f), contentAlignment = Alignment.Center) {
        AppIcon(
            if (bookmarked) Glyph.CupertinoBookmarkFill else Glyph.CupertinoBookmark,
            tint, 22.dp,
            Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        )
    }
}

// ---------- Pickers ----------

/** Choose a surah or a juz for the open reader (QuranSectionPickerScreen). */
@Composable
fun QuranSectionPickerScreen(juz: Boolean, audio: Boolean = false) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    var quran by remember { mutableStateOf(QuranStore.cached) }
    LaunchedEffect(Unit) { if (quran == null) quran = QuranStore.get(context) }
    val playing = QuranAudio.state
    val current = if (!audio) ReaderState.section
    else playing.scopeNumber?.let { if (playing.scope == PlaybackScope.Juz) Section.OfJuz(it) else Section.OfSurah(it) }
    fun choose(section: Section) {
        if (audio) {
            playAudio(context) {
                when (section) {
                    is Section.OfJuz -> QuranAudio.playJuz(context, section.number)
                    is Section.OfSurah -> QuranAudio.playSurah(context, section.number)
                }
            }
        } else {
            ReaderState.section = section
        }
        nav.pop()
    }
    PushedScreen(if (juz) "Choose Juz" else "Choose Surah") {
        if (juz) {
            items(30) { i ->
                PickerTile("Juz ${i + 1}", selected = current == Section.OfJuz(i + 1)) { choose(Section.OfJuz(i + 1)) }
            }
        } else {
            val surahs = quran?.surahs.orEmpty()
            items(surahs.size) { i ->
                val surah = surahs[i]
                PickerTile("${surah.number} - ${surah.englishName}", selected = current == Section.OfSurah(surah.number)) { choose(Section.OfSurah(surah.number)) }
            }
        }
    }
}

/**
 * Jump to an ayah of the open section (QuranAyahPickerScreen). While the
 * reader is playing it plays on from there; with [audio], from Ayah Focus,
 * it moves the recitation to the ayah.
 */
@Composable
fun QuranAyahPickerScreen(audio: Boolean = false) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val quran = QuranStore.cached
    val section = ReaderState.section
    val ayahs = when {
        audio -> QuranAudio.state.ayahs
        quran == null -> emptyList()
        section is Section.OfSurah -> quran.surah(section.number).ayahs
        section is Section.OfJuz -> quran.juz(section.number)
        else -> emptyList()
    }
    val many = ayahs.map { it.surahNumber }.toSet().size > 1
    PushedScreen(tr(R.string.jumpToAyah)) {
        items(ayahs.size) { i ->
            val ayah = ayahs[i]
            val title = if (many) "${quran!!.surah(ayah.surahNumber).englishName} - Ayah ${ayah.numberInSurah}" else "Ayah ${ayah.numberInSurah}"
            PickerTile(title, selected = audio && ayah.number == QuranAudio.state.currentAyah?.number) {
                when {
                    audio -> QuranAudio.jumpTo(ayah.number)
                    else -> {
                        ReaderState.jumpTo = ayah.number
                        if (QuranAudio.state.isPlaying) playFrom(context, section, ayah)
                    }
                }
                nav.pop()
            }
        }
    }
}
