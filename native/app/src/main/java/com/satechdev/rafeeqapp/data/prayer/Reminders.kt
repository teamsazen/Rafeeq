package com.satechdev.rafeeqapp.data.prayer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.satechdev.rafeeqapp.MainActivity
import com.satechdev.rafeeqapp.R
import com.satechdev.rafeeqapp.data.AppLanguage
import com.satechdev.rafeeqapp.data.Settings
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Prayer reminders, as lib/ui/prayer_times/prayer_notification_manager.dart
 * and notification_service.dart: ten days ahead, exact alarms when allowed,
 * the same notification IDs (day × 10 + key) and the same silent and default
 * channels, so the settings people made for those in the Flutter app carry
 * over (the azan channels are new, see createChannels).
 *
 * Beyond Flutter: a daily refresh keeps the ten days topped up even when the
 * app is not opened, and reminders are rebuilt after a reboot, an update and
 * a change of time or time zone.
 */
object Reminders {
    private const val DAYS = 10
    private const val SIGNATURE = "native.last_prayer_times_signature"
    private const val LAST_LOCATION = "native.last_schedule_location"
    private const val FLUTTER_ALARMS_CLEARED = "native.flutter_alarms_cleared"
    private const val REFRESH_ID = 9_999
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_BODY = "body"
    private const val EXTRA_CHANNEL = "channel"
    private const val EXTRA_ID = "id"

