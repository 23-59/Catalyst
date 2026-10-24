package com.goldenminute.catalyst.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenminute.catalyst.LocalizedContent
import com.goldenminute.catalyst.MainActivity
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.data.Category
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.NoteItem
import com.goldenminute.catalyst.data.TodoItem
import com.goldenminute.catalyst.data.appDataStore
import com.goldenminute.catalyst.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuickHighPriorityTasksActivity : ComponentActivity() {

    private val viewModel: TodoViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val lang = runBlocking {
            try {
                newBase.appDataStore.data.first()[DataStoreKeys.LANGUAGE]
            } catch (_: Exception) {
                null
            }
        } ?: if (Locale.getDefault().language == "fa") "fa" else "en"

        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure activity to display over lock screen and wake the screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.cancel(1099)

        val isPreview = intent.getBooleanExtra("is_preview", false)
        if (!isPreview) {
            val currentMode = com.goldenminute.catalyst.service.PowerButtonShortcutManager.currentMode
            val savedMode = runBlocking {
                try {
                    applicationContext.appDataStore.data.first()[DataStoreKeys.POWER_BUTTON_SHORTCUT_MODE] ?: "none"
                } catch (_: Exception) {
                    currentMode
                }
            }
            if (savedMode == "none" || currentMode == "none") {
                android.util.Log.d("QuickTasksActivity", "Finishing: power button shortcut is disabled")
                finish()
                return
            }

            val action = intent.action
            if (savedMode == "double_press" && action == Intent.ACTION_ASSIST) {
                android.util.Log.d("QuickTasksActivity", "Finishing: double_press active, ignoring assist")
                finish()
                return
            }
            if (savedMode == "long_press" && (
                action == android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA ||
                action == android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
            )) {
                android.util.Log.d("QuickTasksActivity", "Finishing: long_press active, ignoring camera")
                finish()
                return
            }
        }

        setContent {
            val context = LocalContext.current
            val haptic = LocalHapticFeedback.current
            val coroutineScope = rememberCoroutineScope()

            val appLanguage by context.appDataStore.data
                .collectAsState(initial = null)
            val lang = appLanguage?.get(DataStoreKeys.LANGUAGE)
                ?: if (Locale.getDefault().language == "fa") "fa" else "en"
            val isRtl = lang == "fa"

            // Data streams from TodoViewModel
            val allTodos: List<TodoItem> by viewModel.allTodosList.collectAsState(initial = emptyList())
            val allNotes: List<NoteItem> by viewModel.allNotes.collectAsState(initial = emptyList())
            val taskCategories by viewModel.taskCategories.collectAsState(initial = emptyList())
            val noteCategories by viewModel.noteCategories.collectAsState(initial = emptyList())
            val categories by viewModel.categories.collectAsState(initial = emptyList())
            val categoriesMap = remember(categories) { categories.associateBy { it.id } }

            // High priority tasks first, then other pending tasks
            val highPriorityTasks = remember(allTodos) {
                allTodos.filter { !it.isCompleted && it.priority == 2 }
                    .sortedBy { it.dueDate ?: Long.MAX_VALUE }
            }
            val otherPendingTasks = remember(allTodos) {
                allTodos.filter { !it.isCompleted && it.priority != 2 }
                    .sortedWith(compareByDescending<TodoItem> { it.priority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
            }

            val tasksToShow: List<TodoItem> = remember(highPriorityTasks, otherPendingTasks) {
                if (highPriorityTasks.isNotEmpty()) highPriorityTasks else otherPendingTasks.take(5)
            }

            // Tab State: 0 = Tasks, 1 = Notes
            var selectedTab by remember { mutableIntStateOf(0) }

            // Sub-screen overlays for adding/editing tasks & notes with all details
            var showAddTaskScreen by remember { mutableStateOf(false) }
            var taskToEdit by remember { mutableStateOf<TodoItem?>(null) }
            var showAddNoteScreen by remember { mutableStateOf(false) }
            var noteToEdit by remember { mutableStateOf<NoteItem?>(null) }
            var showAddCategoryDialog by remember { mutableStateOf<String?>(null) }
            var noteToDelete by remember { mutableStateOf<NoteItem?>(null) }

            CompositionLocalProvider(
                LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                LocalizedContent(localeCode = lang) {
                    MaterialTheme(
                        typography = getTypography(if (isRtl) Vazirmatn else FontFamily.Default)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Main Power Button Dialog Card
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = ObsidianBlack,
                                border = BorderStroke(1.5.dp, BorderColor),
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .clickable(enabled = false) {}
                                    .padding(vertical = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Header with dynamic badge, title & close
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (selectedTab == 0) PriorityHigh.copy(alpha = 0.18f) else NeonCyan.copy(alpha = 0.18f),
                                                border = BorderStroke(1.dp, if (selectedTab == 0) PriorityHigh.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.5f)),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (selectedTab == 0) Icons.Default.Bolt else Icons.Default.Description,
                                                        contentDescription = null,
                                                        tint = if (selectedTab == 0) PriorityHigh else NeonCyan,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = if (selectedTab == 0) stringResource(R.string.quick_tasks_title) else stringResource(R.string.quick_notes_title),
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = if (selectedTab == 0) stringResource(R.string.quick_tasks_subtitle) else stringResource(R.string.quick_notes_subtitle),
                                                    color = TextSecondary,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Top Header Add Button for rapid creation
                                            IconButton(
                                                onClick = {
                                                    if (selectedTab == 0) {
                                                        taskToEdit = null
                                                        showAddTaskScreen = true
                                                    } else {
                                                        noteToEdit = null
                                                        showAddNoteScreen = true
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddCircleOutline,
                                                    contentDescription = if (selectedTab == 0) stringResource(R.string.quick_add_task_btn) else stringResource(R.string.quick_add_note_btn),
                                                    tint = if (selectedTab == 0) PriorityHigh else NeonCyan,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { finish() },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.close),
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Segmented Tab Row: [ Tasks ] [ Notes ]
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(SlateDark)
                                            .border(BorderStroke(1.dp, BorderColor.copy(alpha = 0.25f)), RoundedCornerShape(14.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Tasks Tab
                                        val isTasksActive = selectedTab == 0
                                        val tasksBg by animateColorAsState(
                                            targetValue = if (isTasksActive) PriorityHigh.copy(alpha = 0.18f) else Color.Transparent,
                                            label = "tasks_tab_bg"
                                        )
                                        val tasksBorder by animateColorAsState(
                                            targetValue = if (isTasksActive) PriorityHigh.copy(alpha = 0.5f) else Color.Transparent,
                                            label = "tasks_tab_border"
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = tasksBg,
                                            border = BorderStroke(1.dp, tasksBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clickable { selectedTab = 0 }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = if (isTasksActive) PriorityHigh else TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.quick_tab_tasks),
                                                    color = if (isTasksActive) PriorityHigh else TextSecondary,
                                                    fontWeight = if (isTasksActive) FontWeight.Bold else FontWeight.Medium,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                if (tasksToShow.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isTasksActive) PriorityHigh.copy(alpha = 0.25f) else SlateLight.copy(alpha = 0.3f),
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "${tasksToShow.size}",
                                                                color = if (isTasksActive) PriorityHigh else TextSecondary,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Notes Tab
                                        val isNotesActive = selectedTab == 1
                                        val notesBg by animateColorAsState(
                                            targetValue = if (isNotesActive) NeonCyan.copy(alpha = 0.18f) else Color.Transparent,
                                            label = "notes_tab_bg"
                                        )
                                        val notesBorder by animateColorAsState(
                                            targetValue = if (isNotesActive) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                                            label = "notes_tab_border"
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = notesBg,
                                            border = BorderStroke(1.dp, notesBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clickable { selectedTab = 1 }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = if (isNotesActive) NeonCyan else TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.quick_tab_notes),
                                                    color = if (isNotesActive) NeonCyan else TextSecondary,
                                                    fontWeight = if (isNotesActive) FontWeight.Bold else FontWeight.Medium,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                if (allNotes.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isNotesActive) NeonCyan.copy(alpha = 0.25f) else SlateLight.copy(alpha = 0.3f),
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "${allNotes.size}",
                                                                color = if (isNotesActive) NeonCyan else TextSecondary,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Content area based on selected tab
                                    AnimatedContent(
                                        targetState = selectedTab,
                                        transitionSpec = {
                                            if (targetState > initialState) {
                                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                                    slideOutHorizontally { width -> -width } + fadeOut()
                                            } else {
                                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                                    slideOutHorizontally { width -> width } + fadeOut()
                                            }
                                        },
                                        label = "quick_tab_content"
                                    ) { tabIndex ->
                                        if (tabIndex == 0) {
                                            // Tasks Section
                                            if (tasksToShow.isEmpty()) {
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = SlateDark,
                                                    border = BorderStroke(1.dp, PriorityLow.copy(alpha = 0.3f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(20.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle,
                                                            contentDescription = null,
                                                            tint = PriorityLow,
                                                            modifier = Modifier.size(44.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Text(
                                                            text = stringResource(R.string.quick_tasks_empty),
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = stringResource(R.string.quick_tasks_empty_sub),
                                                            color = TextSecondary,
                                                            textAlign = TextAlign.Center,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 280.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    items(tasksToShow, key = { task: TodoItem -> task.id }) { task: TodoItem ->
                                                        QuickTaskCard(
                                                            task = task,
                                                            onClick = {
                                                                taskToEdit = task
                                                                showAddTaskScreen = true
                                                            },
                                                            onToggleComplete = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                viewModel.toggleTodoCompleted(task.id, true)
                                                                Toast.makeText(
                                                                    context,
                                                                    context.getString(R.string.quick_tasks_completed_toast),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // Notes Section
                                            if (allNotes.isEmpty()) {
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = SlateDark,
                                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(20.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Description,
                                                            contentDescription = null,
                                                            tint = NeonCyan,
                                                            modifier = Modifier.size(44.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Text(
                                                            text = stringResource(R.string.quick_notes_empty),
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            textAlign = TextAlign.Center,
                                                            style = MaterialTheme.typography.bodyLarge
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = stringResource(R.string.quick_notes_empty_sub),
                                                            color = TextSecondary,
                                                            textAlign = TextAlign.Center,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            } else {
                                                LazyColumn(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(max = 280.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    items(allNotes, key = { note: NoteItem -> note.id }) { note: NoteItem ->
                                                        QuickNoteCard(
                                                            note = note,
                                                            category = categoriesMap[note.categoryId],
                                                            onClick = {
                                                                noteToEdit = note
                                                                showAddNoteScreen = true
                                                            },
                                                            onDelete = {
                                                                noteToDelete = note
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Action Buttons Row: [Open Catalyst] and [+ New Task/Note]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Open Catalyst Button with full readability & icon
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(context, MainActivity::class.java).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                    putExtra("TARGET_SCREEN", if (selectedTab == 0) "Tasks" else "Notes")
                                                }
                                                context.startActivity(intent)
                                                finish()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.55f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = NeonCyan.copy(alpha = 0.12f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.OpenInNew,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(R.string.quick_tasks_open_app),
                                                color = NeonCyan,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1
                                            )
                                        }

                                        // 2. Primary Add New Task / Note Button
                                        val accentColor = if (selectedTab == 0) PriorityHigh else NeonCyan
                                        Button(
                                            onClick = {
                                                if (selectedTab == 0) {
                                                    taskToEdit = null
                                                    showAddTaskScreen = true
                                                } else {
                                                    noteToEdit = null
                                                    showAddNoteScreen = true
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = accentColor
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = ObsidianBlack,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (selectedTab == 0) stringResource(R.string.quick_add_task_btn) else stringResource(R.string.quick_add_note_btn),
                                                color = ObsidianBlack,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Full-width, prominent Close Button
                                    OutlinedButton(
                                        onClick = { finish() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.35f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = SlateDark.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.close),
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // Full-screen overlay: Add / Edit Task Screen with all details
                            AnimatedVisibility(
                                visible = showAddTaskScreen,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                BackHandler {
                                    showAddTaskScreen = false
                                    taskToEdit = null
                                }
                                AddTaskScreen(
                                    todoToEdit = taskToEdit,
                                    categories = taskCategories,
                                    viewModel = viewModel,
                                    onDismiss = {
                                        showAddTaskScreen = false
                                        taskToEdit = null
                                    },
                                    onConfirm = { title, desc, dueDate, priority, catId, hasReminder, reminderTime, isDaily, hasTime, isReminderRepeating, associatedPackageNames, timerMode, associatedSections ->
                                        viewModel.insertTodo(
                                            (taskToEdit ?: TodoItem(
                                                title = title,
                                                description = desc,
                                                position = 0
                                            )).copy(
                                                title = title,
                                                description = desc,
                                                dueDate = dueDate,
                                                priority = priority,
                                                categoryId = catId,
                                                hasReminder = hasReminder,
                                                reminderTime = reminderTime,
                                                isDaily = isDaily,
                                                hasTime = hasTime,
                                                isReminderRepeating = isReminderRepeating,
                                                associatedPackageNames = associatedPackageNames,
                                                timerMode = timerMode,
                                                associatedSections = associatedSections
                                            )
                                        )
                                        showAddTaskScreen = false
                                        taskToEdit = null
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.quick_tasks_saved_toast),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }

                            // Full-screen overlay: Add / Edit Note Screen with all details
                            AnimatedVisibility(
                                visible = showAddNoteScreen,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                BackHandler {
                                    showAddNoteScreen = false
                                    noteToEdit = null
                                }
                                AddNoteScreen(
                                    noteToEdit = noteToEdit,
                                    categories = noteCategories,
                                    onDismiss = {
                                        showAddNoteScreen = false
                                        noteToEdit = null
                                    },
                                    onAddCategoryClick = { showAddCategoryDialog = "note" },
                                    onDeleteCategory = { viewModel.deleteCategory(it) },
                                    onConfirm = { title, content, colorIndex, categoryId ->
                                        viewModel.insertNote(
                                            NoteItem(
                                                id = noteToEdit?.id ?: 0,
                                                title = title,
                                                content = content,
                                                createdAt = noteToEdit?.createdAt ?: System.currentTimeMillis(),
                                                colorIndex = colorIndex,
                                                categoryId = categoryId
                                            )
                                        )
                                        showAddNoteScreen = false
                                        noteToEdit = null
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.quick_notes_saved_toast),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }

                            // Sub-dialog: Category Creation if user taps + Category inside note screen
                            if (showAddCategoryDialog != null) {
                                AddCategoryDialog(
                                    onDismiss = { showAddCategoryDialog = null },
                                    onConfirm = { name, color, icon ->
                                        viewModel.addCategory(name, color, icon, type = showAddCategoryDialog!!)
                                        showAddCategoryDialog = null
                                    }
                                )
                            }

                            // Delete confirmation dialog for notes
                            if (noteToDelete != null) {
                                AppAlertDialog(
                                    onDismissRequest = { noteToDelete = null },
                                    title = {
                                        Text(
                                            text = stringResource(R.string.delete_note_title),
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = stringResource(R.string.delete_note_message),
                                            color = TextSecondary
                                        )
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                noteToDelete?.let {
                                                    viewModel.deleteNote(it.id)
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.note_deleted),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                noteToDelete = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PriorityHigh)
                                        ) {
                                            Text(stringResource(R.string.delete_button), color = Color.White)
                                        }
                                    },
                                    dismissButton = {
                                        OutlinedButton(onClick = { noteToDelete = null }) {
                                            Text(stringResource(R.string.cancel), color = TextSecondary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTaskCard(
    task: TodoItem,
    onClick: () -> Unit = {},
    onToggleComplete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SlateDark,
        border = BorderStroke(
            1.dp,
            if (task.priority == 2) PriorityHigh.copy(alpha = 0.45f) else BorderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Complete checkbox
            Checkbox(
                checked = false,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PriorityLow,
                    uncheckedColor = if (task.priority == 2) PriorityHigh else TextSecondary
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (task.dueDate != null) {
                    val isPersian = Locale.getDefault().language == "fa"
                    val dateStr = if (isPersian) {
                        JalaliCalendarHelper.formatJalali(task.dueDate, includeTime = task.hasTime, includeYear = false)
                    } else {
                        val sdf = SimpleDateFormat(if (task.hasTime) "MMM d • h:mm a" else "MMM d", Locale.getDefault())
                        sdf.format(Date(task.dueDate))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (task.dueDate < System.currentTimeMillis()) PriorityHigh else TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = dateStr,
                            color = if (task.dueDate < System.currentTimeMillis()) PriorityHigh else TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickNoteCard(
    note: NoteItem,
    category: Category?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = resolveNotePalette(note.colorIndex)
    val isPersian = Locale.getDefault().language == "fa"
    val dateStr = remember(note.createdAt, isPersian) {
        if (isPersian) {
            JalaliCalendarHelper.formatJalali(note.createdAt, includeTime = true, includeYear = false)
        } else {
            val sdf = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
            sdf.format(Date(note.createdAt))
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SlateDark,
        border = BorderStroke(1.dp, palette.primaryColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Palette accent pill on start edge
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.primaryColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (note.title.isNotBlank()) note.title else stringResource(R.string.quick_note_untitled),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (category != null) {
                        val catColor = remember(category.colorHex) { safeParseColor(category.colorHex) }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catColor.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, catColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = category.name,
                                color = catColor,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = note.content,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
