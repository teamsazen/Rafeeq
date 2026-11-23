package com.satechdev.rafeeqapp.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.AppLanguage
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.prayer.Prayer
import com.satechdev.rafeeqapp.data.prayer.PrayerDay
import com.satechdev.rafeeqapp.data.prayer.formatCountdown
import com.satechdev.rafeeqapp.data.prayer.formatTime
import com.satechdev.rafeeqapp.data.prayer.local
import com.satechdev.rafeeqapp.data.prayer.prayerName
import com.satechdev.rafeeqapp.hero
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.SceneArt
import com.satechdev.rafeeqapp.ui.SceneArtwork
import com.satechdev.rafeeqapp.ui.SkyArtwork
import com.satechdev.rafeeqapp.ui.SkyPhase
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.hijriLong
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val AYAH_PICTURE_OPENED = "ayah_picture_opened_v1"
private const val SIXTEEN_LINE_OPENED = "sixteen_line_quran_opened_v1"
private val tabular = TextStyle(fontFeatureSettings = "tnum")

/** The Rafeeq tab, as lib/ui/app_home/home/home_screen.dart. */
@Composable
fun HomeScreen() {
    val nav = LocalNavigator.current
    val p = palette
    val language = Settings.language

    // The clock ticks every second, on the second.
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            now = LocalDateTime.now()
        }
    }
    val date = now.toLocalDate()
    val location = Settings.location
    val day = remember(location, date, Settings.calcMethod, Settings.asrMethod, Settings.highLatitude) {
        location?.let { PrayerDay.of(it, date, Settings.calcMethod, Settings.asrMethod, Settings.highLatitude) }
    }
    val hijri = remember(date, Settings.hijriCorrection) { Settings.hijriCorrection.hijri(date) }
    fun time(value: LocalDateTime?) = formatTime(value, Settings.timeFormat, language)

    val sky = day?.let {
        SkyPhase.at(now, it.today.sunrise.local(), it.today.maghrib.local(), it.yesterday.maghrib.local(), it.tomorrow.sunrise.local())
    } ?: SkyPhase.at(now)
    val prayerTimes = { nav.push(Route.PrayerTimes) }

    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(tr(R.string.appTitle)) {
            LocationPill(location?.city ?: location?.country ?: tr(R.string.locationNotSet)) { nav.push(Route.Location) }
            Spacer(Modifier.width(12.dp))
        }
        CenteredList(bottom = TabBottomPadding) {
            item(key = "dates") { TodayDates(hijriLong(hijri), now, language) }
            item(key = "focus") {
                Spacer(Modifier.height(16.dp))
                val (previous, next) = day?.previousAndNext(now) ?: ((Prayer.None to null) to (Prayer.None to null))
                PrayerFocusCard(
                    previousPrayer = tr(prayerName(previous.first, date)),
                    previousTime = time(previous.second),
                    nextPrayer = tr(prayerName(next.first, date)),
                    nextTime = time(next.second),
                    countdown = formatCountdown(next.second?.let { Duration.between(now, it) }),
                    progress = day?.progress(now) ?: 0f,
                    sky = sky,
                    onClick = prayerTimes,
                )
            }
            item(key = "explore") {
                Spacer(Modifier.height(24.dp))
                SectionTitle(tr(R.string.homeExplore))
                Spacer(Modifier.height(12.dp))
                ExploreGrid()
            }
            item(key = "today") {
                Spacer(Modifier.height(24.dp))
                SectionTitle(tr(R.string.homeToday))
                Spacer(Modifier.height(12.dp))
                val previous = day?.previousAndNext(now)?.first?.first
                val today = day?.today
                Timeline(
                    listOf(
                        Triple(R.string.prayerFajr, today?.fajr, Prayer.Fajr),
                        Triple(R.string.prayerDhuhr, today?.dhuhr, Prayer.Dhuhr),
                        Triple(R.string.prayerAsr, today?.asr, Prayer.Asr),
                        Triple(R.string.prayerMaghrib, today?.maghrib, Prayer.Maghrib),
                        Triple(R.string.prayerIsha, today?.isha, Prayer.Isha),
                    ).map { (label, at, prayer) -> TimelineItem(tr(label), time(at?.local()), prayer == previous) },
                    prayerTimes,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeCard(Glyph.Twilight, tr(R.string.homeSuhurEnd), time(today?.fajr?.local()?.minusMinutes(1)), prayerTimes)
                    TimeCard(Glyph.Sunny, tr(R.string.iftar), time(today?.maghrib?.local()), prayerTimes)
                    TimeCard(Glyph.Nightlight, tr(R.string.homeTahajjud), time(day?.tahajjudStart), prayerTimes)
                }
            }
            item(key = "forbidden") {
                Spacer(Modifier.height(24.dp))
                SectionTitle(tr(R.string.homeForbiddenSalatTimes))
                Spacer(Modifier.height(12.dp))
                val today = day?.today
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ForbiddenCard(tr(R.string.prayerSunrise), time(today?.sunrise?.local()), time(today?.sunrise?.local()?.plusMinutes(15)), SceneArt.Sunrise, prayerTimes)
                    ForbiddenCard(tr(R.string.homeNoon), time(today?.dhuhr?.local()?.minusMinutes(10)), time(today?.dhuhr?.local()), SceneArt.Noon, prayerTimes)
                    ForbiddenCard(tr(R.string.homeSunset), time(today?.maghrib?.local()?.minusMinutes(15)), time(today?.maghrib?.local()), SceneArt.Sunset, prayerTimes)
                }
            }
            item(key = "share") {
                Spacer(Modifier.height(24.dp))
                SectionTitle(tr(R.string.homeShareRafeeq))
                Spacer(Modifier.height(12.dp))
                SharingCard()
            }
        }
    }
}

