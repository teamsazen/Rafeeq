package com.satechdev.rafeeqapp.data.prayer

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/*
 * A line-for-line Kotlin port of the Dart `adhan` package 2.0.0+1 (MIT,
 * Riajul Islam, after Batoul Apps' Adhan), which the Flutter app uses, so the
 * native app shows the very same times. Times are epoch milliseconds.
 *
 * Dart rounding is kept where it matters: `round()` rounds halves away from
 * zero and `~/` truncates toward zero.
 *
 * Two deliberate differences, both where Dart throws and the Flutter app
 * fails: "twilight angle" with a method that has an Isha interval and no Isha
 * angle (Umm al-Qura, Qatar) uses an Isha night portion of 0, which those
 * methods never read; and a Maghrib angle the sun never reaches keeps Maghrib
 * at sunset. tool/reference checks every other case against Dart.
 */

data class Coordinates(val latitude: Double, val longitude: Double)

enum class Madhab(val shadowLength: Double) { Shafi(1.0), Hanafi(2.0) }

enum class HighLatitudeRule { MiddleOfTheNight, SeventhOfTheNight, TwilightAngle }

enum class Prayer { None, Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha }

data class PrayerAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0,
)

enum class CalculationMethod {
    MuslimWorldLeague, Egyptian, Karachi, UmmAlQura, Dubai, MoonSightingCommittee,
    NorthAmerica, Kuwait, Qatar, Singapore, Turkey, Tehran, Other;

    fun parameters(): CalculationParameters = when (this) {
        MuslimWorldLeague -> CalculationParameters(this, 18.0, ishaAngle = 17.0, methodAdjustments = PrayerAdjustments(dhuhr = 1))
        Egyptian -> CalculationParameters(this, 19.5, ishaAngle = 17.5, methodAdjustments = PrayerAdjustments(dhuhr = 1))
        Karachi -> CalculationParameters(this, 18.0, ishaAngle = 18.0, methodAdjustments = PrayerAdjustments(dhuhr = 1))
        UmmAlQura -> CalculationParameters(this, 18.5, ishaInterval = 90)
        Dubai -> CalculationParameters(this, 18.2, ishaAngle = 18.2, methodAdjustments = PrayerAdjustments(sunrise = -3, dhuhr = 3, asr = 3, maghrib = 3))
        MoonSightingCommittee -> CalculationParameters(this, 18.0, ishaAngle = 18.0, methodAdjustments = PrayerAdjustments(dhuhr = 5, maghrib = 3))
        NorthAmerica -> CalculationParameters(this, 15.0, ishaAngle = 15.0, methodAdjustments = PrayerAdjustments(dhuhr = 1))
        Kuwait -> CalculationParameters(this, 18.0, ishaAngle = 17.5)
        Qatar -> CalculationParameters(this, 18.0, ishaInterval = 90)
        Singapore -> CalculationParameters(this, 20.0, ishaAngle = 18.0, methodAdjustments = PrayerAdjustments(dhuhr = 1))
        Turkey -> CalculationParameters(this, 18.0, ishaAngle = 17.0, methodAdjustments = PrayerAdjustments(sunrise = -7, dhuhr = 5, asr = 4, maghrib = 7))
        Tehran -> CalculationParameters(this, 17.7, ishaAngle = 14.0, maghribAngle = 4.5)
        Other -> CalculationParameters(this, 0.0, ishaAngle = 0.0)
    }
}

data class CalculationParameters(
    val method: CalculationMethod = CalculationMethod.Other,
    val fajrAngle: Double,
    val maghribAngle: Double? = null,
    val ishaAngle: Double? = null,
    val ishaInterval: Int = 0,
    val madhab: Madhab = Madhab.Shafi,
    val highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MiddleOfTheNight,
    val adjustments: PrayerAdjustments = PrayerAdjustments(),
    val methodAdjustments: PrayerAdjustments = PrayerAdjustments(),
) {
    internal fun nightPortions(): Pair<Double, Double> = when (highLatitudeRule) {
        HighLatitudeRule.MiddleOfTheNight -> 1.0 / 2.0 to 1.0 / 2.0
        HighLatitudeRule.SeventhOfTheNight -> 1.0 / 7.0 to 1.0 / 7.0
        HighLatitudeRule.TwilightAngle -> fajrAngle / 60.0 to (ishaAngle ?: 0.0) / 60.0
    }
}

