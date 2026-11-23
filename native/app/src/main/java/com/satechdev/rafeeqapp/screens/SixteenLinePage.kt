package com.satechdev.rafeeqapp.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.layout.wrapContentWidth
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.quran.AyahLine
import com.satechdev.rafeeqapp.data.quran.BismillahLine
import com.satechdev.rafeeqapp.data.quran.JuzNames
import com.satechdev.rafeeqapp.data.quran.RukuEnd
import com.satechdev.rafeeqapp.data.quran.SixteenLine
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
/** Room left of the frame for the ruku sign, as in the printed mushaf. */
private val RUKU_MARGIN = 18.dp
/** From the frame's outer edge to the lines inside it: outer border, gap, inner border. */
private val FRAME_INSET = (1.4 + 3 + 0.6).dp
/** The font has one light weight; a fine outline, as a share of the size, thickens it. */
private const val STROKE = 0.02f

@Composable
fun SixteenLinePage(quran: SixteenLineQuran, page: Int, surahNames: List<String>, englishNames: List<String>, highlightLine: Int? = null) {
    val p = palette
    val urdu = Settings.language.rtl
    val surah = quran.surahForPage(page)
    val juz = quran.juzForPage(page)
    val detail = style(12.sp, p.title.copy(alpha = .72f), lineHeight = 1.2f)
    Column(Modifier.fillMaxSize().padding(start = 4.dp + RUKU_MARGIN, top = 6.dp, end = 10.dp, bottom = 6.dp)) {
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
            PageLines(quran.pages[page - 1], quran.bismillah, surahNames, highlightLine)
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
private fun PageLines(lines: List<SixteenLine>, bismillah: AyahLine, surahNames: List<String>, highlightLine: Int?) {
    val p = palette
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 64)
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
                    Box(Modifier.fillMaxWidth().height(lineHeight)) {
                        Box(
                            Modifier
                                .fillMaxSize()
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
                                is BismillahLine -> WordsLine(bismillah, layout.fontSize, fontPx, with(density) { widthPx.toDp() }, null)
                                is AyahLine -> WordsLine(line, layout.fontSize, fontPx, with(density) { widthPx.toDp() }, layout.naturalWidths[index])
                            }
                        }
                        // Hangs in the margin left of the frame, level with the line it ends on.
                        (line as? AyahLine)?.ruku?.let {
                            RukuSign(it, p.accent, Modifier.align(AbsoluteAlignment.CenterLeft).absoluteOffset(x = -(FRAME_INSET + RUKU_MARGIN)).width(RUKU_MARGIN).height(lineHeight))
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

/** A line of words: justified edge to edge, or centred; a line too long for the width is shrunk. */
@Composable
private fun WordsLine(line: AyahLine, fontSize: TextUnit, fontPx: Float, width: Dp, naturalPx: Float?) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (line.centered || naturalPx == null) {
            Row(
                Modifier.wrapContentWidth(),
                horizontalArrangement = Arrangement.spacedBy(with(density) { gap(fontPx).toDp() }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                line.words.forEach { QuranWord(it, fontSize, fontPx) }
            }
        } else {
            val natural = with(density) { naturalPx.toDp() }
            val shrink = if (natural > width) width / natural else 1f
            Row(
                Modifier.width(if (natural > width) natural else width).graphicsLayer { scaleX = shrink; scaleY = shrink },
                horizontalArrangement = if (line.words.size == 1) Arrangement.Center else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                line.words.forEach { QuranWord(it, fontSize, fontPx) }
            }
        }
    }
}

/** A word drawn twice: a thin outline under the fill, to thicken the font's strokes. */
@Composable
private fun QuranWord(word: String, fontSize: TextUnit, fontPx: Float) {
    val color = palette.title
    Box {
        BasicText(word, style = quranWordStyle(fontSize, color).copy(drawStyle = Stroke(width = fontPx * STROKE, join = StrokeJoin.Round)), softWrap = false, maxLines = 1)
        BasicText(word, style = quranWordStyle(fontSize, color), softWrap = false, maxLines = 1)
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

/** ع with the ruku's number in its surah above and its ayah count below, as printed in the margin. */
@Composable
private fun RukuSign(ruku: RukuEnd, color: Color, modifier: Modifier) {
    fun arabic(n: Int) = n.toString().map { "۰۱۲۳۴۵۶۷۸۹"[it - '0'] }.joinToString("")
    fun s(size: Int) = TextStyle(fontFamily = AppFonts.urdu, fontSize = size.sp, lineHeight = 1.em, color = color)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BasicText(arabic(ruku.numberInSurah), style = s(11))
        BasicText("ع", style = s(17))
        BasicText(arabic(ruku.ayahCount), style = s(11))
    }
}
