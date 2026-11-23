package com.satechdev.rafeeqapp.data.prayer

import androidx.annotation.StringRes
import com.satechdev.rafeeqapp.R
import java.time.LocalDate

/*
 * The Flutter app's prayer settings enums (lib/ui/prayer_times/prayer_times_settings.dart),
 * stored by the same names, in the same order, with the same labels.
 */

enum class PrayerCalcMethod(val stored: String, @StringRes val label: Int) {
    MuslimWorldLeague("muslim_world_league", R.string.calcMethodMWL),
    Egyptian("egyptian", R.string.calcMethodEgyptian),
    Karachi("karachi", R.string.calcMethodKarachi),
    UmmAlQura("umm_al_qura", R.string.calcMethodUmmAlQura),
    Dubai("dubai", R.string.calcMethodDubai),
    MoonSightingCommittee("moon_sighting_committee", R.string.calcMethodMoonsighting),
    NorthAmerica("north_america", R.string.calcMethodNorthAmerica),
    Kuwait("kuwait", R.string.calcMethodKuwait),
    Qatar("qatar", R.string.calcMethodQatar),
    Singapore("singapore", R.string.calcMethodSingapore),
    Turkey("turkey", R.string.calcMethodTurkey),
    Tehran("tehran", R.string.calcMethodTehran),
    Jafari("jafari", R.string.calcMethodJafari);

    /** lib/ui/app_home/home/home_prayer_data.dart: calculationParametersFor. */
    fun parameters(): CalculationParameters = when (this) {
        MuslimWorldLeague -> CalculationMethod.MuslimWorldLeague.parameters()
        Egyptian -> CalculationMethod.Egyptian.parameters()
        Karachi -> CalculationMethod.Karachi.parameters()
        UmmAlQura -> CalculationMethod.UmmAlQura.parameters()
        Dubai -> CalculationMethod.Dubai.parameters()
        MoonSightingCommittee -> CalculationMethod.MoonSightingCommittee.parameters()
        NorthAmerica -> CalculationMethod.NorthAmerica.parameters()
        Kuwait -> CalculationMethod.Kuwait.parameters()
        Qatar -> CalculationMethod.Qatar.parameters()
        Singapore -> CalculationMethod.Singapore.parameters()
        Turkey -> CalculationMethod.Turkey.parameters()
        Tehran -> CalculationMethod.Tehran.parameters()
        Jafari -> CalculationParameters(CalculationMethod.Other, 16.0, maghribAngle = 4.0, ishaAngle = 14.0)
    }

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored } ?: Karachi
    }
}

enum class AsrMethod(val stored: String, @StringRes val label: Int) {
    Hanafi("hanafi", R.string.asrMethodHanafi),
    Standard("others", R.string.asrMethodStandard);

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored } ?: Hanafi
    }
}

enum class HighLatitude(val stored: String, @StringRes val label: Int, val rule: HighLatitudeRule) {
    AngleBased("angleBased", R.string.highLatAngleBased, HighLatitudeRule.TwilightAngle),
    MiddleOfTheNight("middleOfTheNight", R.string.highLatMiddleNight, HighLatitudeRule.MiddleOfTheNight),
    OneSeventh("oneSeventh", R.string.highLatOneSeventh, HighLatitudeRule.SeventhOfTheNight);

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored } ?: AngleBased
    }
}

enum class TimeFormat(val stored: String, @StringRes val label: Int) {
    TwelveHour("twelveHour", R.string.timeFormat12Hour),
    TwentyFourHour("twentyFourHour", R.string.timeFormat24Hour);

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored } ?: TwelveHour
    }
}

/** The Flutter app's UserLocation (lib/data/models/user_location.dart). */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val city: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    /** "automatic" or "manual". */
    val source: String? = null,
) {
    val coordinates get() = Coordinates(latitude, longitude)
}

/** Prayer times for [date] at [location], with the Flutter app's parameters. */
fun prayerTimes(
    location: UserLocation,
    date: LocalDate,
    method: PrayerCalcMethod,
    asr: AsrMethod,
    highLatitude: HighLatitude,
): PrayerTimes {
    // lib/ui/app_home/home/home_prayer_data.dart: madhabFor. Jafari always uses the single shadow.
    val madhab = if (method == PrayerCalcMethod.Jafari || asr == AsrMethod.Standard) Madhab.Shafi else Madhab.Hanafi
    val parameters = method.parameters().copy(madhab = madhab, highLatitudeRule = highLatitude.rule)
    return PrayerTimes(location.coordinates, DateComponents(date.year, date.monthValue, date.dayOfMonth), parameters)
}
