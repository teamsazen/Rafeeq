package com.satechdev.rafeeqapp.ui

import com.satechdev.rafeeqapp.data.AppLanguage
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.hijri.HijriDate
import com.satechdev.rafeeqapp.data.hijri.HijriNames

private const val URDU_DIGITS = "۰۱۲۳۴۵۶۷۸۹"

/** Western digits in [text] as Urdu ones in Urdu (lib/utils/extensions/number_localization.dart). */
fun localDigits(text: String, language: AppLanguage = Settings.language): String =
    if (language != AppLanguage.Urdu) text else buildString {
        text.forEach { append(if (it in '0'..'9') URDU_DIGITS[it - '0'] else it) }
    }

fun localNumber(n: Int, language: AppLanguage = Settings.language) = localDigits(n.toString(), language)

/** "DD MonthName", as HijriDateFormatter.formatShort. */
fun hijriShort(date: HijriDate, language: AppLanguage = Settings.language): String =
    "${localNumber(date.day, language)} ${hijriMonth(date.month, language)}"

/** "DD MonthName, YYYY", as HijriDateFormatter.formatLong. */
fun hijriLong(date: HijriDate, language: AppLanguage = Settings.language): String =
    "${hijriShort(date, language)}, ${localNumber(date.year, language)}"

fun hijriMonth(month: Int, language: AppLanguage = Settings.language): String =
    (if (language == AppLanguage.Urdu) HijriNames.urdu else HijriNames.english)[month - 1]
