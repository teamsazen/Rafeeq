package com.satechdev.rafeeqapp.screens

import androidx.compose.ui.text.drawText
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.quran.AyahLine
import com.satechdev.rafeeqapp.data.quran.BismillahLine
import com.satechdev.rafeeqapp.data.quran.JuzNames
import com.satechdev.rafeeqapp.data.quran.SixteenLineQuran
import com.satechdev.rafeeqapp.data.quran.SurahNameLine
import com.satechdev.rafeeqapp.data.quran.SurahNames
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.style
import kotlin.math.max
import kotlin.math.min

/*
 * One mushaf page, as lib/ui/quran/sixteen_line_quran/sixteen_line_page.dart:
 * a framed block of 16 lines with the surah and juz above and the page
 * number below.
 */

private const val LINES_PER_PAGE = 16
private val LINE_INSET = 8.dp
/** The font has one light weight; a fine outline, as a share of the size, thickens it. */
private const val STROKE = 0.02f

@Composable
fun SixteenLinePage(
    quran: SixteenLineQuran,
    page: Int,
    surahNames: List<String>,
    englishNames: List<String>,
    highlightLine: Int? = null,
    playing: Int? = null,
    onAyahTap: (Int) -> Unit = {},
) {
    val p = palette
    val urdu = Settings.language.rtl
    val surah = quran.surahForPage(page)
    val juz = quran.juzForPage(page)
    val detail = style(12.sp, p.title.copy(alpha = .72f), lineHeight = 1.2f)
    Column(Modifier.fillMaxSize().padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(4.dp, 6.dp, 4.dp, 6.dp)) {
            Txt(if (urdu) SurahNames.urdu[surah] else englishNames[surah - 1], detail, Modifier.weight(1f), maxLines = 1)
            Txt(if (urdu) JuzNames.urduOrdinal[juz - 1] else "${tr(R.string.juz)} $juz", detail)
        }
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .border(1.4.dp, p.selectedCard, RoundedCornerShape(4.dp))
                .padding(3.dp)
                .border(.6.dp, p.selectedCard.copy(alpha = .45f), RoundedCornerShape(2.dp)),
        ) {
            PageLines(quran, page - 1, surahNames, highlightLine, playing, onAyahTap)
        }
        Txt(localNumber(page), detail.copy(fontWeight = FontWeight.SemiBold), Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
    }
}

/** The font size for a page, and each line's natural width at it (Flutter's _PageLayout). */
private class PageLayout(val fontSize: TextUnit, val naturalWidths: List<Float>)

private fun quranWordStyle(size: TextUnit, color: Color) = TextStyle(
    fontFamily = AppFonts.indopak,
    fontSize = size,
    color = color,
    letterSpacing = 0.em,
    textDirection = TextDirection.Rtl,
)

private fun gap(fontSizePx: Float) = fontSizePx * .14f

private fun naturalWidth(words: List<String>, measurer: TextMeasurer, style: TextStyle, fontSizePx: Float): Float =
    words.sumOf { measurer.measure(it, style, maxLines = 1, softWrap = false).size.width.toDouble() }.toFloat() +
        gap(fontSizePx) * max(0, words.size - 1)