/** The location chip in the home and prayer times app bars. */
@Composable
internal fun LocationPill(label: String, onClick: () -> Unit) {
    val p = palette
    Row(
        Modifier
            .pressable(onClick)
            .clip(RoundedCornerShape(32.dp))
            .background(p.card.copy(alpha = .7f))
            .padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(Glyph.Location, p.title, 16.dp)
        Spacer(Modifier.width(4.dp))
        Txt(label, style(13.sp, p.title, FontWeight.Medium, family = AppFonts.sans), Modifier.widthIn(max = 145.dp), maxLines = 1)
    }
}

@Composable
private fun TodayDates(hijri: String, now: LocalDateTime, language: AppLanguage) {
    val p = palette
    val locale = Locale.forLanguageTag(language.code)
    val gregorian = "${localNumber(now.dayOfMonth)} ${now.format(DateTimeFormatter.ofPattern("LLLL", locale))}, ${localNumber(now.year)}"
    val weekday = now.format(DateTimeFormatter.ofPattern("EEEE", locale))
    Column {
        Txt(hijri, style(16.sp, p.title, FontWeight.SemiBold))
        Spacer(Modifier.height(2.dp))
        Txt(gregorian, style(14.sp, p.subtitle))
        Spacer(Modifier.height(2.dp))
        Txt(weekday, style(14.sp, p.subtitle))
    }
}

/**
 * The live sky with the prayer just passed, the next one, a bar between
 * them and the countdown. The next prayer rolls in when it changes, and the
 * card flies to the prayer times screen.
 */
@Composable
private fun PrayerFocusCard(
    previousPrayer: String,
    previousTime: String,
    nextPrayer: String,
    nextTime: String,
    countdown: String,
    progress: Float,
    sky: SkyPhase,
    onClick: () -> Unit,
) {
    val p = palette
    val track = if (p.dark) Color(0xFF34565A) else Color(0xFF9CCFDB)
    val bar by animateFloatAsState(progress.coerceIn(0f, 1f), tween(AppMotion.PAGE, easing = AppMotion.easeOut), label = "progress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .hero("prayer-focus")
            .pressable(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (p.dark) Color(0xFF193438) else Color(0xFFC8EDF7)),
    ) {
        SkyArtwork(sky, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                FocusLabel(previousTime, previousPrayer, alignEnd = false)
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .padding(top = 7.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(track),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(bar).clip(RoundedCornerShape(99.dp)).background(p.accent))
                }
                Spacer(Modifier.width(12.dp))
                RollingLabel(nextPrayer) { FocusLabel(nextTime, it, alignEnd = true) }
            }
            Spacer(Modifier.weight(1f))
            Txt(countdown, style(28.sp, p.title, FontWeight.SemiBold).merge(tabular).copy(letterSpacing = 1.sp), maxLines = 1)
        }
    }
}

