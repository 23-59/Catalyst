package com.goldenminute.catalyst.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.goldenminute.catalyst.MainActivity
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.ui.TimerStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.appDataStore

class TimerService : Service() {

    companion object {
        var isServiceRunning = false
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val CHANNEL_ID = "TimerServiceChannel"
    private val NOTIFICATION_ID = 1001
    private var isObserving = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val actionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_STOP_TIMER" -> {
                    TimerStateManager.resetCurrentMode()
                    stopSelf()
                }
                "ACTION_PAUSE_TIMER" -> {
                    TimerStateManager.isTimerRunning.value = false
                }
                "ACTION_RESUME_TIMER" -> {
                    TimerStateManager.isTimerRunning.value = true
                }
                "ACTION_DISMISS_NOTIFICATION" -> {
                    stopSelf()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        TimerStateManager.initialize(applicationContext)
        createNotificationChannel()

        // Acquire a partial wake lock to prevent the CPU from sleeping
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "catalyst:TimerWakeLock").apply {
            acquire()
        }

        val filter = android.content.IntentFilter().apply {
            addAction("ACTION_STOP_TIMER")
            addAction("ACTION_PAUSE_TIMER")
            addAction("ACTION_RESUME_TIMER")
            addAction("ACTION_DISMISS_NOTIFICATION")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(actionReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification()
        
        if (!isObserving) {
            isObserving = true
            observeTimerState()
        }

        return START_STICKY
    }

    private fun observeTimerState() {
        serviceScope.launch {
            combine(
                TimerStateManager.isTimerRunning,
                TimerStateManager.timeLeft,
                TimerStateManager.timeElapsed,
                TimerStateManager.isPomodoro,
                TimerStateManager.pomodoroMode
            ) { isRunning, left, elapsed, isPom, mode ->
                TimerStateData(isRunning, left, elapsed, isPom, mode)
            }
            .distinctUntilChanged()
            .collect { state ->
                // Keep the notification alive even if paused, so users can resume it.
                updateNotification(state)
            }
        }
    }

    private fun updateNotification(state: TimerStateData) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun startForegroundServiceWithNotification() {
        val initialState = TimerStateData(
            isRunning = TimerStateManager.isTimerRunning.value,
            timeLeft = TimerStateManager.timeLeft.value,
            timeElapsed = TimerStateManager.timeElapsed.value,
            isPomodoro = TimerStateManager.isPomodoro.value,
            pomodoroMode = TimerStateManager.pomodoroMode.value
        )
        
        val notification = buildNotification(initialState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE 
                else 
                    0
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(state: TimerStateData): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent("ACTION_STOP_TIMER").setPackage(packageName)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseResumeAction = if (state.isRunning) "ACTION_PAUSE_TIMER" else "ACTION_RESUME_TIMER"
        val prRequestCode = if (state.isRunning) 2 else 4
        val pauseIntent = Intent(pauseResumeAction).setPackage(packageName)
        val pausePendingIntent = PendingIntent.getBroadcast(
            this, prRequestCode, pauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val saveIntent = Intent(this, MainActivity::class.java).apply {
            action = "ACTION_SAVE_TIMER"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val savePendingIntent = PendingIntent.getActivity(
            this, 3, saveIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val deleteIntent = Intent("ACTION_DISMISS_NOTIFICATION").setPackage(packageName)
        val deletePendingIntent = PendingIntent.getBroadcast(
            this, 5, deleteIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val isFa = runBlocking { 
            applicationContext.appDataStore.data.first()[DataStoreKeys.LANGUAGE] == "fa" 
        }

        val stopStr = if (isFa) "توقف" else "Stop"
        val pauseStr = if (state.isRunning) {
            if (isFa) "مکث" else "Pause"
        } else {
            if (isFa) "ادامه" else "Resume"
        }
        val saveStr = if (isFa) "ذخیره" else "Save"
        
        val title = if (state.isPomodoro) {
            when (state.pomodoroMode) {
                "Focus" -> if (isFa) "تمرکز" else "Focus"
                "Short Break" -> if (isFa) "استراحت کوتاه" else "Short Break"
                "Long Break" -> if (isFa) "استراحت طولانی" else "Long Break"
                else -> state.pomodoroMode
            }
        } else {
            if (isFa) "کرنومتر" else "Stopwatch"
        }
        
        val timeValue = if (state.isPomodoro) state.timeLeft else state.timeElapsed
        
        val totalSeconds = (timeValue / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val contentText = String.format("%02d:%02d", minutes, seconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_name) // Use the generated text image asset
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stat_name, stopStr, stopPendingIntent)
            .addAction(R.drawable.ic_stat_name, pauseStr, pausePendingIntent)
            .addAction(R.drawable.ic_stat_name, saveStr, savePendingIntent)
            .setOngoing(state.isRunning)
            .setDeleteIntent(deletePendingIntent)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active timer in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceScope.cancel()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        unregisterReceiver(actionReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private data class TimerStateData(
        val isRunning: Boolean,
        val timeLeft: Long,
        val timeElapsed: Long,
        val isPomodoro: Boolean,
        val pomodoroMode: String
    )
}
