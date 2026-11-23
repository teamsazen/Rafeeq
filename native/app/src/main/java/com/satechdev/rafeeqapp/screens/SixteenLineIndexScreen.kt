package com.satechdev.rafeeqapp.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.quran.JuzNames
import com.satechdev.rafeeqapp.data.quran.JuzQuarter
import com.satechdev.rafeeqapp.data.quran.QuranStore
import com.satechdev.rafeeqapp.data.quran.QuranText
import com.satechdev.rafeeqapp.data.quran.SixteenLinePosition
import com.satechdev.rafeeqapp.data.quran.SixteenLineQuran
import com.satechdev.rafeeqapp.data.quran.SixteenLineStore
import com.satechdev.rafeeqapp.data.quran.SurahNames
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Spinner
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.launch

/**
 * Picks a place to open in the 16 line Quran: a surah, a juz or one of its
 * quarters, or a page typed into the search (sixteen_line_quran_index_screen.dart).
 */
@Composable
fun SixteenLineIndexScreen(currentPage: Int) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val p = palette
    var quran by remember { mutableStateOf(SixteenLineStore.cached) }
    var text by remember { mutableStateOf(QuranStore.cached) }
    LaunchedEffect(Unit) {
        if (quran == null) quran = SixteenLineStore.get(context)
        if (text == null) text = QuranStore.get(context)
    }
    val search = rememberTextFieldState()
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    fun open(position: SixteenLinePosition) {
        SixteenLineJump.to = position
        nav.pop()
    }

    Column(Modifier.fillMaxSize().background(p.background)) {
        Column(Modifier.fillMaxWidth().background(p.background).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                BackButton(nav::pop)
                Txt(tr(R.string.sixteenLineGoTo), style(18.sp, p.title, FontWeight.SemiBold), Modifier.weight(1f), maxLines = 1)
            }
            Tabs(listOf(tr(R.string.surah), tr(R.string.juz)), pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        }
        val q = quran
        val t = text
        if (q == null || t == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Spinner(p.accent, 36.dp, 3.dp) }
        } else {
            val query = Query(search.text.toString(), q.pageCount)
            IndexSearch(search) { query.page?.let { open(SixteenLinePosition(it)) } }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { tab ->
                if (tab == 0) SurahResults(q, t, query, currentPage, ::open) else JuzResults(q, query, currentPage, ::open)
            }
        }
        Txt(
            tr(R.string.sixteenLineCredit),
            style(12.sp, p.subtitle, align = TextAlign.Center),
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    val mirror = if (Settings.language.rtl) -1f else 1f
    Box(Modifier.size(56.dp).pressable(onBack, scale = .9f), contentAlignment = Alignment.Center) {
        AppIcon(Glyph.ArrowBack, palette.title, 24.dp, Modifier.graphicsLayer { scaleX = mirror })
    }
}

/** What was typed, ready to match against names and numbers (Flutter's _Query). */
private class Query(raw: String, pageCount: Int) {
    val text = raw.trim()
    val key = searchKey(raw)
    /** Set when only a number was typed. */
    val number = key.toIntOrNull()
    /** [number], when there is such a page. */
    val page = number?.takeIf { it in 1..pageCount }
    val isEmpty get() = key.isEmpty()

    fun matches(itemNumber: Int, names: List<String>) = when {
        isEmpty -> true
        number != null -> itemNumber == number
        else -> names.any { searchKey(it).contains(key) }
    }
}

/**
 * Reduces text to what spellings of a name share, so "Yaseen" finds
 * "Ya-Sin", "baqarah" finds "Al-Baqara" and "يس" finds "یس".
 */
internal fun searchKey(text: String): String {
    val digits = buildString {
        for (c in text.lowercase()) {
            val eastern = "٠١٢٣٤٥٦٧٨٩".indexOf(c)
            val urdu = "۰۱۲۳۴۵۶۷۸۹".indexOf(c)
            append(if (eastern >= 0) eastern.toString() else if (urdu >= 0) urdu.toString() else c.toString())
        }
    }
    var key = digits
        // Vowel marks and Quranic signs.
        .replace(Regex("[\\u064B-\\u0670\\u065F\\u06D6-\\u06ED]"), "")
        // Arabic letter forms to their Urdu ones.
        .replace(Regex("[يى]"), "ی")
        .replace("ك", "ک")
        .replace(Regex("[ةه]"), "ہ")
        .replace(Regex("[أإآٱ]"), "ا")
        // Spaces, hyphens and apostrophes.
        .replace(Regex("[^a-z0-9\\u0600-\\u06FF]"), "")
        // Long vowels, written either way.
        .replace("ee", "i")
        .replace("oo", "u")
        .replace(Regex("([a-z])\\1+"), "$1")
    if (key.length > 2 && key.endsWith("h") && key.toIntOrNull() == null) key = key.dropLast(1)
    return key
}