/** Rolls [value] up into place when it changes. */
@Composable
private fun RollingLabel(value: String, content: @Composable (String) -> Unit) {
    val still = reducedMotion()
    AnimatedContent(
        value,
        transitionSpec = {
            if (still) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else (slideInVertically(tween(AppMotion.PAGE, easing = AppMotion.easeOut)) { it / 2 } + fadeIn()) togetherWith
                (slideOutVertically(tween(AppMotion.FAST)) { -it / 2 } + fadeOut(tween(AppMotion.FAST)))
        },
        label = "roll",
    ) { content(it) }
}

@Composable
private fun FocusLabel(time: String, prayer: String, alignEnd: Boolean) {
    val p = palette
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Txt(time, style(16.sp, p.title, FontWeight.SemiBold).merge(tabular), maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Txt(prayer, style(15.sp, p.title, FontWeight.Medium), maxLines = 1)
    }
}

private class ExploreItem(val label: String, val icon: Glyph, val badge: Boolean = false, val dot: Boolean = false, val onClick: () -> Unit)

@Composable
private fun ExploreGrid() {
    val nav = LocalNavigator.current
    var ayahPictureNew by remember { mutableStateOf(!Settings.flag(AYAH_PICTURE_OPENED)) }
    var sixteenLineNew by remember { mutableStateOf(!Settings.flag(SIXTEEN_LINE_OPENED)) }
    val items = listOf(
        ExploreItem(tr(R.string.quran), Glyph.QuranSolid) { nav.tab = 1 },
        ExploreItem(tr(R.string.dailyPrayerLabel), Glyph.MenuBookRounded) { nav.push(Route.Placeholder(R.string.dailyPrayerLabel)) },
        ExploreItem(tr(R.string.qibla), Glyph.KaabaSolid) { nav.tab = 2 },
        ExploreItem(tr(R.string.homeCalendar), Glyph.IslamicCalendar) { nav.push(Route.Placeholder(R.string.homeCalendar)) },
        ExploreItem(tr(R.string.homeAllahsNames), Glyph.AllahSolid) { nav.push(Route.Placeholder(R.string.homeAllahsNames)) },
        ExploreItem(tr(R.string.homeAyahPicture), Glyph.PhotoOnRectangle, badge = ayahPictureNew) {
            Settings.setFlag(AYAH_PICTURE_OPENED)
            ayahPictureNew = false
            nav.push(Route.Placeholder(R.string.homeAyahPicture))
        },
        ExploreItem(tr(R.string.sixteenLineQuran), Glyph.QuranBookSolid, dot = sixteenLineNew) {
            Settings.setFlag(SIXTEEN_LINE_OPENED)
            sixteenLineNew = false
            nav.push(Route.SixteenLine)
        },
    )
    BoxWithConstraints {
        val ratio = if (maxWidth < 360.dp) 1.2f else 1.18f
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { ExploreCard(it, Modifier.weight(1f).aspectRatio(ratio)) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ExploreCard(item: ExploreItem, modifier: Modifier) {
    val p = palette
    Box(modifier.pressable(item.onClick, scale = .95f)) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(p.card).padding(10.dp)) {
            Txt(
                item.label,
                style(13.sp, p.title, FontWeight.Medium, lineHeight = 1.15f),
                Modifier.align(Alignment.TopStart).padding(end = if (item.badge) 28.dp else if (item.dot) 14.dp else 0.dp),
                maxLines = 2,
            )
            AppIcon(item.icon, p.selectedCard, 36.dp, Modifier.align(Alignment.BottomEnd))
        }
        if (item.dot && !item.badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(10.dp)
                    .background(Color(0xFFE53935), CircleShape)
                    .border(1.5.dp, p.card, CircleShape),
            )
        }
        if (item.badge) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .offset(x = 4.dp, y = (-4).dp)
                    .height(20.dp)
                    .widthIn(min = 20.dp, max = 32.dp)
                    .background(Color(0xFFE53935), RoundedCornerShape(100))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) { Txt("NEW", style(10.sp, Color.White, FontWeight.Bold, family = AppFonts.sans), maxLines = 1) }
        }
    }
}

