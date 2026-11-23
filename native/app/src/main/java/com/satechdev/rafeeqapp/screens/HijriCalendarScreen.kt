package com.satechdev.rafeeqapp.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.hijri.HijriDate
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.hijriMonth
import com.satechdev.rafeeqapp.ui.localNumber
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.reducedMotion
import com.satechdev.rafeeqapp.ui.style
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** A Hijri month, counted from year 0 so months compare and step simply. */
private data class HijriMonth(val year: Int, val month: Int) {
    val index get() = year * 12 + (month - 1)
    fun plus(n: Int): HijriMonth {
        val i = index + n
        return HijriMonth(i / 12, i % 12 + 1)
    }
}

/**
 * The Hijri calendar (lib/ui/hijri_calendar/hijri_calendar_screen.dart):
 * a month at a time, each day with its Gregorian date, Monday first.
 */
@Composable
fun HijriCalendarScreen() {
    val nav = LocalNavigator.current
    val p = palette
    val correction = Settings.hijriCorrection
    val today = correction.hijri(LocalDate.now())
    val todayMonth = HijriMonth(today.year, today.month)
    var shown by remember { mutableStateOf(todayMonth) }
    var selected by remember { mutableStateOf(Triple(today.year, today.month, today.day)) }
    // The corrected Hijri day falls on a different Gregorian day than the table says.
    fun gregorian(m: HijriMonth, day: Int): LocalDate = HijriDate.toGregorian(m.year, m.month, day).minusDays(correction.days)
    val rtl = Settings.language.rtl
    val swipe = with(LocalDensity.current) { 64.dp.toPx() }

    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(tr(R.string.hijriCalendarTitle), onBack = nav::pop)
        Column(
            Modifier.fillMaxSize().widthIn(max = 620.dp).align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, top = 12.dp, end = 12.dp)
                .navigationBarsPadding(),
        ) {
            MonthHeader(
                shown, first = gregorian(shown, 1), isCurrent = shown == todayMonth,
                onPrevious = { shown = shown.plus(-1) },
                onNext = { shown = shown.plus(1) },
                onToday = { shown = todayMonth; selected = Triple(today.year, today.month, today.day) },
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card)
                    .border(1.dp, p.divider, RoundedCornerShape(16.dp))
                    // Swipe for the next or previous month; the next one lies the way the language reads.
                    .pointerInput(rtl) {
                        var dx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dx = 0f },
                            onDragEnd = {
                                if (abs(dx) > swipe) {
                                    val forward = if (rtl) dx > 0 else dx < 0
                                    shown = shown.plus(if (forward) 1 else -1)
                                }
                            },
                        ) { _, amount -> dx += amount }
                    }
                    .padding(start = 8.dp, top = 14.dp, end = 8.dp, bottom = 8.dp),
            ) {
                WeekDays()
                Spacer(Modifier.height(8.dp))
                val still = reducedMotion()
                AnimatedContent(
                    shown,
                    transitionSpec = {
                        val forward = targetState.index > initialState.index
                        // Later months come in from the reading end.
                        val sign = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
                        if (still) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (slideInHorizontally(tween(280)) { it * sign / 3 } + fadeIn(tween(280))) togetherWith
                            (slideOutHorizontally(tween(200)) { -it * sign / 3 } + fadeOut(tween(160)))
                    },
                    label = "month",
                ) { month ->
                    DayGrid(
                        firstWeekday = gregorian(month, 1).dayOfWeek,
                        days = HijriDate.daysInMonth(month.year, month.month),
                        gregorianDay = { gregorian(month, it).dayOfMonth },
                        isToday = { month == todayMonth && it == today.day },
                        isSelected = { selected == Triple(month.year, month.month, it) },
                        onSelect = { selected = Triple(month.year, month.month, it) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MonthHeader(
    month: HijriMonth,
    first: LocalDate,
    isCurrent: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val p = palette
    val language = Settings.language
    val urdu = language.rtl
    val gregorian = first.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag(language.code)))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card)
            .border(1.dp, p.divider, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(if (urdu) Glyph.ChevronRight else Glyph.ChevronLeft, onPrevious)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                "${hijriMonth(month.month)} ${localNumber(month.year)}" to gregorian,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                label = "title",
            ) { (title, sub) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Txt(title, style(18.sp, p.title, FontWeight.Bold, align = TextAlign.Center), maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    Txt(sub, style(13.sp, p.subtitle, FontWeight.Medium, align = TextAlign.Center), maxLines = 1)
                }
            }
            // Back to this month, from any other.
            AnimatedVisibility(
                !isCurrent,
                enter = scaleIn(spring(dampingRatio = .6f, stiffness = 500f), .6f) + fadeIn(tween(150)),
                exit = scaleOut(tween(120), .6f) + fadeOut(tween(120)),
            ) {
                Box(
                    Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50)).background(p.selectedCard)
                        .pressable(onToday, scale = .92f).padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Txt(tr(R.string.homeToday), style(13.sp, p.selectedTitle, FontWeight.SemiBold))
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        StepButton(if (urdu) Glyph.ChevronLeft else Glyph.ChevronRight, onNext)
    }
}

