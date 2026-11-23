package com.satechdev.rafeeqapp.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 16 line index search, with the examples from the Flutter app. */
class SearchKeyTest {
    @Test
    fun spellingsOfANameShareAKey() {
        assertTrue(searchKey("Ya-Sin").contains(searchKey("Yaseen")))
        assertTrue(searchKey("Al-Baqara").contains(searchKey("baqarah")))
        assertEquals(searchKey("یس"), searchKey("يس"))
    }

    @Test
    fun urduAndArabicDigitsAreNumbers() {
        assertEquals("36", searchKey("۳۶"))
        assertEquals("36", searchKey("٣٦"))
    }
}
