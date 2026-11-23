package com.satechdev.rafeeqapp.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.hijri.HijriCorrection
import com.satechdev.rafeeqapp.data.prayer.AsrMethod
import com.satechdev.rafeeqapp.data.prayer.HighLatitude
import com.satechdev.rafeeqapp.data.prayer.PrayerCalcMethod
import com.satechdev.rafeeqapp.data.prayer.TimeFormat
import com.satechdev.rafeeqapp.tr
import com.satechdev.rafeeqapp.ui.hijriShort
import java.time.LocalDate

/*
 * The prayer settings pickers, as the Flutter picker screens in lib/ui/app_home/home: one
 * card per choice, in the Flutter app's order.
 */

@Composable
fun CalculationMethodScreen() = PushedScreen(tr(R.string.calculationMethod)) {
    PrayerCalcMethod.entries.forEach { method ->
        item(key = method.stored) {
            PickerTile(tr(method.label), selected = Settings.calcMethod == method) { Settings.setCalcMethod(method) }
        }
    }
}

@Composable
fun JuristicMethodScreen() = PushedScreen(tr(R.string.juristicMethod)) {
    AsrMethod.entries.forEach { method ->
        item(key = method.stored) {
            PickerTile(tr(method.label), selected = Settings.asrMethod == method) { Settings.setAsrMethod(method) }
        }
    }
}

@Composable
fun HighLatitudeRuleScreen() = PushedScreen(tr(R.string.highLatitudeRule)) {
    HighLatitude.entries.forEach { rule ->
        item(key = rule.stored) {
            PickerTile(tr(rule.label), selected = Settings.highLatitude == rule) { Settings.setHighLatitude(rule) }
        }
    }
}

/** Each correction shows the Hijri date it would give today. */
@Composable
fun HijriCorrectionScreen() {
    val today = remember { LocalDate.now() }
    PushedScreen(tr(R.string.hijriCorrection)) {
        HijriCorrection.entries.forEach { correction ->
            item(key = correction.stored) {
                PickerTile(
                    tr(correction.label),
                    selected = Settings.hijriCorrection == correction,
                    subtitle = hijriShort(correction.hijri(today)),
                ) { Settings.setHijriCorrection(correction) }
            }
        }
    }
}

@Composable
fun TimeFormatScreen() = PushedScreen(tr(R.string.timeFormat)) {
    TimeFormat.entries.forEach { format ->
        item(key = format.stored) {
            PickerTile(tr(format.label), selected = Settings.timeFormat == format) { Settings.setTimeFormat(format) }
        }
    }
}