private class TimelineItem(val label: String, val time: String, val active: Boolean)

/** Today's five prayers on a line; the one in its time is the big dot, which grows when it changes. */
@Composable
private fun Timeline(prayers: List<TimelineItem>, onClick: () -> Unit) {
    val p = palette
    Column(
        Modifier
            .fillMaxWidth()
            .pressable(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(p.card)
            .padding(start = 12.dp, top = 18.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(20.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(2.dp).background(p.selectedCard))
            Row(Modifier.fillMaxWidth()) {
                prayers.forEach { prayer ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val dot by animateFloatAsState(if (prayer.active) 24f else 12f, tween(AppMotion.PAGE, easing = AppMotion.easeOut), label = "dot")
                        val color = if (prayer.active) p.accent else p.selectedCard
                        Box(Modifier.size(dot.dp).background(color, CircleShape))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            prayers.forEach { prayer ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt(prayer.label, style(12.sp, p.subtitle, FontWeight.Medium), maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Txt(prayer.time, style(12.sp, p.title, FontWeight.SemiBold, align = TextAlign.Center).merge(tabular), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TimeCard(icon: Glyph, label: String, time: String, onClick: () -> Unit) {
    val p = palette
    Column(
        Modifier
            .weight(1f)
            .pressable(onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(p.card)
            .padding(10.dp),
    ) {
        AppIcon(icon, p.accent, 25.dp)
        Spacer(Modifier.height(16.dp))
        Txt(label, style(13.sp, p.subtitle), maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Txt(time, style(15.sp, p.title, FontWeight.SemiBold).merge(tabular), maxLines = 1)
    }
}

@Composable
private fun ForbiddenCard(label: String, start: String, end: String, scene: SceneArt, onClick: () -> Unit) {
    val p = palette
    Row(
        Modifier.fillMaxWidth().pressable(onClick).clip(RoundedCornerShape(10.dp)).background(p.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)).background(p.background)) {
            SceneArtwork(scene, width = 80.dp, height = 64.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(label, style(14.sp, p.subtitle, FontWeight.Medium))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(start, style(14.sp, p.title, FontWeight.SemiBold), maxLines = 1)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f).height(1.dp).background(p.selectedCard))
                Spacer(Modifier.width(8.dp))
                Txt(end, style(14.sp, p.title, FontWeight.SemiBold), maxLines = 1)
            }
        }
    }
}

@Composable
private fun SharingCard() {
    val p = palette
    val context = LocalContext.current
    val hadith = tr(R.string.homeHadithText)
    val source = tr(R.string.homeHadithSource)
    val shareLabel = tr(R.string.shareThisApp)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card).padding(10.dp)) {
        Txt(tr(R.string.homeAdFreeMessage), style(17.sp, p.accent, FontWeight.SemiBold, lineHeight = 1.4f))
        Spacer(Modifier.height(12.dp))
        Txt(hadith, style(16.sp, p.title, lineHeight = 1.55f))
        Spacer(Modifier.height(12.dp))
        Txt(source, style(12.sp, p.subtitle))
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .pressable({
                    val text = "$hadith\n\n$source\n\n$shareLabel: https://play.google.com/store/apps/details?id=com.satechdev.rafeeqapp"
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, null))
                })
                .clip(RoundedCornerShape(12.dp))
                .background(p.accent),
            contentAlignment = Alignment.Center,
        ) { Txt(tr(R.string.homeShareNow), style(15.sp, p.checkForeground, FontWeight.SemiBold)) }
    }
}
