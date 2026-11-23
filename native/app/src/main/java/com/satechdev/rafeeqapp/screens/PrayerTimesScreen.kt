package com.satechdev.rafeeqapp.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.satechdev.rafeeqapp.LocalNavigator
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.Route
import com.satechdev.rafeeqapp.data.AppLanguage
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.hijri.HijriDate
import com.satechdev.rafeeqapp.data.prayer.NotifKey
import com.satechdev.rafeeqapp.data.prayer.NotificationTiming
import com.satechdev.rafeeqapp.data.prayer.Prayer
import com.satechdev.rafeeqapp.data.prayer.PrayerDay
import com.satechdev.rafeeqapp.data.prayer.Reminders
import com.satechdev.rafeeqapp.data.prayer.formatTime
import com.satechdev.rafeeqapp.data.prayer.local
import com.satechdev.rafeeqapp.hero
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.AppFonts
import com.satechdev.rafeeqapp.ui.AppIcon
import com.satechdev.rafeeqapp.ui.AppMotion
import com.satechdev.rafeeqapp.ui.AppSwitch
import com.satechdev.rafeeqapp.ui.Glyph
import com.satechdev.rafeeqapp.ui.Txt
import com.satechdev.rafeeqapp.ui.palette
import com.satechdev.rafeeqapp.ui.pressable
import com.satechdev.rafeeqapp.ui.style
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

private val tabular = TextStyle(fontFeatureSettings = "tnum")

/** Today's prayers, forbidden times and the night, as lib/ui/prayer_times/prayer_times_screen.dart. */
@Composable
fun PrayerTimesScreen() {
    val nav = LocalNavigator.current
    val p = palette
    val location = Settings.location
    Column(Modifier.fillMaxSize().background(p.background)) {
        AppBar(tr(R.string.prayerTimes), onBack = nav::pop) {
            LocationPill(location?.city?.takeIf { it.isNotBlank() } ?: tr(R.string.locationNotSet)) { nav.push(Route.Location) }
            Spacer(Modifier.width(12.dp))
        }
        CenteredList(bottom = ScreenBottomPadding) {
            if (location == null) {
                item(key = "no-location") { NoLocation { nav.push(Route.Location) } }
            } else {
                item(key = "content") { PrayerTimesContent() }
            }
        }
    }
}

@Composable
private fun NoLocation(onClick: () -> Unit) {
    val p = palette
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(Glyph.LocationOff, p.accent, 38.dp)
        Spacer(Modifier.height(12.dp))
        Txt(tr(R.string.locationRequiredTitle), style(18.sp, p.title, FontWeight.Bold, align = TextAlign.Center))
        Spacer(Modifier.height(8.dp))
        Txt(tr(R.string.locationRequiredDescription), style(14.sp, p.subtitle, lineHeight = 1.4f, align = TextAlign.Center))
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth().height(40.dp).pressable(onClick).clip(RoundedCornerShape(50)).background(p.accent),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(Glyph.Location, p.checkForeground, 18.dp)
            Spacer(Modifier.width(8.dp))
            Txt(tr(R.string.grantLocationPermission), style(14.sp, p.checkForeground, FontWeight.Medium))
        }
    }
}

private class PrayerRow(val prayer: Prayer, val name: String, val start: LocalDateTime, val key: NotifKey, val icon: Glyph)

private class ForbiddenRow(val label: String, val start: String, val end: String, val icon: Glyph)

