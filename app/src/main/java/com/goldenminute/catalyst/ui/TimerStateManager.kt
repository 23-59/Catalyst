package com.goldenminute.catalyst.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.RecordedTimeItem
import com.goldenminute.catalyst.data.TodoDatabase
import com.goldenminute.catalyst.data.TodoRepository
import com.goldenminute.catalyst.data.timerDataStore
import com.goldenminute.catalyst.service.SmartTrackerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object TimerStateManager {
    private const val TAG = "TimerStateManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var appContext: Context? = null
    private var isInitialized = false

    val focusLength = MutableStateFlow(25 * 60 * 1000L)
    val shortBreakLength = MutableStateFlow(5 * 60 * 1000L)
    val longBreakLength = MutableStateFlow(25 * 60 * 1000L)
    
    val isTimerRunning = MutableStateFlow(false)
    val isPomodoro = MutableStateFlow(true)
    val pomodoroMode = MutableStateFlow("Focus") // "Focus", "Short Break", "Long Break"
    val timeLeft = MutableStateFlow(25 * 60 * 1000L)
    val timeElapsed = MutableStateFlow(0L)
    val pomodoroCycle = MutableStateFlow(0) // Cycles completed (0 to 3)
    val activeTaskId = MutableStateFlow(-1)
    val isIdleMode = MutableStateFlow(false)
    
    var mindfulnessCooldownUntil = 0L // Timestamp until which Mindfulness Checks are ignored

    private var targetEndTime = 0L
    private var startTime = 0L

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext

        // Dismiss any lingering timer notification
        dismissTimerNotification()

        scope.launch {
            val prefs = appContext!!.timerDataStore.data.first()

            // Restore state
            isPomodoro.value = prefs[DataStoreKeys.IS_POMODORO] ?: true
            pomodoroMode.value = prefs[DataStoreKeys.POMODORO_MODE] ?: "Focus"
            pomodoroCycle.value = prefs[DataStoreKeys.POMODORO_CYCLE] ?: 0
            activeTaskId.value = prefs[DataStoreKeys.ACTIVE_TASK_ID] ?: -1
            isIdleMode.value = prefs[DataStoreKeys.IS_IDLE_MODE] ?: false
            
            val running = prefs[DataStoreKeys.IS_TIMER_RUNNING] ?: false
            val savedTimeLeft = prefs[DataStoreKeys.TIME_LEFT] ?: (25 * 60 * 1000L)
            val savedTimeElapsed = prefs[DataStoreKeys.TIME_ELAPSED] ?: 0L
            targetEndTime = prefs[DataStoreKeys.TARGET_END_TIME] ?: 0L
            startTime = prefs[DataStoreKeys.START_TIME] ?: 0L

            if (running) {
                val now = System.currentTimeMillis()
                if (isPomodoro.value) {
                    val remaining = targetEndTime - now
                    if (remaining <= 0) {
                        isTimerRunning.value = false
                        // Cycle progression logic if timer ended while in background
                        if (pomodoroMode.value == "Focus") {
                            pomodoroCycle.value += 1
                            if (pomodoroCycle.value >= 3) {
                                pomodoroCycle.value = 0
                                pomodoroMode.value = "Long Break"
                            } else {
                                pomodoroMode.value = "Short Break"
                            }
                        } else {
                            pomodoroMode.value = "Focus"
                        }
                        timeLeft.value = getLengthForMode(pomodoroMode.value)
                    } else {
                        timeLeft.value = remaining
                        isTimerRunning.value = true
                    }
                } else {
                    timeElapsed.value = now - startTime
                    isTimerRunning.value = true
                }
            } else {
                timeLeft.value = savedTimeLeft
                timeElapsed.value = savedTimeElapsed
                isTimerRunning.value = false
            }

            // Track state changes to persist
            launch {
                isTimerRunning.collect { runningState ->
                    val now = System.currentTimeMillis()
                    if (runningState) {
                        if (isPomodoro.value) {
                            targetEndTime = now + timeLeft.value
                        } else {
                            startTime = now - timeElapsed.value
                        }
                    }
                    dismissTimerNotification()
                    saveState()
                }
            }
            launch { isPomodoro.collect { saveState() } }
            launch { pomodoroMode.collect { saveState() } }
            launch { activeTaskId.collect { saveState() } }
            launch { isIdleMode.collect { saveState() } }

            // Precise update loop that handles sleep / doze mode seamlessly and silently counts in the background
            while (true) {
                delay(200L) // State update rate (~5fps)
                val currentTime = System.currentTimeMillis()

                if (isTimerRunning.value) {
                    if (isPomodoro.value) {
                        val remaining = targetEndTime - currentTime
                        if (remaining > 0) {
                            timeLeft.value = remaining
                        } else {
                            // Stop precisely when 0 is reached
                            isTimerRunning.value = false
                            if (pomodoroMode.value == "Focus") {
                                pomodoroCycle.value += 1
                                if (pomodoroCycle.value >= 3) {
                                    pomodoroCycle.value = 0
                                    setPomodoroMode("Long Break")
                                } else {
                                    setPomodoroMode("Short Break")
                                }
                            } else {
                                setPomodoroMode("Focus")
                            }
                        }
                    } else {
                        timeElapsed.value = currentTime - startTime
                    }
                }
            }
        }
    }

    private fun dismissTimerNotification() {
        appContext?.let { ctx ->
            try {
                val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(1001)
                ctx.stopService(Intent(ctx, com.goldenminute.catalyst.service.TimerService::class.java))
            } catch (_: Exception) {}
        }
    }

    private fun saveState() {
        appContext?.let { ctx ->
            scope.launch {
                ctx.timerDataStore.edit { prefs ->
                    prefs[DataStoreKeys.IS_TIMER_RUNNING] = isTimerRunning.value
                    prefs[DataStoreKeys.IS_POMODORO] = isPomodoro.value
                    prefs[DataStoreKeys.POMODORO_MODE] = pomodoroMode.value
                    prefs[DataStoreKeys.TIME_LEFT] = timeLeft.value
                    prefs[DataStoreKeys.TIME_ELAPSED] = timeElapsed.value
                    prefs[DataStoreKeys.POMODORO_CYCLE] = pomodoroCycle.value
                    prefs[DataStoreKeys.ACTIVE_TASK_ID] = activeTaskId.value
                    prefs[DataStoreKeys.IS_IDLE_MODE] = isIdleMode.value
                    prefs[DataStoreKeys.TARGET_END_TIME] = targetEndTime
                    prefs[DataStoreKeys.START_TIME] = startTime
                    prefs[DataStoreKeys.TIMER_FOCUS_LENGTH] = focusLength.value
                    prefs[DataStoreKeys.TIMER_SHORT_BREAK_LENGTH] = shortBreakLength.value
                    prefs[DataStoreKeys.TIMER_LONG_BREAK_LENGTH] = longBreakLength.value
                }
            }
        }
    }

    fun setPomodoroMode(mode: String) {
        pomodoroMode.value = mode
        isTimerRunning.value = false
        timeLeft.value = getLengthForMode(mode)
        saveState()
    }

    fun resetCurrentMode() {
        isTimerRunning.value = false
        timeLeft.value = getLengthForMode(pomodoroMode.value)
        timeElapsed.value = 0L
        saveState()
    }

    fun updateModeLength(mode: String, newLength: Long) {
        when (mode) {
            "Focus" -> focusLength.value = newLength
            "Short Break" -> shortBreakLength.value = newLength
            "Long Break" -> longBreakLength.value = newLength
        }
        if (pomodoroMode.value == mode) {
            isTimerRunning.value = false
            timeLeft.value = newLength
            saveState()
        }
    }

    private fun getLengthForMode(mode: String): Long {
        return when (mode) {
            "Focus" -> focusLength.value
            "Short Break" -> shortBreakLength.value
            "Long Break" -> longBreakLength.value
            else -> focusLength.value
        }
    }

    fun autoStartTask(taskId: Int) {
        val currentTaskId = activeTaskId.value
        
        // If switching from one assigned task to another, auto-save the previous one first
        if (currentTaskId != -1 && currentTaskId != taskId) {
            autoPauseAndSaveTask(currentTaskId)
        }

        // ★ Fix: If timer is already running for THIS task, don't restart
        if (currentTaskId == taskId && isTimerRunning.value) {
            return
        }

        activeTaskId.value = taskId
        isIdleMode.value = false
        if (!isTimerRunning.value) {
            startTime = System.currentTimeMillis()
            timeElapsed.value = 0L
            isTimerRunning.value = true
        }
        saveState()
    }

    fun autoPauseTask(idle: Boolean = false) {
        if (isTimerRunning.value) {
            isTimerRunning.value = false
            if (idle) {
                isIdleMode.value = true
            }
            saveState()
        }
    }

    /**
     * Auto-records and saves the session for the given task when the user leaves the assigned app.
     * Merges with the most recent record if it's within 2 minutes to prevent hundreds of micro-records.
     */
    fun autoPauseAndSaveTask(taskId: Int) {
        val isPom = isPomodoro.value
        val currentElapsed = timeElapsed.value
        val currentLeft = timeLeft.value
        val focusLen = focusLength.value

        isTimerRunning.value = false

        appContext?.let { ctx ->
            scope.launch {
                try {
                    SmartTrackerService.commitCurrentSectionTime()
                    val rawSectionsMap = SmartTrackerService.sessionAppSections.toMap()
                    // Remove "Unknown" and transient "General" sections if other specific sections exist
                    val hasSpecific = rawSectionsMap.keys.any { it != "Unknown" && it != "General" }
                    val sectionsMap = if (hasSpecific) {
                        rawSectionsMap.filterKeys { it != "Unknown" && it != "General" }
                    } else if (rawSectionsMap.keys.any { it != "Unknown" }) {
                        rawSectionsMap.filterKeys { it != "Unknown" }
                    } else {
                        rawSectionsMap
                    }
                    SmartTrackerService.resetSectionTracking()

                    val db = TodoDatabase.getDatabase(ctx, scope)
                    val repository = TodoRepository(db.todoDao(), db.categoryDao(), db.noteDao())
                    val todo = repository.getTodoById(taskId)

                    if (todo != null) {
                        val sectionTotal = sectionsMap.values.sum()
                        val timeSpent = if (isPom) {
                            maxOf((focusLen - currentLeft).coerceAtLeast(0L), sectionTotal)
                        } else {
                            val computed = if (startTime > 0L) (System.currentTimeMillis() - startTime).coerceAtLeast(0L) else 0L
                            maxOf(currentElapsed, computed, sectionTotal)
                        }

                        // ★ Minimum session threshold: don't record if session did not exceed 1 minute (60 seconds)
                        if (timeSpent <= 60_000L) {
                            Log.d(TAG, "Auto-save skipped for task $taskId: session did not exceed 1 minute (${timeSpent}ms)")
                            return@launch
                        }

                        val existingList = RecordedTimeItem.parseList(todo.recordedTimes).toMutableList()
                        val now = System.currentTimeMillis()

                        // Filter out "Unknown" and sections that did not have time more than 1 minute
                        val cleanSections = sectionsMap
                            .filterNot { it.key == "Unknown" }
                            .filter { it.value > 60_000L }

                        // Continuation check:
                        // Only merge with the IMMEDIATELY preceding record if:
                        // 1) It has the same mode (Pomodoro vs Stopwatch)
                        // 2) It finished within the last 3 minutes (180,000 ms)
                        // 3) It occurred on the exact same calendar day
                        val lastRecord = existingList.lastOrNull()
                        val canMerge = lastRecord != null &&
                            lastRecord.isPomodoro == isPom &&
                            (now - lastRecord.timestamp <= 180_000L) &&
                            isSameCalendarDay(now, lastRecord.timestamp)

                        if (canMerge) {
                            val target = lastRecord!!
                            val mergedSections = target.appSections.toMutableMap()
                            for ((sec, time) in cleanSections) {
                                mergedSections[sec] = (mergedSections[sec] ?: 0L) + time
                            }
                            val updatedRecord = lastRecord.copy(
                                value = lastRecord.value + timeSpent,
                                timestamp = now,
                                appSections = mergedSections
                            )
                            existingList[existingList.lastIndex] = updatedRecord
                            Log.d(TAG, "Merged session for task $taskId with recent record: +${timeSpent}ms (new total=${updatedRecord.value}ms, sections: $mergedSections)")
                        } else {
                            val newRecord = RecordedTimeItem(
                                isPomodoro = isPom,
                                value = timeSpent,
                                timestamp = now,
                                appSections = cleanSections
                            )
                            existingList.add(newRecord)
                            Log.d(TAG, "Auto-saved distinct session for task $taskId: +${timeSpent}ms (sections: $cleanSections, total records: ${existingList.size})")
                        }

                        val updatedTodo = todo.copy(
                            elapsedTime = todo.elapsedTime + timeSpent,
                            pomodoroCycles = if (isPom) todo.pomodoroCycles + (timeSpent / focusLen).toInt().coerceAtLeast(1) else todo.pomodoroCycles,
                            recordedTimes = RecordedTimeItem.serializeList(existingList)
                        )
                        db.todoDao().updateTodo(updatedTodo)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to auto-save session for task $taskId", e)
                }
            }
        }

        startTime = 0L
        timeElapsed.value = 0L
        if (isPom) {
            timeLeft.value = focusLength.value
        }
        saveState()
    }

    private fun isSameCalendarDay(t1: Long, t2: Long): Boolean {
        val c1 = java.util.Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = java.util.Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR) &&
               c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
