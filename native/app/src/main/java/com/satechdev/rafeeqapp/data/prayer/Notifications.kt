package com.satechdev.rafeeqapp.data.prayer

import androidx.annotation.StringRes
import com.satechdev.rafeeqapp.R

/*
 * The Flutter app's notification settings (lib/ui/prayer_times/prayer_times_settings.dart),
 * stored by the same names inside prayer_times_settings.
 */

/** What a notification is for; Suhoor and Iftar have their own toggles. */
enum class NotifKey(@StringRes val label: Int) {
    Suhoor(R.string.suhoor),
    Fajr(R.string.prayerFajr),
    Sunrise(R.string.prayerSunrise),
    Dhuhr(R.string.prayerDhuhr),
    Asr(R.string.prayerAsr),
    Iftar(R.string.iftar),
    Maghrib(R.string.prayerMaghrib),
    Isha(R.string.prayerIsha),
    Eid(R.string.eidPrayer);

    val stored get() = name.replaceFirstChar(Char::lowercase)

    val isDailyPrayer get() = this in listOf(Fajr, Dhuhr, Asr, Maghrib, Isha)

    /** The azan sound is for the five prayers only. */
    val availableSounds get() = if (isDailyPrayer) NotificationSound.entries else listOf(NotificationSound.Silent, NotificationSound.Default)

    /** The Flutter defaults: the azan at alarm volume for the five prayers, the default sound otherwise. */
    val defaultSound get() = if (isDailyPrayer) NotificationSound.Azan else NotificationSound.Default
    val defaultVolume get() = if (isDailyPrayer) VolumeType.Alarm else VolumeType.Notification

    companion object {
        fun of(stored: String) = entries.firstOrNull { it.stored == stored }
    }
}

enum class NotificationSound(val stored: String, @StringRes val label: Int) {
    Silent("silent", R.string.soundSilent),
    Default("default_", R.string.soundDefault),
    /** assets/sounds/azan1.mp3. */
    Azan("falahSound", R.string.soundSalah);

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored }
    }
}

enum class VolumeType(val stored: String) {
    Notification("notification"), Alarm("alarm");

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored }
    }
}

enum class NotificationTiming(val stored: String, val minutesEarlier: Int) {
    OnTime("onTime", 0),
    FiveMinutesEarlier("fiveMinutesEarlier", 5),
    TenMinutesEarlier("tenMinutesEarlier", 10),
    FifteenMinutesEarlier("fifteenMinutesEarlier", 15);

    companion object {
        fun of(stored: String?) = entries.firstOrNull { it.stored == stored } ?: OnTime
    }
}

/** Every notification setting, with the Flutter defaults for anything never saved. */
data class NotificationSettings(
    val enabled: Map<NotifKey, Boolean> = NotifKey.entries.associateWith { true },
    val sounds: Map<NotifKey, NotificationSound> = NotifKey.entries.associateWith { it.defaultSound },
    val volumes: Map<NotifKey, VolumeType> = NotifKey.entries.associateWith { it.defaultVolume },
    val timing: NotificationTiming = NotificationTiming.OnTime,
) {
    fun isEnabled(key: NotifKey) = enabled[key] == true
    fun sound(key: NotifKey) = sounds[key] ?: key.defaultSound
    fun volume(key: NotifKey) = volumes[key] ?: key.defaultVolume
}
