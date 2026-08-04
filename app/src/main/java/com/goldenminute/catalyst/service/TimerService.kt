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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.appDataStore
import androidx.datastore.preferences.core.edit

class TimerService : Service() {

    companion object {
        var isServiceRunning = false
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val CHANNEL_ID = "TimerServiceChannel"
    private val NOTIFICATION_ID = 1001
    private var isObserving = false
    private var wakeLock: PowerManager.WakeLock? = null

    // Actions are now handled in onStartCommand directly.

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

        // Receiver removed, using onStartCommand for actions instead
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            "ACTION_SAVE_TIMER" -> {
                handleSaveTimer()
            }
        }
        
        if (intent?.action == null || intent.action == "ACTION_START_TIMER") {
            startForegroundServiceWithNotification()
            
            if (!isObserving) {
                isObserving = true
                observeTimerState()
            }
        }

        return START_STICKY
    }

    private fun handleSaveTimer() {
        val activeId = TimerStateManager.activeTaskId.value
        val isIdle = TimerStateManager.isIdleMode.value
        val isPom = TimerStateManager.isPomodoro.value
        val elapsed = TimerStateManager.timeElapsed.value
        
        val database = com.goldenminute.catalyst.data.TodoDatabase.getDatabase(applicationContext, serviceScope)
        val repository = com.goldenminute.catalyst.data.TodoRepository(database.todoDao(), database.categoryDao(), database.noteDao())
        
        serviceScope.launch {
            if (activeId != -1) {
                val newRecords = mutableListOf<com.goldenminute.catalyst.data.RecordedTimeItem>()
                if (isPom) {
                    newRecords.add(com.goldenminute.catalyst.data.RecordedTimeItem(
                        isPomodoro = true, value = 1L, timestamp = System.currentTimeMillis()
                    ))
                } else if (elapsed > 0) {
                    newRecords.add(com.goldenminute.catalyst.data.RecordedTimeItem(
                        isPomodoro = false, value = elapsed, timestamp = System.currentTimeMillis()
                    ))
                }
                
                if (newRecords.isNotEmpty()) {
                    val todo = repository.getTodoById(activeId)
                    if (todo != null) {
                        val existingList = com.goldenminute.catalyst.data.RecordedTimeItem.parseList(todo.recordedTimes).toMutableList()
                        existingList.addAll(newRecords)
                        
                        var extraTime = 0L
                        var extraCycles = 0
                        for (rec in newRecords) {
                            if (rec.isPomodoro) extraCycles += rec.value.toInt()
                            else extraTime += rec.value
                        }
                        
                        val updatedTodo = todo.copy(
                            elapsedTime = todo.elapsedTime + extraTime,
                            pomodoroCycles = todo.pomodoroCycles + extraCycles,
                            recordedTimes = com.goldenminute.catalyst.data.RecordedTimeItem.serializeList(existingList)
                        )
                        database.todoDao().updateTodo(updatedTodo)
                    }
                }
            } else if (isIdle) {
                var newRecord: com.goldenminute.catalyst.data.RecordedTimeItem? = null
                if (isPom) {
                    newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                        isPomodoro = true, value = 1L, timestamp = System.currentTimeMillis()
                    )
                } else if (elapsed > 0) {
                    newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                        isPomodoro = false, value = elapsed, timestamp = System.currentTimeMillis()
                    )
                }
                
                if (newRecord != null) {
                    val currentListStr = applicationContext.appDataStore.data.first()[DataStoreKeys.FREE_MODE_SESSIONS] ?: ""
                    val currentList = com.goldenminute.catalyst.data.RecordedTimeItem.parseList(currentListStr).toMutableList()
                    currentList.add(newRecord)
                    val serialized = com.goldenminute.catalyst.data.RecordedTimeItem.serializeList(currentList)
                    applicationContext.appDataStore.edit { it[DataStoreKeys.FREE_MODE_SESSIONS] = serialized }
                    
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(applicationContext, applicationContext.getString(R.string.free_focus_session_saved), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            TimerStateManager.isTimerRunning.value = false
            TimerStateManager.timeElapsed.value = 0L
            stopSelf()
        }
    }

    private fun observeTimerState() {
        serviceScope.launch {
            combine(
                TimerStateManager.isTimerRunning,
                TimerStateManager.timeLeft.map { it / 1000 },
                TimerStateManager.timeElapsed.map { it / 1000 },
                TimerStateManager.isPomodoro,
                TimerStateManager.pomodoroMode
            ) { isRunning, leftSec, elapsedSec, isPom, mode ->
                TimerStateData(isRunning, leftSec, elapsedSec, isPom, mode)
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

        val stopIntent = Intent(this, TimerService::class.java).apply { action = "ACTION_STOP_TIMER" }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseResumeAction = if (state.isRunning) "ACTION_PAUSE_TIMER" else "ACTION_RESUME_TIMER"
        val prRequestCode = if (state.isRunning) 2 else 4
        val pauseIntent = Intent(this, TimerService::class.java).apply { action = pauseResumeAction }
        val pausePendingIntent = PendingIntent.getService(
            this, prRequestCode, pauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val saveIntent = Intent(this, TimerService::class.java).apply { action = "ACTION_SAVE_TIMER" }
        val savePendingIntent = PendingIntent.getService(
            this, 3, saveIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        val deleteIntent = Intent(this, TimerService::class.java).apply { action = "ACTION_DISMISS_NOTIFICATION" }
        val deletePendingIntent = PendingIntent.getService(
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
        
        val timeValueSeconds = if (state.isPomodoro) state.timeLeft else state.timeElapsed
        
        val hours = timeValueSeconds / 3600
        val minutes = (timeValueSeconds % 3600) / 60
        val seconds = timeValueSeconds % 60
        
        val contentText = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

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
