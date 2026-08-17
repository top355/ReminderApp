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
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("提醒")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