    /** Reschedules when anything that changes the times has changed since last time (or always, with [force]). */
    fun schedule(context: Context, force: Boolean = false) {
        val app = context.applicationContext
        clearFlutterAlarms(app)
        val location = Settings.location ?: return
        val prefs = prefs(app)
        val signature = signature(location)
        if (!force && prefs.getString(SIGNATURE, null) == signature) return

        cancelAll(app)
        val settings = Settings.notifications
        if (NotifKey.entries.none(settings::isEnabled) || !NotificationManagerCompat.from(app).areNotificationsEnabled()) {
            prefs.edit().putString(SIGNATURE, signature).apply()
            return
        }
        createChannels(app)
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        for (day in 0 until DAYS) {
            val date = today.plusDays(day.toLong())
            val times = prayerTimes(location, date, Settings.calcMethod, Settings.asrMethod, Settings.highLatitude)
            val hijri = Settings.hijriCorrection.hijri(date)
            val ramadan = hijri.month == 9
            fun add(key: NotifKey, at: Long) {
                val time = at - settings.timing.minutesEarlier * 60_000L
                if (time < now || !settings.isEnabled(key)) return
                val id = day * 10 + key.ordinal
                val local = time.local()
                alarm(app, id, time, title(key, Settings.language, local), body(Settings.language), channelFor(settings.sound(key), settings.volume(key)))
            }
            if (ramadan) {
                add(NotifKey.Suhoor, times.fajr)
                add(NotifKey.Iftar, times.maghrib)
            }
            add(NotifKey.Fajr, times.fajr)
            add(NotifKey.Sunrise, times.sunrise)
            add(NotifKey.Dhuhr, times.dhuhr)
            add(NotifKey.Asr, times.asr)
            add(NotifKey.Maghrib, times.maghrib)
            add(NotifKey.Isha, times.isha)
            if ((ramadan && hijri.day >= 29) || (hijri.month == 10 && hijri.day == 1)) {
                add(NotifKey.Eid, times.sunrise + 30 * 60_000L)
            }
        }
        // Top the ten days up tomorrow shortly after midnight, even if the app stays closed.
        val refresh = today.plusDays(1).atTime(0, 5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        setAlarm(app, refresh, refreshIntent(app))
        prefs.edit().putString(SIGNATURE, signature).apply()
    }

    /** Reschedules when the location moved 5 km or more, as Flutter's _handleLocationChange. */
    fun onLocationChanged(context: Context) {
        val location = Settings.location ?: return
        val prefs = prefs(context)
        val last = prefs.getString(LAST_LOCATION, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val moved = last == null || distanceKm(last.optDouble("lat"), last.optDouble("lng"), location.latitude, location.longitude) >= 5.0
        prefs.edit().putString(LAST_LOCATION, JSONObject().put("lat", location.latitude).put("lng", location.longitude).toString()).apply()
        if (moved) schedule(context, force = true)
    }

    /** Debug builds only (DebugReminderReceiver): one azan reminder [seconds] from now, to try the channels and sound. */
    fun scheduleTest(context: Context, seconds: Int) {
        createChannels(context)
        val at = System.currentTimeMillis() + seconds * 1000L
        alarm(context, DAYS * 10 - 1, at, "Test · ${title(NotifKey.Maghrib, Settings.language, at.local())}", body(Settings.language), channelFor(NotificationSound.Azan, VolumeType.Alarm))
    }

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    // ---------- Content (prayer_notification_content.dart) ----------

    fun title(key: NotifKey, language: AppLanguage, time: LocalDateTime): String {
        val urdu = language == AppLanguage.Urdu
        return when (key) {
            NotifKey.Suhoor -> if (urdu) "سحری" else "Suhoor"
            NotifKey.Fajr -> if (urdu) "فجر" else "Fajr"
            NotifKey.Sunrise -> if (urdu) "طلوع آفتاب" else "Sunrise"
            NotifKey.Dhuhr -> if (time.dayOfWeek == DayOfWeek.FRIDAY) (if (urdu) "جمعہ" else "Jumma") else (if (urdu) "ظہر" else "Dhuhr")
            NotifKey.Asr -> if (urdu) "عصر" else "Asr"
            NotifKey.Iftar -> if (urdu) "افطار" else "Iftar"
            NotifKey.Maghrib -> if (urdu) "مغرب" else "Maghrib"
            NotifKey.Isha -> if (urdu) "عشاء" else "Isha"
            NotifKey.Eid -> if (urdu) "عید کی نماز" else "Eid Prayer"
        }
    }

    fun body(language: AppLanguage) = if (language == AppLanguage.Urdu) "نماز کا وقت ہے۔" else "It is prayer time."

    // ---------- Channels (notification_service.dart) ----------

    private fun channelFor(sound: NotificationSound, volume: VolumeType) = when (sound) {
        NotificationSound.Silent -> "prayer_silent"
        NotificationSound.Default -> "prayer_default"
        NotificationSound.Azan -> if (volume == VolumeType.Alarm) "prayer_azan_alarm" else "prayer_azan_notification"
    }

    internal fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        // The azan by name, not by resource number: the Flutter app's azan channels stored its
        // own APK's number, which can point at another file in this one. A channel's sound
        // cannot change once made, so the azan has new channels and the old ones go.
        manager.deleteNotificationChannel("prayer_custom_azan1_alarm")
        manager.deleteNotificationChannel("prayer_custom_azan1_notif")
        val azan = Uri.parse("android.resource://${context.packageName}/raw/azan1")
        fun channel(id: String, name: String, sound: Uri?, usage: Int?, silent: Boolean) =
            NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for prayer times"
                if (silent) {
                    setSound(null, null)
                    enableVibration(false)
                } else {
                    enableVibration(true)
                    if (sound != null && usage != null) {
                        setSound(sound, AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    }
                }
            }
        // Existing channels (from the Flutter app too) keep their settings; this only adds missing ones.
        manager.createNotificationChannels(
            listOf(
                channel("prayer_silent", "Prayer Times - Silent", null, null, silent = true),
                channel("prayer_default", "Prayer Times - Default", null, null, silent = false),
                channel("prayer_azan_alarm", "Prayer Times - Custom (Alarm Volume)", azan, AudioAttributes.USAGE_ALARM, silent = false),
                channel("prayer_azan_notification", "Prayer Times - Custom (Notification Volume)", azan, AudioAttributes.USAGE_NOTIFICATION, silent = false),
            ),
        )
    }

    // ---------- Alarms ----------

    private fun alarm(context: Context, id: Int, at: Long, title: String, body: String, channel: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("com.satechdev.rafeeqapp.REMINDER")
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_BODY, body)
            .putExtra(EXTRA_CHANNEL, channel)
        setAlarm(context, at, PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
    }

    private fun setAlarm(context: Context, at: Long, operation: PendingIntent) {
        val manager = context.getSystemService(AlarmManager::class.java)
        if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        } else {
            // Without the exact alarm permission, as Flutter's inexactAllowWhileIdle.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        }
    }

    private fun refreshIntent(context: Context) = PendingIntent.getBroadcast(
        context, REFRESH_ID,
        Intent(context, ReminderReceiver::class.java).setAction("com.satechdev.rafeeqapp.REFRESH"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun cancelAll(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(refreshIntent(context))
        for (id in 0 until DAYS * 10) {
            val intent = Intent(context, ReminderReceiver::class.java).setAction("com.satechdev.rafeeqapp.REMINDER")
            PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
                manager.cancel(it)
                it.cancel()
            }
        }
    }

    /**
     * The Flutter app's alarms point at its plugin's receiver, which the
     * native app does not have: cancel them once so they never fire into nothing.
     */
    private fun clearFlutterAlarms(context: Context) {
        val prefs = prefs(context)
        if (prefs.getBoolean(FLUTTER_ALARMS_CLEARED, false)) return
        val manager = context.getSystemService(AlarmManager::class.java)
        val receiver = ComponentName(context.packageName, "com.dexterous.flutterlocalnotifications.ScheduledNotificationReceiver")
        for (id in 0 until DAYS * 10) {
            val intent = Intent().setComponent(receiver)
            runCatching {
                PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            }.getOrNull()?.let {
                manager.cancel(it)
                it.cancel()
            }
        }
        prefs.edit().putBoolean(FLUTTER_ALARMS_CLEARED, true).apply()
    }

    internal fun show(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: "prayer_default"
        createChannels(context)
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
        }
    }

    // ---------- Helpers ----------

    private fun prefs(context: Context) = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE)

    /** Location, method, timing, language, today's times and the date: anything that changes what is scheduled. */
    private fun signature(location: UserLocation): String {
        val today = LocalDate.now()
        val times = prayerTimes(location, today, Settings.calcMethod, Settings.asrMethod, Settings.highLatitude)
        val n = Settings.notifications
        return listOf(
            location.latitude, location.longitude, Settings.calcMethod.stored, Settings.asrMethod.stored,
            Settings.highLatitude.stored, n.timing.stored, Settings.language.code, Settings.hijriCorrection.stored,
            n.enabled.toString(), n.sounds.toString(), n.volumes.toString(),
            times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha, today,
        ).joinToString("|")
    }

    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * asin(sqrt(a))
    }
}

/** Shows a reminder, or refreshes the schedule, when an alarm fires. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.satechdev.rafeeqapp.REMINDER" -> Reminders.show(context, intent)
            "com.satechdev.rafeeqapp.REFRESH" -> {
                Settings.load(context)
                Reminders.schedule(context, force = true)
            }
        }
    }
}

/** Rebuilds the reminders after a reboot, an app update, or a change of time or time zone. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Settings.load(context)
        Reminders.schedule(context, force = true)
    }
}
