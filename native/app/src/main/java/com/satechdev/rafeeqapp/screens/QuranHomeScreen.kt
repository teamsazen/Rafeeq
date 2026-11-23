package com.satechdev.rafeeqapp.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.quran.JuzNames
import com.satechdev.rafeeqapp.data.quran.QuranStore
import com.satechdev.rafeeqapp.data.quran.QuranText
import com.satechdev.rafeeqapp.data.quran.SurahNames
import com.satechdev.rafeeqapp.data.quran.withoutBismillah
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.quranStyle
import com.satechdev.rafeeqapp.ui.style
import com.satechdev.rafeeqapp.hero
import kotlinx.coroutines.launch

private val tabular = TextStyle(fontFeatureSettings = "tnum")

/** The Qur'an tab, as lib/ui/quran/read_quran_home_screen.dart: surahs, juz and bookmarks. */
@Composable
fun QuranHomeScreen(onBack: () -> Unit) {
    val p = palette
    val context = LocalContext.current
    var quran by remember { mutableStateOf(QuranStore.cached) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (quran == null) quran = runCatching { QuranStore.get(context) }.onFailure { failed = true }.getOrNull()
    }
    var searching by remember { mutableStateOf(false) }
    val search = rememberTextFieldState()
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(p.background)) {
        // App bar: the title, or the search field while searching.
        Column(Modifier.fillMaxWidth().background(p.background).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                BackArrow(onBack)
                AnimatedContent(searching, Modifier.weight(1f), transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "title") { on ->
                    if (on) {
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focus.requestFocus() }
                        BasicTextField(
                            search,
                            Modifier.fillMaxWidth().focusRequester(focus),
                            textStyle = style(16.sp, p.title, FontWeight.Medium),
                            lineLimits = TextFieldLineLimits.SingleLine,
                            cursorBrush = SolidColor(p.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            decorator = { field ->
                                Box(Modifier.padding(vertical = 10.dp)) {
                                    if (search.text.isEmpty()) Txt(tr(R.string.searchQuranHint), style(15.sp, p.subtitle), maxLines = 1)
                                    field()
                                }
                            },
                        )
                    } else {
                        Txt(tr(R.string.quran), style(18.sp, p.title, FontWeight.SemiBold), maxLines = 1)
                    }
                }
                Box(
                    Modifier.size(48.dp).pressable({
                        searching = !searching
                        if (!searching) search.edit { replace(0, length, "") }
                    }, scale = .9f),
                    contentAlignment = Alignment.Center,
                ) { AppIcon(if (searching) Glyph.Close else Glyph.Search, p.title, 23.dp) }
                Spacer(Modifier.width(2.dp))
            }
            if (!searching) {
                Tabs(listOf(tr(R.string.surah), tr(R.string.juz), tr(R.string.bookmarks)), pager.currentPage) {
                    scope.launch { pager.animateScrollToPage(it) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        }

        val text = quran
        when {
            text == null && failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Txt(tr(R.string.errorLoadingQuranData), style(14.sp, p.title))
            }
            text == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) }
            searching -> SurahList(text, search.text.toString())
            else -> HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                when (page) {
                    0 -> SurahList(text, "")
                    1 -> JuzList()
                    else -> BookmarksList(text)
                }
            }
        }
    }
}

@Composable
private fun BackArrow(onBack: () -> Unit) {
    val mirror = if (Settings.language.rtl) -1f else 1f
    Box(Modifier.size(56.dp).pressable(onBack, scale = .9f), contentAlignment = Alignment.Center) {
        AppIcon(Glyph.ArrowBack, palette.title, 24.dp, Modifier.graphicsLayer { scaleX = mirror })
    }
}

/**
 * Start-aligned tabs whose underline slides to the chosen label on a spring
 * (Flutter's scrollable TabBar with a label-sized indicator). Positions are
 * absolute, so the underline follows the label in Urdu too.
 */
@Composable
private fun Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = palette
    val density = LocalDensity.current
    var lefts by remember { mutableStateOf(List(labels.size) { 0f }) }
    var widths by remember { mutableStateOf(List(labels.size) { 0f }) }
    val motion = spring<Dp>(dampingRatio = .8f, stiffness = Spring.StiffnessMediumLow)
    val x by animateDpAsState(with(density) { lefts[selected].toDp() }, motion, label = "tab-x")
    val w by animateDpAsState(with(density) { widths[selected].toDp() }, motion, label = "tab-w")
    Box(Modifier.fillMaxWidth().height(48.dp)) {
        Row(Modifier.fillMaxHeight().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            labels.forEachIndexed { index, label ->
                val color by animateColorAsState(if (index == selected) p.title else p.subtitle, tween(150), label = "tab-color")
                Box(
                    Modifier.fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { onSelect(index) }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Txt(
                        label,
                        style(15.sp, color, if (index == selected) FontWeight.SemiBold else FontWeight.Medium),
                        Modifier.onGloballyPositioned { c ->
                            val left = c.positionInRoot().x
                            if (lefts[index] != left || widths[index] != c.size.width.toFloat()) {
                                lefts = lefts.toMutableList().also { it[index] = left }
                                widths = widths.toMutableList().also { it[index] = c.size.width.toFloat() }
                            }
                        },
                        maxLines = 1,
                    )
                }
            }
        }
        Box(Modifier.align(AbsoluteAlignment.BottomLeft).absoluteOffset(x = x).width(w).height(2.dp).background(p.accent))
    }
}