/** A calendar day, as the Dart `DateComponents`. */
data class DateComponents(val year: Int, val month: Int, val day: Int)

private const val DAY = 86_400_000L
private const val HOUR = 3_600_000L
private const val MINUTE = 60_000L
private const val SECOND = 1_000L

class PrayerTimes(
    val coordinates: Coordinates,
    val dateComponents: DateComponents,
    val parameters: CalculationParameters,
    /** Used for the day of the year, as Dart's `DateTime(year)` is in local time. */
    private val zone: TimeZone = TimeZone.getDefault(),
) {
    val fajr: Long
    val sunrise: Long
    val dhuhr: Long
    val asr: Long
    val maghrib: Long
    val isha: Long

    init {
        val date = utcMidnight(dateComponents)
        val year = dateComponents.year
        val dayOfYear = dayOfYear(date, year, zone)
        val solarTime = SolarTime(date, coordinates)

        val transit = componentsAt(date, solarTime.transit)
        val sunriseComponents = componentsAt(date, solarTime.sunrise)
        val sunsetComponents = componentsAt(date, solarTime.sunset)
        val tomorrow = date + DAY
        val tomorrowSolarTime = SolarTime(tomorrow, coordinates)
        val tomorrowSunrise = componentsAt(tomorrow, tomorrowSolarTime.sunrise)

        val tempDhuhr = transit
        val tempSunrise = sunriseComponents
        val tempAsr = componentsAt(date, solarTime.afternoon(parameters.madhab.shadowLength))
        val night = tomorrowSunrise - sunsetComponents

        var tempFajr: Long? = null
        var value = solarTime.hourAngle(-parameters.fajrAngle, false)
        if (value.isFinite()) tempFajr = componentsAt(date, value)
        if (parameters.method == CalculationMethod.MoonSightingCommittee && coordinates.latitude >= 55) {
            tempFajr = sunriseComponents + (-1 * night / 7000) * SECOND
        }

        val (fajrPortion, ishaPortion) = parameters.nightPortions()
        val safeFajr = if (parameters.method == CalculationMethod.MoonSightingCommittee) {
            seasonAdjustedMorningTwilight(coordinates.latitude, dayOfYear, year, sunriseComponents)
        } else {
            val nightFraction = (fajrPortion * night / 1000).toLong()
            sunriseComponents - nightFraction * SECOND
        }
        if (tempFajr == null || tempFajr < safeFajr) tempFajr = safeFajr

        var tempIsha: Long? = null
        if (parameters.ishaInterval > 0) {
            tempIsha = sunsetComponents + parameters.ishaInterval * 60 * SECOND
        } else {
            value = solarTime.hourAngle(-parameters.ishaAngle!!, true)
            if (value.isFinite()) tempIsha = componentsAt(date, value)
            if (parameters.method == CalculationMethod.MoonSightingCommittee && coordinates.latitude >= 55) {
                tempIsha = sunsetComponents + (night / 7000) * SECOND
            }
            val safeIsha = if (parameters.method == CalculationMethod.MoonSightingCommittee) {
                seasonAdjustedEveningTwilight(coordinates.latitude, dayOfYear, year, sunsetComponents)
            } else {
                val nightFraction = (ishaPortion * night / 1000).toLong()
                sunsetComponents + nightFraction * SECOND
            }
            if (tempIsha == null || tempIsha > safeIsha) tempIsha = safeIsha
        }

        var tempMaghrib = sunsetComponents
        parameters.maghribAngle?.let { angle ->
            // Dart throws when the sun never gets this low (Reykjavik in June); Maghrib stays at sunset.
            val hour = solarTime.hourAngle(-1 * angle, true)
            if (hour.isFinite()) {
                val angleBased = componentsAt(date, hour)
                if (sunsetComponents < angleBased && tempIsha!! > angleBased) tempMaghrib = angleBased
            }
        }

        val a = parameters.adjustments
        val m = parameters.methodAdjustments
        fajr = roundedMinute(tempFajr + (a.fajr + m.fajr) * MINUTE)
        sunrise = roundedMinute(tempSunrise + (a.sunrise + m.sunrise) * MINUTE)
        dhuhr = roundedMinute(tempDhuhr + (a.dhuhr + m.dhuhr) * MINUTE)
        asr = roundedMinute(tempAsr + (a.asr + m.asr) * MINUTE)
        maghrib = roundedMinute(tempMaghrib + (a.maghrib + m.maghrib) * MINUTE)
        isha = roundedMinute(tempIsha!! + (a.isha + m.isha) * MINUTE)
    }

    fun timeFor(prayer: Prayer): Long? = when (prayer) {
        Prayer.Fajr -> fajr
        Prayer.Sunrise -> sunrise
        Prayer.Dhuhr -> dhuhr
        Prayer.Asr -> asr
        Prayer.Maghrib -> maghrib
        Prayer.Isha -> isha
        Prayer.None -> null
    }

    fun currentPrayer(now: Long): Prayer = when {
        isha - now <= 0 -> Prayer.Isha
        maghrib - now <= 0 -> Prayer.Maghrib
        asr - now <= 0 -> Prayer.Asr
        dhuhr - now <= 0 -> Prayer.Dhuhr
        sunrise - now <= 0 -> Prayer.Sunrise
        fajr - now <= 0 -> Prayer.Fajr
        else -> Prayer.None
    }

    fun nextPrayer(now: Long): Prayer = when {
        isha - now <= 0 -> Prayer.None
        maghrib - now <= 0 -> Prayer.Isha
        asr - now <= 0 -> Prayer.Maghrib
        dhuhr - now <= 0 -> Prayer.Asr
        sunrise - now <= 0 -> Prayer.Dhuhr
        fajr - now <= 0 -> Prayer.Sunrise
        else -> Prayer.Fajr
    }

    companion object {
        fun utcMidnight(d: DateComponents): Long =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
                clear()
                set(d.year, d.month - 1, d.day)
                timeInMillis
            }

        /** Dart: `date.difference(DateTime(year)).inDays + 1`, with `DateTime(year)` in local time. */
        internal fun dayOfYear(date: Long, year: Int, zone: TimeZone): Int {
            val yearStart = Calendar.getInstance(zone).run {
                clear()
                set(year, Calendar.JANUARY, 1)
                timeInMillis
            }
            return ((date - yearStart) / DAY).toInt() + 1
        }

        /** Dart's TimeComponents.fromDouble(value).dateComponents(date). */
        internal fun componentsAt(date: Long, value: Double): Long {
            require(value.isFinite()) { "value should not be infinite or NaN" }
            val hours = floor(value)
            val minutes = floor((value - hours) * 60.0)
            val seconds = floor((value - (hours + minutes / 60.0)) * 60 * 60)
            return date + hours.toLong() * HOUR + minutes.toLong() * MINUTE + seconds.toLong() * SECOND
        }

        /** Dart's CalendarUtil.roundedMinute: seconds of 30 or more round up. */
        internal fun roundedMinute(time: Long): Long {
            val seconds = Math.floorMod(time / SECOND, 60L)
            val wholeMinute = time - Math.floorMod(time, MINUTE)
            return wholeMinute + if (seconds >= 30) MINUTE else 0
        }

        private fun seasonAdjustedMorningTwilight(latitude: Double, day: Int, year: Int, sunrise: Long): Long {
            val a = 75 + ((28.65 / 55.0) * abs(latitude))
            val b = 75 + ((19.44 / 55.0) * abs(latitude))
            val c = 75 + ((32.74 / 55.0) * abs(latitude))
            val d = 75 + ((48.10 / 55.0) * abs(latitude))
            val adjustment = season(a, b, c, d, daysSinceSolstice(day, year, latitude))
            return sunrise - dartRound(adjustment * 60.0).toLong() * SECOND
        }

        private fun seasonAdjustedEveningTwilight(latitude: Double, day: Int, year: Int, sunset: Long): Long {
            val a = 75 + ((25.60 / 55.0) * abs(latitude))
            val b = 75 + ((2.050 / 55.0) * abs(latitude))
            val c = 75 - ((9.210 / 55.0) * abs(latitude))
            val d = 75 + ((6.140 / 55.0) * abs(latitude))
            val adjustment = season(a, b, c, d, daysSinceSolstice(day, year, latitude))
            return sunset + dartRound(adjustment * 60.0).toLong() * SECOND
        }

        private fun season(a: Double, b: Double, c: Double, d: Double, dyy: Int): Double = when {
            dyy < 91 -> a + (b - a) / 91.0 * dyy
            dyy < 137 -> b + (c - b) / 46.0 * (dyy - 91)
            dyy < 183 -> c + (d - c) / 46.0 * (dyy - 137)
            dyy < 229 -> d + (c - d) / 46.0 * (dyy - 183)
            dyy < 275 -> c + (b - c) / 46.0 * (dyy - 229)
            else -> b + (a - b) / 91.0 * (dyy - 275)
        }

        internal fun daysSinceSolstice(dayOfYear: Int, year: Int, latitude: Double): Int {
            val leap = year % 4 == 0 && !(year % 100 == 0 && year % 400 != 0)
            val southernOffset = if (leap) 173 else 172
            val daysInYear = if (leap) 366 else 365
            return if (latitude >= 0) {
                val d = dayOfYear + 10
                if (d >= daysInYear) d - daysInYear else d
            } else {
                val d = dayOfYear - southernOffset
                if (d < 0) d + daysInYear else d
            }
        }
    }
}

