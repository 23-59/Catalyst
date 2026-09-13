package com.goldenminute.catalyst.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import com.goldenminute.catalyst.data.appDataStore
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.*
import com.goldenminute.catalyst.receiver.ReminderScheduler
import com.goldenminute.catalyst.widget.TasksWidget
import com.goldenminute.catalyst.widget.NotesWidget
import androidx.glance.appwidget.updateAll
import com.goldenminute.catalyst.R
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import retrofit2.HttpException
import com.squareup.moshi.Moshi
import java.io.InputStream
import java.io.OutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    var currentScreenState = mutableStateOf("Tasks")
    var previousScreenState = mutableStateOf("Tasks")
    private val database = TodoDatabase.getDatabase(application, viewModelScope)
    private val repository = TodoRepository(database.todoDao(), database.categoryDao(), database.noteDao(), database.doomscrollVideoDao())
    val selectedFocusTodo: StateFlow<TodoItem?> = combine(
        com.goldenminute.catalyst.ui.TimerStateManager.activeTaskId,
        repository.allTodos
    ) { activeId, todos ->
        if (activeId != -1) {
            todos.find { it.id == activeId }
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
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
                        if (parts.size >= 3) { // Note: converted to Kotlin size
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
                freeModeSessions.value = prefs[DataStoreKeys.FREE_MODE_SESSIONS] ?: ""

                val apps = prefs[DataStoreKeys.DISTRACTING_APPS]
                distractingApps.value = apps ?: com.goldenminute.catalyst.service.SmartTrackerService.DEFAULT_DISTRACTING_PACKAGES
                doomscrollingAlertEnabled.value = prefs[DataStoreKeys.DOOMSCROLLING_ALERT_ENABLED] ?: false

                TimerStateManager.initialize(getApplication())
                TimerStateManager.focusLength.value = prefs[DataStoreKeys.FOCUS_LENGTH] ?: (25 * 60 * 1000L)
                TimerStateManager.shortBreakLength.value = prefs[DataStoreKeys.SHORT_BREAK_LENGTH] ?: (5 * 60 * 1000L)
                TimerStateManager.longBreakLength.value = prefs[DataStoreKeys.LONG_BREAK_LENGTH] ?: (25 * 60 * 1000L)

                if (!isInitialized.value) {
                    isInitialized.value = true
                }
            }
        }
        
    }

    fun checkAndResetDailyTasks() {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val startOfToday = now.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val prefs = getApplication<Application>().appDataStore.data.first()
            val lastResetTime = prefs[DataStoreKeys.LAST_DAILY_RESET_TIME] ?: 0L
            
            if (startOfToday > lastResetTime) {
                repository.resetDailyTasks(startOfToday)
                repository.clearExpiredTaskSessions(startOfToday)
                getApplication<Application>().appDataStore.edit { it[DataStoreKeys.LAST_DAILY_RESET_TIME] = startOfToday }
            }
        }
    }

    val isInitialized = MutableStateFlow(false)

    val appLanguage = MutableStateFlow("en")
    val appTheme = MutableStateFlow("cosmic_dark")
    val customThemeColor = MutableStateFlow(android.graphics.Color.parseColor("#9D4EDD"))
    val customThemeApplyToBg = MutableStateFlow(true)
    
    val savedCustomThemes = MutableStateFlow<List<CustomTheme>>(emptyList())
    val deletedPredefinedThemes = MutableStateFlow<Set<String>>(emptySet())

    val isFirstTime = MutableStateFlow(true)
    val isNotesGridView = MutableStateFlow(true)
    val userGeminiApiKey = MutableStateFlow("")
    
    val defaultAiHubPrompt = "Analyze the following tasks and focus data. Provide a unified, concise, and encouraging advisory on the user's performance and focus efficiency.\n\nApp Context:\n{APP_CONTEXT}\n\nTasks:\n{TASKS_DATA}\n\nRecent Focus Data:\n{FOCUS_DATA}"
    val defaultAiDigestionPrompt = "You are a helpful productivity assistant. Please generate a short 'Daily Digest' for the user based on their pending tasks and recent notes. Be encouraging, concise, and format the response with bullet points and short paragraphs.\n\nApp Context:\n{APP_CONTEXT}\n\nPending Tasks:\n{TASKS_DATA}\n\nRecent Notes:\n{NOTES_DATA}"
    
    val userAiHubPrompt = MutableStateFlow(defaultAiHubPrompt)
    val userAiDigestionPrompt = MutableStateFlow(defaultAiDigestionPrompt)
    
    val dailyFocusGoalHours = MutableStateFlow(2.0f)
    val freeModeSessions = MutableStateFlow("")

    val distractingApps = MutableStateFlow<Set<String>>(com.goldenminute.catalyst.service.SmartTrackerService.DEFAULT_DISTRACTING_PACKAGES)
    val doomscrollingAlertEnabled = MutableStateFlow(false)

    fun setDistractingApps(apps: Set<String>) {
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.DISTRACTING_APPS] = apps } }
        distractingApps.value = apps
    }

    fun setDoomscrollingAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().appDataStore.edit {
                it[DataStoreKeys.DOOMSCROLLING_ALERT_ENABLED] = enabled
            }
        }
        doomscrollingAlertEnabled.value = enabled
    }

    val doomscrollRecords: StateFlow<List<com.goldenminute.catalyst.data.DoomscrollVideoRecord>> =
        repository.allDoomscrollRecords
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalDoomscrollVideosCount: StateFlow<Int> =
        doomscrollRecords.map { it.size }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalDoomscrollDurationMs: StateFlow<Long> =
        doomscrollRecords.map { list -> list.sumOf { it.durationMs } }
            .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    fun clearDoomscrollRecords() {
        viewModelScope.launch {
            repository.clearAllDoomscrollRecords()
        }
    }

    fun setAiHubPrompt(prompt: String) {
        val p = prompt.ifBlank { defaultAiHubPrompt }
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.AI_HUB_PROMPT] = p } }
        userAiHubPrompt.value = p
    }

    fun setAiDigestionPrompt(prompt: String) {
        val p = prompt.ifBlank { defaultAiDigestionPrompt }
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.AI_DIGESTION_PROMPT] = p } }
        userAiDigestionPrompt.value = p
    }

    fun setDailyFocusGoalHours(hours: Float) {
        val clamped = hours.coerceIn(0.5f, 24.0f)
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.DAILY_FOCUS_GOAL_HOURS] = clamped } }
        dailyFocusGoalHours.value = clamped
    }

    fun addFreeModeFocusSession(record: com.goldenminute.catalyst.data.RecordedTimeItem) {
        viewModelScope.launch {
            val currentList = com.goldenminute.catalyst.data.RecordedTimeItem.parseList(freeModeSessions.value).toMutableList()
            currentList.add(record)
            val serialized = com.goldenminute.catalyst.data.RecordedTimeItem.serializeList(currentList)
            freeModeSessions.value = serialized
            getApplication<Application>().appDataStore.edit { it[DataStoreKeys.FREE_MODE_SESSIONS] = serialized }
        }
    }

    fun saveCurrentTimerSession(context: Context) {
        val todo = selectedFocusTodo.value
        val isPom = isPomodoro.value
        val elapsed = timeElapsed.value
        
        if (todo != null) {
            if (isPom) {
                val newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                    isPomodoro = true,
                    value = 1L,
                    timestamp = System.currentTimeMillis()
                )
                addSessionRecords(todo.id, listOf(newRecord))
            } else {
                if (elapsed > 0) {
                    val newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                        isPomodoro = false,
                        value = elapsed,
                        timestamp = System.currentTimeMillis()
                    )
                    addSessionRecords(todo.id, listOf(newRecord))
                }
            }
        } else if (TimerStateManager.isIdleMode.value) {
            if (isPom) {
                val newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                    isPomodoro = true,
                    value = 1L,
                    timestamp = System.currentTimeMillis()
                )
                addFreeModeFocusSession(newRecord)
            } else {
                if (elapsed > 0) {
                    val newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                        isPomodoro = false,
                        value = elapsed,
                        timestamp = System.currentTimeMillis()
                    )
                    addFreeModeFocusSession(newRecord)
                }
            }
            android.widget.Toast.makeText(context, context.getString(com.goldenminute.catalyst.R.string.free_focus_session_saved), android.widget.Toast.LENGTH_SHORT).show()
        }
        
        TimerStateManager.isTimerRunning.value = false
        TimerStateManager.timeElapsed.value = 0L
    }

    val aiInsight = MutableStateFlow<String?>(null)
    val isAiLoading = MutableStateFlow(false)
    
    fun fetchUnifiedAiHubInsight(todos: List<TodoItem>, categories: List<Category>, period: String) {
        viewModelScope.launch {
            isAiLoading.value = true
            try {
                val apiKey = userGeminiApiKey.value.takeIf { it.isNotEmpty() } ?: com.goldenminute.catalyst.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty()) {
                    aiInsight.value = "ERROR_API_KEY"
                    return@launch
                }
                
                val now = Calendar.getInstance().timeInMillis
                val oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000L
                
                val appContext = "Current Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(now))}, Daily Focus Goal: ${dailyFocusGoalHours.value} hours"
                
                val tasksData = todos.joinToString("\n") { 
                    val priorityStr = when (it.priority) { 0 -> "Low"; 1 -> "Medium"; 2 -> "High"; else -> "Unknown" }
                    val dueStr = it.dueDate?.let { d -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(d)) } ?: "None"
                    val catName = categories.find { c -> c.id == it.categoryId }?.name ?: "None"
                    "- Task: ${it.title} | Priority: $priorityStr | Due: $dueStr | Daily: ${it.isDaily} | Completed: ${it.isCompleted} | Category: $catName | Desc: ${it.description.take(50)}"
                }
                
                val recentRecords = mutableListOf<String>()
                val focusLen = focusLength.value
                
                todos.forEach { todo ->
                    val recs = com.goldenminute.catalyst.data.RecordedTimeItem.parseList(todo.recordedTimes)
                    recs.filter { it.timestamp > oneWeekAgo }.forEach { r ->
                        val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(r.timestamp))
                        val typeStr = if (r.isPomodoro) "Pomodoro (${focusLen / 60000}m)" else "Stopwatch"
                        val valStr = if (r.isPomodoro) "${r.value} cycle(s)" else "${r.value / 60000}m"
                        recentRecords.add("- [$dateStr] Task '${todo.title}': $typeStr - $valStr")
                    }
                }
                
                val freeRecords = com.goldenminute.catalyst.data.RecordedTimeItem.parseList(freeModeSessions.value)
                freeRecords.filter { it.timestamp > oneWeekAgo }.forEach { r ->
                    val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(r.timestamp))
                    val typeStr = if (r.isPomodoro) "Pomodoro (${focusLen / 60000}m)" else "Stopwatch"
                    val valStr = if (r.isPomodoro) "${r.value} cycle(s)" else "${r.value / 60000}m"
                    recentRecords.add("- [$dateStr] Free Session: $typeStr - $valStr")
                }
                
                val focusData = if (recentRecords.isNotEmpty()) {
                    recentRecords.sorted().joinToString("\n")
                } else {
                    "No focus sessions recorded in the last 7 days."
                }
                
                var prompt = userAiHubPrompt.value
                prompt = prompt.replace("{APP_CONTEXT}", appContext)
                prompt = prompt.replace("{TASKS_DATA}", tasksData)
                prompt = prompt.replace("{FOCUS_DATA}", focusData)
                
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = prompt)))
                    )
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                aiInsight.value = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No insight available."
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    aiInsight.value = "ERROR_403"
                } else {
                    aiInsight.value = "Failed to fetch insight: ${e.message}"
                }
            } catch (e: Exception) {
                aiInsight.value = "Failed to fetch insight: ${e.message}"
            } finally {
                isAiLoading.value = false
            }
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.GEMINI_API_KEY] = key } }
        userGeminiApiKey.value = key
    }

    fun generateSubTasks(title: String, description: String, onResult: (List<String>) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val apiKey = userGeminiApiKey.value.takeIf { it.isNotEmpty() } ?: com.goldenminute.catalyst.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty()) {
                    onError("API Key is missing. Please add it in Settings.")
                    return@launch
                }
                val promptText = "Generate a concise list of actionable sub-tasks for the following task:\nTitle: $title\nDescription: $description\n\nReturn ONLY the list of sub-tasks, one per line, starting with a dash (-). Do not include any other text."
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = promptText)))
                    )
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val subTasks = text.lines()
                    .map { it.trim() }
                    .filter { it.startsWith("-") }
                    .map { it.removePrefix("-").trim() }
                    .filter { it.isNotEmpty() }
                if (subTasks.isNotEmpty()) {
                    onResult(subTasks)
                } else {
                    onError("No sub-tasks could be generated.")
                }
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    onError(getApplication<Application>().getString(R.string.error_403_proxy))
                } else {
                    onError(e.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun suggestTaskDetails(rawText: String, onResult: (String, String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val apiKey = userGeminiApiKey.value.takeIf { it.isNotEmpty() } ?: com.goldenminute.catalyst.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty()) {
                    onError("API Key is missing. Please add it in Settings.")
                    return@launch
                }
                val promptText = "Based on the following raw text (e.g., from a note), extract a concise and actionable task title, and provide a polished task description. Return the result strictly in this format, without markdown:\nTITLE: <title>\nDESCRIPTION: <description>\n\nRaw text:\n$rawText"
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = promptText)))
                    )
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""

                val titleMatch = Regex("TITLE:(.*?)(?=DESCRIPTION:|$)", RegexOption.DOT_MATCHES_ALL).find(text)
                val descMatch = Regex("DESCRIPTION:(.*?)$", RegexOption.DOT_MATCHES_ALL).find(text)

                val newTitle = titleMatch?.groupValues?.get(1)?.trim() ?: ""
                val newDesc = descMatch?.groupValues?.get(1)?.trim() ?: ""

                if (newTitle.isNotEmpty()) {
                    onResult(newTitle, newDesc)
                } else {
                    onError("Could not generate task details.")
                }
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    onError(getApplication<Application>().getString(R.string.error_403_proxy))
                } else {
                    onError(e.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun setCustomThemeState(color: Int, applyToBg: Boolean) {
        viewModelScope.launch { getApplication<Application>().appDataStore.edit {
            it[DataStoreKeys.CUSTOM_THEME_COLOR] = color
            it[DataStoreKeys.CUSTOM_THEME_APPLY_BG] = applyToBg
        } }
        customThemeColor.value = color
        customThemeApplyToBg.value = applyToBg
    }
    
    fun addCustomTheme(name: String, color: Int, isLightMode: Boolean, applyToBg: Boolean) {
        val prefix = if (isLightMode) "custom_light" else "custom_dark"
        val newId = "${prefix}_${java.util.UUID.randomUUID()}"
        val newTheme = CustomTheme(newId, name, color, applyToBg)
        val currentThemes = savedCustomThemes.value.toMutableList()
        currentThemes.add(newTheme)
        
        // Serialize and save
        val serialized = currentThemes.joinToString(";;") { "${it.id}|${it.name}|${it.color}|${it.applyToBackground}" }
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.SAVED_CUSTOM_THEMES] = serialized } }
        
        savedCustomThemes.value = currentThemes
        
        // Automatically switch to the newly created theme
        setCustomThemeState(color, applyToBg)
        setTheme(newId)
    }
    
    fun deleteTheme(id: String) {
        if (id.startsWith("custom_")) {
            val currentThemes = savedCustomThemes.value.filter { it.id != id }
            
            // Serialize and save
            val serialized = currentThemes.joinToString(";;") { "${it.id}|${it.name}|${it.color}|${it.applyToBackground}" }
            viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.SAVED_CUSTOM_THEMES] = serialized } }
            
            savedCustomThemes.value = currentThemes
        } else {
            val deleted = deletedPredefinedThemes.value.toMutableSet()
            deleted.add(id)
            viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.DELETED_PREDEFINED_THEMES] = deleted } }
            deletedPredefinedThemes.value = deleted
        }
        
        if (appTheme.value == id) {
            setTheme("cosmic_dark")
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.LANGUAGE] = lang } }
        appLanguage.value = lang
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.THEME] = theme } }
        appTheme.value = theme
        if (theme.startsWith("custom_")) {
            val customTheme = savedCustomThemes.value.find { it.id == theme }
            if (customTheme != null) {
                setCustomThemeState(customTheme.color, customTheme.applyToBackground)
            }
        }
    }

    fun completeWelcome() {
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.FIRST_TIME] = false } }
        isFirstTime.value = false
    }

    fun setNotesGridView(isGridView: Boolean) {
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.IS_NOTES_GRID_VIEW] = isGridView } }
        isNotesGridView.value = isGridView
    }

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val taskCategories: StateFlow<List<Category>> = repository.taskCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val noteCategories: StateFlow<List<Category>> = repository.noteCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSubTasks: StateFlow<List<SubTask>> = repository.allSubTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTodosList: StateFlow<List<TodoItem>> = repository.allTodos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _todos = repository.allTodos
    
    // Filtering states
    val selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedNoteCategoryId = MutableStateFlow<Int?>(null)
    val selectedPriorityId = MutableStateFlow<Int?>(null) // 0=Low, 1=Medium, 2=High
    val searchQuery = MutableStateFlow("")
    val selectedTab = MutableStateFlow("All") // "All", "Today", "Upcoming", "Completed"

    // Pomodoro / Focus states are delegated to TimerStateManager
    val focusLength = TimerStateManager.focusLength
    val shortBreakLength = TimerStateManager.shortBreakLength
    val longBreakLength = TimerStateManager.longBreakLength
    val pomodoroCycle = TimerStateManager.pomodoroCycle // Cycles completed (0 to 3)

    fun setFocusLength(length: Long) {
        val roundedLength = (length / 60000L) * 60000L
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.FOCUS_LENGTH] = roundedLength } }
        TimerStateManager.updateModeLength("Focus", roundedLength)
    }

    fun setShortBreakLength(length: Long) {
        val roundedLength = (length / 60000L) * 60000L
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.SHORT_BREAK_LENGTH] = roundedLength } }
        TimerStateManager.updateModeLength("Short Break", roundedLength)
    }

    fun setLongBreakLength(length: Long) {
        val roundedLength = (length / 60000L) * 60000L
        viewModelScope.launch { getApplication<Application>().appDataStore.edit { it[DataStoreKeys.LONG_BREAK_LENGTH] = roundedLength } }
        TimerStateManager.updateModeLength("Long Break", roundedLength)
    }

    val isTimerRunning = TimerStateManager.isTimerRunning
    val isPomodoro = TimerStateManager.isPomodoro
    val pomodoroMode = TimerStateManager.pomodoroMode // "Focus", "Short Break", "Long Break"
    val timeLeft = TimerStateManager.timeLeft
    val timeElapsed = TimerStateManager.timeElapsed
    val isIdleMode = TimerStateManager.isIdleMode
    
    fun setPomodoroMode(mode: String) {
        TimerStateManager.setPomodoroMode(mode)
    }

    fun resetCurrentMode() {
        TimerStateManager.resetCurrentMode()
    }


    // Combined filtered list
    val filteredTodos: StateFlow<List<TodoItem>> = combine(
        _todos,
        selectedCategoryId,
        selectedPriorityId,
        searchQuery,
        selectedTab
    ) { todosList, categoryId, priorityId, search, tab ->
        var list = todosList

        // 1. Text Search
        if (search.isNotBlank()) {
            list = list.filter {
                it.title.contains(search, ignoreCase = true) || 
                it.description.contains(search, ignoreCase = true)
            }
        }

        // 2. Category Filter
        if (categoryId != null) {
            list = list.filter { it.categoryId == categoryId }
        }

        // 3. Priority Filter
        if (priorityId != null) {
            list = list.filter { it.priority == priorityId }
        }

        // 4. Tab Filter
        when (tab) {
            "Today" -> {
                val now = Calendar.getInstance()
                val startOfToday = now.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val endOfToday = now.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                list = list.filter { 
                    !it.isCompleted && (it.isDaily || (it.dueDate != null && it.dueDate in startOfToday..endOfToday))
                }
            }
            "Upcoming" -> {
                val now = Calendar.getInstance()
                val endOfToday = now.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                list = list.filter { 
                    !it.isCompleted && !it.isDaily && (it.dueDate != null && it.dueDate > endOfToday)
                }
            }
            "Completed" -> {
                list = list.filter { it.isCompleted }
            }
            "Undone" -> {
                list = list.filter { !it.isCompleted && it.isDaily }
            }
            "All" -> {
                // Return all pending tasks (and optionally completed based on sorting order, 
                // but usually the room model sorts isCompleted ASC, so completed are at the bottom)
            }
        }

        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Statistics Flow for visual dashboard cards
    val todoStats: StateFlow<TodoStats> = _todos.map { list ->
        val total = list.size
        val completed = list.count { it.isCompleted }
        val pending = total - completed
        val highPriorityPending = list.count { !it.isCompleted && it.priority == 2 }
        
        // Count due today
        val now = Calendar.getInstance()
        val startOfToday = now.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfToday = now.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        val dueToday = list.count { !it.isCompleted && (it.isDaily || (it.dueDate != null && it.dueDate in startOfToday..endOfToday)) }

        TodoStats(total, completed, pending, highPriorityPending, dueToday)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodoStats()
    )

    fun insertTodo(todo: TodoItem) {
        viewModelScope.launch {
            val todoId = if (todo.id == 0) {
                repository.insertTodo(todo).toInt()
            } else {
                repository.updateTodo(todo)
                todo.id
            }
            if (todo.hasReminder && todo.reminderTime != null && todo.reminderTime > System.currentTimeMillis()) {
                ReminderScheduler.schedule(
                    getApplication(),
                    todoId,
                    todo.title,
                    "Task: ${todo.title} is due now!",
                    todo.reminderTime,
                    todo.isReminderRepeating
                )
            }
            TasksWidget().updateAll(getApplication())
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
            ReminderScheduler.cancel(getApplication(), todo.id)
            TasksWidget().updateAll(getApplication())
        }
    }

    fun undoDeleteTodo(todo: TodoItem, subTasks: List<SubTask>) {
        viewModelScope.launch {
            repository.insertTodo(todo)
            subTasks.forEach {
                repository.insertSubTask(it)
            }
            if (todo.hasReminder && todo.reminderTime != null && todo.reminderTime > System.currentTimeMillis()) {
                ReminderScheduler.schedule(
                    getApplication(),
                    todo.id,
                    todo.title,
                    "Task: ${todo.title} is due now!",
                    todo.reminderTime,
                    todo.isReminderRepeating
                )
            }
            TasksWidget().updateAll(getApplication())
        }
    }

    fun addElapsedTime(todoId: Int, timeToAdd: Long) {
        viewModelScope.launch {
            repository.addElapsedTime(todoId, timeToAdd)
        }
    }

    fun addPomodoroCycles(todoId: Int, cyclesToAdd: Int) {
        viewModelScope.launch {
            repository.addPomodoroCycles(todoId, cyclesToAdd)
        }
    }

    fun addSessionRecords(todoId: Int, newRecords: List<com.goldenminute.catalyst.data.RecordedTimeItem>) {
        viewModelScope.launch {
            val todo = repository.getTodoById(todoId) ?: return@launch
            val existingList = com.goldenminute.catalyst.data.RecordedTimeItem.parseList(todo.recordedTimes).toMutableList()
            existingList.addAll(newRecords)
            
            var extraTime = 0L
            var extraCycles = 0
            for (rec in newRecords) {
                if (rec.isPomodoro) {
                    extraCycles += rec.value.toInt()
                } else {
                    extraTime += rec.value
                }
            }
            
            val updatedTodo = todo.copy(
                elapsedTime = todo.elapsedTime + extraTime,
                pomodoroCycles = todo.pomodoroCycles + extraCycles,
                recordedTimes = com.goldenminute.catalyst.data.RecordedTimeItem.serializeList(existingList)
            )
            repository.updateTodo(updatedTodo)

        }
    }

    fun deleteSessionRecord(todoId: Int, index: Int) {
        viewModelScope.launch {
            val todo = repository.getTodoById(todoId) ?: return@launch
            val existingList = com.goldenminute.catalyst.data.RecordedTimeItem.parseList(todo.recordedTimes).toMutableList()
            if (index in existingList.indices) {
                val removed = existingList.removeAt(index)
                var subtractTime = 0L
                var subtractCycles = 0
                if (removed.isPomodoro) {
                    subtractCycles = removed.value.toInt()
                } else {
                    subtractTime = removed.value
                }
                
                val updatedTodo = todo.copy(
                    elapsedTime = (todo.elapsedTime - subtractTime).coerceAtLeast(0L),
                    pomodoroCycles = (todo.pomodoroCycles - subtractCycles).coerceAtLeast(0),
                    recordedTimes = com.goldenminute.catalyst.data.RecordedTimeItem.serializeList(existingList)
                )
                repository.updateTodo(updatedTodo)

            }
        }
    }

    fun toggleTodoCompleted(todoId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            val completedDate = if (isCompleted) System.currentTimeMillis() else null
            repository.updateCompletionStatus(todoId, isCompleted, completedDate)
            repository.updateAllSubTasksCompletion(todoId, isCompleted)
            
            if (isCompleted) {
                ReminderScheduler.cancel(getApplication(), todoId)
            } else {
                val todo = repository.getTodoById(todoId)
                if (todo != null && todo.hasReminder && todo.reminderTime != null && todo.reminderTime > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(
                        getApplication(),
                        todoId,
                        todo.title,
                        "Task: ${todo.title} is due now!",
                        todo.reminderTime,
                        todo.isReminderRepeating
                    )
                }
            }
            TasksWidget().updateAll(getApplication())
        }
    }

    fun updateTodoPositions(items: List<TodoItem>) {
        viewModelScope.launch {
            items.forEachIndexed { index, todo ->
                repository.updateTodoPosition(todo.id, index)
            }
            TasksWidget().updateAll(getApplication())
        }
    }

    fun addCategory(name: String, colorHex: String, iconName: String, type: String = "task") {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, colorHex = colorHex, iconName = iconName, type = type))
            TasksWidget().updateAll(getApplication())
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            if (selectedCategoryId.value == category.id) {
                selectedCategoryId.value = null
            }
            if (selectedNoteCategoryId.value == category.id) {
                selectedNoteCategoryId.value = null
            }
            TasksWidget().updateAll(getApplication())
        }
    }

    fun addSubTask(todoId: Int, title: String) {
        viewModelScope.launch {
            repository.insertSubTask(SubTask(todoId = todoId, title = title))
            TasksWidget().updateAll(getApplication())
        }
    }

    fun deleteSubTask(subTaskId: Int) {
        viewModelScope.launch {
            repository.deleteSubTaskById(subTaskId)
            TasksWidget().updateAll(getApplication())
        }
    }

    fun toggleSubTaskCompleted(subTaskId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateSubTaskCompletion(subTaskId, isCompleted)
            TasksWidget().updateAll(getApplication())
        }
    }

    // --- Notes ---
    val allNotes: StateFlow<List<NoteItem>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertNote(note: NoteItem) {
        viewModelScope.launch {
            repository.insertNote(note)
            NotesWidget().updateAll(getApplication())
        }
    }

    fun updateNote(note: NoteItem) {
        viewModelScope.launch {
            repository.updateNote(note)
            NotesWidget().updateAll(getApplication())
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
            NotesWidget().updateAll(getApplication())
        }
    }

    fun suggestCategory(title: String, description: String, categories: List<Category>, onResult: (Category?) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val apiKey = userGeminiApiKey.value.takeIf { it.isNotEmpty() } ?: com.goldenminute.catalyst.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty()) {
                    onError("API Key is missing. Please add it in Settings.")
                    return@launch
                }
                
                val categoryNames = categories.map { it.name }.joinToString(", ")
                val promptText = "Suggest the most relevant category for the following task from this list: [$categoryNames]. \n" +
                        "Task Title: $title\n" +
                        "Task Description: $description\n\n" +
                        "Return ONLY the exact name of the category from the list, with no extra text or punctuation. If none apply, return 'None'."
                        
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = promptText)))
                    )
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                val suggestedName = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
                
                val matchedCategory = categories.find { it.name.equals(suggestedName, ignoreCase = true) }
                onResult(matchedCategory)
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    onError(getApplication<Application>().getString(R.string.error_403_proxy))
                } else {
                    onError(e.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteNotesInBulk(noteIds: List<Int>) {
        viewModelScope.launch {
            noteIds.forEach { id ->
                repository.deleteNoteById(id)
            }
            NotesWidget().updateAll(getApplication())
        }
    }

    fun updateNotesColorInBulk(noteIds: List<Int>, colorIndex: Int) {
        viewModelScope.launch {
            val currentNotes = allNotes.value
            currentNotes.filter { noteIds.contains(it.id) }.forEach { note ->
                repository.updateNote(note.copy(colorIndex = colorIndex))
            }
            NotesWidget().updateAll(getApplication())
        }
    }

    fun exportBackup(
        outputStream: OutputStream, 
        selectedCategoryIds: Set<Int>,
        selectedTodoIds: Set<Int>,
        selectedSubTaskIds: Set<Int>,
        selectedNoteIds: Set<Int>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val backupData = BackupData(
                    categories = categories.value.filter { selectedCategoryIds.contains(it.id) },
                    todos = allTodosList.value.filter { selectedTodoIds.contains(it.id) },
                    subTasks = allSubTasks.value.filter { selectedSubTaskIds.contains(it.id) },
                    notes = allNotes.value.filter { selectedNoteIds.contains(it.id) }
                )
                
                withContext(Dispatchers.IO) {
                    val moshi = Moshi.Builder().build()
                    val adapter = moshi.adapter(BackupData::class.java)
                    val json = adapter.toJson(backupData)
                    
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun importBackup(inputStream: InputStream, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    InputStreamReader(inputStream).use { reader ->
                        reader.readText()
                    }
                }

                val moshi = Moshi.Builder().build()
                val adapter = moshi.adapter(BackupData::class.java)
                val backupData = adapter.fromJson(json)

                if (backupData != null) {
                    withContext(Dispatchers.IO) {
                        database.clearAllTables()
                        
                        val categoryDao = database.categoryDao()
                        val todoDao = database.todoDao()
                        val noteDao = database.noteDao()

                        backupData.categories.forEach { categoryDao.insertCategory(it) }
                        backupData.todos.forEach { todoDao.insertTodo(it) }
                        backupData.subTasks.forEach { todoDao.insertSubTask(it) }
                        backupData.notes.forEach { noteDao.insertNote(it) }
                    }
                    
                    NotesWidget().updateAll(getApplication())
                    TasksWidget().updateAll(getApplication())
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }
}

data class TodoStats(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val highPriorityPending: Int = 0,
    val dueToday: Int = 0
)

data class CustomTheme(val id: String, val name: String, val color: Int, val applyToBackground: Boolean = true)
