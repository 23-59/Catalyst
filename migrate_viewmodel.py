import re

with open('app/src/main/java/com/goldenminute/catalyst/ui/TodoViewModel.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Make a backup
with open('app/src/main/java/com/goldenminute/catalyst/ui/TodoViewModel.kt.bak', 'w', encoding='utf-8') as f:
    f.write(content)

# We want to replace sharedPrefs completely.
# 1. Imports
imports = '''import androidx.datastore.preferences.core.edit
import com.goldenminute.catalyst.data.appDataStore
import com.goldenminute.catalyst.data.DataStoreKeys'''
content = content.replace('import androidx.lifecycle.viewModelScope', 'import androidx.lifecycle.viewModelScope\n' + imports)

# 2. Properties
props_old = '''    private val sharedPrefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val appLanguage = MutableStateFlow(sharedPrefs.getString("language", "en") ?: "en")
    val appTheme = MutableStateFlow(sharedPrefs.getString("theme", "cosmic_dark") ?: "cosmic_dark")
    val customThemeColor = MutableStateFlow(sharedPrefs.getInt("custom_theme_color", android.graphics.Color.parseColor("#9D4EDD")))
    val customThemeApplyToBg = MutableStateFlow(sharedPrefs.getBoolean("custom_theme_apply_bg", true))
    
    val savedCustomThemes = MutableStateFlow<List<CustomTheme>>(emptyList())
    val deletedPredefinedThemes = MutableStateFlow<Set<String>>(sharedPrefs.getStringSet("deleted_predefined_themes", emptySet()) ?: emptySet())


    val isFirstTime = MutableStateFlow(sharedPrefs.getBoolean("first_time", true))
    val isNotesGridView = MutableStateFlow(sharedPrefs.getBoolean("is_notes_grid_view", true))
    val userGeminiApiKey = MutableStateFlow(sharedPrefs.getString("gemini_api_key", "") ?: "")
    
    val defaultAiHubPrompt = "Analyze the following tasks and focus data. Provide a unified, concise, and encouraging advisory on the user's performance and focus efficiency.\\n\\nApp Context:\\n{APP_CONTEXT}\\n\\nTasks:\\n{TASKS_DATA}\\n\\nRecent Focus Data:\\n{FOCUS_DATA}"
    val defaultAiDigestionPrompt = "You are a helpful productivity assistant. Please generate a short 'Daily Digest' for the user based on their pending tasks and recent notes. Be encouraging, concise, and format the response with bullet points and short paragraphs.\\n\\nApp Context:\\n{APP_CONTEXT}\\n\\nPending Tasks:\\n{TASKS_DATA}\\n\\nRecent Notes:\\n{NOTES_DATA}"
    
    val userAiHubPrompt = MutableStateFlow(sharedPrefs.getString("ai_hub_prompt", "")?.takeIf { it.isNotBlank() } ?: defaultAiHubPrompt)
    val userAiDigestionPrompt = MutableStateFlow(sharedPrefs.getString("ai_digestion_prompt", "")?.takeIf { it.isNotBlank() } ?: defaultAiDigestionPrompt)
    
    val dailyFocusGoalHours = MutableStateFlow(sharedPrefs.getFloat("daily_focus_goal_hours", 2.0f))'''

props_new = '''    val isInitialized = MutableStateFlow(false)

    val appLanguage = MutableStateFlow("en")
    val appTheme = MutableStateFlow("cosmic_dark")
    val customThemeColor = MutableStateFlow(android.graphics.Color.parseColor("#9D4EDD"))
    val customThemeApplyToBg = MutableStateFlow(true)
    
    val savedCustomThemes = MutableStateFlow<List<CustomTheme>>(emptyList())
    val deletedPredefinedThemes = MutableStateFlow<Set<String>>(emptySet())

    val isFirstTime = MutableStateFlow(true)
    val isNotesGridView = MutableStateFlow(true)
    val userGeminiApiKey = MutableStateFlow("")
    
    val defaultAiHubPrompt = "Analyze the following tasks and focus data. Provide a unified, concise, and encouraging advisory on the user's performance and focus efficiency.\\n\\nApp Context:\\n{APP_CONTEXT}\\n\\nTasks:\\n{TASKS_DATA}\\n\\nRecent Focus Data:\\n{FOCUS_DATA}"
    val defaultAiDigestionPrompt = "You are a helpful productivity assistant. Please generate a short 'Daily Digest' for the user based on their pending tasks and recent notes. Be encouraging, concise, and format the response with bullet points and short paragraphs.\\n\\nApp Context:\\n{APP_CONTEXT}\\n\\nPending Tasks:\\n{TASKS_DATA}\\n\\nRecent Notes:\\n{NOTES_DATA}"
    
    val userAiHubPrompt = MutableStateFlow(defaultAiHubPrompt)
    val userAiDigestionPrompt = MutableStateFlow(defaultAiDigestionPrompt)
    
    val dailyFocusGoalHours = MutableStateFlow(2.0f)'''

content = content.replace(props_old, props_new)

# 3. Add initialization to init {
init_old = '''    init {
        checkAndResetDailyTasks()
    }'''

init_new = '''    init {
        checkAndResetDailyTasks()
        viewModelScope.launch {
            getApplication<Application>().appDataStore.data.collect { prefs ->
                appLanguage.value = prefs[DataStoreKeys.LANGUAGE] ?: "en"
                appTheme.value = prefs[DataStoreKeys.THEME] ?: "cosmic_dark"
                customThemeColor.value = prefs[DataStoreKeys.CUSTOM_THEME_COLOR] ?: android.graphics.Color.parseColor("#9D4EDD")
                customThemeApplyToBg.value = prefs[DataStoreKeys.CUSTOM_THEME_APPLY_BG] ?: true
                
                val savedThemesStr = prefs[DataStoreKeys.SAVED_CUSTOM_THEMES] ?: ""
                if (savedThemesStr.isNotBlank()) {
                    val loadedThemes = savedThemesStr.split(";;").mapNotNull {
                        val parts = it.split("|")
                        if (parts.size >= 3) {
                            val applyToBg = if (parts.size > 3) parts[3].toBoolean() else true
                            CustomTheme(parts[0], parts[1], parts[2].toInt(), applyToBg)
                        } else null
                    }
                    savedCustomThemes.value = loadedThemes
                } else {
                    savedCustomThemes.value = emptyList()
                }

                deletedPredefinedThemes.value = prefs[DataStoreKeys.DELETED_PREDEFINED_THEMES] ?: emptySet()
                isFirstTime.value = prefs[DataStoreKeys.FIRST_TIME] ?: true
                isNotesGridView.value = prefs[DataStoreKeys.IS_NOTES_GRID_VIEW] ?: true
                userGeminiApiKey.value = prefs[DataStoreKeys.GEMINI_API_KEY] ?: ""
                
                val hubPrompt = prefs[DataStoreKeys.AI_HUB_PROMPT] ?: ""
                userAiHubPrompt.value = hubPrompt.takeIf { it.isNotBlank() } ?: defaultAiHubPrompt
                
                val digestPrompt = prefs[DataStoreKeys.AI_DIGESTION_PROMPT] ?: ""
                userAiDigestionPrompt.value = digestPrompt.takeIf { it.isNotBlank() } ?: defaultAiDigestionPrompt
                
                dailyFocusGoalHours.value = prefs[DataStoreKeys.DAILY_FOCUS_GOAL_HOURS] ?: 2.0f

                TimerStateManager.initialize(getApplication())
                TimerStateManager.focusLength.value = prefs[DataStoreKeys.FOCUS_LENGTH] ?: (25 * 60 * 1000L)
                TimerStateManager.shortBreakLength.value = prefs[DataStoreKeys.SHORT_BREAK_LENGTH] ?: (5 * 60 * 1000L)
                TimerStateManager.longBreakLength.value = prefs[DataStoreKeys.LONG_BREAK_LENGTH] ?: (25 * 60 * 1000L)

                if (!isInitialized.value) {
                    isInitialized.value = true
                }
            }
        }
    }'''

content = content.replace(init_old, init_new)

# 4. Remove old TimerStateManager initialization from bottom of the file
old_timer_init = '''        // Load custom themes
        val savedThemesStr = sharedPrefs.getString("saved_custom_themes", "")
        if (!savedThemesStr.isNullOrBlank()) {
            val loadedThemes = savedThemesStr.split(";;").mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 3) {
                    val applyToBg = if (parts.size > 3) parts[3].toBoolean() else true
                    CustomTheme(parts[0], parts[1], parts[2].toInt(), applyToBg)
                } else null
            }
            savedCustomThemes.value = loadedThemes
        }

        // Initialize TimerStateManager with Context
        TimerStateManager.initialize(application)

        // Initialize TimerStateManager lengths from sharedPrefs (app_settings)
        val fLen = sharedPrefs.getLong("focus_length", 25 * 60 * 1000L)
        TimerStateManager.focusLength.value = fLen
        TimerStateManager.shortBreakLength.value = sharedPrefs.getLong("short_break_length", 5 * 60 * 1000L)
        TimerStateManager.longBreakLength.value = sharedPrefs.getLong("long_break_length", 25 * 60 * 1000L)'''

content = content.replace(old_timer_init, "// Migrated to main init block via DataStore")

# 5. Fix edit() calls manually to avoid regex capturing group complexity
replacements = {
    'sharedPrefs.edit().putString("ai_hub_prompt", p).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.AI_HUB_PROMPT] = p } }',
    'sharedPrefs.edit().putString("ai_digestion_prompt", p).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.AI_DIGESTION_PROMPT] = p } }',
    'sharedPrefs.edit().putFloat("daily_focus_goal_hours", clamped).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.DAILY_FOCUS_GOAL_HOURS] = clamped } }',
    'sharedPrefs.edit().putString("gemini_api_key", key).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.GEMINI_API_KEY] = key } }',
    'sharedPrefs.edit().putString("language", lang).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.LANGUAGE] = lang } }',
    'sharedPrefs.edit().putString("theme", themeKey).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.THEME] = themeKey } }',
    'sharedPrefs.edit().putInt("custom_theme_color", color).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.CUSTOM_THEME_COLOR] = color } }',
    'sharedPrefs.edit().putBoolean("custom_theme_apply_bg", apply).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.CUSTOM_THEME_APPLY_BG] = apply } }',
    'sharedPrefs.edit().putBoolean("is_notes_grid_view", isGrid).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.IS_NOTES_GRID_VIEW] = isGrid } }',
    'sharedPrefs.edit().putString("saved_custom_themes", themesStr).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.SAVED_CUSTOM_THEMES] = themesStr } }',
    'sharedPrefs.edit().putStringSet("deleted_predefined_themes", deletedPredefinedThemes.value).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.DELETED_PREDEFINED_THEMES] = deletedPredefinedThemes.value } }',
    'sharedPrefs.edit().putBoolean("first_time", false).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.FIRST_TIME] = false } }',
    'sharedPrefs.edit().putLong("focus_length", roundedLength).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.FOCUS_LENGTH] = roundedLength } }',
    'sharedPrefs.edit().putLong("short_break_length", roundedLength).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.SHORT_BREAK_LENGTH] = roundedLength } }',
    'sharedPrefs.edit().putLong("long_break_length", roundedLength).apply()': 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.LONG_BREAK_LENGTH] = roundedLength } }',
}

for old, new in replacements.items():
    content = content.replace(old, new)

with open('app/src/main/java/com/goldenminute/catalyst/ui/TodoViewModel.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print('Success')
