package com.satechdev.rafeeqapp.data.prayer

import androidx.annotation.StringRes
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.AppLanguage
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/** A prayer time as a local date-time. */
fun Long.local(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

/** The name of [prayer]; Dhuhr is Jumma on Fridays, as on the Flutter home screen. */
@StringRes
fun prayerName(prayer: Prayer, today: LocalDate = LocalDate.now()): Int = when (prayer) {
    Prayer.Fajr -> R.string.prayerFajr
    Prayer.Sunrise -> R.string.prayerSunrise
    Prayer.Dhuhr -> if (today.dayOfWeek == DayOfWeek.FRIDAY) R.string.prayerJumma else R.string.prayerDhuhr
    Prayer.Asr -> R.string.prayerAsr
    Prayer.Maghrib -> R.string.prayerMaghrib
    Prayer.Isha -> R.string.prayerIsha
    Prayer.None -> R.string.prayerNone
}

private val twelveHour = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
private val twentyFourHour = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

/**
 * "06:31 PM" or "18:31" in English digits, as TimeFormatOption.formatTime. In
 * Urdu the time is isolated left to right so AM/PM stays after the digits.
 */
fun formatTime(time: LocalDateTime?, format: TimeFormat, language: AppLanguage): String {
    if (time == null) return "--:-- --"
    val value = (if (format == TimeFormat.TwelveHour) twelveHour else twentyFourHour).format(time)
    return if (language.rtl) "⁦$value⁩" else value
}

/** "HH:MM:SS", never negative. */
fun formatCountdown(duration: Duration?): String {
    if (duration == null) return "--:--:--"
    val safe = if (duration.isNegative) Duration.ZERO else duration
    val seconds = safe.seconds
    return "%02d:%02d:%02d".format(Locale.ENGLISH, seconds / 3600, seconds / 60 % 60, seconds % 60)
}

/**
 * The home screen's view of the day (Flutter's _HomePrayerSnapshot): the
 * prayer just passed, the next one, how far along between them, and when
 * Tahajjud starts.
 */
class PrayerDay(
    val yesterday: PrayerTimes,
    val today: PrayerTimes,
    val tomorrow: PrayerTimes,
) {
    fun previousAndNext(now: LocalDateTime): Pair<Pair<Prayer, LocalDateTime>, Pair<Prayer, LocalDateTime>> {
        val prayers = listOf(
            Prayer.Fajr to today.fajr.local(),
            Prayer.Dhuhr to today.dhuhr.local(),
            Prayer.Asr to today.asr.local(),
            Prayer.Maghrib to today.maghrib.local(),
            Prayer.Isha to today.isha.local(),
        )
        var previous = Prayer.Isha to yesterday.isha.local()
        var next = prayers.first()
        for (prayer in prayers) {
            if (!prayer.second.isAfter(now)) {
                previous = prayer
            } else {
                next = prayer
                break
            }
        }
        if (!now.isBefore(today.isha.local())) {
            previous = Prayer.Isha to today.isha.local()
            next = Prayer.Fajr to tomorrow.fajr.local()
        }
        return previous to next
    }

    fun progress(now: LocalDateTime): Float {
        val (previous, next) = previousAndNext(now)
        val interval = Duration.between(previous.second, next.second).toMillis()
        val elapsed = Duration.between(previous.second, now).toMillis()
        return if (interval <= 0) 0f else (elapsed.toFloat() / interval).coerceIn(0f, 1f)
    }

    /** Two thirds of the way from Maghrib to tomorrow's Fajr. */
    val tahajjudStart: LocalDateTime
        get() {
            val night = tomorrow.fajr - today.maghrib
            return (today.maghrib + (night * 2 / 3.0).roundToLong()).local()
        }

    companion object {
        fun of(location: UserLocation, date: LocalDate, method: PrayerCalcMethod, asr: AsrMethod, highLatitude: HighLatitude) =
            PrayerDay(
                prayerTimes(location, date.minusDays(1), method, asr, highLatitude),
                prayerTimes(location, date, method, asr, highLatitude),
                prayerTimes(location, date.plusDays(1), method, asr, highLatitude),
            )
    }
}
