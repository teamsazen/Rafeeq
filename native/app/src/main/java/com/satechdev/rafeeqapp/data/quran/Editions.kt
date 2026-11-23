package com.satechdev.rafeeqapp.data.quran

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** A translation or transliteration from alquran.cloud (lib/data/model/quran_models.dart). */
data class Edition(
    val identifier: String,
    val language: String,
    val name: String,
    val englishName: String,
    val type: String,
    val direction: String?,
)

/** A translation search hit. */
data class SearchMatch(val number: Int, val text: String, val surahNumber: Int, val numberInSurah: Int)

/**
 * The Flutter app's quran_text_cache_entries table, in the same SQLite file
 * (app_flutter/rafeeq_db, version 2), so translations downloaded in the
 * Flutter app stay downloaded.
 */
private class TextCacheDb(context: Context) : SQLiteOpenHelper(
    context,
    // path_provider's documents directory on Android is Context.getDir("flutter").
    File(context.getDir("flutter", Context.MODE_PRIVATE), "rafeeq_db").absolutePath,
    null,
    2,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE)
        db.execSQL(INDEX)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 2 added the table (lib/data/app_database/app_database.dart).
        if (oldVersion < 2) db.execSQL(CREATE)
    }

    companion object {
        const val TABLE = "quran_text_cache_entries"
        const val CREATE = """
            CREATE TABLE IF NOT EXISTS $TABLE (
              edition_id TEXT NOT NULL,
              fetch_type TEXT NOT NULL,
              fetch_number INTEGER NOT NULL,
              ayah_number INTEGER NOT NULL,
              ayah_text TEXT NOT NULL,
              PRIMARY KEY (edition_id, ayah_number)
            )"""
        const val INDEX = "CREATE INDEX IF NOT EXISTS idx_quran_text_cache_lookup ON $TABLE (edition_id, fetch_type, fetch_number)"
    }
}

/**
 * Translations and transliterations, as lib/data/repo/quran_api_repo.dart:
 * memory, then the SQLite cache, then the network (only once per surah, juz
 * or edition).
 */
object Editions {
    private const val BASE = "https://api.alquran.cloud/v1"
    const val TOTAL_AYAHS = 6236

    @Volatile private var db: TextCacheDb? = null
    private val memory = ConcurrentHashMap<String, Map<Int, String>>()

    private fun db(context: Context) = db ?: synchronized(this) { db ?: TextCacheDb(context.applicationContext).also { db = it } }

    /** Global ayah number to text for a surah of [edition]. */
    suspend fun surah(context: Context, number: Int, edition: String) = scope(context, "surah", number, edition)

    /** Global ayah number to text for a juz of [edition]. */
    suspend fun juz(context: Context, number: Int, edition: String) = scope(context, "juz", number, edition)

    private suspend fun scope(context: Context, type: String, number: Int, edition: String): Map<Int, String> {
        val key = "$type/$number/$edition"
        memory[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            val cached = read(context, "edition_id = ? AND fetch_type = ? AND fetch_number = ?", arrayOf(edition, type, number.toString()))
            val result = cached.ifEmpty {
                val json = JSONObject(get("$BASE/$type/$number/$edition")).getJSONObject("data")
                ayahs(json).also { write(context, type, number, edition, it) }
            }
            result.also { memory[key] = it }
        }
    }

    /** Every ayah of [edition]; downloads it whole once and caches it per surah. */
    suspend fun full(context: Context, edition: String): Map<Int, String> = withContext(Dispatchers.IO) {
        memory["full/$edition"]?.let { return@withContext it }
        val count = db(context).readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${TextCacheDb.TABLE} WHERE edition_id = ?", arrayOf(edition),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        val result = if (count >= TOTAL_AYAHS) {
            read(context, "edition_id = ?", arrayOf(edition))
        } else {
            val surahs = JSONObject(get("$BASE/quran/$edition")).getJSONObject("data").getJSONArray("surahs")
            buildMap {
                for (i in 0 until surahs.length()) {
                    val surah = surahs.getJSONObject(i)
                    val texts = ayahs(surah)
                    write(context, "surah", surah.getInt("number"), edition, texts)
                    putAll(texts)
                }
            }
        }
        result.also { memory["full/$edition"] = it }
    }

    /** The translations (or transliterations) alquran.cloud offers. */
    suspend fun list(type: String = "translation"): List<Edition> = withContext(Dispatchers.IO) {
        val data = JSONObject(get("$BASE/edition?type=$type&format=text")).getJSONArray("data")
        List(data.length()) { i ->
            val e = data.getJSONObject(i)
            Edition(e.optString("identifier"), e.optString("language"), e.optString("name"), e.optString("englishName"), e.optString("type"), e.optString("direction").takeIf { it.isNotEmpty() && it != "null" })
        }
    }

    /** Searches a translation (alquran.cloud's /search); empty when nothing matches. */
    suspend fun search(query: String, edition: String): List<SearchMatch> = withContext(Dispatchers.IO) {
        val url = "$BASE/search/${URLEncoder.encode(query, "UTF-8").replace("+", "%20")}/all/$edition"
        val body = try {
            get(url)
        } catch (e: NotFound) {
            return@withContext emptyList()
        }
        val matches = JSONObject(body).getJSONObject("data").optJSONArray("matches") ?: return@withContext emptyList()
        List(matches.length()) { i ->
            val m = matches.getJSONObject(i)
            SearchMatch(m.getInt("number"), m.getString("text"), m.getJSONObject("surah").getInt("number"), m.getInt("numberInSurah"))
        }
    }

    // ---------- Internals ----------

    private fun ayahs(json: JSONObject): Map<Int, String> {
        val list = json.getJSONArray("ayahs")
        return buildMap { for (i in 0 until list.length()) list.getJSONObject(i).let { put(it.getInt("number"), it.getString("text")) } }
    }

    private fun read(context: Context, where: String, args: Array<String>): Map<Int, String> =
        db(context).readableDatabase.query(TextCacheDb.TABLE, arrayOf("ayah_number", "ayah_text"), where, args, null, null, null).use { c ->
            buildMap { while (c.moveToNext()) put(c.getInt(0), c.getString(1)) }
        }

    private fun write(context: Context, type: String, number: Int, edition: String, texts: Map<Int, String>) {
        val database = db(context).writableDatabase
        database.beginTransaction()
        try {
            texts.forEach { (ayah, text) ->
                database.insertWithOnConflict(
                    TextCacheDb.TABLE, null,
                    ContentValues().apply {
                        put("edition_id", edition)
                        put("fetch_type", type)
                        put("fetch_number", number)
                        put("ayah_number", ayah)
                        put("ayah_text", text)
                    },
                    SQLiteDatabase.CONFLICT_REPLACE,
                )
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }

    private class NotFound : Exception()

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 60_000
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> return connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> throw NotFound()
                else -> error("alquran.cloud: HTTP ${connection.responseCode} for $url")
            }
        } finally {
            connection.disconnect()
        }
    }
}