@Composable
private fun StepButton(icon: Glyph, onClick: () -> Unit) {
    val p = palette
    Box(Modifier.size(40.dp).clip(CircleShape).background(p.background).pressable(onClick, scale = .88f), contentAlignment = Alignment.Center) {
        AppIcon(icon, p.title, 24.dp)
    }
}

/** MON to SUN, in the app's language. */
@Composable
private fun WeekDays() {
    val p = palette
    val locale = Locale.forLanguageTag(Settings.language.code)
    Row(Modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Txt(
                day.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
                style(12.sp, p.subtitle, FontWeight.Bold, align = TextAlign.Center),
                Modifier.weight(1f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DayGrid(
    firstWeekday: DayOfWeek,
    days: Int,
    gregorianDay: (Int) -> Int,
    isToday: (Int) -> Boolean,
    isSelected: (Int) -> Boolean,
    onSelect: (Int) -> Unit,
) {
    val offset = firstWeekday.value - 1
    val rows = (offset + days + 6) / 7
    Column(Modifier.padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(.85f)) {
                        if (day in 1..days) {
                            DayCell(day, gregorianDay(day), isToday(day), isSelected(day)) { onSelect(day) }
                        }
                    }
                }
            }
        }
    }
}

/** A day: Hijri date over the Gregorian one. Choosing it pops it into the accent colour. */
@Composable
private fun DayCell(day: Int, gregorian: Int, today: Boolean, selected: Boolean, onClick: () -> Unit) {
    val p = palette
    val still = reducedMotion()
    val fill by animateColorAsState(
        when {
            selected -> p.accent
            today -> p.accent.copy(alpha = .1f)
            else -> p.background
        },
        tween(180), label = "day",
    )
    val ink by animateColorAsState(if (selected) p.background else p.title, tween(180), label = "ink")
    val sub by animateColorAsState(if (selected) p.background.copy(alpha = .82f) else p.subtitle, tween(180), label = "sub")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected && !still) {
            pop.snapTo(.86f)
            pop.animateTo(1f, spring(dampingRatio = .45f, stiffness = 520f))
        }
    }
    Column(
        Modifier.fillMaxSize()
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .border(if (today && !selected) 1.5.dp else 1.dp, if (today && !selected) p.accent else p.divider, RoundedCornerShape(10.dp))
            .pressable(onClick, scale = .94f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Txt(localNumber(day), style(14.sp, ink, FontWeight.Bold, family = AppFonts.sans))
        Spacer(Modifier.height(2.dp))
        Txt(gregorian.toString(), style(12.sp, sub, family = AppFonts.sans))
    }
}
