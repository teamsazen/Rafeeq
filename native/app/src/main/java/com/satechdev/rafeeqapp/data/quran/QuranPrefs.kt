package com.satechdev.rafeeqapp.data.quran

import android.util.Base64
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream

/** The reader's backgrounds, stored by index as the Flutter AyahViewMode. */
enum class AyahViewMode {
    Default, PaperLight, PaperCream, PaperDark, PaperWood, PaperAged, GradientBlue, GradientPurple, GradientGreen;

    companion object {
        fun of(index: Int?) = entries.getOrNull(index ?: 0) ?: Default
    }
}

/** lib/ui/quran/quran_view_settings.dart's QuranViewSettings, with its defaults. */
data class QuranViewSettings(
    val arabicFontSize: Float = 28f,
    val translationFontSize: Float = 16f,
    val transliterationFontSize: Float = 14f,
    val showTranslation: Boolean = true,
    val showTransliteration: Boolean = false,
    val translationEditionId: String = "en.asad",
    val transliterationEditionId: String = "en.transliteration",
    val arabicFontFamily: String = "PDMS Saleem Quran",
    val translationFontFamily: String = "GoogleSans",
    val viewMode: AyahViewMode = AyahViewMode.Default,
    val continueToNextSurah: Boolean = false,
    val surahRepeat: Boolean = false,
    val autoHidePlayerControls: Boolean = false,
)

/**
 * shared_preferences' string list format (Android): a prefix, then "!" and a
 * JSON array, or, from older plugin versions, a serialized Java list.
 */
object PrefsList {
    const val PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIGxpc3Qu"

    fun decode(raw: String?): List<String> {
        if (raw == null || !raw.startsWith(PREFIX)) return emptyList()
        val body = raw.removePrefix(PREFIX)
        return runCatching {
            if (body.startsWith("!")) {
                val a = JSONArray(body.substring(1))
                List(a.length()) { a.getString(it) }
            } else {
                ObjectInputStream(ByteArrayInputStream(Base64.decode(body, Base64.DEFAULT))).use { s ->
                    (s.readObject() as List<*>).map { it.toString() }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encode(values: Collection<String>) = "$PREFIX!${JSONArray(values.toList())}"
}

/** A bookmarked ayah, stored as "surah:ayah" (lib/data/services/bookmark_service.dart). */
data class Bookmark(val surah: Int, val ayah: Int) {
    val key get() = "$surah:$ayah"

    companion object {
        fun of(key: String): Bookmark? {
            val (s, a) = key.split(':').takeIf { it.size == 2 } ?: return null
            return Bookmark(s.toIntOrNull() ?: return null, a.toIntOrNull() ?: return null)
        }
    }
}
