package com.satechdev.rafeeqapp.data.hijri

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** The Kotlin port against the Dart hijri package (resources/hijri_reference.json, from tool/reference). */
class HijriTest {
    private val reference = JSONObject(javaClass.classLoader!!.getResource("hijri_reference.json").readText())

    @Test
    fun everyDayFrom2020To2035MatchesDart() {
        val days = reference.getJSONArray("days")
        for (i in 0 until days.length()) {
            val d = days.getJSONArray(i)
            val h = HijriDate.of(LocalDate.of(d.getInt(0), d.getInt(1), d.getInt(2)))
            assertEquals(d.toString(), HijriDate(d.getInt(3), d.getInt(4), d.getInt(5), d.getInt(6), d.getInt(7)), h)
        }
        assertEquals(5844, days.length())
    }

    @Test
    fun monthStartsMatchDart() {
        val starts = reference.getJSONArray("starts")
        for (i in 0 until starts.length()) {
            val s = starts.getJSONArray(i)
            assertEquals(s.toString(), LocalDate.of(s.getInt(2), s.getInt(3), s.getInt(4)), HijriDate.toGregorian(s.getInt(0), s.getInt(1), 1))
            assertEquals(s.toString(), s.getInt(5), HijriDate.daysInMonth(s.getInt(0), s.getInt(1)))
        }
    }

    @Test
    fun correctionMovesTheGregorianDay() {
        val day = LocalDate.of(2026, 11, 23)
        assertEquals(HijriDate.of(day.minusDays(1)), HijriCorrection.Minus1Day.hijri(day))
        assertEquals(HijriDate.of(day.plusDays(2)), HijriCorrection.Plus2Days.hijri(day))
    }
}
