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

    /**
     * Which ayah each word belongs to, as surah * 1000 + ayah, by page and
     * line (null for banner and bismillah lines). A word belongs to the ayah
     * whose end ring comes next, which can be on the next page.
     */
    val wordAyahs: List<List<IntArray?>>

    /** For a line that ends a ruku, the index of the word (the ring) that ends it, by page and line; -1 otherwise. */
    val rukuRings: List<IntArray>

    /** The page each ayah (surah * 1000 + ayah) starts on. */
    val ayahPages: Map<Int, Int>

    init {
        val words = pages.map { page -> page.map { line -> (line as? AyahLine)?.let { IntArray(it.words.size) } } }
        val rings = pages.map { page -> IntArray(page.size) { -1 } }
        val ends = HashMap<Int, Int>()
        val pending = ArrayList<Triple<Int, Int, Int>>()
        var surah = 1
        // Where each surah's rukus have reached, to find the ayah a ruku ends on.
        var rukuAyah = 0
        pages.forEachIndexed { p, page ->
            page.forEachIndexed { l, line ->
                when (line) {
                    is SurahNameLine -> {
                        surah = line.surahNumber
                        rukuAyah = 0
                        pending.clear()
                    }
                    is AyahLine -> {
                        val rukuEnd = line.ruku?.let { rukuAyah += it.ayahCount; rukuAyah }
                        line.words.forEachIndexed { w, word ->
                            pending += Triple(p, l, w)
                            val ayah = RINGLESS_ENDS[Triple(p, l, w)]?.rem(1000) ?: when {
                                // The IndoPak text counts Al-Fatiha without its bismillah (an empty
                                // ring) and ends ayah 6 at "an'amta 'alayhim"; recitation counts the
                                // bismillah as ayah 1, so its rings are one ayah on.
                                surah == 1 && word.contains('\uF61E') -> 1
                                surah == 1 -> ayahEndNumber(word)?.let { if (it < 6) it + 1 else 7 }
                                else -> ayahEndNumber(word)
                            }
                            if (ayah != null) {
                                val key = surah * 1000 + ayah
                                ends[key] = pending.first().first + 1
                                pending.forEach { (pp, ll, ww) -> words[pp][ll]!![ww] = key }
                                pending.clear()
                                if (ayah == rukuEnd) rings[p][l] = w
                            }
                        }
                    }
                    BismillahLine -> Unit
                }
            }
        }
        wordAyahs = words
        rukuRings = rings
        ayahPages = ends
    }

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

/** The ayah number on [word]'s end ring (1 to 286), or null when it does not end an ayah. */
fun ayahEndNumber(word: String): Int? {
    val code = word.codePoints().filter { it in 0xF500..0xF61D }.findFirst()
    return if (code.isPresent) code.asInt - 0xF500 + 1 else null
}

/**
 * Ayah ends the text marks with a sajdah ring or a bare sign instead of a
 * numbered ring, found by scripts/sixteen_line_ayah_ends.py: page index,
 * line, word to surah * 1000 + ayah.
 */
private val RINGLESS_ENDS = mapOf(
    Triple(64, 14, 2) to 3171,
    Triple(100, 15, 5) to 5031,
    Triple(118, 3, 6) to 6036,
    Triple(145, 8, 4) to 7091,
    Triple(158, 8, 2) to 7206,
    Triple(225, 2, 6) to 13015,
    Triple(244, 7, 6) to 16050,
    Triple(263, 7, 2) to 17109,
    Triple(276, 0, 2) to 19031,
    Triple(277, 14, 6) to 19058,
    Triple(300, 6, 9) to 22018,
    Triple(306, 10, 1) to 22077,
    Triple(327, 12, 1) to 25058,
    Triple(327, 15, 2) to 25060,
    Triple(337, 10, 5) to 26208,
    Triple(340, 8, 11) to 27026,
    Triple(374, 2, 4) to 32015,
    Triple(374, 5, 8) to 32018,
    Triple(383, 6, 1) to 33060,
    Triple(408, 10, 3) to 38024,
    Triple(420, 4, 4) to 40006,
    Triple(426, 11, 9) to 40069,
    Triple(431, 15, 8) to 41038,
    Triple(447, 4, 4) to 44044,
    Triple(470, 1, 1) to 51054,
    Triple(474, 13, 9) to 53062,
    Triple(534, 7, 4) to 84021,
    Triple(539, 7, 1) to 91011,
    Triple(541, 15, 4) to 96019,
)

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
