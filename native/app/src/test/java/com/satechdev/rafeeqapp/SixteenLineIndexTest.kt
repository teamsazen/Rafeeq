package com.satechdev.rafeeqapp

import com.satechdev.rafeeqapp.data.quran.AyahLine
import com.satechdev.rafeeqapp.data.quran.SixteenLineQuran
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The 16 line mushaf's word to ayah index and ruku rings, over the whole Quran. */
class SixteenLineIndexTest {
    private val quran = SixteenLineQuran.parse(File("../../assets/quran_16_line.json").readText())

    @Test
    fun everyAyahEndsOnAPage() {
        // 6236 ayahs; the IndoPak text marks every one with a ring.
        assertEquals(6236, quran.ayahPages.size)
        assertEquals(1, quran.ayahPages[1001])
        assertEquals(quran.pageCount, quran.ayahPages[114006])
    }

    @Test
    fun everyWordHasItsAyah() {
        quran.pages.forEachIndexed { p, page ->
            page.forEachIndexed { l, line ->
                if (line is AyahLine) quran.wordAyahs[p][l]!!.forEach { assertTrue("page ${p + 1} line $l", it > 1000) }
            }
        }
    }

    @Test
    fun everyRukuEndsAtARingOnItsLine() {
        var rukus = 0
        quran.pages.forEachIndexed { p, page ->
            page.forEachIndexed { l, line ->
                if (line is AyahLine && line.ruku != null) {
                    rukus++
                    assertTrue("ruku on page ${p + 1} line $l has no ring", quran.rukuRings[p][l] >= 0)
                }
            }
        }
        assertTrue(rukus > 500)
    }
}

/** The reader's IndoPak text, built from the 16 line mushaf. */
class IndoPakTextTest {
    private val text = com.satechdev.rafeeqapp.data.quran.IndoPakText.build(
        SixteenLineQuran.parse(File("../../assets/quran_16_line.json").readText()),
    )

    @Test
    fun everyAyahHasItsWordsAndNoRing() {
        assertEquals(6236, text.size)
        text.forEach { (key, ayah) ->
            assertTrue("$key is empty", ayah.any { it.code in 0x0600..0x06FF })
            assertTrue("$key keeps its ring", ayah.none { it.code in 0xF500..0xF61E })
        }
    }

    @Test
    fun fatihaCountsTheBismillahAsAyahOne() {
        assertTrue(text.getValue(1001).startsWith("بِسْمِ"))
        assertTrue(text.getValue(1002).startsWith("اَلْحَمْدُ"))
        assertTrue(text.getValue(1007).startsWith("صِرَاطَ"))
        assertTrue(text.getValue(2001).isNotEmpty())
        assertTrue(text.getValue(114006).isNotEmpty())
    }
}
