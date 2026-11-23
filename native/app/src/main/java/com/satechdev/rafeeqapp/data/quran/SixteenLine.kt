package com.satechdev.rafeeqapp.data.quran

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/*
 * The IndoPak 16 line (Taj Company) mushaf, as
 * lib/ui/quran/sixteen_line_quran/sixteen_line_quran_data.dart. The layout
 * is from the Quranic Universal Library and the text is QuranWBW's IndoPak
 * v5.4, used with their permission.
 */

sealed interface SixteenLine

/** The decorated banner that opens a surah. */
data class SurahNameLine(val surahNumber: Int) : SixteenLine

data object BismillahLine : SixteenLine

/** The end of a ruku, marked with ع in the margin of its line. */
data class RukuEnd(val numberInSurah: Int, val ayahCount: Int)

/** Words in reading order; ayah end signs are words of their own. Short lines sit in the middle. */
data class AyahLine(val words: List<String>, val centered: Boolean, val ruku: RukuEnd? = null) : SixteenLine

/** A page and, when known, the line on it to point out. */
data class SixteenLinePosition(val page: Int, val line: Int? = null)

/** Where a juz is read in four: its start, rub', nisf and thalatha. */
enum class JuzQuarter { Start, Rub, Nisf, Thalatha }

class SixteenLineQuran(
    /** Page n is pages[n - 1]. */
    val pages: List<List<SixteenLine>>,
    val surahStartPages: List<Int>,
    val juzStartPages: List<Int>,
    /** The bismillah over each surah, word by word. */
    val bismillah: AyahLine,
    val juzQuarters: List<SixteenLinePosition>,
) {
    val pageCount get() = pages.size

    fun juzQuarter(juz: Int, quarter: JuzQuarter) = juzQuarters[(juz - 1) * 4 + quarter.ordinal]

    /** The banner that opens [surah]. */
    fun surahStart(surah: Int): SixteenLinePosition {
        val page = surahStartPages[surah - 1]
        val line = pages[page - 1].indexOfFirst { it is SurahNameLine && it.surahNumber == surah }
        return SixteenLinePosition(page, line.takeIf { it >= 0 })
    }

    fun juzForPage(page: Int) = lastStartAtOrBefore(juzStartPages, page)

    /** The surah being read at the top of [page]. */
    fun surahForPage(page: Int): Int {
        val lines = pages[page - 1]
        (lines.firstOrNull() as? SurahNameLine)?.let { return it.surahNumber }
        // A surah can start lower on the page, so look for the previous start.
        var surah = 1
        surahStartPages.forEachIndexed { i, start -> if (start < page) surah = i + 1 }
        return surah
    }

    private fun lastStartAtOrBefore(starts: List<Int>, page: Int): Int {
        var number = 1
        starts.forEachIndexed { i, start -> if (start <= page) number = i + 1 }
        return number
    }

    companion object {
        fun parse(source: String): SixteenLineQuran {
            val json = JSONObject(source)
            val pages = json.getJSONArray("pages").let { list ->
                List(list.length()) { p ->
                    val page = list.getJSONArray(p)
                    List(page.length()) { l ->
                        val line = page.getJSONArray(l)
                        when (line.getString(0)) {
                            "s" -> SurahNameLine(line.getInt(1))
                            "b" -> BismillahLine
                            else -> AyahLine(
                                strings(line.getJSONArray(2)),
                                centered = line.getInt(1) == 1,
                                ruku = if (line.length() > 3) line.getJSONArray(3).getJSONArray(0).let { RukuEnd(it.getInt(0), it.getInt(1)) } else null,
                            )
                        }
                    }
                }
            }
            return SixteenLineQuran(
                pages = pages,
                surahStartPages = ints(json.getJSONArray("surahStart")),
                juzStartPages = ints(json.getJSONArray("juzStart")),
                bismillah = AyahLine(strings(json.getJSONArray("bismillah")), centered = true),
                juzQuarters = json.getJSONArray("quarters").let { q -> List(q.length()) { q.getJSONArray(it).let { r -> SixteenLinePosition(r.getInt(0), r.getInt(1)) } } },
            )
        }

        private fun strings(a: JSONArray) = List(a.length()) { a.getString(it) }
        private fun ints(a: JSONArray) = List(a.length()) { a.getInt(it) }
    }
}

/**
 * Whether [word] ends an ayah: the text marks it with a private-use
 * character, U+F500 for ayah 1 to U+F61D for ayah 286, which the font draws
 * as the numbered ring. U+F61E is the bismillah's empty ring.
 */
fun isAyahEnd(word: String) = word.codePoints().anyMatch { it in 0xF500..0xF61E }

/** Loads assets/quran_16_line.json once, off the main thread. */
object SixteenLineStore {
    private val lock = Mutex()
    @Volatile private var loaded: SixteenLineQuran? = null

    val cached get() = loaded

    suspend fun get(context: Context): SixteenLineQuran = loaded ?: lock.withLock {
        loaded ?: withContext(Dispatchers.IO) {
            SixteenLineQuran.parse(context.assets.open("quran_16_line.json").bufferedReader().use { it.readText() })
        }.also { loaded = it }
    }
}