/** Dart's SunnahTimes: the middle and last third of the night after [today]. */
class SunnahTimes(today: PrayerTimes) {
    val middleOfTheNight: Long
    val lastThirdOfTheNight: Long

    init {
        val next = PrayerTimes.utcMidnight(today.dateComponents) + 86_400_000L
        val tomorrow = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
            timeInMillis = next
            PrayerTimes(today.coordinates, DateComponents(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH)), today.parameters)
        }
        val nightSeconds = (tomorrow.fajr - today.maghrib) / 1000
        middleOfTheNight = PrayerTimes.roundedMinute(today.maghrib + (nightSeconds / 2) * 1000)
        lastThirdOfTheNight = PrayerTimes.roundedMinute(today.maghrib + (nightSeconds * (2.0 / 3.0)).toLong() * 1000)
    }
}

/** Dart's Qibla: the direction of the Kaaba from [coordinates], in degrees from north. */
fun qiblaDirection(coordinates: Coordinates): Double {
    val makkahLatitude = 21.4225241
    val makkahLongitude = 39.8261818
    val longitudeDelta = rad(makkahLongitude) - rad(coordinates.longitude)
    val latitudeRadians = rad(coordinates.latitude)
    val term1 = sin(longitudeDelta)
    val term2 = cos(latitudeRadians) * tan(rad(makkahLatitude))
    val term3 = sin(latitudeRadians) * cos(longitudeDelta)
    return unwindAngle(deg(atan2(term1, term2 - term3)))
}