@Composable
private fun PrayerTimesContent() {
    val nav = LocalNavigator.current
    val location = Settings.location ?: return
    val language = Settings.language
    val date = remember { LocalDate.now() }
    // Ticks on the minute so the highlighted prayer moves on while the screen is open.
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val day = remember(location, Settings.calcMethod, Settings.asrMethod, Settings.highLatitude) {
        PrayerDay.of(location, date, Settings.calcMethod, Settings.asrMethod, Settings.highLatitude)
    }
    fun time(value: LocalDateTime) = formatTime(value, Settings.timeFormat, language)
    val times = day.today
    val friday = date.dayOfWeek == DayOfWeek.FRIDAY
    val rows = buildList {
        add(PrayerRow(Prayer.Fajr, tr(R.string.prayerFajr), times.fajr.local(), NotifKey.Fajr, Glyph.Twilight))
        add(PrayerRow(Prayer.Dhuhr, tr(if (friday) R.string.prayerJumma else R.string.prayerDhuhr), times.dhuhr.local(), NotifKey.Dhuhr, Glyph.Sunny))
        add(PrayerRow(Prayer.Asr, tr(R.string.prayerAsr), times.asr.local(), NotifKey.Asr, Glyph.Cloudy))
        add(PrayerRow(Prayer.Maghrib, tr(R.string.prayerMaghrib), times.maghrib.local(), NotifKey.Maghrib, Glyph.Twilight))
        add(PrayerRow(Prayer.Isha, tr(R.string.prayerIsha), times.isha.local(), NotifKey.Isha, Glyph.Nightlight))
        // Eid prayer, half an hour after sunrise, from the last days of Ramadan to 1 Shawwal.
        val hijri = HijriDate.of(date)
        if ((hijri.month == 9 && hijri.day >= 29) || (hijri.month == 10 && hijri.day == 1)) {
            add(PrayerRow(Prayer.None, tr(R.string.eidPrayer), times.sunrise.local().plusMinutes(30), NotifKey.Eid, Glyph.Mosque))
        }
    }
    val ranges = listOf(
        Prayer.Fajr to (times.fajr to times.dhuhr), Prayer.Dhuhr to (times.dhuhr to times.asr),
        Prayer.Asr to (times.asr to times.maghrib), Prayer.Maghrib to (times.maghrib to times.isha),
        Prayer.Isha to (times.isha to day.tomorrow.fajr),
    )
    val active = ranges.firstOrNull { (_, r) -> nowMillis >= r.first && nowMillis < r.second }?.first

    var dialog by remember { mutableStateOf<ForbiddenRow?>(null) }
    Column {
        SectionLabel(tr(R.string.prayerTimes))
        Spacer(Modifier.height(8.dp))
        rows.forEach { row ->
            val isActive = row.prayer == active
            PrayerTimeCard(row, time(row.start), isActive, if (isActive) Modifier.hero("prayer-focus") else Modifier) {
                nav.push(Route.NotificationEditor(row.key))
            }
            Spacer(Modifier.height(8.dp))
        }
        SectionLabel(tr(R.string.homeForbiddenSalatTimes))
        Spacer(Modifier.height(8.dp))
        val forbidden = listOf(
            ForbiddenRow(tr(R.string.prayerSunrise), time(times.sunrise.local()), time(times.sunrise.local().plusMinutes(15)), Glyph.Twilight),
            ForbiddenRow(tr(R.string.homeNoon), time(times.dhuhr.local().minusMinutes(10)), time(times.dhuhr.local()), Glyph.Sunny),
            ForbiddenRow(tr(R.string.homeSunset), time(times.maghrib.local().minusMinutes(15)), time(times.maghrib.local()), Glyph.Twilight),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            forbidden.forEach { row -> ForbiddenTimeCard(row) { dialog = row } }
        }
        Spacer(Modifier.height(16.dp))
        SectionLabel(tr(R.string.suhoor))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Triple(tr(R.string.homeSuhurEnd), times.fajr.local().minusMinutes(1), Glyph.Twilight),
                Triple(tr(R.string.iftar), times.maghrib.local(), Glyph.Sunny),
                Triple(tr(R.string.homeTahajjud), day.tahajjudStart, Glyph.Nightlight),
            ).forEach { (label, at, icon) -> NightCard(label, time(at), icon, Modifier.weight(1f)) }
        }
    }
    dialog?.let { ForbiddenDialog(it) { dialog = null } }
}