@Composable
private fun PageLines(quran: SixteenLineQuran, pageIndex: Int, surahNames: List<String>, highlightLine: Int?, playing: Int?, onAyahTap: (Int) -> Unit) {
    val p = palette
    val lines = quran.pages[pageIndex]
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 256)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val lineHeightGuess = maxH / LINES_PER_PAGE
        // Waqf signs after a line's last word hang past it on the left, so that side keeps extra room.
        val lineInsetPx = with(density) { LINE_INSET.toPx() }
        val endInsetPx = lineInsetPx + lineHeightGuess * .22f
        val widthPx = maxW - lineInsetPx - endInsetPx
        // The two opening pages hold only a few lines, printed larger.
        val slots = if (lines.size <= 8) lines.size + 6 else LINES_PER_PAGE
        val lineHeightPx = maxH / slots
        val layout = remember(lines, widthPx, lineHeightPx) {
            // One size for the page: as large as the line height allows, shrunk until the widest line fits.
            val basePx = lineHeightPx * .82f
            val base = with(density) { basePx.toSp() }
            val probe = quranWordStyle(base, Color.Black)
            val probed = lines.map { if (it is AyahLine) naturalWidth(it.words, measurer, probe, basePx) else 0f }
            val widest = probed.maxOrNull() ?: 0f
            val scale = if (widest <= 0f) 1f else min(1f, widthPx * .97f / widest)
            PageLayout(base * scale, probed.map { it * scale })
        }
        val fontPx = with(density) { layout.fontSize.toPx() }
        val lineHeight = with(density) { lineHeightPx.toDp() }
        // The page reads right to left whatever the app's language: start is the right edge.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                lines.forEachIndexed { index, line ->
                    val last = index == lines.size - 1
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(lineHeight)
                            .highlight(index == highlightLine, p.accent)
                            .drawBehind {
                                // Ruled like a printed mushaf: a hairline under every line but the last.
                                if (!last) drawLine(p.selectedCard.copy(alpha = .35f), Offset(0f, size.height), Offset(size.width, size.height), .6.dp.toPx())
                            }
                            .padding(
                                start = LINE_INSET,
                                end = if (line is SurahNameLine) LINE_INSET else with(density) { endInsetPx.toDp() },
                            ),
                    ) {
                        when (line) {
                            is SurahNameLine -> SurahBanner(line.surahNumber, surahNames[line.surahNumber - 1], p.accent, lineHeight)
                            is BismillahLine -> WordsCanvas(quran.bismillah.words, null, true, layout.fontSize, fontPx, null, -1, null, measurer) {}
                            is AyahLine -> WordsCanvas(
                                line.words, quran.wordAyahs[pageIndex][index], line.centered, layout.fontSize, fontPx,
                                layout.naturalWidths[index], quran.rukuRings[pageIndex][index], playing, measurer, onAyahTap,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tints a line, then fades it: shows where a jump landed, which can be part way down the page. */
@Composable
private fun Modifier.highlight(enabled: Boolean, color: Color): Modifier {
    if (!enabled) return this
    val tint = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        tint.animateTo(0f, keyframes { durationMillis = 2800; 1f at 1540 })
    }
    return this.background(color.copy(alpha = .2f * tint.value))
}

/** Where each word of a line sits: its left edge and the line's scale. */
private class WordPlaces(val lefts: FloatArray, val scale: Float, val gap: Float)

/**
 * Right to left: justified edge to edge, or centred; a line too long for
 * the width is shrunk about its middle.
 */
private fun place(widths: FloatArray, width: Float, centered: Boolean, naturalPx: Float?, fontPx: Float): WordPlaces {
    val n = widths.size
    val total = widths.sum()
    val baseGap = gap(fontPx)
    val lefts = FloatArray(n)
    if (centered || naturalPx == null || n == 1) {
        val lineW = total + baseGap * (n - 1)
        var right = (width + lineW) / 2
        for (i in 0 until n) { lefts[i] = right - widths[i]; right -= widths[i] + baseGap }
        return WordPlaces(lefts, 1f, baseGap)
    }
    val scale = if (naturalPx > width) width / naturalPx else 1f
    val space = if (scale < 1f) baseGap else (width - total) / (n - 1)
    val lineW = (total + space * (n - 1)) * scale
    var right = (width + lineW) / 2
    for (i in 0 until n) { lefts[i] = right - widths[i] * scale; right -= (widths[i] + space) * scale }
    return WordPlaces(lefts, scale, space * scale)
}

/**
 * A line drawn on one canvas: each word measured once and painted twice,
 * a thin outline under the fill to thicken the font's strokes. Far lighter
 * than a text node per word, so pages turn without a hitch. The ayah being
 * recited is lit, a tap picks the ayah under the finger, and ع stands over
 * the ring that ends a ruku, as printed.
 */
@Composable
private fun WordsCanvas(
    words: List<String>,
    keys: IntArray?,
    centered: Boolean,
    fontSize: TextUnit,
    fontPx: Float,
    naturalPx: Float?,
    rukuWord: Int,
    playing: Int?,
    measurer: TextMeasurer,
    onAyahTap: (Int) -> Unit,
) {
    val p = palette
    val ink = p.title
    val layouts = remember(words, fontSize) { words.map { measurer.measure(it, quranWordStyle(fontSize, Color.Black), maxLines = 1, softWrap = false) } }
    val widths = remember(layouts) { FloatArray(layouts.size) { layouts[it].size.width.toFloat() } }
    val ain = remember(fontSize, rukuWord) {
        if (rukuWord < 0) null else measurer.measure("ع", TextStyle(fontFamily = AppFonts.urdu, fontSize = fontSize * .5f, lineHeight = 1.em), maxLines = 1, softWrap = false)
    }
    val lit = keys != null && playing != null && keys.any { it == playing }
    val glow by animateFloatAsState(if (lit) 1f else 0f, tween(260), label = "recited")
    var places by remember { mutableStateOf<WordPlaces?>(null) }
    Canvas(
        Modifier
            .fillMaxSize()
            .then(
                if (keys == null) Modifier else Modifier.pointerInput(keys) {
                    detectTapGestures { tap ->
                        val at = places ?: return@detectTapGestures
                        val i = at.lefts.indices.firstOrNull { tap.x >= at.lefts[it] - at.gap / 2 && tap.x <= at.lefts[it] + widths[it] * at.scale + at.gap / 2 }
                        if (i != null) onAyahTap(keys[i])
                    }
                },
            ),
    ) {
        val at = place(widths, size.width, centered, naturalPx, fontPx).also { places = it }
        if (glow > 0f && keys != null) {
            // The words of the ayah being recited, each with half the space either side so they join up.
            val pad = 3.dp.toPx()
            keys.forEachIndexed { i, key ->
                if (key == playing) {
                    drawRoundRect(
                        p.accent.copy(alpha = .2f * glow),
                        Offset(at.lefts[i] - at.gap / 2, pad),
                        Size(widths[i] * at.scale + at.gap, size.height - pad * 2),
                        CornerRadius(4.dp.toPx()),
                    )
                }
            }
        }
        val outline = Stroke(width = fontPx * STROKE * at.scale, join = StrokeJoin.Round)
        layouts.forEachIndexed { i, text ->
            val top = (size.height - text.size.height * at.scale) / 2
            withTransform({ translate(at.lefts[i], top); scale(at.scale, at.scale, Offset.Zero) }) {
                drawText(text, ink, drawStyle = outline)
                drawText(text, ink)
            }
            if (i == rukuWord && ain != null) {
                val x = at.lefts[i] + (widths[i] * at.scale - ain.size.width) / 2
                drawText(ain, p.accent, Offset(x, (top - ain.size.height * .45f).coerceAtLeast(-ain.size.height * .2f)))
            }
        }
    }
}

/** QUL's surah name font turns "surah001" into the heading, drawn in the accent colour. */
@Composable
private fun SurahBanner(surah: Int, name: String, color: Color, lineHeight: Dp) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize().semantics(mergeDescendants = true) { contentDescription = name }, contentAlignment = Alignment.Center) {
        BasicText(
            "surah" + surah.toString().padStart(3, '0'),
            style = TextStyle(
                fontFamily = AppFonts.surahNames,
                fontSize = with(density) { (lineHeight * .95f).toSp() },
                lineHeight = 1.em,
                color = color,
                textDirection = TextDirection.Ltr,
            ),
            maxLines = 1,
            softWrap = false,
        )
    }
}
