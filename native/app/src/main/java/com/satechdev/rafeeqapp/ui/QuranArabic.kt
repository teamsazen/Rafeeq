package com.satechdev.rafeeqapp.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.quran.Ayah
import com.satechdev.rafeeqapp.data.quran.IndoPakText
import com.satechdev.rafeeqapp.data.quran.QuranScript
import com.satechdev.rafeeqapp.data.quran.withoutBismillah

/** AlQuran Neo draws smaller than PDMS Saleem at the same size; this evens them out. */
private const val INDOPAK_SCALE = 1.08f

/** The ayah's text in the chosen script: IndoPak, or alquran.cloud's Uthmani (without a repeated bismillah). */
@Composable
fun quranText(ayah: Ayah, script: QuranScript = Settings.quranView.script): String {
    if (script == QuranScript.Uthmani) return ayah.withoutBismillah()
    val context = LocalContext.current
    var text by remember { mutableStateOf(IndoPakText.cached) }
    LaunchedEffect(Unit) { if (text == null) text = runCatching { IndoPakText.get(context) }.getOrNull() }
    // Until the IndoPak text has loaded (a moment, once), the Uthmani stands in.
    return text?.get(ayah.surahNumber * 1000 + ayah.numberInSurah) ?: ayah.withoutBismillah()
}

/** The style for Quran Arabic in [script] at [size]. */
@Composable
fun quranArabicStyle(size: TextUnit, color: Color, align: TextAlign, script: QuranScript = Settings.quranView.script): TextStyle =
    if (script == QuranScript.Uthmani) {
        quranStyle(size, color, align).copy(textDirection = TextDirection.Rtl)
    } else {
        TextStyle(
            fontFamily = AppFonts.indopak,
            fontSize = size * INDOPAK_SCALE,
            lineHeight = 2.05.em,
            letterSpacing = 0.em,
            color = color,
            textAlign = align,
            textDirection = TextDirection.Rtl,
        )
    }

/** An ayah in the reader's chosen script, in the font's own weight. */
@Composable
fun QuranArabic(
    ayah: Ayah,
    size: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Right,
    maxLines: Int = Int.MAX_VALUE,
) {
    val script = Settings.quranView.script
    BasicText(quranText(ayah, script), modifier, quranArabicStyle(size, color, align, script), overflow = TextOverflow.Ellipsis, maxLines = maxLines)
}