@Composable
private fun SectionLabel(text: String) = Txt(text, style(15.sp, palette.title, FontWeight.SemiBold))

@Composable
private fun PrayerTimeCard(row: PrayerRow, time: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = palette
    val enabled = Settings.notifications.isEnabled(row.key)
    val card by animateColorAsState(if (active) p.selectedCard else p.card, tween(AppMotion.FAST), label = "card")
    Row(
        modifier.fillMaxWidth().pressable(onClick).clip(RoundedCornerShape(12.dp)).background(card).padding(start = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(row.name, style(16.sp, if (active) p.selectedTitle else p.title, FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            Txt(time, style(14.sp, if (active) p.selectedSubtitle else p.subtitle, family = AppFonts.sans).merge(tabular))
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            AppIcon(if (enabled) Glyph.BellFill else Glyph.Bell, if (active) p.selectedTitle else p.title, 20.dp)
        }
    }
}

@Composable
private fun ForbiddenTimeCard(row: ForbiddenRow, onClick: () -> Unit) {
    val p = palette
    Row(
        Modifier.fillMaxWidth().pressable(onClick).clip(RoundedCornerShape(12.dp)).background(p.card).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(row.icon, p.accent, 24.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Txt(row.label, style(16.sp, p.title, FontWeight.SemiBold))
            Spacer(Modifier.height(4.dp))
            Txt("${row.start} – ${row.end}", style(14.sp, p.subtitle, family = AppFonts.sans).merge(tabular))
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { AppIcon(Glyph.InfoCircle, p.title, 20.dp) }
    }
}

@Composable
private fun NightCard(label: String, time: String, icon: Glyph, modifier: Modifier) {
    val p = palette
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(p.card).padding(start = 10.dp, top = 12.dp, end = 10.dp, bottom = 11.dp)) {
        AppIcon(icon, p.accent, 23.dp)
        Spacer(Modifier.height(12.dp))
        Txt(label, style(12.sp, p.subtitle), maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Txt(time, style(14.sp, p.title, FontWeight.Bold, family = AppFonts.sans).merge(tabular), maxLines = 1)
    }
}

@Composable
private fun ForbiddenDialog(row: ForbiddenRow, onDismiss: () -> Unit) {
    val p = palette
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.card).padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(row.icon, p.accent, 26.dp)
                Spacer(Modifier.width(10.dp))
                Txt(row.label, style(19.sp, p.title, FontWeight.Bold))
            }
            Spacer(Modifier.height(16.dp))
            Txt("${row.start} – ${row.end}", style(16.sp, p.title, family = AppFonts.sans).merge(tabular))
            Spacer(Modifier.height(12.dp))
            Txt("Avoid voluntary prayer during this interval.", style(14.sp, p.subtitle, family = AppFonts.sans, lineHeight = 1.35f))
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().height(40.dp).pressable(onDismiss).clip(RoundedCornerShape(12.dp)).border(1.dp, p.subtitle.copy(alpha = .5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Txt(tr(R.string.cancel), style(14.sp, p.accent, FontWeight.Medium)) }
        }
    }
}

// ---------- Notifications ----------

/** Asks for notification permission (Android 13+) when a reminder is turned on, as Flutter's toggleNotification. */
@Composable
private fun rememberNotificationToggle(): (NotifKey, Boolean) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<NotifKey?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.let { if (granted) Settings.setNotificationEnabled(it, true) }
        pending = null
        // As Flutter's requestPermissions: exact alarms are asked for along with notifications (Android 12+).
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Reminders.canScheduleExact(context)) {
            runCatching {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null)),
                )
            }
        }
    }
    return { key, on ->
        val needsPermission = on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pending = key
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Settings.setNotificationEnabled(key, on)
        }
    }
}

