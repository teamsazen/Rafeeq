package com.satechdev.rafeeqapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.satechdev.rafeeqapp.R

private val materialIcons = FontFamily(Font(R.font.material_icons))
private val cupertinoIcons = FontFamily(Font(R.font.cupertino_icons))
private val islamicIcons = FontFamily(Font(R.font.islamic_icons))

/**
 * The icons the Flutter app draws, from the same three icon fonts, by code
 * point: Material Icons (Apache 2.0), Cupertino Icons (MIT) and Flutter
 * Islamic Icons (BSD 2-Clause). Code points are Flutter's `Icons.*`,
 * `CupertinoIcons.*` and `FlutterIslamicIcons.*`.
 */
enum class Glyph(internal val family: FontFamily, internal val codePoint: Int) {
    // Material
    Translate(materialIcons, 0xf45e),
    Explore(materialIcons, 0xf037),
    Palette(materialIcons, 0xf24f),
    Layers(materialIcons, 0xf157),
    Public(materialIcons, 0xf2d4),
    Schedule(materialIcons, 0xf339),
    AccessTime(materialIcons, 0xee2d),
    Notifications(materialIcons, 0xf235),
    MenuBook(materialIcons, 0xf1c2),
    ChatBubble(materialIcons, 0xf62f),
    StarBorder(materialIcons, 0xf01cf),
    Share(materialIcons, 0xf378),
    BugReport(materialIcons, 0xef04),
    Code(materialIcons, 0xf653),
    PrivacyTip(materialIcons, 0xf2d0),
    Gavel(materialIcons, 0xf0c0),
    Description(materialIcons, 0xefae),
    Info(materialIcons, 0xf815),
    Tag(materialIcons, 0xf0216),
    ArrowBack(materialIcons, 0xf572),
    MyLocation(materialIcons, 0xf1fe),
    MyLocationRounded(materialIcons, 0xf8f0),
    Check(materialIcons, 0xf636),
    ArrowDown(materialIcons, 0xf82b),
    Search(materialIcons, 0xf013d),
    CloudOff(materialIcons, 0xef61),
    MenuBookRounded(materialIcons, 0xf8b4),
    Twilight(materialIcons, 0xf4bd),
    Sunny(materialIcons, 0xf4bc),
    Nightlight(materialIcons, 0xf214),
    Cloudy(materialIcons, 0xf4b8),
    Mosque(materialIcons, 0xf0636),
    LocationOff(materialIcons, 0xf192),
    NotificationsActive(materialIcons, 0xf234),
    VolumeUp(materialIcons, 0xf4a8),
    Close(materialIcons, 0xf647),
    Bookmark(materialIcons, 0xf5d1),
    BookmarkBorder(materialIcons, 0xf5cf),
    Play(materialIcons, 0xf00a0),
    SkipNext(materialIcons, 0xf0192),
    ChevronLeft(materialIcons, 0xf63a),
    ChevronRight(materialIcons, 0xf63b),
    SkipPrevious(materialIcons, 0xf0193),
    Pause(materialIcons, 0xf0056),
    NumberedList(materialIcons, 0xf793),
    BulletedList(materialIcons, 0xf792),
    Tune(materialIcons, 0xf0258),
    PlayCircle(materialIcons, 0xf00a2),
    FormatSize(materialIcons, 0xf798),
    TranslateRounded(materialIcons, 0xf0250),
    TextFields(materialIcons, 0xf021e),
    RecordVoice(materialIcons, 0xf2f3),
    RestartAlt(materialIcons, 0xf0106),

    // Cupertino
    Clock(cupertinoIcons, 0xf4be),
    Calendar(cupertinoIcons, 0xf5b0),
    Settings(cupertinoIcons, 0xf411),
    SettingsSolid(cupertinoIcons, 0xf412),
    Location(cupertinoIcons, 0xf6ee),
    CheckMark(cupertinoIcons, 0xf3fd),
    LocationSlash(cupertinoIcons, 0xf6f6),
    PhotoOnRectangle(cupertinoIcons, 0xf76a),
    Bell(cupertinoIcons, 0xf3e1),
    BellFill(cupertinoIcons, 0xf3e2),
    InfoCircle(cupertinoIcons, 0xf44c),
    CupertinoPause(cupertinoIcons, 0xf477),
    CupertinoPlay(cupertinoIcons, 0xf487),
    CupertinoClear(cupertinoIcons, 0xf404),
    CupertinoBookmark(cupertinoIcons, 0xf3e9),
    CupertinoBookmarkFill(cupertinoIcons, 0xf3ea),
    CupertinoShare(cupertinoIcons, 0xf4ca),

    // Flutter Islamic Icons
    Quran(islamicIcons, 0xe81b),
    QuranSolid(islamicIcons, 0xe83b),
    Qibla(islamicIcons, 0xe81a),
    QiblaSolid(islamicIcons, 0xe838),
    IslamicCalendar(islamicIcons, 0xe806),
    AllahSolid(islamicIcons, 0xe81f),
    KaabaSolid(islamicIcons, 0xe829),
    QuranBookSolid(islamicIcons, 0xe83a),
}

/** Draws [glyph] in a [size] square, like Flutter's `Icon`, never scaled by the font size setting. */
@Composable
fun AppIcon(
    glyph: Glyph,
    color: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
    shadow: Shadow? = null,
) {
    val fontSize = with(LocalDensity.current) { (size.toPx() / fontScale).toSp() }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        BasicText(
            String(Character.toChars(glyph.codePoint)),
            style = TextStyle(
                color = color,
                fontFamily = glyph.family,
                fontSize = fontSize,
                lineHeight = fontSize,
                shadow = shadow,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
        )
    }
}
