package com.example.reminderapp

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val repo = ReminderRepository(context)
            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            repo.getAll().forEach { reminder ->
                AlarmScheduler.schedule(context, alarmManager, reminder)
            }
        }
    }
}
