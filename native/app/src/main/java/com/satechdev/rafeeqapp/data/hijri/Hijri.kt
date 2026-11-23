package com.satechdev.rafeeqapp.data.hijri

import androidx.annotation.StringRes
import com.satechdev.rafeeqapp.R
import java.time.LocalDate
import kotlin.math.floor

/**
 * A Hijri (Umm al-Qura) date, ported from the Dart `hijri` package 3.0.1
 * that the Flutter app uses, so both show the same day. [weekday] is 1 for
 * Monday to 7 for Sunday, as Dart's.
 */
data class HijriDate(val year: Int, val month: Int, val day: Int, val lengthOfMonth: Int, val weekday: Int) {
    companion object {
        private const val OFFSET = 16260

        /** Dart's HijriCalendar.fromDate. */
        fun of(date: LocalDate): HijriDate {
            var m = date.monthValue
            var y = date.year
            val day = date.dayOfMonth
            if (m < 3) {
                y -= 1
                m += 12
            }
            val a = floor(y / 100.0).toInt()
            val jgc = a - floor(a / 4.0).toInt() - 2
            val cjdn = floor(365.25 * (y + 4716)).toInt() + floor(30.6001 * (m + 1)).toInt() + day - jgc - 1524

            val mcjdn = cjdn - 2400000
            var i = 0
            while (i < ummAlQuraMonthStarts.size) {
                if (start(i) > mcjdn) break
                i++
            }
            val iln = i + OFFSET
            val ii = floor((iln - 1) / 12.0).toInt()
            val wd = Math.floorMod(cjdn + 1, 7)
            return HijriDate(
                year = ii + 1,
                month = iln - 12 * ii,
                day = mcjdn - start(i - 1) + 1,
                lengthOfMonth = start(i) - start(i - 1),
                weekday = if (wd == 0) 7 else wd,
            )
        }

        /** Dart's hijriToGregorian. */
        fun toGregorian(year: Int, month: Int, day: Int): LocalDate {
            val iln = ((year - 1) * 12) + 1 + (month - 1)
            val mcjdn = day + start(iln - OFFSET - 1) - 1
            return julianToGregorian(mcjdn + 2400000)
        }

        /** Days in a Hijri month (Dart's getDaysInMonth). */
        fun daysInMonth(year: Int, month: Int): Int {
            val i = ((year - 1) * 12) + 1 + (month - 1) - OFFSET
            return start(i) - start(i - 1)
        }

        private fun start(index: Int): Int {
            require(index in ummAlQuraMonthStarts.indices) {
                "Valid date should be between 1356 AH (14 March 1937 CE) to 1500 AH (16 November 2077 CE)"
            }
            return ummAlQuraMonthStarts[index]
        }

        private fun julianToGregorian(julianDate: Int): LocalDate {
            val z = floor(julianDate + 0.5).toInt()
            var a = floor((z - 1867216.25) / 36524.25).toInt()
            a = z + 1 + a - floor(a / 4.0).toInt()
            val b = a + 1524
            val c = floor((b - 122.1) / 365.25).toInt()
            val d = floor(365.25 * c).toInt()
            val e = floor((b - d) / 30.6001).toInt()
            val day = b - d - floor(e * 30.6001).toInt()
            val month = e - if (e > 13.5) 13 else 1
            val year = c - if (month > 2.5) 4716 else 4715
            return LocalDate.of(year, month, day)
        }
    }
}

/** The Flutter app's Hijri correction: the Gregorian day is moved before converting. */
enum class HijriCorrection(val stored: String, val days: Long, @StringRes val label: Int) {
    Minus2Days("minus2Days", -2, R.string.hijriMinus2Days),
    Minus1Day("minus1Day", -1, R.string.hijriMinus1Day),
    None("none", 0, R.string.hijriNone),
    Plus1Day("plus1Day", 1, R.string.hijriPlus1Day),
    Plus2Days("plus2Days", 2, R.string.hijriPlus2Days);

    fun hijri(date: LocalDate) = HijriDate.of(date.plusDays(days))

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored } ?: None
    }
}

/** English month names from the Dart package; Urdu ones from lib/utils/hijri_date_formatter.dart. */
object HijriNames {
    val english = listOf(
        "Muharram", "Safar", "Rabi' Al-Awwal", "Rabi' Al-Thani", "Jumada Al-Awwal", "Jumada Al-Thani",
        "Rajab", "Sha'aban", "Ramadan", "Shawwal", "Dhu Al-Qi'dah", "Dhu Al-Hijjah",
    )
    val urdu = listOf(
        "محرم", "صفر", "ربیع الاول", "ربیع الثانی", "جمادی الاول", "جمادی الثانی",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدہ", "ذو الحجہ",
    )
}
