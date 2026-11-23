package com.satechdev.rafeeqapp.ui

import com.satechdev.rafeeqapp.R
import android.content.res.AssetManager
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Every colour the app paints with, from the Flutter app's SettingsPalette
 * (lib/ui/app_home/home/settings_palette.dart) and its bottom bar.
 */
@Immutable
data class Palette(
    val dark: Boolean,
    val background: Color,
    val card: Color,
    val selectedCard: Color,
    /** The ayah being recited and the Explore icons: lighter than selectedCard in the dark theme, to stand out. */
    val highlight: Color,
    val title: Color,
    val subtitle: Color,
    val accent: Color,
    val divider: Color,
    val checkForeground: Color,
    val selectedTitle: Color,
    val selectedSubtitle: Color,
    val navTop: Color,
    val navBottom: Color,
    val navBorder: Color,
    val navForeground: Color,
    val navSelected: Color,
    val navIndicator: Color,
    val navShadow: Color,
) {
    companion object {
        val Light = Palette(
            dark = false,
            background = Color(0xFFFFFEFA),
            card = Color(0xFFEFE8D9),
            selectedCard = Color(0xFF634E25),
            highlight = Color(0xFF634E25),
            title = Color(0xFF202624),
            subtitle = Color(0xFF46524F),
            accent = Color(0xFF157E7B),
            divider = Color(0xFFF2EEE5),
            checkForeground = Color(0xFFFFFEFA),
            selectedTitle = Color(0xFFFFFEFA),
            selectedSubtitle = Color(0xFFE9E1D3),
            navTop = Color(0xF7FFFEFA),
            navBottom = Color(0xFFFFFEFA),
            navBorder = Color(0xFFF2EEE5),
            navForeground = Color(0xFF425553),
            navSelected = Color(0xFF125E5C),
            navIndicator = Color(0xFF125E5C).copy(alpha = .1f),
            navShadow = Color.Transparent,
        )
        val Dark = Palette(
            dark = true,
            background = Color(0xFF111212),
            card = Color(0xFF172021),
            selectedCard = Color(0xFF285853),
            highlight = Color(0xFF6FB3AB),
            title = Color(0xFFF6F6F2),
            subtitle = Color(0xFFC4C9C7),
            accent = Color(0xFFC0A35C),
            divider = Color(0xFF242727),
            checkForeground = Color(0xFF1B3030),
            selectedTitle = Color(0xFFF6F6F2),
            selectedSubtitle = Color(0xFFC4C9C7),
            navTop = Color.Black.copy(alpha = .32f),
            navBottom = Color.Black.copy(alpha = .58f),
            navBorder = Color.White.copy(alpha = .14f),
            navForeground = Color.White.copy(alpha = .68f),
            navSelected = Color.White,
            navIndicator = Color.White.copy(alpha = .16f),
            navShadow = Color.Black.copy(alpha = .54f),
        )
    }
}

val LocalPalette = staticCompositionLocalOf { Palette.Dark }

val palette: Palette
    @Composable @ReadOnlyComposable get() = LocalPalette.current

/**
 * The Flutter app's fonts, read from its assets/fonts (the Flutter assets
 * folder is the APK's assets, see app/build.gradle.kts). GoogleSans ships one weight; heavier text
 * is synthesised, as Flutter does. Call [init] before drawing.
 */
object AppFonts {
    lateinit var sans: FontFamily; private set
    lateinit var urdu: FontFamily; private set
    lateinit var quran: FontFamily; private set
    /** QuranWBW's AlQuran Neo for the 16 line Quran (not in git; see README "Getting Started"). */
    lateinit var indopak: FontFamily; private set
    /** QUL's surah name font: the text "surah001" draws surah 1's banner. */
    lateinit var surahNames: FontFamily; private set

    fun init(assets: AssetManager) {
        // Cut to the Latin scripts by scripts/subset_fonts.py (2 MB to 116 KB).
        sans = FontFamily(Font(R.font.google_sans, FontWeight.Normal))
        urdu = FontFamily(Font("fonts/JameelNooriNastaleeq.ttf", assets))
        quran = FontFamily(Font("fonts/PDMSSaleemQuran.ttf", assets))
        indopak = FontFamily(Font("fonts/AlQuranNeo5x3-Arabic.ttf", assets))
        surahNames = FontFamily(Font("fonts/SurahNameV2.ttf", assets))
    }
}

/** Strong ease-out for things entering or responding; short and interruptible (from 99 Names). */
object AppMotion {
    val easeOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val drawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    const val PRESS = 100
    const val RELEASE = 180
    const val FAST = 200
    const val PAGE = 380
}
