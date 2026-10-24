package com.goldenminute.catalyst.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore


val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_prefs")

object DataStoreKeys {
    // App Settings
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
    val CUSTOM_THEME_COLOR = intPreferencesKey("custom_theme_color")
    val CUSTOM_THEME_APPLY_BG = booleanPreferencesKey("custom_theme_apply_bg")
    val SAVED_CUSTOM_THEMES = stringPreferencesKey("saved_custom_themes")
    val DELETED_PREDEFINED_THEMES = stringSetPreferencesKey("deleted_predefined_themes")
    val FIRST_TIME = booleanPreferencesKey("first_time")
    val IS_NOTES_GRID_VIEW = booleanPreferencesKey("is_notes_grid_view")
    val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
    val AI_HUB_PROMPT = stringPreferencesKey("ai_hub_prompt")
    val AI_DIGESTION_PROMPT = stringPreferencesKey("ai_digestion_prompt")
    val DISTRACTING_APPS = stringSetPreferencesKey("distracting_apps")
    val DOOMSCROLLING_ALERT_ENABLED = booleanPreferencesKey("doomscrolling_alert_enabled")
    val DAILY_FOCUS_GOAL_HOURS = floatPreferencesKey("daily_focus_goal_hours")
    val FREE_MODE_SESSIONS = stringPreferencesKey("free_mode_sessions")
    val POWER_BUTTON_SHORTCUT_MODE = stringPreferencesKey("power_button_shortcut_mode")

    val FOCUS_LENGTH = longPreferencesKey("focus_length")
    val SHORT_BREAK_LENGTH = longPreferencesKey("short_break_length")
    val LONG_BREAK_LENGTH = longPreferencesKey("long_break_length")
    val LAST_DAILY_RESET_TIME = longPreferencesKey("last_daily_reset_time")

    // Timer Prefs
    val IS_POMODORO = booleanPreferencesKey("isPomodoro")
    val POMODORO_MODE = stringPreferencesKey("pomodoroMode")
    val POMODORO_CYCLE = intPreferencesKey("pomodoroCycle")
    val IS_TIMER_RUNNING = booleanPreferencesKey("isTimerRunning")
    val TIME_LEFT = longPreferencesKey("timeLeft")
    val TIME_ELAPSED = longPreferencesKey("timeElapsed")
    val TARGET_END_TIME = longPreferencesKey("targetEndTime")
    val START_TIME = longPreferencesKey("startTime")
    val TIMER_FOCUS_LENGTH = longPreferencesKey("focusLength")
    val TIMER_SHORT_BREAK_LENGTH = longPreferencesKey("shortBreakLength")
    val TIMER_LONG_BREAK_LENGTH = longPreferencesKey("longBreakLength")
    val ACTIVE_TASK_ID = intPreferencesKey("activeTaskId")
    val IS_IDLE_MODE = booleanPreferencesKey("isIdleMode")
}


