package com.satechdev.rafeeqapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** Light, system or dark, stored by name as the Flutter app's `ThemeMode`. */
enum class ThemeMode(val stored: String) {
    Light("light"), System("system"), Dark("dark");

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored }
    }
}

/** The app's language, stored by code as the Flutter app's `AppLanguage`. */
enum class AppLanguage(val code: String) {
    English("en"), Urdu("ur");

    val rtl get() = this == Urdu

    companion object {
        fun of(code: String?) = entries.firstOrNull { it.code == code }
    }
}

/**
 * App settings, kept where the Flutter app kept them: the shared_preferences
 * file "FlutterSharedPreferences", keys prefixed "flutter.". Each group is a
 * JSON string, as Flutter's StorageBox wrote it. Writes change only the field
 * they own and keep every other field, so an update from the Flutter app keeps
 * everything and nothing the Flutter app knew is lost.
 */
object Settings {
    private lateinit var prefs: SharedPreferences

    private const val APP_SETTINGS = "app_settings"
    private const val PRAYER_SETTINGS = "prayer_times_settings"
    private const val USER_LOCATION = "prefs.user_location"
    private const val ABOUT_OPENED = "about_opened_v1"

    // Defaults are the Flutter app's (lib/core/app_settings_notifier.dart).
    @set:JvmName("assignThemeMode")
    var themeMode by mutableStateOf(ThemeMode.Dark); private set
    @set:JvmName("assignLanguage")
    var language by mutableStateOf(AppLanguage.English); private set
    @set:JvmName("assignHijriCorrection")
    var hijriCorrection by mutableStateOf("none"); private set

    // Read here for the settings rows; their screens come in phase 2.
    var prayerCalcMethod by mutableStateOf("karachi"); private set
    var asrMethod by mutableStateOf("hanafi"); private set
    var highLatitude by mutableStateOf("angleBased"); private set
    var timeFormat by mutableStateOf("twelveHour"); private set
    var city by mutableStateOf<String?>(null); private set
    var country by mutableStateOf<String?>(null); private set
    var aboutOpened by mutableStateOf(false); private set

    fun load(context: Context) {
        prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        json(APP_SETTINGS).let {
            themeMode = ThemeMode.of(it.optString("themeMode")) ?: ThemeMode.Dark
            language = AppLanguage.of(it.optString("language")) ?: AppLanguage.English
            hijriCorrection = it.optString("hijriCorrection", "none")
        }
        json(PRAYER_SETTINGS).let {
            prayerCalcMethod = it.optString("prayerCalcMethod", "karachi")
            asrMethod = it.optString("asrCalculationMethod", "hanafi")
            highLatitude = it.optString("highLatitudeAdjustment", "angleBased")
            timeFormat = it.optString("timeFormat", "twelveHour")
        }
        json(USER_LOCATION).let {
            city = it.optString("city").takeIf { s -> s.isNotBlank() }
            country = it.optString("country").takeIf { s -> s.isNotBlank() }
        }
        aboutOpened = prefs.getBoolean(k(ABOUT_OPENED), false)
    }

    fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
        update(APP_SETTINGS) { put("themeMode", mode.stored) }
    }

    fun setLanguage(value: AppLanguage) {
        language = value
        update(APP_SETTINGS) { put("language", value.code) }
    }

    fun markAboutOpened() {
        aboutOpened = true
        prefs.edit().putBoolean(k(ABOUT_OPENED), true).apply()
    }

    // --- storage ---

    private fun k(key: String) = "flutter.$key"

    private fun json(key: String): JSONObject =
        runCatching { JSONObject(prefs.getString(k(key), null) ?: "{}") }.getOrDefault(JSONObject())

    /** Changes [key]'s JSON in place, filling in the Flutter defaults if it was never saved. */
    private fun update(key: String, change: JSONObject.() -> Unit) {
        val value = json(key)
        if (key == APP_SETTINGS && value.length() == 0) {
            value.put("language", language.code)
            value.put("hijriCorrection", hijriCorrection)
            value.put("themeMode", themeMode.stored)
            value.put("themeIndex", 0)
            value.put("ramadanViewMode", 0)
            value.put("eventPreviews", org.json.JSONArray())
        }
        value.change()
        prefs.edit().putString(k(key), value.toString()).apply()
    }
}
