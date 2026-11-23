package com.satechdev.rafeeqapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.satechdev.rafeeqapp.data.hijri.HijriCorrection
import com.satechdev.rafeeqapp.data.prayer.AsrMethod
import com.satechdev.rafeeqapp.data.quran.AyahViewMode
import com.satechdev.rafeeqapp.data.quran.Bookmark
import com.satechdev.rafeeqapp.data.quran.PrefsList
import com.satechdev.rafeeqapp.data.quran.QuranViewSettings
import com.satechdev.rafeeqapp.data.prayer.HighLatitude
import com.satechdev.rafeeqapp.data.prayer.NotifKey
import com.satechdev.rafeeqapp.data.prayer.NotificationSettings
import com.satechdev.rafeeqapp.data.prayer.NotificationSound
import com.satechdev.rafeeqapp.data.prayer.NotificationTiming
import com.satechdev.rafeeqapp.data.prayer.VolumeType
import com.satechdev.rafeeqapp.data.prayer.PrayerCalcMethod
import com.satechdev.rafeeqapp.data.prayer.Reminders
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
    private const val QURAN_VIEW = "quran_view_settings"
    private const val BOOKMARKS = "ayah_bookmarks"

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
    @set:JvmName("assignNotifications")
    var notifications by mutableStateOf(NotificationSettings()); private set
    @set:JvmName("assignLocation")
    var location by mutableStateOf<UserLocation?>(null); private set
    var aboutOpened by mutableStateOf(false); private set
    @set:JvmName("assignQuranView")
    var quranView by mutableStateOf(QuranViewSettings()); private set
    @set:JvmName("assignBookmarks")
    var bookmarks by mutableStateOf(emptyList<Bookmark>()); private set

    private lateinit var app: Context

    /** Anything that changes the reminders reschedules them, as Flutter's PrayerNotificationManager listens. */
    private fun remindersChanged() {
        if (::app.isInitialized) Reminders.schedule(app, force = true)
    }

    fun load(context: Context) {
        app = context.applicationContext
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
            notifications = parseNotifications(it)
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
        quranView = parseQuranView(json(QURAN_VIEW))
        bookmarks = PrefsList.decode(prefs.getString(k(BOOKMARKS), null)).mapNotNull(Bookmark::of)
    }

    // --- Quran ---

    fun setQuranView(value: QuranViewSettings) {
        quranView = value
        update(QURAN_VIEW) {
            put("arabicFontSize", value.arabicFontSize.toDouble())
            put("translationFontSize", value.translationFontSize.toDouble())
            put("transliterationFontSize", value.transliterationFontSize.toDouble())
            put("showTranslation", value.showTranslation)
            put("showTransliteration", value.showTransliteration)
            put("translationEditionId", value.translationEditionId)
            put("transliterationEditionId", value.transliterationEditionId)
            put("arabicFontFamily", value.arabicFontFamily)
            put("translationFontFamily", value.translationFontFamily)
            put("viewMode", value.viewMode.ordinal)
            put("continueToNextSurah", value.continueToNextSurah)
            put("surahRepeat", value.surahRepeat)
            put("autoHidePlayerControls", value.autoHidePlayerControls)
        }
    }

    fun isBookmarked(surah: Int, ayah: Int) = bookmarks.any { it.surah == surah && it.ayah == ayah }

    fun toggleBookmark(surah: Int, ayah: Int) {
        val mark = Bookmark(surah, ayah)
        bookmarks = if (mark in bookmarks) bookmarks - mark else bookmarks + mark
        prefs.edit().putString(k(BOOKMARKS), PrefsList.encode(bookmarks.map { it.key })).apply()
    }

    private fun parseQuranView(j: JSONObject): QuranViewSettings {
        val d = QuranViewSettings()
        fun num(name: String, default: Float) = if (j.has(name)) j.optDouble(name, default.toDouble()).toFloat() else default
        fun bool(name: String, default: Boolean) = if (j.has(name)) j.optBoolean(name, default) else default
        fun str(name: String, default: String) = j.optString(name).takeIf { it.isNotEmpty() && it != "null" } ?: default
        return QuranViewSettings(
            arabicFontSize = num("arabicFontSize", d.arabicFontSize),
            translationFontSize = num("translationFontSize", d.translationFontSize),
            transliterationFontSize = num("transliterationFontSize", d.transliterationFontSize),
            showTranslation = bool("showTranslation", d.showTranslation),
            showTransliteration = bool("showTransliteration", d.showTransliteration),
            translationEditionId = str("translationEditionId", d.translationEditionId),
            transliterationEditionId = str("transliterationEditionId", d.transliterationEditionId),
            arabicFontFamily = str("arabicFontFamily", d.arabicFontFamily),
            translationFontFamily = str("translationFontFamily", d.translationFontFamily),
            viewMode = AyahViewMode.of(if (j.has("viewMode")) j.optInt("viewMode") else null),
            continueToNextSurah = bool("continueToNextSurah", d.continueToNextSurah),
            surahRepeat = bool("surahRepeat", d.surahRepeat),
            autoHidePlayerControls = bool("autoHidePlayerControls", d.autoHidePlayerControls),
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        Analytics.themeChanged(mode.stored)
        themeMode = mode
        update(APP_SETTINGS) { put("themeMode", mode.stored) }
    }

    fun setLanguage(value: AppLanguage) {
        Analytics.languageChanged(value.code)
        language = value
        update(APP_SETTINGS) { put("language", value.code) }
        remindersChanged()
    }

    fun setHijriCorrection(value: HijriCorrection) {
        Analytics.hijriCorrectionChanged(value.stored)
        hijriCorrection = value
        update(APP_SETTINGS) { put("hijriCorrection", value.stored) }
        remindersChanged()
    }

    fun setCalcMethod(value: PrayerCalcMethod) {
        Analytics.calculationMethodChanged(value.stored)
        calcMethod = value
        update(PRAYER_SETTINGS) { put("prayerCalcMethod", value.stored) }
        remindersChanged()
    }

    fun setAsrMethod(value: AsrMethod) {
        Analytics.juristicMethodChanged(value.stored)
        asrMethod = value
        update(PRAYER_SETTINGS) { put("asrCalculationMethod", value.stored) }
        remindersChanged()
    }

    fun setHighLatitude(value: HighLatitude) {
        Analytics.highLatitudeChanged(value.stored)
        highLatitude = value
        update(PRAYER_SETTINGS) { put("highLatitudeAdjustment", value.stored) }
        remindersChanged()
    }

    fun setTimeFormat(value: TimeFormat) {
        timeFormat = value
        update(PRAYER_SETTINGS) { put("timeFormat", value.stored) }
    }

    fun setNotificationEnabled(key: NotifKey, enabled: Boolean) {
        saveNotifications(notifications.copy(enabled = notifications.enabled + (key to enabled)))
    }

    fun setNotificationSound(key: NotifKey, sound: NotificationSound) {
        if (sound !in key.availableSounds) return
        saveNotifications(notifications.copy(sounds = notifications.sounds + (key to sound)))
    }

    fun setNotificationTiming(timing: NotificationTiming) {
        saveNotifications(notifications.copy(timing = timing))
    }

    private fun saveNotifications(value: NotificationSettings) {
        notifications = value
        update(PRAYER_SETTINGS) {
            put("notificationSettings", JSONObject().apply { value.enabled.forEach { (k, v) -> put(k.stored, v) } })
            put("notificationSounds", JSONObject().apply { value.sounds.forEach { (k, v) -> put(k.stored, v.stored) } })
            put("notificationVolumeTypes", JSONObject().apply { value.volumes.forEach { (k, v) -> put(k.stored, v.stored) } })
            put("notificationTiming", value.timing.stored)
        }
        remindersChanged()
    }

    /** As PrayerTimesSettings.fromMap, including the old index keys (0 Fajr … 5 Isha). */
    private fun parseNotifications(json: JSONObject): NotificationSettings {
        val defaults = NotificationSettings()
        val legacy = listOf(NotifKey.Fajr, NotifKey.Sunrise, NotifKey.Dhuhr, NotifKey.Asr, NotifKey.Maghrib, NotifKey.Isha)
        val enabled = defaults.enabled.toMutableMap()
        json.optJSONObject("notificationSettings")?.let { map ->
            map.keys().forEach { name ->
                val key = NotifKey.of(name) ?: name.toIntOrNull()?.let(legacy::getOrNull) ?: return@forEach
                (map.opt(name) as? Boolean)?.let { enabled[key] = it }
            }
        }
        val sounds = defaults.sounds.toMutableMap()
        json.optJSONObject("notificationSounds")?.let { map ->
            map.keys().forEach { name ->
                val key = NotifKey.of(name) ?: return@forEach
                NotificationSound.of(map.optString(name))?.let { sounds[key] = it }
            }
        }
        val volumes = defaults.volumes.toMutableMap()
        json.optJSONObject("notificationVolumeTypes")?.let { map ->
            map.keys().forEach { name ->
                val key = NotifKey.of(name) ?: return@forEach
                VolumeType.of(map.optString(name))?.let { volumes[key] = it }
            }
        }
        return NotificationSettings(enabled, sounds, volumes, NotificationTiming.of(json.optString("notificationTiming")))
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
        Reminders.onLocationChanged(app)
    }

    /** A shared_preferences int, which Flutter's setInt stores as a long. */
    fun int(key: String): Int? = (prefs.all[k(key)] as? Number)?.toInt()

    fun setInt(key: String, value: Int) {
        prefs.edit().putLong(k(key), value.toLong()).apply()
    }

    /** Every stored key and value, for the developer panel in debug builds. */
    fun everything(): Map<String, Any?> = prefs.all.toSortedMap()

    /** Removes one stored key (as stored, with its prefix) and reloads, for the developer panel. */
    fun remove(storedKey: String) {
        prefs.edit().remove(storedKey).commit()
        load(app)
    }

    /** Removes every stored key and reloads, for the developer panel. */
    fun clearAll() {
        prefs.edit().clear().commit()
        load(app)
    }

    /** A plain shared_preferences string, such as the chosen reciter. */
    fun string(key: String): String? = prefs.getString(k(key), null)

    fun setString(key: String, value: String) {
        prefs.edit().putString(k(key), value).apply()
    }

    /** A plain shared_preferences bool with its default, such as isQuranAutoScrollEnabled. */
    fun bool(key: String, default: Boolean): Boolean = prefs.getBoolean(k(key), default)

    /** A plain shared_preferences flag, such as a "seen" marker. */
    fun flag(key: String): Boolean = prefs.getBoolean(k(key), false)

    fun setFlag(key: String) {
        prefs.edit().putBoolean(k(key), true).apply()
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