/** A filled search box; Go opens a typed page number. */
@Composable
private fun IndexSearch(state: TextFieldState, onGo: () -> Unit) {
    val p = palette
    BasicTextField(
        state,
        Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, end = 12.dp).clip(RoundedCornerShape(12.dp)).background(p.card),
        textStyle = style(16.sp, p.title),
        lineLimits = TextFieldLineLimits.SingleLine,
        cursorBrush = SolidColor(p.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        onKeyboardAction = { onGo() },
        decorator = { field ->
            Row(Modifier.height(48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(Glyph.Search, p.subtitle, 24.dp)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.weight(1f)) {
                    if (state.text.isEmpty()) Txt(tr(R.string.sixteenLineSearchHint), style(16.sp, p.subtitle), maxLines = 1)
                    field()
                }
                if (state.text.isNotEmpty()) {
                    Box(Modifier.size(40.dp).pressable({ state.clearText() }, scale = .9f), contentAlignment = Alignment.Center) {
                        AppIcon(Glyph.Close, p.subtitle, 22.dp)
                    }
                }
            }
        },
    )
}

/** Results, led by the typed page when there is one. */
@Composable
private fun Results(query: Query, open: (SixteenLinePosition) -> Unit, items: List<@Composable () -> Unit>) {
    val p = palette
    val page = query.page
    if (page == null && items.isEmpty()) {
        Txt(tr(R.string.sixteenLineNoMatch, query.text), style(15.sp, p.subtitle, align = TextAlign.Center), Modifier.fillMaxWidth().padding(24.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (page != null) {
            item(key = "go") { PickerTile(tr(R.string.sixteenLineGoToPageNumber, localNumber(page)), selected = false) { open(SixteenLinePosition(page)) } }
        }
        items(items.size) { items[it]() }
    }
}

@Composable
private fun SurahResults(quran: SixteenLineQuran, text: QuranText, query: Query, currentPage: Int, open: (SixteenLinePosition) -> Unit) {
    val urdu = Settings.language.rtl
    val current = quran.surahForPage(currentPage)
    Results(
        query, open,
        text.surahs
            .filter { query.matches(it.number, listOf(it.englishName, it.englishNameTranslation, it.name, SurahNames.urdu[it.number])) }
            .map { surah ->
                @Composable {
                    PickerTile(
                        "${localNumber(surah.number)}. ${if (urdu) SurahNames.urdu[surah.number] else surah.englishName}",
                        selected = surah.number == current,
                        subtitle = tr(R.string.sixteenLinePageNumber, localNumber(quran.surahStartPages[surah.number - 1])),
                    ) { open(quran.surahStart(surah.number)) }
                }
            },
    )
}

@Composable
private fun JuzResults(quran: SixteenLineQuran, query: Query, currentPage: Int, open: (SixteenLinePosition) -> Unit) {
    val urdu = Settings.language.rtl
    val current = quran.juzForPage(currentPage)
    var quarters by remember { mutableStateOf<Int?>(null) }
    Results(
        query, open,
        (1..quran.juzStartPages.size)
            .filter { query.matches(it, listOf(JuzNames.english[it - 1], JuzNames.urdu[it - 1], JuzNames.arabic[it - 1])) }
            .map { juz ->
                @Composable {
                    PickerTile(
                        "${localNumber(juz)}. ${if (urdu) JuzNames.urdu[juz - 1] else JuzNames.english[juz - 1]}",
                        selected = juz == current,
                        subtitle = tr(R.string.sixteenLinePageNumber, localNumber(quran.juzStartPages[juz - 1])),
                    ) { quarters = juz }
                }
            },
    )
    quarters?.let { juz -> QuarterDialog(quran, juz, onDismiss = { quarters = null }) { quarters = null; open(it) } }
}

/** The juz's start, quarter, half and three quarter marks, each with its page. */
@Composable
private fun QuarterDialog(quran: SixteenLineQuran, juz: Int, onDismiss: () -> Unit, onPick: (SixteenLinePosition) -> Unit) {
    val p = palette
    val urdu = Settings.language.rtl
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.background).padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 16.dp)) {
            Txt(
                "${tr(R.string.juz)} ${localNumber(juz)} · ${if (urdu) JuzNames.urdu[juz - 1] else JuzNames.english[juz - 1]}",
                style(17.sp, p.title, FontWeight.SemiBold, lineHeight = 1.3f, align = TextAlign.Center),
                Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            JuzQuarter.entries.forEach { quarter ->
                if (quarter != JuzQuarter.Start) Spacer(Modifier.height(8.dp))
                val title = tr(
                    when (quarter) {
                        JuzQuarter.Start -> R.string.sixteenLineJuzStart
                        JuzQuarter.Rub -> R.string.sixteenLineJuzRub
                        JuzQuarter.Nisf -> R.string.sixteenLineJuzNisf
                        JuzQuarter.Thalatha -> R.string.sixteenLineJuzSalasa
                    },
                )
                val position = quran.juzQuarter(juz, quarter)
                PickerTile(title, selected = false, subtitle = tr(R.string.sixteenLinePageNumber, localNumber(position.page))) { onPick(position) }
            }
        }
    }
}
