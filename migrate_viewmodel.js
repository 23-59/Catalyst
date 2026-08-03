const fs = require('fs');

const path = 'app/src/main/java/com/goldenminute/catalyst/ui/TodoViewModel.kt';
let content = fs.readFileSync(path, 'utf8');

// 1. Add Imports
content = content.replace('import androidx.lifecycle.viewModelScope', `import androidx.lifecycle.viewModelScope\nimport androidx.datastore.preferences.core.edit\nimport com.goldenminute.catalyst.data.appDataStore\nimport com.goldenminute.catalyst.data.DataStoreKeys`);

// 2. Replace Properties
const propsOld = `    private val sharedPrefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
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
    
    val dailyFocusGoalHours = MutableStateFlow(sharedPrefs.getFloat("daily_focus_goal_hours", 2.0f))`;

const propsNew = `    val isInitialized = MutableStateFlow(false)

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
    
    val dailyFocusGoalHours = MutableStateFlow(2.0f)`;

content = content.replace(propsOld, propsNew);

// 3. Replace init { } block
const initOld = `    init {
        checkAndResetDailyTasks()
    }`;

const initNew = `    init {
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
                        if (parts.length >= 3) { // Note: converted to Kotlin size
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
    }`;

content = content.replace(initOld, initNew);

// 4. Remove second initialization logic
const timerInitOld = `        // Load custom themes
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
        TimerStateManager.longBreakLength.value = sharedPrefs.getLong("long_break_length", 25 * 60 * 1000L)`;

content = content.replace(timerInitOld, "// Migrated to DataStore");

// 5. Replace explicit put calls
const replacements = [
    ['sharedPrefs.edit().putString("ai_hub_prompt", p).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.AI_HUB_PROMPT] = p } }'],
    ['sharedPrefs.edit().putString("ai_digestion_prompt", p).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.AI_DIGESTION_PROMPT] = p } }'],
    ['sharedPrefs.edit().putFloat("daily_focus_goal_hours", clamped).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.DAILY_FOCUS_GOAL_HOURS] = clamped } }'],
    ['sharedPrefs.edit().putString("gemini_api_key", key).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.GEMINI_API_KEY] = key } }'],
    ['sharedPrefs.edit().putString("language", lang).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.LANGUAGE] = lang } }'],
    ['sharedPrefs.edit().putString("theme", theme).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.THEME] = theme } }'],
    ['sharedPrefs.edit().putInt("custom_theme_color", color).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.CUSTOM_THEME_COLOR] = color } }'],
    ['sharedPrefs.edit().putBoolean("custom_theme_apply_bg", apply).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.CUSTOM_THEME_APPLY_BG] = apply } }'],
    ['sharedPrefs.edit().putBoolean("is_notes_grid_view", isGridView).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.IS_NOTES_GRID_VIEW] = isGridView } }'],
    ['sharedPrefs.edit().putString("saved_custom_themes", serialized).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.SAVED_CUSTOM_THEMES] = serialized } }'],
    ['sharedPrefs.edit().putStringSet("deleted_predefined_themes", deleted).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.DELETED_PREDEFINED_THEMES] = deleted } }'],
    ['sharedPrefs.edit().putBoolean("first_time", false).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.FIRST_TIME] = false } }'],
    ['sharedPrefs.edit().putLong("focus_length", roundedLength).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.FOCUS_LENGTH] = roundedLength } }'],
    ['sharedPrefs.edit().putLong("short_break_length", roundedLength).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.SHORT_BREAK_LENGTH] = roundedLength } }'],
    ['sharedPrefs.edit().putLong("long_break_length", roundedLength).apply()', 'viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.LONG_BREAK_LENGTH] = roundedLength } }']
];

for (const [old, next] of replacements) {
    content = content.split(old).join(next);
}

// Complex replacement for multiline
content = content.replace(
`        sharedPrefs.edit()
            .putInt("custom_theme_color", color)
            .putBoolean("custom_theme_apply_bg", apply)
            .apply()`, 
`        viewModelScope.launch { getApplication<Application>().appDataStore.edit {
            it[DataStoreKeys.CUSTOM_THEME_COLOR] = color
            it[DataStoreKeys.CUSTOM_THEME_APPLY_BG] = apply
        } }`
);

fs.writeFileSync(path, content, 'utf8');
console.log('Success');