// ---------- Internals ----------

private fun rad(degrees: Double) = degrees * (Math.PI / 180.0)
private fun deg(radians: Double) = radians * (180.0 / Math.PI)

private fun normalizeWithBound(value: Double, max: Double) = value - (max * floor(value / max))
private fun unwindAngle(value: Double) = normalizeWithBound(value, 360.0)
private fun closestAngle(angle: Double): Double =
    if (angle >= -180 && angle <= 180) angle else angle - (360 * dartRound(angle / 360))

/** Dart's round() and roundToDouble(): halves go away from zero (Kotlin's round() goes to even). */
internal fun dartRound(x: Double): Double = if (x >= 0) floor(x + 0.5) else -floor(-x + 0.5)

private fun julianDay(year: Int, month: Int, day: Int, hours: Double = 0.0): Double {
    val y = if (month > 2) year else year - 1
    val m = if (month > 2) month else month + 12
    val d = day + (hours / 24)
    val a = y / 100
    val b = (2 - a + (a / 4.0)).toInt()
    val i0 = (365.25 * (y + 4716)).toInt()
    val i1 = (30.6001 * (m + 1)).toInt()
    return i0 + i1 + d + b - 1524.5
}

/** Dart's julianDayByDate on a UTC midnight (hours and minutes are 0). */
private fun julianDayAt(utcMillis: Long): Double = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
    timeInMillis = utcMillis
    julianDay(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH), (get(Calendar.HOUR_OF_DAY) + get(Calendar.MINUTE)) / 60.0)
}

private fun julianCentury(jd: Double) = (jd - 2451545.0) / 36525

