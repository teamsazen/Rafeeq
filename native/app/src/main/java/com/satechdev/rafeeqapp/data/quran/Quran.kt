package com.satechdev.rafeeqapp.data.quran

import android.content.Context
import android.util.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The ayah's text without the Bismillah that opens the first ayah of 112
 * surahs (shown once in the header instead). Fatiha keeps it: there it is
 * the first ayah. The text spells it two ways (بِسْمِ and بِّسْمِ) and puts
 * the shadda before the fatha on ٱللَّهِ, so the Flutter app's literal
 * replace never matched; this compares the letters of the first two words
 * and drops all four.
 */
fun Ayah.withoutBismillah(): String {
    if (surahNumber == 1 || numberInSurah != 1) return text
    val words = text.split(' ')
    return if (words.size > 4 && letters(words[0]) == "بسم" && letters(words[1]) == "ٱلله") words.drop(4).joinToString(" ") else text
}

/** The letters of an Arabic word without harakat or Quranic marks, so mark order never matters. */
internal fun letters(word: String) = word.filterNot { it in '\u064B'..'\u065F' || it == '\u0670' || it in '\u06D6'..'\u06ED' }

/** An ayah of the Uthmani text in assets/quran.json (alquran.cloud's quran-uthmani). */
data class Ayah(
    /** 1 to 6236 across the Quran. */
    val number: Int,
    val numberInSurah: Int,
    val surahNumber: Int,
    val juz: Int,
    val page: Int,
    val ruku: Int,
    val text: String,
)

data class Surah(
    val number: Int,
    /** The Arabic name, as "سُورَةُ ٱلْفَاتِحَةِ". */
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    /** "Meccan" or "Medinan". */
    val revelationType: String,
    val ayahs: List<Ayah>,
)

/** The whole Quran, as the Flutter app's QuranData. */
class QuranText(val surahs: List<Surah>) {
    val ayahs: List<Ayah> by lazy { surahs.flatMap { it.ayahs } }

    fun surah(number: Int) = surahs[number - 1]

    fun juz(number: Int) = ayahs.filter { it.juz == number }

    /** The first ayah of each juz. */
    val juzStarts: List<Ayah> by lazy { (1..30).map { j -> ayahs.first { it.juz == j } } }
}

/** Loads assets/quran.json once, off the main thread, and keeps it. */
object QuranStore {
    private val lock = Mutex()
    @Volatile private var loaded: QuranText? = null

    val cached get() = loaded

    suspend fun get(context: Context): QuranText = loaded ?: lock.withLock {
        loaded ?: withContext(Dispatchers.IO) { read(context) }.also { loaded = it }
    }

    /** Streams the file instead of building a 4.6 MB JSON tree. */
    private fun read(context: Context): QuranText {
        val surahs = mutableListOf<Surah>()
        JsonReader(context.assets.open("quran.json").bufferedReader()).use { r ->
            r.beginObject()
            while (r.hasNext()) {
                if (r.nextName() != "data") { r.skipValue(); continue }
                r.beginObject()
                while (r.hasNext()) {
                    if (r.nextName() != "surahs") { r.skipValue(); continue }
                    r.beginArray()
                    while (r.hasNext()) surahs += readSurah(r, surahs.size + 1)
                    r.endArray()
                }
                r.endObject()
            }
            r.endObject()
        }
        return QuranText(surahs)
    }

    private fun readSurah(r: JsonReader, number: Int): Surah {
        var name = ""
        var englishName = ""
        var translation = ""
        var revelation = ""
        val ayahs = mutableListOf<Ayah>()
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "name" -> name = r.nextString()
                "englishName" -> englishName = r.nextString()
                "englishNameTranslation" -> translation = r.nextString()
                "revelationType" -> revelation = r.nextString()
                "ayahs" -> {
                    r.beginArray()
                    while (r.hasNext()) ayahs += readAyah(r, number)
                    r.endArray()
                }
                else -> r.skipValue()
            }
        }
        r.endObject()
        return Surah(number, name, englishName, translation, revelation, ayahs)
    }

    private fun readAyah(r: JsonReader, surah: Int): Ayah {
        var number = 0
        var inSurah = 0
        var juz = 0
        var page = 0
        var ruku = 0
        var text = ""
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "number" -> number = r.nextInt()
                "numberInSurah" -> inSurah = r.nextInt()
                "juz" -> juz = r.nextInt()
                "page" -> page = r.nextInt()
                "ruku" -> ruku = r.nextInt()
                // The first ayah carries a byte order mark in the file.
                "text" -> text = r.nextString().removePrefix("﻿")
                else -> r.skipValue()
            }
        }
        r.endObject()
        return Ayah(number, inSurah, surah, juz, page, ruku, text)
    }
}
