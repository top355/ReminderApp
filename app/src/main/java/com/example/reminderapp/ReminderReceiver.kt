package com.example.reminderapp

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Acquire a WakeLock so the device stays awake long enough to fire the notification.
        // Without this, the device might go back to sleep before the notification is posted,
        // especially on HarmonyOS compatibility layers.
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "reminderapp:fire"
        )
        wakeLock.acquire(15_000) // 15 seconds max

        try {
            val label = intent.getStringExtra("label") ?: "提醒时间到了"
            val reminderId = intent.getIntExtra("id", -1)

            showNotification(context, label)

            // 触发后处理：每天重复则排到明天；单次则触发后从列表移除。
            if (reminderId != -1) {
                val repo = ReminderRepository(context)
                val reminder = repo.getAll().firstOrNull { it.id == reminderId }
                if (reminder != null) {
                    if (reminder.repeat) {
                        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        AlarmScheduler.schedule(context, am, reminder)
                    } else {
                        repo.remove(reminder.id)
                    }
                }
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun showNotification(context: Context, label: String) {
        val channelId = "reminder_channel"
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "提醒通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                enableVibration(true)
                // Bypass Do Not Disturb so the alarm always sounds
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use FLAG_INSISTENT so the alarm keeps ringing until the user dismisses it
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("提醒")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 500, 500, 500, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
