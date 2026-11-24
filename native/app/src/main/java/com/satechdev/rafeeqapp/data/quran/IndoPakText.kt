package com.satechdev.rafeeqapp.data.quran

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Each ayah in the IndoPak script that South Asia reads from print: the
 * QuranWBW IndoPak v5.4 text of the 16 line Quran, gathered ayah by ayah
 * with SixteenLineQuran.wordAyahs, for the reader (drawn with the same
 * AlQuran Neo font; QuranWBW's permission covers showing their text).
 *
 * The words are kept as given. Only the ayah's own end ring is left off,
 * since the reader numbers each ayah beside it, and the IndoPak count of
 * Al-Fatiha differs from recitation's, so its rings would read one short.
 */
object IndoPakText {
    private val lock = Mutex()
    @Volatile private var loaded: Map<Int, String>? = null

    val cached get() = loaded

    /** IndoPak text by surah * 1000 + ayah. */
    suspend fun get(context: Context): Map<Int, String> = loaded ?: lock.withLock {
        loaded ?: withContext(Dispatchers.Default) { build(SixteenLineStore.get(context)) }.also { loaded = it }
    }

    fun build(quran: SixteenLineQuran): Map<Int, String> {
        val words = LinkedHashMap<Int, MutableList<String>>()
        quran.pages.forEachIndexed { p, page ->
            page.forEachIndexed { l, line ->
                val keys = quran.wordAyahs[p][l] ?: return@forEachIndexed
                (line as AyahLine).words.forEachIndexed { w, word -> words.getOrPut(keys[w]) { ArrayList() } += word }
            }
        }
        return words.mapValues { (_, list) -> withoutEndRing(list).joinToString(" ") }
    }

    /** Drops the end ring, the last word's numbered circle (and sajdah variants), keeping any sign with it. */
    internal fun withoutEndRing(words: List<String>): List<String> {
        if (words.isEmpty()) return words
        val last = words.last()
        val kept = last.filterNot { it.code in 0xF500..0xF6FF }
        val hasLetters = last.any { it.code in 0x0600..0x06FF }
        return when {
            hasLetters -> words // a word that ends the ayah with no ring of its own
            kept.isBlank() -> words.dropLast(1)
            else -> words.dropLast(1) + kept
        }
    }
}
