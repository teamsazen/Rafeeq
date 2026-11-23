package com.satechdev.rafeeqapp.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics, with the Flutter app's screen and event names so the
 * reports carry on across the move (lib/data/services/analytics_service.dart).
 * Firebase only starts in release builds, which carry its config; elsewhere
 * every call does nothing.
 */
object Analytics {
    @Volatile private var firebase: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        firebase = FirebaseAnalytics.getInstance(context).apply { setAnalyticsCollectionEnabled(true) }
    }

    fun event(name: String, vararg params: Pair<String, String>) {
        val f = firebase ?: return
        f.logEvent(name, Bundle().apply { params.forEach { (k, v) -> putString(k, v) } })
    }

    fun screen(name: String) {
        val f = firebase ?: return
        f.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, name)
        })
    }

    // ---------- Settings, as AnalyticsEventNames ----------

    fun languageChanged(code: String) {
        val value = when (code) { "en" -> "english"; "ur" -> "urdu"; else -> "other" }
        event("settings_language_changed_to_$value", "language" to value)
    }

    fun themeChanged(stored: String) {
        val value = stored.takeIf { it in setOf("light", "dark", "system") } ?: "other"
        event("settings_theme_changed_to_$value", "theme" to value)
    }

    /** [stored] is already the snake_case name Flutter meant to send. */
    fun calculationMethodChanged(stored: String) = event("calc_method_to_$stored", "calculation_method" to stored)

    fun juristicMethodChanged(stored: String) {
        val value = if (stored == "others") "standard" else stored
        event("juristic_method_to_$value", "juristic_method" to value)
    }

    fun highLatitudeChanged(stored: String) {
        val value = when (stored) {
            "angleBased" -> "angle_based"
            "middleOfTheNight" -> "middle_of_night"
            "oneSeventh" -> "one_seventh"
            else -> "other"
        }
        event("high_latitude_rule_to_$value", "high_latitude_rule" to value)
    }

    fun hijriCorrectionChanged(stored: String) {
        val value = when (stored) {
            "minus2Days" -> "minus_2_days"
            "minus1Day" -> "minus_1_day"
            "plus1Day" -> "plus_1_day"
            "plus2Days" -> "plus_2_days"
            else -> "none"
        }
        event("hijri_correction_to_$value", "hijri_correction" to value)
    }
}
