package com.satechdev.rafeeqapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.satechdev.rafeeqapp.data.hijri.HijriCorrection
import com.satechdev.rafeeqapp.data.prayer.AsrMethod
import com.satechdev.rafeeqapp.data.prayer.HighLatitude
import com.satechdev.rafeeqapp.data.prayer.PrayerCalcMethod
import com.satechdev.rafeeqapp.data.prayer.TimeFormat
import com.satechdev.rafeeqapp.data.prayer.UserLocation
import org.json.JSONArray
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
 * JSON string, as Flutter's StorageBox wrote it. Writes change only the
 * fields they own and keep every other field, so an update from the Flutter
 * app keeps everything and nothing the Flutter app knew is lost.
 */
object Settings {
    private lateinit var prefs: SharedPreferences

    private const val APP_SETTINGS = "app_settings"
    private const val PRAYER_SETTINGS = "prayer_times_settings"
    private const val USER_LOCATION = "prefs.user_location"
    private const val ABOUT_OPENED = "about_opened_v1"

    // Defaults are the Flutter app's (lib/core/app_settings_notifier.dart and
    // lib/ui/prayer_times/prayer_times_settings.dart).
    @set:JvmName("assignThemeMode")
    var themeMode by mutableStateOf(ThemeMode.Dark); private set
    @set:JvmName("assignLanguage")
    var language by mutableStateOf(AppLanguage.English); private set
    @set:JvmName("assignHijriCorrection")
    var hijriCorrection by mutableStateOf(HijriCorrection.None); private set
    @set:JvmName("assignCalcMethod")
    var calcMethod by mutableStateOf(PrayerCalcMethod.Karachi); private set
    @set:JvmName("assignAsrMethod")
    var asrMethod by mutableStateOf(AsrMethod.Hanafi); private set
    @set:JvmName("assignHighLatitude")
    var highLatitude by mutableStateOf(HighLatitude.AngleBased); private set
    @set:JvmName("assignTimeFormat")
    var timeFormat by mutableStateOf(TimeFormat.TwelveHour); private set
    @set:JvmName("assignLocation")
    var location by mutableStateOf<UserLocation?>(null); private set
    var aboutOpened by mutableStateOf(false); private set

    fun load(context: Context) {
        prefs = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)
        json(APP_SETTINGS).let {
            themeMode = ThemeMode.of(it.optString("themeMode")) ?: ThemeMode.Dark
            language = AppLanguage.of(it.optString("language")) ?: AppLanguage.English
            hijriCorrection = HijriCorrection.of(it.optString("hijriCorrection"))
        }
        json(PRAYER_SETTINGS).let {
            calcMethod = PrayerCalcMethod.of(it.optString("prayerCalcMethod"))
            asrMethod = AsrMethod.of(it.optString("asrCalculationMethod"))
            highLatitude = HighLatitude.of(it.optString("highLatitudeAdjustment"))
            timeFormat = TimeFormat.of(it.optString("timeFormat"))
        }
        location = json(USER_LOCATION).takeIf { it.has("latitude") }?.let {
            UserLocation(
                latitude = it.optDouble("latitude"),
                longitude = it.optDouble("longitude"),
                city = it.optString("city").takeIf(String::isNotBlank),
                country = it.optString("country").takeIf(String::isNotBlank),
                countryCode = it.optString("countryCode").takeIf(String::isNotBlank),
                source = it.optString("source").takeIf(String::isNotBlank),
            )
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

    fun setHijriCorrection(value: HijriCorrection) {
        hijriCorrection = value
        update(APP_SETTINGS) { put("hijriCorrection", value.stored) }
    }

    fun setCalcMethod(value: PrayerCalcMethod) {
        calcMethod = value
        update(PRAYER_SETTINGS) { put("prayerCalcMethod", value.stored) }
    }

    fun setAsrMethod(value: AsrMethod) {
        asrMethod = value
        update(PRAYER_SETTINGS) { put("asrCalculationMethod", value.stored) }
    }

    fun setHighLatitude(value: HighLatitude) {
        highLatitude = value
        update(PRAYER_SETTINGS) { put("highLatitudeAdjustment", value.stored) }
    }

    fun setTimeFormat(value: TimeFormat) {
        timeFormat = value
        update(PRAYER_SETTINGS) { put("timeFormat", value.stored) }
    }

    /** Replaces the location, as the Flutter app's UserLocation.toMap wrote it. */
    fun setLocation(value: UserLocation) {
        location = value
        val map = JSONObject()
            .put("latitude", value.latitude)
            .put("longitude", value.longitude)
            .put("city", value.city ?: JSONObject.NULL)
            .put("country", value.country ?: JSONObject.NULL)
            .put("countryCode", value.countryCode ?: JSONObject.NULL)
            .put("source", value.source ?: JSONObject.NULL)
        prefs.edit().putString(k(USER_LOCATION), map.toString()).apply()
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
        if (value.length() == 0) {
            when (key) {
                APP_SETTINGS -> value
                    .put("language", language.code)
                    .put("hijriCorrection", hijriCorrection.stored)
                    .put("themeMode", themeMode.stored)
                    .put("themeIndex", 0)
                    .put("ramadanViewMode", 0)
                    .put("eventPreviews", JSONArray())
                PRAYER_SETTINGS -> value
                    .put("asrCalculationMethod", asrMethod.stored)
                    .put("prayerCalcMethod", calcMethod.stored)
                    .put("highLatitudeAdjustment", highLatitude.stored)
                    .put("timeFormat", timeFormat.stored)
            }
        }
        value.change()
        prefs.edit().putString(k(key), value.toString()).apply()
    }
}