private class SolarCoordinates(julianDay: Double) {
    val declination: Double
    val rightAscension: Double
    val apparentSiderealTime: Double

    init {
        val t = julianCentury(julianDay)
        val l0 = Astronomical.meanSolarLongitude(t)
        val lp = Astronomical.meanLunarLongitude(t)
        val omega = Astronomical.ascendingLunarNodeLongitude(t)
        val lambda = rad(Astronomical.apparentSolarLongitude(t, l0))
        val theta0 = Astronomical.meanSiderealTime(t)
        val deltaPsi = Astronomical.nutationInLongitude(l0, lp, omega)
        val deltaEpsilon = Astronomical.nutationInObliquity(l0, lp, omega)
        val epsilon0 = Astronomical.meanObliquityOfTheEcliptic(t)
        val epsilonApp = rad(Astronomical.apparentObliquityOfTheEcliptic(t, epsilon0))
        declination = deg(asin(sin(epsilonApp) * sin(lambda)))
        rightAscension = unwindAngle(deg(atan2(cos(epsilonApp) * sin(lambda), cos(lambda))))
        apparentSiderealTime = theta0 + (((deltaPsi * 3600) * cos(rad(epsilon0 + deltaEpsilon))) / 3600)
    }
}

private class SolarTime(today: Long, private val observer: Coordinates) {
    val transit: Double
    val sunrise: Double
    val sunset: Double
    private val solar = SolarCoordinates(julianDayAt(today))
    private val prevSolar = SolarCoordinates(julianDayAt(today - DAY))
    private val nextSolar = SolarCoordinates(julianDayAt(today + DAY))
    private val approximateTransit =
        Astronomical.approximateTransit(observer.longitude, solar.apparentSiderealTime, solar.rightAscension)

    init {
        val solarAltitude = -50.0 / 60.0
        transit = Astronomical.correctedTransit(
            approximateTransit, observer.longitude, solar.apparentSiderealTime,
            solar.rightAscension, prevSolar.rightAscension, nextSolar.rightAscension,
        )
        sunrise = hourAngle(solarAltitude, false)
        sunset = hourAngle(solarAltitude, true)
    }

    fun hourAngle(angle: Double, afterTransit: Boolean): Double = Astronomical.correctedHourAngle(
        approximateTransit, angle, observer, afterTransit, solar.apparentSiderealTime,
        solar.rightAscension, prevSolar.rightAscension, nextSolar.rightAscension,
        solar.declination, prevSolar.declination, nextSolar.declination,
    )

    fun afternoon(shadowLength: Double): Double {
        val tangent = abs(observer.latitude - solar.declination)
        val inverse = shadowLength + tan(rad(tangent))
        val angle = deg(atan(1.0 / inverse))
        return hourAngle(angle, true)
    }
}

private object Astronomical {
    fun meanSolarLongitude(t: Double) = unwindAngle(280.4664567 + 36000.76983 * t + 0.0003032 * t.pow(2))

    fun meanLunarLongitude(t: Double) = unwindAngle(218.3165 + 481267.8813 * t)

    fun apparentSolarLongitude(t: Double, l0: Double): Double {
        val longitude = l0 + solarEquationOfTheCenter(t, meanSolarAnomaly(t))
        val omega = 125.04 - (1934.136 * t)
        return unwindAngle(longitude - 0.00569 - (0.00478 * sin(rad(omega))))
    }

    fun ascendingLunarNodeLongitude(t: Double) =
        unwindAngle(125.04452 - 1934.136261 * t + 0.0020708 * t.pow(2) + t.pow(3) / 450000)

    fun meanSolarAnomaly(t: Double) = unwindAngle(357.52911 + 35999.05029 * t - 0.0001537 * t.pow(2))

    fun solarEquationOfTheCenter(t: Double, m: Double): Double {
        val mRad = rad(m)
        val term1 = (1.914602 - (0.004817 * t) - (0.000014 * t.pow(2))) * sin(mRad)
        val term2 = (0.019993 - (0.000101 * t)) * sin(2 * mRad)
        val term3 = 0.000289 * sin(3 * mRad)
        return term1 + term2 + term3
    }

