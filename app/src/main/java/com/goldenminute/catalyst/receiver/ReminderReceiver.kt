package com.goldenminute.catalyst.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.goldenminute.catalyst.MainActivity
import com.goldenminute.catalyst.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getIntExtra("todo_id", 0)
        val todoTitle = intent.getStringExtra("todo_title") ?: context.getString(R.string.default_reminder_title)
        val todoDesc = intent.getStringExtra("todo_desc") ?: context.getString(R.string.default_reminder_desc)
        val isRepeating = intent.getBooleanExtra("is_repeating", false)
        val triggerTime = intent.getLongExtra("trigger_time", 0L)

        showNotification(context, todoId, todoTitle, todoDesc)

        if (isRepeating && triggerTime > 0) {
            val nextTriggerTime = triggerTime + 24 * 60 * 60 * 1000L
            ReminderScheduler.schedule(context, todoId, todoTitle, todoDesc, nextTriggerTime, true)
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "todo_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.reminder_channel_desc)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name) // Use the generated text image asset
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(id, notification)
    }
}
