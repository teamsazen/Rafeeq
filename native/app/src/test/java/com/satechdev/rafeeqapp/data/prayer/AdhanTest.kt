package com.satechdev.rafeeqapp.data.prayer

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * The Kotlin port against the Dart adhan package the Flutter app uses
 * (resources/adhan_reference.json, from tool/adhan_reference).
 */
class AdhanTest {
    private val zone = TimeZone.getTimeZone("America/New_York")
    private val reference = JSONObject(
        javaClass.classLoader!!.getResource("adhan_reference.json").readText(),
    )

    private fun parameters(method: String): CalculationParameters = when (method) {
        "jafari" -> CalculationParameters(CalculationMethod.Other, 16.0, maghribAngle = 4.0, ishaAngle = 14.0)
        else -> CalculationMethod.valueOf(method.split('_').joinToString("") { it.replaceFirstChar(Char::uppercase) }).parameters()
    }

    private val places = mapOf(
        "jhelum" to Coordinates(32.9405, 73.7276), "karachi" to Coordinates(24.8607, 67.0011),
        "makkah" to Coordinates(21.4225, 39.8262), "london" to Coordinates(51.5072, -0.1276),
        "oslo" to Coordinates(59.9139, 10.7522), "newyork" to Coordinates(40.7128, -74.0060),
        "sydney" to Coordinates(-33.8688, 151.2093), "reykjavik" to Coordinates(64.1466, -21.9426),
    )

    @Test
    fun matchesDartForEveryCase() {
        val cases = reference.getJSONArray("cases")
        var compared = 0
        var dartFailed = 0
        for (i in 0 until cases.length()) {
            val c: JSONArray = cases.getJSONArray(i)
            val (y, m, d) = c.getString(1).split('-').map(String::toInt)
            val params = parameters(c.getString(2)).copy(
                madhab = if (c.getString(3) == "hanafi") Madhab.Hanafi else Madhab.Shafi,
                highLatitudeRule = when (c.getString(4)) {
                    "middle_of_the_night" -> HighLatitudeRule.MiddleOfTheNight
                    "seventh_of_the_night" -> HighLatitudeRule.SeventhOfTheNight
                    else -> HighLatitudeRule.TwilightAngle
                },
            )
            // Where Dart throws, the port must still give times (see Adhan.kt).
            val times = PrayerTimes(places.getValue(c.getString(0)), DateComponents(y, m, d), params, zone)
            if (c.get(5) == "error") {
                dartFailed++
                assertTrue(times.fajr < times.sunrise && times.maghrib <= times.isha)
                continue
            }
            val label = c.toString()
            assertEquals(label, c.getLong(5), times.fajr)
            assertEquals(label, c.getLong(6), times.sunrise)
            assertEquals(label, c.getLong(7), times.dhuhr)
            assertEquals(label, c.getLong(8), times.asr)
            assertEquals(label, c.getLong(9), times.maghrib)
            assertEquals(label, c.getLong(10), times.isha)
            compared++
        }
        assertEquals(3540, compared)
        assertEquals(204, dartFailed)
    }

    @Test
    fun sunnahTimesAndQiblaMatchDart() {
        val jhelum = Coordinates(32.9405, 73.7276)
        val times = PrayerTimes(
            jhelum,
            DateComponents(2026, 11, 23),
            CalculationMethod.Karachi.parameters().copy(madhab = Madhab.Hanafi),
            zone,
        )
        val sunnah = SunnahTimes(times)
        val expected = reference.getJSONArray("sunnah")
        assertEquals(expected.getLong(0), sunnah.middleOfTheNight)
        assertEquals(expected.getLong(1), sunnah.lastThirdOfTheNight)
        assertEquals(reference.getDouble("qibla"), qiblaDirection(jhelum), 1e-9)
    }
}