    fun meanObliquityOfTheEcliptic(t: Double) =
        23.439291 - 0.013004167 * t - 0.0000001639 * t.pow(2) + 0.0000005036 * t.pow(3)

    fun apparentObliquityOfTheEcliptic(t: Double, epsilon0: Double): Double {
        val o = 125.04 - (1934.136 * t)
        return epsilon0 + (0.00256 * cos(rad(o)))
    }

    fun meanSiderealTime(t: Double): Double {
        val jd = (t * 36525) + 2451545.0
        val theta = 280.46061837 + 360.98564736629 * (jd - 2451545) + 0.000387933 * t.pow(2) - t.pow(3) / 38710000
        return unwindAngle(theta)
    }

    fun nutationInLongitude(l0: Double, lp: Double, omega: Double): Double {
        val term1 = (-17.2 / 3600) * sin(rad(omega))
        val term2 = (1.32 / 3600) * sin(2 * rad(l0))
        val term3 = (0.23 / 3600) * sin(2 * rad(lp))
        val term4 = (0.21 / 3600) * sin(2 * rad(omega))
        return term1 - term2 - term3 + term4
    }

    fun nutationInObliquity(l0: Double, lp: Double, omega: Double): Double {
        val term1 = (9.2 / 3600) * cos(rad(omega))
        val term2 = (0.57 / 3600) * cos(2 * rad(l0))
        val term3 = (0.10 / 3600) * cos(2 * rad(lp))
        val term4 = (0.09 / 3600) * cos(2 * rad(omega))
        return term1 + term2 + term3 - term4
    }

    fun altitudeOfCelestialBody(phi: Double, delta: Double, h: Double): Double {
        val term1 = sin(rad(phi)) * sin(rad(delta))
        val term2 = cos(rad(phi)) * cos(rad(delta)) * cos(rad(h))
        return deg(asin(term1 + term2))
    }

    fun approximateTransit(l: Double, theta0: Double, alpha2: Double): Double {
        val lw = l * -1
        return normalizeWithBound((alpha2 + lw - theta0) / 360, 1.0)
    }

    fun correctedTransit(m0: Double, l: Double, theta0: Double, alpha2: Double, alpha1: Double, alpha3: Double): Double {
        val lw = l * -1
        val theta = unwindAngle(theta0 + (360.985647 * m0))
        val alpha = unwindAngle(interpolateAngles(alpha2, alpha1, alpha3, m0))
        val h = closestAngle(theta - lw - alpha)
        val deltaM = h / -360
        return (m0 + deltaM) * 24
    }

    fun correctedHourAngle(
        m0: Double, h0: Double, coordinates: Coordinates, afterTransit: Boolean, theta0: Double,
        alpha2: Double, alpha1: Double, alpha3: Double, delta2: Double, delta1: Double, delta3: Double,
    ): Double {
        val lw = coordinates.longitude * -1
        val term1 = sin(rad(h0)) - (sin(rad(coordinates.latitude)) * sin(rad(delta2)))
        val term2 = cos(rad(coordinates.latitude)) * cos(rad(delta2))
        val h0Angle = deg(acos(term1 / term2))
        val m = if (afterTransit) m0 + (h0Angle / 360) else m0 - (h0Angle / 360)
        val theta = unwindAngle(theta0 + (360.985647 * m))
        val alpha = unwindAngle(interpolateAngles(alpha2, alpha1, alpha3, m))
        val delta = interpolate(delta2, delta1, delta3, m)
        val h = (theta - lw - alpha)
        val altitude = altitudeOfCelestialBody(coordinates.latitude, delta, h)
        val term3 = altitude - h0
        val term4 = 360 * cos(rad(delta)) * cos(rad(coordinates.latitude)) * sin(rad(h))
        val deltaM = term3 / term4
        return (m + deltaM) * 24
    }

    fun interpolate(y2: Double, y1: Double, y3: Double, n: Double): Double {
        val a = y2 - y1
        val b = y3 - y2
        val c = b - a
        return y2 + ((n / 2) * (a + b + (n * c)))
    }

    fun interpolateAngles(y2: Double, y1: Double, y3: Double, n: Double): Double {
        val a = unwindAngle(y2 - y1)
        val b = unwindAngle(y3 - y2)
        val c = b - a
        return y2 + ((n / 2) * (a + b + (n * c)))
    }
}
