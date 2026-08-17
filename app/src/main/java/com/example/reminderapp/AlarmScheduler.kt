package com.example.reminderapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {

    fun schedule(
        context: Context,
        alarmManager: AlarmManager,
        reminder: Reminder
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", reminder.id)
            putExtra("label", reminder.label.ifBlank { "提醒时间到了" })
        }
        val pi = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time already passed today, schedule for tomorrow.
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // setAlarmClock is the highest-priority alarm API.
        // The system treats it as a user-visible alarm (shows alarm icon in status bar)
        // and will not batch/delay it. This is far more reliable than setExactAndAllowWhileIdle
        // on stock Android AND on compatibility layers like Zhuoyitong on HarmonyOS.
        val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pi)
        alarmManager.setAlarmClock(info, pi)
    }

    fun cancel(context: Context, alarmManager: AlarmManager, reminderId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    /** Reschedule all stored reminders. Call this on app start and on boot. */
    fun rescheduleAll(context: Context, alarmManager: AlarmManager) {
        val repo = ReminderRepository(context)
        repo.getAll().forEach { reminder ->
            schedule(context, alarmManager, reminder)
        }
    }
}
