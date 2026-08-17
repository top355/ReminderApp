package com.example.reminderapp

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Reschedule all alarms on any system event that might have cleared them.
        // HarmonyOS compatibility layers may deliver different boot/time-change broadcasts
        // than stock Android, so we listen for all of them.
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        AlarmScheduler.rescheduleAll(context, alarmManager)
    }
}
