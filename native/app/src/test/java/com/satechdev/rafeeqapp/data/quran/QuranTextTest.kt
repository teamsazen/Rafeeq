package com.satechdev.rafeeqapp.data.quran

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the Bismillah handling against every surah in assets/quran.json. */
class QuranTextTest {
    private val surahs = JSONObject(File("../../assets/quran.json").readText()).getJSONObject("data").getJSONArray("surahs")

    private fun ayah(surah: Int, index: Int): Ayah {
        val a = surahs.getJSONObject(surah - 1).getJSONArray("ayahs").getJSONObject(index)
        return Ayah(a.getInt("number"), a.getInt("numberInSurah"), surah, a.getInt("juz"), a.getInt("page"), a.getInt("ruku"), a.getString("text").removePrefix("﻿"))
    }

    @Test
    fun bismillahLeavesEveryFirstAyahButFatihaAndTawbah() {
        var stripped = 0
        for (surah in 1..114) {
            val first = ayah(surah, 0)
            val text = first.withoutBismillah()
            if (text != first.text) stripped++
            if (surah != 1) assertFalse("Surah $surah still starts with Bismillah", letters(text.substringBefore(' ')) == "بسم")
        }
        assertEquals(112, stripped)
        assertEquals("الٓمٓ", ayah(2, 0).withoutBismillah())
        assertTrue(ayah(1, 0).withoutBismillah().startsWith("بِسْمِ"))
        assertEquals(ayah(9, 0).text, ayah(9, 0).withoutBismillah())
    }
}
