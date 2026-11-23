package com.satechdev.rafeeqapp.screens

import com.satechdev.rafeeqapp.data.audio.QuranAudio
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.Settings
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
import kotlin.math.hypot

/** Flutter Prefs.isQuranAutoScrollEnabled, shared with the reader. */
private const val AUTO_SCROLL_KEY = "isQuranAutoScrollEnabled"

/** The page the 16 line reader was last on (Flutter's sixteen_line_quran_last_page). */
const val SIXTEEN_LINE_LAST_PAGE = "sixteen_line_quran_last_page"

/** A jump from the index, consumed by the reader (the Flutter index returns a position). */
object SixteenLineJump {
    var to by mutableStateOf<SixteenLinePosition?>(null)
}

/** Reads the IndoPak 16 line mushaf page by page, as sixteen_line_quran_screen.dart. */
@Composable
fun SixteenLineScreen() {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val p = palette
    var quran by remember { mutableStateOf(SixteenLineStore.cached) }
    var text by remember { mutableStateOf(QuranStore.cached) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        runCatching {
            if (text == null) text = QuranStore.get(context)
            if (quran == null) quran = SixteenLineStore.get(context)
        }.onFailure { failed = true }
    }
    var page by remember { mutableStateOf(Settings.int(SIXTEEN_LINE_LAST_PAGE) ?: 1) }
    val q = quran
    val urdu = Settings.language.rtl

    Column(Modifier.fillMaxSize().background(p.background)) {
        Column(Modifier.fillMaxWidth().background(p.background).statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                val mirror = if (urdu) -1f else 1f
                Box(Modifier.size(56.dp).pressable(nav::pop, scale = .9f), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.ArrowBack, p.title, 24.dp, Modifier.graphicsLayer { scaleX = mirror })
                }
                Column(Modifier.weight(1f).pressable(if (q == null) null else ({ nav.push(Route.SixteenLineIndex(page)) })).padding(vertical = 4.dp)) {
                    if (q != null && text != null) {
                        val surah = q.surahForPage(page)
                        Txt(if (urdu) SurahNames.urdu[surah] else text!!.surah(surah).englishName, style(17.sp, p.title, FontWeight.Bold), maxLines = 1)
                        Txt("${tr(R.string.juz)} ${localNumber(q.juzForPage(page))} · ${tr(R.string.sixteenLinePageNumber, localNumber(page))}", style(12.sp, p.subtitle), maxLines = 1)
                    } else {
                        Txt(tr(R.string.sixteenLineQuran), style(18.sp, p.title, FontWeight.SemiBold), maxLines = 1)
                    }
                }
                Box(Modifier.size(48.dp).pressable(if (q == null) null else ({ nav.push(Route.SixteenLineIndex(page)) }), scale = .9f), contentAlignment = Alignment.Center) {
                    AppIcon(Glyph.BulletedList, p.title, 24.dp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        }
        val audio = QuranAudio.state
        Box(Modifier.weight(1f).fillMaxWidth().then(if (audio.isVisible) Modifier else Modifier.navigationBarsPadding()), contentAlignment = Alignment.Center) {
            when {
                q != null && text != null -> SwipeReader(q, text!!, page) { page = it; Settings.setInt(SIXTEEN_LINE_LAST_PAGE, it) }
                failed -> Txt(tr(R.string.legalRetry), style(14.sp, p.title))
                else -> Spinner(p.accent, 36.dp, 3.dp)
            }
        }
        // The player sits under the page while something is recited.
        Box(Modifier.background(if (audio.isVisible) p.card else p.background).navigationBarsPadding()) { MiniPlayerSlot(PlayerPlace.Elsewhere) }
    }
}

/**
 * One page at a time, laid out right to left so the next page comes in
 * from the left like a printed mushaf. Two fingers zoom up to three times;
 * while zoomed, or with two fingers down, one finger moves around the page
 * instead of turning it.
 */
@Composable
private fun SwipeReader(quran: SixteenLineQuran, text: QuranText, initialPage: Int, onPageChanged: (Int) -> Unit) {
    val pager = rememberPagerState(initialPage = (initialPage - 1).coerceIn(0, quran.pageCount - 1)) { quran.pageCount }
    var highlight by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(SixteenLineJump.to) {
        val jump = SixteenLineJump.to ?: return@LaunchedEffect
        SixteenLineJump.to = null
        pager.scrollToPage(jump.page - 1)
        highlight = jump.line?.let { jump.page to it }
        onPageChanged(jump.page)
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { index ->
            if (highlight?.first != index + 1) highlight = null
            onPageChanged(index + 1)
        }
    }
    // Tap an ayah to hear it; the one being recited is lit, and the page turns to follow it.
    val context = LocalContext.current
    val audio = QuranAudio.state
    val playing = audio.currentAyah?.takeIf { audio.isVisible }?.let { it.surahNumber * 1000 + it.numberInSurah }
    val onAyahTap: (Int) -> Unit = { key ->
        if (key == playing && !audio.isComplete) QuranAudio.togglePlayPause()
        else playAudio(context) { QuranAudio.playSurah(context, key / 1000, key % 1000) }
    }
    LaunchedEffect(playing) {
        val target = playing?.let { quran.ayahPages[it] } ?: return@LaunchedEffect
        if (audio.isPlaying && target - 1 != pager.currentPage && Settings.bool(AUTO_SCROLL_KEY, true)) pager.animateScrollToPage(target - 1)
    }
    val surahNames = remember(text) { text.surahs.map { it.name } }
    val englishNames = remember(text) { text.surahs.map { it.englishName } }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var fingers by remember { mutableStateOf(0) }
    val zoomed = scale > 1f
    LaunchedEffect(pager.currentPage) { scale = 1f; offset = Offset.Zero }

    fun clamp(o: Offset, s: Float) = Offset(
        o.x.coerceIn(viewport.width * (1 - s), 0f),
        o.y.coerceIn(viewport.height * (1 - s), 0f),
    )

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { viewport = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var startSpan = 0f
                    var startScale = 1f
                    var startFocal = Offset.Zero
                    var startOffset = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val down = event.changes.filter { it.pressed }
                        if (down.size != fingers) {
                            fingers = down.size
                            if (down.size >= 2) {
                                startSpan = hypot(down[0].position.x - down[1].position.x, down[0].position.y - down[1].position.y)
                                startFocal = (down[0].position + down[1].position) / 2f
                                startScale = scale
                                startOffset = offset
                            }
                            if (down.size < 2 && scale < 1.05f) { scale = 1f; offset = Offset.Zero }
                            continue
                        }
                        if (down.size >= 2 && startSpan > 0f) {
                            val span = hypot(down[0].position.x - down[1].position.x, down[0].position.y - down[1].position.y)
                            val focal = (down[0].position + down[1].position) / 2f
                            val s = (startScale * span / startSpan).coerceIn(1f, 3f)
                            // Keep the spot first pinched under the fingers as they move.
                            val spot = (startFocal - startOffset) / startScale
                            scale = s
                            offset = clamp(focal - spot * s, s)
                            down.forEach { it.consume() }
                        } else if (down.size == 1 && zoomed) {
                            val change = down[0]
                            offset = clamp(offset + (change.position - change.previousPosition), scale)
                            change.consume()
                        }
                    }
                }
            },
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                pager,
                Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = fingers < 2 && !zoomed,
                key = { it },
                // A short, slow swipe still turns the page: past a fifth of it, never more than one page.
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pager,
                    pagerSnapDistance = PagerSnapDistance.atMost(1),
                    snapPositionalThreshold = .18f,
                ),
            ) { index ->
                val appDirection = if (Settings.language.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides appDirection) {
                    val current = index == pager.currentPage
                    Box(
                        Modifier.fillMaxSize().clipToBounds().graphicsLayer {
                            if (current && zoomed) {
                                transformOrigin = TransformOrigin(0f, 0f)
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                        },
                    ) {
                        SixteenLinePage(quran, index + 1, surahNames, englishNames, highlight?.takeIf { it.first == index + 1 }?.second, playing, onAyahTap)
                    }
                }
            }
        }
    }
}