/** Two digits, as _formatQuranIndex: "07", "۰۷". */
private fun index(n: Int) = (if (n < 10) localNumber(0) else "") + localNumber(n)

@Composable
private fun SurahList(quran: QuranText, query: String) {
    val nav = LocalNavigator.current
    val urdu = Settings.language.rtl
    val q = query.trim().lowercase()
    val surahs = if (q.isEmpty()) quran.surahs else quran.surahs.filter {
        it.number.toString() == q || it.name.contains(q) || it.englishName.lowercase().contains(q) ||
            it.englishNameTranslation.lowercase().contains(q) || SurahNames.urdu[it.number].contains(q)
    }
    IndexList(surahs.size) { i ->
        val surah = surahs[i]
        IndexCard(
            heroKey = "surah-${surah.number}",
            index = index(surah.number),
            title = if (urdu) SurahNames.urdu[surah.number] else surah.englishName,
            subtitle = if (urdu) SurahNames.urduMeanings[surah.number] else surah.englishNameTranslation,
            onClick = { nav.push(Route.QuranReader(surah = surah.number)) },
            // Audio comes with the player; until then the button opens the surah.
            onPlay = { nav.push(Route.QuranReader(surah = surah.number)) },
        )
    }
}

@Composable
private fun JuzList() {
    val nav = LocalNavigator.current
    val urdu = Settings.language.rtl
    IndexList(30) { i ->
        val juz = i + 1
        IndexCard(
            heroKey = "juz-$juz",
            index = index(juz),
            title = if (urdu) JuzNames.urdu[i] else JuzNames.english[i],
            subtitle = if (urdu) JuzNames.urduOrdinal[i] else "Juz $juz",
            onClick = { nav.push(Route.QuranReader(juz = juz)) },
            onPlay = { nav.push(Route.QuranReader(juz = juz)) },
        )
    }
}

@Composable
private fun IndexList(count: Int, item: @Composable (Int) -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = TabBottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count) { item(it) }
        }
    }
}

/** A surah or juz row: number, name, meaning, and the play button. The name flies into the reader. */
@Composable
private fun IndexCard(heroKey: String, index: String, title: String, subtitle: String, onClick: () -> Unit, onPlay: () -> Unit) {
    val p = palette
    Row(
        Modifier.fillMaxWidth().pressable(onClick).clip(RoundedCornerShape(12.dp)).background(p.card).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(index, style(16.sp, p.accent, FontWeight.SemiBold).merge(tabular).copy(textDirection = TextDirection.Ltr), Modifier.width(32.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, style(16.sp, p.title, FontWeight.SemiBold), Modifier.hero(heroKey), maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Txt(subtitle, style(14.sp, p.subtitle), maxLines = 1)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(48.dp).pressable(onPlay, scale = .85f), contentAlignment = Alignment.Center) {
            AppIcon(Glyph.Play, p.accent.copy(alpha = .75f), 26.dp)
        }
    }
}


/** Bookmarked ayahs in mushaf order (lib/ui/quran/quran_bookmarks_tab.dart). */
@Composable
private fun BookmarksList(quran: QuranText) {
    val nav = LocalNavigator.current
    val p = palette
    val urdu = Settings.language.rtl
    val marks = Settings.bookmarks
        .mapNotNull { b -> quran.surahs.getOrNull(b.surah - 1)?.let { s -> s.ayahs.firstOrNull { it.numberInSurah == b.ayah }?.let { s to it } } }
        .sortedWith(compareBy({ it.first.number }, { it.second.numberInSurah }))
    if (marks.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AppIcon(Glyph.BookmarkBorder, p.subtitle.copy(alpha = .6f), 48.dp)
            Spacer(Modifier.height(16.dp))
            Txt(tr(R.string.noBookmarksYet), style(17.sp, p.title, FontWeight.SemiBold, align = TextAlign.Center))
            Spacer(Modifier.height(6.dp))
            Txt(tr(R.string.bookmarkAyahsPrompt), style(14.sp, p.subtitle, align = TextAlign.Center))
        }
        return
    }
    IndexList(marks.size) { i ->
        val (surah, ayah) = marks[i]
        val text = ayah.withoutBismillah()
        Column(
            Modifier.fillMaxWidth().pressable({ nav.push(Route.QuranReader(surah = surah.number, ayah = ayah.numberInSurah)) })
                .clip(RoundedCornerShape(12.dp)).background(p.card).padding(start = 12.dp, top = 6.dp, end = 4.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = p.title, fontWeight = FontWeight.SemiBold)) {
                            append(if (urdu) SurahNames.urdu[surah.number] else surah.englishName)
                        }
                        withStyle(SpanStyle(color = p.subtitle)) { append("  ·  " + tr(R.string.ayahNumber, localNumber(ayah.numberInSurah))) }
                    },
                    Modifier.weight(1f),
                    style = style(15.sp, p.title),
                    maxLines = 1,
                )
                Box(Modifier.size(48.dp).pressable({ Settings.toggleBookmark(surah.number, ayah.numberInSurah) }, scale = .85f), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.Bookmark, p.accent, 22.dp)
                }
            }
            Txt(
                text,
                quranStyle(22.sp, p.title, TextAlign.Right).copy(textDirection = TextDirection.Rtl),
                Modifier.fillMaxWidth().padding(end = 8.dp),
                maxLines = 2,
            )
        }
    }
}