private fun soundTitle(language: AppLanguage) = if (language == AppLanguage.Urdu) "آواز" else "Sound"
private fun timingTitle(language: AppLanguage) = if (language == AppLanguage.Urdu) "اطلاع کا وقت" else "Notification Time"
private fun timingLabel(timing: NotificationTiming, language: AppLanguage): String {
    val urdu = language == AppLanguage.Urdu
    if (timing.minutesEarlier == 0) return if (urdu) "وقت پر" else "On time"
    return "${timing.minutesEarlier} ${if (urdu) "منٹ پہلے" else "minutes earlier"}"
}

@Composable
fun NotificationSettingsScreen() {
    val nav = LocalNavigator.current
    val toggle = rememberNotificationToggle()
    val language = Settings.language
    val settings = Settings.notifications
    val all = NotifKey.entries.all(settings::isEnabled)
    Column(Modifier.fillMaxSize().background(palette.background)) {
        AppBar(tr(R.string.notification), onBack = nav::pop)
        CenteredList(bottom = ScreenBottomPadding) {
            item(key = "all") {
                SettingsCard(Glyph.NotificationsActive, tr(R.string.notificationEnabled), tr(R.string.notificationPermissionDescription), switch = all) {
                    NotifKey.entries.forEach { if (settings.isEnabled(it) == all) toggle(it, !all) }
                }
                Spacer(Modifier.height(8.dp))
                SettingsCard(Glyph.Schedule, timingTitle(language), timingLabel(settings.timing, language)) { nav.push(Route.NotificationTiming) }
                Spacer(Modifier.height(16.dp))
                Txt(tr(R.string.prayerAndFastingReminders), style(15.sp, palette.title, FontWeight.SemiBold))
                Spacer(Modifier.height(8.dp))
            }
            NotifKey.entries.forEach { key ->
                item(key = key.stored) {
                    NotificationRow(tr(key.label), settings.isEnabled(key), { toggle(key, !settings.isEnabled(key)) }) {
                        nav.push(Route.NotificationEditor(key))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(title: String, enabled: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    val p = palette
    Row(
        Modifier.fillMaxWidth().pressable(onClick).clip(RoundedCornerShape(12.dp)).background(p.card).padding(start = 14.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(Glyph.Notifications, p.accent, 23.dp)
        Spacer(Modifier.width(12.dp))
        Txt(title, style(15.sp, p.title, FontWeight.SemiBold), Modifier.weight(1f))
        AppSwitch(enabled) { onToggle() }
    }
}

@Composable
fun NotificationEditorScreen(key: NotifKey) {
    val nav = LocalNavigator.current
    val toggle = rememberNotificationToggle()
    val language = Settings.language
    val settings = Settings.notifications
    PushedScreen(tr(key.label)) {
        item(key = "enabled") {
            SettingsCard(Glyph.NotificationsActive, tr(R.string.notificationEnabled), tr(R.string.notificationPermissionDescription), switch = settings.isEnabled(key)) {
                toggle(key, !settings.isEnabled(key))
            }
        }
        item(key = "sound") {
            SettingsCard(Glyph.VolumeUp, soundTitle(language), tr(settings.sound(key).label)) { nav.push(Route.NotificationSound(key)) }
        }
        item(key = "timing") {
            SettingsCard(Glyph.Schedule, timingTitle(language), timingLabel(settings.timing, language)) { nav.push(Route.NotificationTiming) }
        }
    }
}

@Composable
fun NotificationSoundScreen(key: NotifKey) {
    val language = Settings.language
    PushedScreen("${tr(key.label)} · ${soundTitle(language)}") {
        key.availableSounds.forEach { sound ->
            item(key = sound.stored) {
                PickerTile(tr(sound.label), selected = Settings.notifications.sound(key) == sound) { Settings.setNotificationSound(key, sound) }
            }
        }
    }
}

@Composable
fun NotificationTimingScreen() {
    val language = Settings.language
    PushedScreen(timingTitle(language)) {
        NotificationTiming.entries.forEach { timing ->
            item(key = timing.stored) {
                PickerTile(timingLabel(timing, language), selected = Settings.notifications.timing == timing) { Settings.setNotificationTiming(timing) }
            }
        }
    }
}
