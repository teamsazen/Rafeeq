package com.satechdev.rafeeqapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.satechdev.rafeeqapp.data.Settings
import com.satechdev.rafeeqapp.data.prayer.Reminders

/**
 * Debug builds only: lets a developer try reminders from the shell.
 *
 *     adb shell am broadcast -n com.satechdev.rafeeqapp.debug/com.satechdev.rafeeqapp.DebugReminderReceiver -a test --ei seconds 60
 *     adb shell am broadcast -n com.satechdev.rafeeqapp.debug/com.satechdev.rafeeqapp.DebugReminderReceiver -a reschedule
 *     adb shell am broadcast -n com.satechdev.rafeeqapp.debug/com.satechdev.rafeeqapp.DebugReminderReceiver -a channels
 */
class DebugReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Settings.load(context)
        when (intent.action) {
            "test" -> Reminders.scheduleTest(context, intent.getIntExtra("seconds", 60))
            "reschedule" -> Reminders.schedule(context, force = true)
            "channels" -> Reminders.createChannels(context)
        }
    }
}
