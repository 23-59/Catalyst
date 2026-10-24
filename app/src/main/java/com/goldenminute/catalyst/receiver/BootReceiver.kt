package com.goldenminute.catalyst.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.goldenminute.catalyst.data.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_TIMEZONE_CHANGED || 
            intent.action == Intent.ACTION_TIME_CHANGED) {
            
            com.goldenminute.catalyst.service.PowerButtonShortcutManager.init(context)

            val scope = CoroutineScope(Dispatchers.IO)
            val db = TodoDatabase.getDatabase(context, scope)
            scope.launch {
                val currentTime = System.currentTimeMillis()
                val pendingReminders = db.todoDao().getPendingReminders(currentTime)
                for (todo in pendingReminders) {
                    todo.reminderTime?.let { time ->
                        ReminderScheduler.schedule(
                            context,
                            todo.id,
                            todo.title,
                            todo.description,
                            time
                        )
                    }
                }
            }
        }
    }
}
