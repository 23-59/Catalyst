const fs = require('fs');

const path = 'app/src/main/java/com/goldenminute/catalyst/ui/TimerStateManager.kt';
let content = fs.readFileSync(path, 'utf8');

const newContent = `package com.goldenminute.catalyst.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.timerDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object TimerStateManager {
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

    private var targetEndTime = 0L
    private var startTime = 0L

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        appContext = context.applicationContext

        scope.launch {
            val prefs = appContext!!.timerDataStore.data.first()

            // Restore state
            isPomodoro.value = prefs[DataStoreKeys.IS_POMODORO] ?: true
            pomodoroMode.value = prefs[DataStoreKeys.POMODORO_MODE] ?: "Focus"
            pomodoroCycle.value = prefs[DataStoreKeys.POMODORO_CYCLE] ?: 0
            
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
                    saveState()
                }
            }
            launch { isPomodoro.collect { saveState() } }
            launch { pomodoroMode.collect { saveState() } }

            // Precise update loop that handles sleep / doze mode seamlessly
            while (true) {
                delay(20L) // UI refresh rate (~50fps)
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
}
`;

fs.writeFileSync(path, newContent, 'utf8');
console.log('Success TimerStateManager');
