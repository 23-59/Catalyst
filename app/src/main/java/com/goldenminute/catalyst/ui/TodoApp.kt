package com.goldenminute.catalyst.ui

import android.app.DatePickerDialog
import kotlinx.coroutines.launch
import android.app.TimePickerDialog
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.*
import com.goldenminute.catalyst.ui.theme.LocalThemeRevealState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.data.*
import com.goldenminute.catalyst.ui.theme.*
import java.text.SimpleDateFormat
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.blurEffect
import androidx.core.graphics.toColorInt

fun getSystemLocale(): Locale = Locale.getDefault()

@Composable
fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Work" -> Icons.Default.Work
        "Person", "Personal" -> Icons.Default.Person
        "ShoppingCart", "Shopping" -> Icons.Default.ShoppingCart
        "Favorite", "Health" -> Icons.Default.Favorite
        "Book", "Study" -> Icons.Default.Book
        "AttachMoney", "Finance" -> Icons.Default.AttachMoney
        "DirectionsRun", "Fitness" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Brush", "Creative" -> Icons.Default.Brush
        else -> Icons.AutoMirrored.Filled.List
    }
}

val premiumEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(viewModel: TodoViewModel) {
    val todos by viewModel.filteredTodos.collectAsState()
    val notes by viewModel.allNotes.collectAsState()
    var localTodos by remember { mutableStateOf<List<TodoItem>>(emptyList()) }

    LaunchedEffect(todos) {
        localTodos = todos
    }

    val allTodos by viewModel.allTodosList.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val taskCategories by viewModel.taskCategories.collectAsState()
    val noteCategories by viewModel.noteCategories.collectAsState()
    val stats by viewModel.todoStats.collectAsState()
    val subTasks by viewModel.allSubTasks.collectAsState()

    val selectedCatId by viewModel.selectedCategoryId.collectAsState()
    val selectedPriorityId by viewModel.selectedPriorityId.collectAsState()
    val searchText by viewModel.searchQuery.collectAsState()
    val currentTab by viewModel.selectedTab.collectAsState()
    val currentTheme by viewModel.appTheme.collectAsState()
    val customThemeColor by viewModel.customThemeColor.collectAsState()

    val subTasksGrouped = remember(subTasks) { subTasks.groupBy { it.todoId } }
    val categoriesMap = remember(categories) { categories.associateBy { it.id } }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var todoToEdit by remember { mutableStateOf<TodoItem?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf<String?>(null) }
    var showSearchBox by remember { mutableStateOf(false) }
    var showDailyDigestModal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var searchIsListening by remember { mutableStateOf(false) }
    var searchVoiceErrorMessage by remember { mutableStateOf<String?>(null) }
    val searchSpeechManager = remember { SpeechRecognizerManager(context) }

    val searchPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            searchVoiceErrorMessage = null
            searchSpeechManager.startListening(
                onResult = { text ->
                    viewModel.searchQuery.value = text
                },
                onError = { error ->
                    searchVoiceErrorMessage = error
                },
                onListeningStateChange = { listening ->
                    searchIsListening = listening
                }
            )
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.voice_permission_rationale),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val toggleSearchVoiceInput = {
        if (searchIsListening) {
            searchSpeechManager.stopListening()
            searchIsListening = false
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                searchVoiceErrorMessage = null
                searchSpeechManager.startListening(
                    onResult = { text ->
                        viewModel.searchQuery.value = text
                    },
                    onError = { error ->
                        searchVoiceErrorMessage = error
                    },
                    onListeningStateChange = { listening ->
                        searchIsListening = listening
                    }
                )
            } else {
                searchPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    DisposableEffect(searchSpeechManager) {
        onDispose {
            searchSpeechManager.stopListening()
        }
    }

    var currentScreen by viewModel.currentScreenState // "Tasks" or "Notes"
    var previousScreen by viewModel.previousScreenState

    LaunchedEffect(currentScreen) {
        showSearchBox = false
        viewModel.searchQuery.value = ""
    }

    val selectedTabIndex by remember(currentScreen) {
        derivedStateOf {
            when (currentScreen) {
                "Tasks" -> 0
                "Focus" -> 1
                "Notes" -> 3
                "Settings" -> 4
                else -> 0
            }
        }
    }
    val animatedGlowCenter by animateFloatAsState(
        targetValue = selectedTabIndex.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "glowCenter"
    )

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteItem?>(null) }
    var isTasksFullScreen by remember { mutableStateOf(false) }
    var isNotesFullScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = isTasksFullScreen || isNotesFullScreen) {
        isTasksFullScreen = false
        isNotesFullScreen = false
    }

    BackHandler(enabled = currentScreen != "Tasks" && !isTasksFullScreen && !isNotesFullScreen) {
        currentScreen = if (currentScreen == "Progress") {
            if (previousScreen != "Progress") previousScreen else "Tasks"
        } else {
            "Tasks"
        }
    }

    val listState = rememberLazyListState()
    val notesListState = rememberLazyStaggeredGridState()

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedTaskIds = remember { mutableStateListOf<Int>() }

    var isNotesMultiSelectMode by remember { mutableStateOf(false) }
    val selectedNoteIds = remember { mutableStateListOf<Int>() }
    var showBulkNoteColorDialog by remember { mutableStateOf(false) }

    var todoToDelete by remember { mutableStateOf<TodoItem?>(null) }
    var noteToDelete by remember { mutableStateOf<NoteItem?>(null) }

    var todoToShowDetails by remember { mutableStateOf<TodoItem?>(null) }
    var noteToShowDetails by remember { mutableStateOf<NoteItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val hazeState = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = SlateDark,
                        contentColor = TextPrimary,
                        actionColor = NeonCyan,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            },
            floatingActionButton = {
                val isTasksFS = currentScreen == "Tasks" && isTasksFullScreen
                val isNotesFS = currentScreen == "Notes" && isNotesFullScreen
                if (isTasksFS || isNotesFS) {
                    val fsAddInteractionSource = remember { MutableInteractionSource() }
                    val fsAddIsPressed by fsAddInteractionSource.collectIsPressedAsState()
                    val fsAddScale by animateFloatAsState(
                        targetValue = if (fsAddIsPressed) 0.85f else 1f,
                        animationSpec = tween(durationMillis = 400, easing = premiumEasing),
                        label = "fsAddScale"
                    )
                    val fsAddRotation by animateFloatAsState(
                        targetValue = if (fsAddIsPressed) 90f else 0f,
                        animationSpec = tween(durationMillis = 400, easing = premiumEasing),
                        label = "fsAddRotation"
                    )

                    FloatingActionButton(
                        onClick = {
                            if (isTasksFS) {
                                todoToEdit = null
                                showAddTaskDialog = true
                            } else {
                                noteToEdit = null
                                showAddNoteDialog = true
                            }
                        },
                        interactionSource = fsAddInteractionSource,
                        containerColor = NeonCyan.copy(alpha = 0.85f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(bottom = 16.dp, end = 8.dp)
                            .graphicsLayer { scaleX = fsAddScale; scaleY = fsAddScale }
                            .testTag("fullscreen_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (isTasksFS) "Add Task" else "Add Note",
                            modifier = Modifier.graphicsLayer { rotationZ = fsAddRotation }
                        )
                    }
                }
            },
            topBar = {
                if (currentScreen != "Progress") {
                    val isTasksFS = currentScreen == "Tasks" && isTasksFullScreen
                    val isNotesFS = currentScreen == "Notes" && isNotesFullScreen
                    val isAnyFS = isTasksFS || isNotesFS

                    if (isAnyFS) {
                        Column(
                            modifier = Modifier
                                .background(Color.Transparent)
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .shadow(SurfaceElevation, RoundedCornerShape(14.dp))
                                            .background(SlateDark, RoundedCornerShape(14.dp))
                                            .border(
                                                BorderStroke(1.dp, BorderColor),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                if (isTasksFS) isTasksFullScreen = false
                                                if (isNotesFS) isNotesFullScreen = false
                                            }
                                            .testTag("fullscreen_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Exit Full Screen",
                                            tint = TextPrimary
                                        )
                                    }
                                    Text(
                                        text = if (isTasksFS) stringResource(R.string.tasks_fullscreen) else stringResource(
                                            R.string.notes_fullscreen
                                        ),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextPrimary,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp)
                                    )
                                }

                                val fsExitInteractionSource =
                                    remember { MutableInteractionSource() }
                                val fsExitIsPressed by fsExitInteractionSource.collectIsPressedAsState()
                                val fsExitScale by animateFloatAsState(
                                    targetValue = if (fsExitIsPressed) 0.85f else 1f,
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = premiumEasing
                                    ),
                                    label = "fsExitScale"
                                )
                                val fsExitRotation by animateFloatAsState(
                                    targetValue = if (fsExitIsPressed) -45f else 0f,
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = premiumEasing
                                    ),
                                    label = "fsExitRotation"
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .graphicsLayer {
                                            scaleX = fsExitScale
                                            scaleY = fsExitScale
                                            rotationZ = fsExitRotation
                                        }
                                        .shadow(SurfaceElevation, RoundedCornerShape(14.dp))
                                        .background(SlateDark, RoundedCornerShape(14.dp))
                                        .border(
                                            BorderStroke(1.dp, BorderColor),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(
                                            interactionSource = fsExitInteractionSource,
                                            indication = null
                                        ) {
                                            if (isTasksFS) isTasksFullScreen = false
                                            if (isNotesFS) isNotesFullScreen = false
                                        }
                                        .testTag("fullscreen_exit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Full Screen",
                                        tint = NeonCyan
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .background(Color.Transparent)
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = if (currentScreen == "Settings") stringResource(R.string.settings_title) else stringResource(
                                            R.string.app_name
                                        ),
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-1).sp
                                        ),
                                        color = TextPrimary
                                    )
                                    if (currentScreen != "Settings") {
                                        val dateText = remember {
                                            val isPersian = getSystemLocale().language == "fa"
                                            if (isPersian) {
                                                val cal = Calendar.getInstance()
                                                val jd = JalaliCalendarHelper.gregorianToJalali(
                                                    cal.get(Calendar.YEAR),
                                                    cal.get(Calendar.MONTH) + 1,
                                                    cal.get(Calendar.DAY_OF_MONTH)
                                                )
                                                val dayOfWeekName =
                                                    JalaliCalendarHelper.getPersianDayOfWeekName(
                                                        cal.get(Calendar.DAY_OF_WEEK)
                                                    )
                                                val monthName =
                                                    JalaliCalendarHelper.JalaliDate.getMonthName(jd.month)
                                                val dayStr =
                                                    JalaliCalendarHelper.toPersianDigits(jd.day.toString())
                                                "$dayOfWeekName، $dayStr $monthName"
                                            } else {
                                                val sdf = SimpleDateFormat(
                                                    "EEEE, d MMMM",
                                                    getSystemLocale()
                                                )
                                                sdf.format(Date())
                                            }
                                        }
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                // Header Actions (Settings & Add Task)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (currentScreen != "Focus" && currentScreen != "Settings") {
                                        // Search Toggle Option
                                        val searchInteractionSource =
                                            remember { MutableInteractionSource() }
                                        val searchIsPressed by searchInteractionSource.collectIsPressedAsState()
                                        val searchScale by animateFloatAsState(
                                            if (searchIsPressed) 0.9f else 1f,
                                            label = "search_scale"
                                        )
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .scale(searchScale)
                                                .shadow(SurfaceElevation, RoundedCornerShape(14.dp))
                                                .background(SlateDark, RoundedCornerShape(14.dp))
                                                .border(
                                                    BorderStroke(
                                                        1.dp,
                                                        if (showSearchBox) NeonCyan else BorderColor
                                                    ), RoundedCornerShape(14.dp)
                                                )
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable(
                                                    interactionSource = searchInteractionSource,
                                                    indication = null,
                                                    onClick = { showSearchBox = !showSearchBox }
                                                )
                                                .testTag("search_toggle_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Toggle Search",
                                                tint = if (showSearchBox) NeonCyan else TextSecondary
                                            )
                                        }

                                        // Daily Digest Option
                                        val digestInteractionSource =
                                            remember { MutableInteractionSource() }
                                        val digestIsPressed by digestInteractionSource.collectIsPressedAsState()
                                        val digestScale by animateFloatAsState(
                                            if (digestIsPressed) 0.9f else 1f,
                                            label = "digest_scale"
                                        )
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .scale(digestScale)
                                                .shadow(SurfaceElevation, RoundedCornerShape(14.dp))
                                                .background(SlateDark, RoundedCornerShape(14.dp))
                                                .border(
                                                    BorderStroke(1.dp, BorderColor),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable(
                                                    interactionSource = digestInteractionSource,
                                                    indication = null,
                                                    onClick = { showDailyDigestModal = true }
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Daily Digest",
                                                tint = NeonPurple
                                            )
                                        }
                                        // Full Screen Toggle
                                        if (currentScreen == "Tasks" || currentScreen == "Notes") {
                                            val fsInteractionSource =
                                                remember { MutableInteractionSource() }
                                            val fsIsPressed by fsInteractionSource.collectIsPressedAsState()
                                            val fsScale by animateFloatAsState(
                                                targetValue = if (fsIsPressed) 0.85f else 1f,
                                                animationSpec = tween(
                                                    durationMillis = 400,
                                                    easing = premiumEasing
                                                ),
                                                label = "fs_scale"
                                            )
                                            val fsRotation by animateFloatAsState(
                                                targetValue = if (fsIsPressed) 45f else 0f,
                                                animationSpec = tween(
                                                    durationMillis = 400,
                                                    easing = premiumEasing
                                                ),
                                                label = "fs_rotation"
                                            )

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .scale(fsScale)
                                                    .graphicsLayer { rotationZ = fsRotation }
                                                    .shadow(
                                                        SurfaceElevation,
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .background(
                                                        SlateDark,
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .border(
                                                        BorderStroke(1.dp, BorderColor),
                                                        RoundedCornerShape(14.dp)
                                                    )
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable(
                                                        interactionSource = fsInteractionSource,
                                                        indication = null,
                                                        onClick = {
                                                            if (currentScreen == "Tasks") {
                                                                isTasksFullScreen = true
                                                            } else {
                                                                isNotesFullScreen = true
                                                            }
                                                        }
                                                    )
                                                    .testTag("fullscreen_enter_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fullscreen,
                                                    contentDescription = "Enter Full Screen",
                                                    tint = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Modern Search TextField with glowing aesthetic
                            AnimatedVisibility(
                                visible = showSearchBox && currentScreen != "Focus",
                                enter = slideInVertically(),
                                exit = slideOutVertically() + fadeOut()
                            ) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { viewModel.searchQuery.value = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_text_input")
                                        .padding(bottom = 16.dp),
                                    placeholder = {
                                        Text(
                                            stringResource(R.string.search_placeholder),
                                            color = TextMuted
                                        )
                                    },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = stringResource(R.string.search_placeholder),
                                            tint = TextSecondary
                                        )
                                    },
                                    trailingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            if (searchText.isNotEmpty()) {
                                                IconButton(onClick = {
                                                    viewModel.searchQuery.value = ""
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = stringResource(R.string.cancel),
                                                        tint = TextSecondary
                                                    )
                                                }
                                            }
                                            IconButton(onClick = toggleSearchVoiceInput) {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = "Voice Search",
                                                    tint = if (searchIsListening) NeonPurple else TextSecondary,
                                                    modifier = if (searchIsListening) {
                                                        val infiniteTransition =
                                                            rememberInfiniteTransition(label = "searchPulse")
                                                        val scale by infiniteTransition.animateFloat(
                                                            initialValue = 1f,
                                                            targetValue = 1.3f,
                                                            animationSpec = infiniteRepeatable(
                                                                animation = tween(
                                                                    800,
                                                                    easing = LinearEasing
                                                                ),
                                                                repeatMode = RepeatMode.Reverse
                                                            ),
                                                            label = "searchMicScale"
                                                        )
                                                        Modifier.scale(scale)
                                                    } else Modifier
                                                )
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                                        unfocusedBorderColor = BorderColor,
                                        focusedContainerColor = SlateDark,
                                        unfocusedContainerColor = SlateDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                val isTasksFS = currentScreen == "Tasks" && isTasksFullScreen
                val isNotesFS = currentScreen == "Notes" && isNotesFullScreen
                if (isMultiSelectMode) {
                    val bottomBarBgColor = SlateDark
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                            .height(68.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(15.dp)
                            )
                            .background(bottomBarBgColor, RoundedCornerShape(15.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(15.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.items_selected, selectedTaskIds.size),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Select/Deselect All Toggle
                            val allSelected = localTodos.isNotEmpty() && localTodos.all {
                                selectedTaskIds.contains(it.id)
                            }
                            IconButton(
                                onClick = {
                                    if (allSelected) {
                                        selectedTaskIds.clear()
                                        isMultiSelectMode = false
                                    } else {
                                        localTodos.forEach {
                                            if (!selectedTaskIds.contains(it.id)) {
                                                selectedTaskIds.add(it.id)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allSelected) stringResource(R.string.action_deselect_all) else stringResource(
                                        R.string.action_select_all
                                    ),
                                    tint = NeonCyan
                                )
                            }

                            IconButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val tasksToComplete =
                                        localTodos.filter { selectedTaskIds.contains(it.id) }
                                    tasksToComplete.forEach {
                                        viewModel.toggleTodoCompleted(
                                            it.id,
                                            true
                                        )
                                    }
                                    isMultiSelectMode = false
                                    selectedTaskIds.clear()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Mark Completed",
                                    tint = NeonCyan
                                )
                            }
                            IconButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val tasksToDelete =
                                        localTodos.filter { selectedTaskIds.contains(it.id) }
                                    tasksToDelete.forEach { viewModel.deleteTodo(it) }
                                    isMultiSelectMode = false
                                    selectedTaskIds.clear()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = PriorityHigh
                                )
                            }
                            IconButton(
                                onClick = {
                                    isMultiSelectMode = false
                                    selectedTaskIds.clear()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                } else if (isNotesMultiSelectMode) {
                    val bottomBarBgColor = SlateDark
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp, start = 24.dp, end = 24.dp)
                            .height(68.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(15.dp)
                            )
                            .background(bottomBarBgColor, RoundedCornerShape(15.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(15.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.items_selected, selectedNoteIds.size),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Select/Deselect All Toggle
                            val allSelected =
                                notes.isNotEmpty() && notes.all { selectedNoteIds.contains(it.id) }
                            IconButton(
                                onClick = {
                                    if (allSelected) {
                                        selectedNoteIds.clear()
                                        isNotesMultiSelectMode = false
                                    } else {
                                        notes.forEach {
                                            if (!selectedNoteIds.contains(it.id)) {
                                                selectedNoteIds.add(it.id)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allSelected) stringResource(R.string.action_deselect_all) else stringResource(
                                        R.string.action_select_all
                                    ),
                                    tint = NeonCyan
                                )
                            }

                            IconButton(
                                onClick = {
                                    showBulkNoteColorDialog = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Assign Theme",
                                    tint = NeonCyan
                                )
                            }
                            IconButton(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val notesToDelete =
                                        notes.filter { selectedNoteIds.contains(it.id) }
                                    viewModel.deleteNotesInBulk(selectedNoteIds.toList())
                                    isNotesMultiSelectMode = false
                                    selectedNoteIds.clear()

                                    coroutineScope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.journal_entries_deleted, notesToDelete.size),
                                            actionLabel = context.getString(R.string.undo),
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            notesToDelete.forEach { note ->
                                                viewModel.insertNote(note)
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = PriorityHigh
                                )
                            }
                            IconButton(
                                onClick = {
                                    isNotesMultiSelectMode = false
                                    selectedNoteIds.clear()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                } else if (!isTasksFS && !isNotesFS && currentScreen != "Progress") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Interactive Bottom Navigation with High-Fidelity Glassmorphic Glow
                        val layoutDirection = LocalLayoutDirection.current
                        val isRtl = layoutDirection == LayoutDirection.Rtl

                        val isThemeAnimating = LocalThemeRevealState.current.isAnimating
                        AnimatedVisibility(
                            visible = !isThemeAnimating,
                            enter = fadeIn(animationSpec = tween(500)),
                            exit = fadeOut(animationSpec = tween(50))
                        ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // 1. Base Glassmorphism Background (Aligned to bottom)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .drawBehind {
                                        val shadowColor = "#40000000".toColorInt()
                                        val paint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.TRANSPARENT
                                            setShadowLayer(
                                                30f, // radius
                                                0f, // dx
                                                20f, // dy
                                                shadowColor
                                            )
                                        }
                                        drawContext.canvas.nativeCanvas.drawRoundRect(
                                            android.graphics.RectF(0f, 0f, size.width, size.height),
                                            15.dp.toPx(),
                                            15.dp.toPx(),
                                            paint
                                        )
                                    }
                                    .clip(RoundedCornerShape(15.dp))
                                    .hazeEffect(state = hazeState) {
                                        blurEffect {
                                            blurRadius = 30.dp
                                            noiseFactor = 0.1f
                                        }
                                    }
                                    .background(SlateDark.copy(alpha = 0.4f))
                                    .then(
                                        if (currentTheme == "slate_light" || currentTheme.startsWith("custom_light")) {
                                            Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(15.dp))
                                        } else {
                                            Modifier.border(
                                                width = 1.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.4f),
                                                        Color.White.copy(alpha = 0.1f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(15.dp)
                                            )
                                        }
                                    )
                            )

                            // 2. Premium Animated Glow (Following Active Tab)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .blur(45.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            ) {
                                val tabWidth = size.width / 5f
                                val glowX = if (isRtl) {
                                    size.width - (tabWidth * animatedGlowCenter + tabWidth / 2f)
                                } else {
                                    tabWidth * animatedGlowCenter + tabWidth / 2f
                                }

                                drawCircle(
                                    color = NeonCyan.copy(alpha = 0.55f),
                                    radius = size.height / 1.4f,
                                    center = Offset(glowX, size.height / 2f)
                                )
                            }

                            // 3. Bottom Bevel Gleam (Reflective Edge)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(15.dp))
                            ) {
                                val path = Path().apply {
                                    addRoundRect(
                                        RoundRect(
                                            size.toRect(),
                                            CornerRadius(15.dp.toPx())
                                        )
                                    )
                                }
                                val length = PathMeasure().apply { setPath(path, false) }.length
                                val tabWidth = size.width / 5f
                                val glowX = if (isRtl) {
                                    size.width - (tabWidth * animatedGlowCenter + tabWidth / 2f)
                                } else {
                                    tabWidth * animatedGlowCenter + tabWidth / 2f
                                }

                                drawPath(
                                    path = path,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            NeonCyan.copy(alpha = 0f),
                                            NeonCyan.copy(alpha = 0.9f),
                                            NeonCyan.copy(alpha = 0.9f),
                                            NeonCyan.copy(alpha = 0f)
                                        ),
                                        startX = glowX - tabWidth / 2f,
                                        endX = glowX + tabWidth / 2f
                                    ),
                                    style = Stroke(
                                        width = 3f,
                                        pathEffect = PathEffect.dashPathEffect(
                                            intervals = floatArrayOf(length / 2f, length)
                                        )
                                    )
                                )
                            }

                            // 4. Interactive Icons & Text (Transparent Row)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Tasks Tab
                                val tasksSelected = currentScreen == "Tasks"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { currentScreen = "Tasks" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.TaskAlt,
                                            contentDescription = stringResource(R.string.tab_nav_tasks),
                                            tint = if (tasksSelected) Color.White else TextSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        AnimatedVisibility(visible = tasksSelected) {
                                            Text(
                                                text = stringResource(R.string.tab_nav_tasks),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Focus Tab
                                val focusSelected = currentScreen == "Focus"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { currentScreen = "Focus" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Timer,
                                            contentDescription = stringResource(R.string.tab_nav_focus),
                                            tint = if (focusSelected) Color.White else TextSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        AnimatedVisibility(visible = focusSelected) {
                                            Text(
                                                text = stringResource(R.string.tab_nav_focus),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Central Add Button
                                val addInteractionSource = remember { MutableInteractionSource() }
                                val addIsPressed by addInteractionSource.collectIsPressedAsState()
                                val addScale by animateFloatAsState(
                                    targetValue = if (addIsPressed) 0.85f else 1f,
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = premiumEasing
                                    ),
                                    label = "addScale"
                                )
                                val addRotation by animateFloatAsState(
                                    targetValue = if (addIsPressed) 90f else 0f,
                                    animationSpec = tween(
                                        durationMillis = 400,
                                        easing = premiumEasing
                                    ),
                                    label = "addRotation"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .fillMaxHeight()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .graphicsLayer {
                                                scaleX = addScale
                                                scaleY = addScale
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(NeonCyan, NeonPurple)
                                                )
                                            )
                                            .clickable(
                                                interactionSource = addInteractionSource,
                                                indication = null
                                            ) {
                                                if (currentScreen == "Notes") {
                                                    noteToEdit = null
                                                    showAddNoteDialog = true
                                                } else {
                                                    todoToEdit = null
                                                    showAddTaskDialog = true
                                                }
                                            }
                                            .testTag("add_action_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.add_task),
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .graphicsLayer { rotationZ = addRotation }
                                        )
                                    }
                                }

                                // Notes Tab
                                val notesSelected = currentScreen == "Notes"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { currentScreen = "Notes" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = stringResource(R.string.tab_nav_notes),
                                            tint = if (notesSelected) Color.White else TextSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        AnimatedVisibility(visible = notesSelected) {
                                            Text(
                                                text = stringResource(R.string.tab_nav_notes),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Settings Tab
                                val settingsSelected = currentScreen == "Settings"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { currentScreen = "Settings" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = stringResource(R.string.settings_title),
                                            tint = if (settingsSelected) Color.White else TextSecondary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        AnimatedVisibility(visible = settingsSelected) {
                                            Text(
                                                text = stringResource(R.string.settings_title),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        } // End of AnimatedVisibility
                    }
                }
            }
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val bottomGapDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 54.dp
            val backgroundColor = MaterialTheme.colorScheme.background
            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier
                    .fillMaxSize()
                    
                    .hazeSource(state = hazeState)
                    .background(backgroundColor)
                    .drawBehind {
                        val r1 = maxOf(1f, size.maxDimension * 0.45f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x246366F1), Color.Transparent),
                                center = Offset(x = 0f, y = 0f),
                                radius = r1
                            ),
                            center = Offset(x = 0f, y = 0f),
                            radius = r1
                        )
                        val r2 = maxOf(1f, size.maxDimension * 0.55f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x2B8B5CF6), Color.Transparent),
                                center = Offset(x = size.width, y = size.height * 0.85f),
                                radius = r2
                            ),
                            center = Offset(x = size.width, y = size.height * 0.85f),
                            radius = r2
                        )
                    },
                transitionSpec = {
                    if (targetState == "Progress") {
                        (scaleIn(
                            initialScale = 0.85f,
                            animationSpec = tween(500, easing = premiumEasing)
                        ) + fadeIn(animationSpec = tween(500, easing = premiumEasing))) togetherWith
                                (scaleOut(
                                    targetScale = 1.15f,
                                    animationSpec = tween(500, easing = premiumEasing)
                                ) + fadeOut(animationSpec = tween(500, easing = premiumEasing)))
                    } else if (initialState == "Progress") {
                        (scaleIn(
                            initialScale = 1.15f,
                            animationSpec = tween(500, easing = premiumEasing)
                        ) + fadeIn(animationSpec = tween(500, easing = premiumEasing))) togetherWith
                                (scaleOut(
                                    targetScale = 0.85f,
                                    animationSpec = tween(500, easing = premiumEasing)
                                ) + fadeOut(animationSpec = tween(500, easing = premiumEasing)))
                    } else {
                        val targetIndex = when (targetState) {
                            "Tasks" -> 0
                            "Focus" -> 1
                            "Progress" -> 2
                            "Notes" -> 3
                            "Settings" -> 4
                            else -> 0
                        }
                        val initialIndex = when (initialState) {
                            "Tasks" -> 0
                            "Focus" -> 1
                            "Progress" -> 2
                            "Notes" -> 3
                            "Settings" -> 4
                            else -> 0
                        }
                        // Simplify direction: forward (right) index increases, backward (left) index decreases.
                        // We use the index difference to determine slide direction.
                        val slideDirection = if (targetIndex > initialIndex) 1 else -1
                        val isRtl = layoutDirection == LayoutDirection.Rtl
                        val finalDirection = if (isRtl) -slideDirection else slideDirection

                        (slideInHorizontally(
                            initialOffsetX = { width -> width * finalDirection },
                            animationSpec = tween(500, easing = premiumEasing)
                        ) + fadeIn(animationSpec = tween(500, easing = premiumEasing))) togetherWith
                                (slideOutHorizontally(
                                    targetOffsetX = { width -> -width * finalDirection },
                                    animationSpec = tween(500, easing = premiumEasing)
                                ) + fadeOut(animationSpec = tween(500, easing = premiumEasing)))
                    }
                },
                label = "screen_transition"
            ) { screen ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val canvasSize = size
                            val gradientHeight = bottomGapDp.toPx()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startY = canvasSize.height - gradientHeight,
                                    endY = canvasSize.height
                                ),
                                topLeft = Offset(0f, canvasSize.height - gradientHeight),
                                size = Size(canvasSize.width, gradientHeight),
                                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                            )
                        }
                ) {
                    when (screen) {
                    "Tasks" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    start = innerPadding.calculateStartPadding(layoutDirection),
                                    end = innerPadding.calculateEndPadding(layoutDirection)
                                )
                                .padding(horizontal = 20.dp)
                        ) {
                            AnimatedVisibility(visible = showDailyDigestModal) {
                                DailyDigestCard(
                                    viewModel = viewModel,
                                    onDismiss = { showDailyDigestModal = false },
                                    todos = allTodos,
                                    notes = notes,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            AnimatedVisibility(
                                visible = !isTasksFullScreen,
                                enter = expandVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(400)),
                                exit = shrinkVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(400))
                            ) {
                                Column {
                                    // Stats Carousel / Banner (Visually stunning interactive cards)
                                    StatsBanner(stats, onProgressClick = { currentScreen = "Progress" })

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Time filters tab selectors (All, Today, Upcoming, Completed)
                                    TabFilters(
                                        currentTab = currentTab,
                                        onTabSelected = { viewModel.selectedTab.value = it }
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Custom Category tagging list selector + "Add Category" quick launcher
                                    CategoryRowSelector(
                                        categories = taskCategories,
                                        selectedId = selectedCatId,
                                        onSelectId = { viewModel.selectedCategoryId.value = it },
                                        onAddCategoryClick = { showAddCategoryDialog = "task" },
                                        onDeleteCategory = { viewModel.deleteCategory(it) }
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Priority filtering strip
                                    PriorityFilterStrip(
                                        selectedPriorityId = selectedPriorityId,
                                        onPrioritySelected = { 
                                            viewModel.selectedPriorityId.value = it
                                            if (it == null) {
                                                coroutineScope.launch { 
                                                    kotlinx.coroutines.delay(100)
                                                    if (listState.layoutInfo.totalItemsCount > 0) {
                                                        listState.animateScrollToItem(0)
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(22.dp))
                                }
                            }

                            // Todo Items display
                            if (todos.isEmpty()) {
                                EmptyStateView(
                                    hasFilters = selectedCatId != null || selectedPriorityId != null || searchText.isNotEmpty() || currentTab != "All",
                                    onClearFilters = {
                                        viewModel.selectedCategoryId.value = null
                                        viewModel.selectedPriorityId.value = null
                                        viewModel.searchQuery.value = ""
                                        viewModel.selectedTab.value = "All"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(bottom = innerPadding.calculateBottomPadding() + 20.dp),
                                    onCreateTaskClick = { showAddTaskDialog = true }
                                )
                            } else {
                                SwipeToFullScreenContainer(
                                    isAlreadyFullScreen = isTasksFullScreen,
                                    canScrollBackward = listState.canScrollBackward,
                                    onEnterFullScreen = { isTasksFullScreen = true },
                                    indicatorTopPadding = 0.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(20.dp),
                                        contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 160.dp)
                                    ) {
                                        items(localTodos, key = { it.id }) { item ->
                                            val itemSubTasks = subTasksGrouped[item.id] ?: emptyList()
                                            val categoryOfTodo =
                                                item.categoryId?.let { categoriesMap[it] }

                                            TodoCard(
                                                todo = item,
                                                category = categoryOfTodo,
                                                subTasks = itemSubTasks,
                                                modifier = Modifier.animateItem(),
                                                isMultiSelectMode = isMultiSelectMode,
                                                isSelected = selectedTaskIds.contains(item.id),
                                                onSelect = {
                                                    if (selectedTaskIds.contains(item.id)) {
                                                        selectedTaskIds.remove(item.id)
                                                        if (selectedTaskIds.isEmpty()) {
                                                            isMultiSelectMode = false
                                                        }
                                                    } else {
                                                        selectedTaskIds.add(item.id)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!isMultiSelectMode) {
                                                        isMultiSelectMode = true
                                                        selectedTaskIds.add(item.id)
                                                    }
                                                },
                                                onToggleComplete = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    viewModel.toggleTodoCompleted(
                                                        item.id,
                                                        !item.isCompleted
                                                    )
                                                },
                                                onDelete = { todoToDelete = item },
                                                onShowDetails = {
                                                    todoToShowDetails = item
                                                },
                                                onEdit = {
                                                    todoToEdit = item
                                                    showAddTaskDialog = true
                                                },
                                                onToggleSubTask = { sub ->
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    viewModel.toggleSubTaskCompleted(
                                                        sub.id,
                                                        !sub.isCompleted
                                                    )
                                                },
                                                onAddSubTask = { text ->
                                                    viewModel.addSubTask(
                                                        item.id,
                                                        text
                                                    )
                                                },
                                                onDeleteSubTask = { sub ->
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    viewModel.deleteSubTask(sub.id)
                                                },
                                                onDeleteSessionRecord = { index ->
                                                    viewModel.deleteSessionRecord(
                                                        item.id,
                                                        index
                                                    )
                                                },
                                                onSmartSuggest = { onComplete ->
                                                    viewModel.generateSubTasks(
                                                        title = item.title,
                                                        description = item.description,
                                                        onResult = { subTasks ->
                                                            subTasks.forEach {
                                                                viewModel.addSubTask(
                                                                    item.id,
                                                                    it
                                                                )
                                                            }
                                                            onComplete()
                                                        },
                                                        onError = { error ->
                                                            Toast.makeText(
                                                                context,
                                                                error,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            onComplete()
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } // End of Tasks Column
                    }

                    "Focus" -> {
                        FocusScreen(
                            viewModel = viewModel,
                            innerPadding = innerPadding
                        )
                    }

                    "Progress" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            ProgressScreen(
                                viewModel = viewModel,
                                todos = allTodos,
                                categories = taskCategories,
                                onBackClick = {
                                    currentScreen =
                                        if (previousScreen != "Progress") previousScreen else "Tasks"
                                }
                            )
                        }
                    }

                    "Settings" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    start = innerPadding.calculateStartPadding(layoutDirection),
                                    end = innerPadding.calculateEndPadding(layoutDirection)
                                )
                        ) {
                            SettingsScreen(
                                viewModel = viewModel,
                                bottomPadding = innerPadding.calculateBottomPadding() + 130.dp,
                                isThemeAnimating = LocalThemeRevealState.current.isAnimating
                            )
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    start = innerPadding.calculateStartPadding(layoutDirection),
                                    end = innerPadding.calculateEndPadding(layoutDirection)
                                )
                                .padding(horizontal = 12.dp)
                        ) {
                            AnimatedVisibility(visible = showDailyDigestModal) {
                                DailyDigestCard(
                                    viewModel = viewModel,
                                    onDismiss = { showDailyDigestModal = false },
                                    todos = allTodos,
                                    notes = notes,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            }
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .weight(1f, fill = false)) {
                                NotesScreen(
                                    viewModel = viewModel,
                                    listState = notesListState,
                                    isFullScreen = isNotesFullScreen,
                                    onEnterFullScreen = { isNotesFullScreen = true },
                                    bottomPadding = innerPadding.calculateBottomPadding() + 160.dp,
                                    isInMultiSelectMode = isNotesMultiSelectMode,
                                    selectedNoteIds = selectedNoteIds,
                                    onToggleSelectNote = { note ->
                                        if (selectedNoteIds.contains(note.id)) {
                                            selectedNoteIds.remove(note.id)
                                            if (selectedNoteIds.isEmpty()) {
                                                isNotesMultiSelectMode = false
                                            }
                                        } else {
                                            selectedNoteIds.add(note.id)
                                        }
                                    },
                                    onStartMultiSelect = { note ->
                                        isNotesMultiSelectMode = true
                                        selectedNoteIds.add(note.id)
                                    },
                                    onNoteClick = { note ->
                                        if (isNotesMultiSelectMode) {
                                            if (selectedNoteIds.contains(note.id)) {
                                                selectedNoteIds.remove(note.id)
                                                if (selectedNoteIds.isEmpty()) {
                                                    isNotesMultiSelectMode = false
                                                }
                                            } else {
                                                selectedNoteIds.add(note.id)
                                            }
                                        } else {
                                            noteToShowDetails = note
                                        }
                                    },
                                    onDeleteNote = {
                                        noteToDelete = it
                                    },
                                    onAddNoteClick = {
                                        noteToEdit = null
                                        showAddNoteDialog = true
                                    },
                                    onAddCategoryClick = {
                                        showAddCategoryDialog = "note"
                                    }
                                )
                            }
                        }
                    }
                }
                }
            } // End of AnimatedContent
        }

        if (todoToShowDetails != null) {
            TaskDetailsDialog(
                todo = todoToShowDetails!!,
                category = todoToShowDetails!!.categoryId?.let { categoriesMap[it] },
                subTasks = subTasksGrouped[todoToShowDetails!!.id] ?: emptyList(),
                onDismiss = { todoToShowDetails = null },
                onEdit = {
                    todoToEdit = todoToShowDetails
                    todoToShowDetails = null
                    showAddTaskDialog = true
                },
                onDelete = {
                    todoToDelete = todoToShowDetails
                    todoToShowDetails = null
                },
                onToggleSubTask = { sub -> viewModel.toggleSubTaskCompleted(sub.id, !sub.isCompleted) }
            )
        }

        if (noteToShowDetails != null) {
            NoteDetailsDialog(
                note = noteToShowDetails!!,
                category = categoriesMap[noteToShowDetails!!.categoryId],
                onDismiss = { noteToShowDetails = null },
                onEdit = {
                    noteToEdit = noteToShowDetails
                    noteToShowDetails = null
                    showAddNoteDialog = true
                },
                onDelete = {
                    noteToDelete = noteToShowDetails
                    noteToShowDetails = null
                }
            )
        }

        // Task Creation Screen
        AnimatedVisibility(
            visible = showAddTaskDialog,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = androidx.compose.ui.Modifier.zIndex(100f)
        ) {
            BackHandler {
                showAddTaskDialog = false
                todoToEdit = null
            }
            AddTaskScreen(
                todoToEdit = todoToEdit,
                categories = taskCategories,
                viewModel = viewModel,
                onDismiss = {
                    showAddTaskDialog = false
                    todoToEdit = null
                },
                onConfirm = { title, desc, dueDate, priority, catId, hasReminder, reminderTime, isDaily, hasTime, isReminderRepeating, associatedPackageName, trackingMode ->
                    viewModel.insertTodo(
                        TodoItem(
                            id = todoToEdit?.id ?: 0,
                            title = title,
                            description = desc,
                            dueDate = dueDate,
                            priority = priority,
                            categoryId = catId,
                            isCompleted = todoToEdit?.isCompleted ?: false,
                            hasReminder = hasReminder,
                            reminderTime = reminderTime,
                            isDaily = isDaily,
                            hasTime = hasTime,
                            position = todoToEdit?.position ?: 0,
                            isReminderRepeating = isReminderRepeating,
                            associatedPackageName = associatedPackageName,
                            trackingMode = trackingMode
                        )
                    )
                    showAddTaskDialog = false
                    todoToEdit = null
                }
            )
        }

        // Category Creation Dialog
        if (showAddCategoryDialog != null) {
            AddCategoryDialog(
                onDismiss = { showAddCategoryDialog = null },
                onConfirm = { name, color, icon ->
                    viewModel.addCategory(name, color, icon, type = showAddCategoryDialog!!)
                    showAddCategoryDialog = null
                }
            )
        }

        AnimatedVisibility(
            visible = showAddNoteDialog,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = androidx.compose.ui.Modifier.zIndex(100f)
        ) {
            BackHandler {
                showAddNoteDialog = false
                noteToEdit = null
            }
            AddNoteScreen(
                noteToEdit = noteToEdit,
                categories = noteCategories,
                onDismiss = {
                    showAddNoteDialog = false
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
                    showAddNoteDialog = false
                    noteToEdit = null
                }
            )
        }

        if (showBulkNoteColorDialog) {
            AppAlertDialog(
                onDismissRequest = { showBulkNoteColorDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.assign_color_theme),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.assign_color_theme_desc,
                                selectedNoteIds.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val notesPalette = getNotesPaletteList()
                            notesPalette.forEachIndexed { index, palette ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.updateNotesColorInBulk(
                                                selectedNoteIds.toList(),
                                                index
                                            )
                                            showBulkNoteColorDialog = false
                                            isNotesMultiSelectMode = false
                                            selectedNoteIds.clear()
                                        }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        palette.primaryColor,
                                                        palette.secondaryColor
                                                    )
                                                )
                                            )
                                            .border(
                                                BorderStroke(
                                                    1.5.dp,
                                                    Color.White.copy(alpha = 0.2f)
                                                ), CircleShape
                                            )
                                    )
                                    Text(
                                        text = palette.label.split(" ").firstOrNull() ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { showBulkNoteColorDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                containerColor = SlateDark,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }

        // Delete Task Confirmation Dialog
        if (todoToDelete != null) {
            val todo = todoToDelete!!
            AppDialog(
                onDismissRequest = { todoToDelete = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ObsidianBlack)
                        .border(
                            BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.3f)),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Alert Icon Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(PriorityHigh.copy(alpha = 0.12f), CircleShape)
                                .border(
                                    BorderStroke(2.dp, PriorityHigh.copy(alpha = 0.4f)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = PriorityHigh,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Titles
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.delete_task_title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.delete_task_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Task Context Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDark, RoundedCornerShape(16.dp))
                                .border(
                                    BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f)),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Priority Color Stripe
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(32.dp)
                                        .background(
                                            color = when (todo.priority) {
                                                2 -> PriorityHigh
                                                1 -> PriorityMedium
                                                else -> PriorityLow
                                            },
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = todo.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (todo.description.isNotEmpty()) {
                                        Text(
                                            text = todo.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            Button(
                                onClick = { todoToDelete = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            // Delete Button
                            Button(
                                onClick = {
                                    todoToDelete?.let { deletedTodo ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val deletedSubTasks =
                                            subTasksGrouped[deletedTodo.id] ?: emptyList()
                                        viewModel.deleteTodo(deletedTodo)

                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.task_deleted),
                                                actionLabel = context.getString(R.string.undo),
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.undoDeleteTodo(
                                                    deletedTodo,
                                                    deletedSubTasks
                                                )
                                            }
                                        }
                                    }
                                    todoToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PriorityHigh,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false,
                                        ambientColor = PriorityHigh,
                                        spotColor = PriorityHigh
                                    )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_delete),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Note Confirmation Dialog
        if (noteToDelete != null) {
            val note = noteToDelete!!
            AppDialog(
                onDismissRequest = { noteToDelete = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(ObsidianBlack)
                        .border(
                            BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.3f)),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Alert Icon Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(PriorityHigh.copy(alpha = 0.12f), CircleShape)
                                .border(
                                    BorderStroke(2.dp, PriorityHigh.copy(alpha = 0.4f)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = PriorityHigh,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Titles
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.delete_note_title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.delete_note_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        // Note Context Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDark, RoundedCornerShape(16.dp))
                                .border(
                                    BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f)),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = note.title.ifEmpty { if (getSystemLocale().language == "fa") "بدون عنوان" else "Untitled" },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (note.content.isNotEmpty()) {
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            Button(
                                onClick = { noteToDelete = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.cancel),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            // Delete Button
                            Button(
                                onClick = {
                                    noteToDelete?.let { deletedNote ->
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.deleteNote(deletedNote.id)

                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(R.string.note_deleted),
                                                actionLabel = context.getString(R.string.undo),
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                viewModel.insertNote(deletedNote)
                                            }
                                        }
                                    }
                                    noteToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PriorityHigh,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false,
                                        ambientColor = PriorityHigh,
                                        spotColor = PriorityHigh
                                    )
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_delete),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun StatsBanner(stats: TodoStats, onProgressClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // High priority pending Counter Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SlateDark, RoundedCornerShape(20.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Icon(
                    imageVector = Icons.Default.PriorityHigh,
                    contentDescription = "High priority count icon",
                    tint = PriorityHigh,
                    modifier = Modifier.size(20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(R.string.urgent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${stats.highPriorityPending}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
        }

        // Today Due count
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SlateDark, RoundedCornerShape(20.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = stringResource(R.string.today),
                    tint = NeonPurple,
                    modifier = Modifier.size(20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(R.string.today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${stats.dueToday}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }
        }

        // Complete Ratio Ring with premium layout background gradient
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x388B5CF6),
                            Color(0x386366F1)
                        ) // Beautiful violet/indigo frosted glass gradient
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable { onProgressClick() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Circle progress visual
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(46.dp)
                ) {
                    val ratio = if (stats.total > 0) stats.completed.toFloat() / stats.total else 0f
                    Canvas(modifier = Modifier.size(46.dp)) {
                        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        drawCircle(
                            color = TextPrimary.copy(alpha = 0.4f),
                            radius = size.minDimension / 2 - stroke.width / 2,
                            style = stroke
                        )
                        drawArc(
                            color = NeonPurple,
                            startAngle = -90f,
                            sweepAngle = ratio * 360f,
                            useCenter = false,
                            topLeft = Offset(stroke.width / 2, stroke.width / 2),
                            size = Size(size.width - stroke.width, size.height - stroke.width),
                            style = stroke
                        )
                    }
                    Text(
                        text = "${(ratio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.progress),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.tasks_done, stats.completed, stats.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun TabFilters(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf("All", "Today", "Upcoming", "Completed", "Undone")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(tabs) { tabName ->
            val isSelected = currentTab == tabName

            // Animations for selection state
            val backgroundColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else SlateDark.copy(alpha = 0.5f)
            val borderColor = if (isSelected) NeonCyan else BorderColor.copy(alpha = 0.5f)
            val iconColor = if (isSelected) NeonCyan else TextSecondary.copy(alpha = 0.7f)
            val textColor = if (isSelected) TextPrimary else TextSecondary
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "tab_scale"
            )

            val icon = when (tabName) {
                "All" -> Icons.AutoMirrored.Filled.List
                "Today" -> Icons.Default.Today
                "Upcoming" -> Icons.Default.DateRange
                "Completed" -> Icons.Default.CheckCircle
                "Undone" -> Icons.Default.RadioButtonUnchecked
                else -> Icons.AutoMirrored.Filled.List
            }

            Surface(
                modifier = Modifier
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(RoundedCornerShape(100.dp))
                    .clickable { onTabSelected(tabName) }
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .testTag("tab_filter_$tabName"),
                color = backgroundColor,
                shape = RoundedCornerShape(100.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tabName,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )

                    val displayName = when (tabName) {
                        "All" -> stringResource(R.string.tab_all)
                        "Today" -> stringResource(R.string.tab_today)
                        "Upcoming" -> stringResource(R.string.tab_upcoming)
                        "Completed" -> stringResource(R.string.tab_completed)
                        "Undone" -> stringResource(R.string.tab_undone)
                        else -> tabName
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryRowSelector(
    categories: List<Category>,
    selectedId: Int?,
    onSelectId: (Int?) -> Unit,
    onAddCategoryClick: () -> Unit,
    onDeleteCategory: ((Category) -> Unit)? = null
) {
    var categoryPendingDelete by remember { mutableStateOf<Category?>(null) }

    // Confirm-delete dialog
    if (categoryPendingDelete != null) {
        val cat = categoryPendingDelete!!
        AppAlertDialog(
            onDismissRequest = { categoryPendingDelete = null },
            containerColor = SlateDark,
            title = {
                Text(
                    stringResource(R.string.delete_tag_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.delete_tag_confirm, cat.name),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory?.invoke(cat)
                    if (selectedId == cat.id) onSelectId(null)
                    categoryPendingDelete = null
                }) {
                    Text(stringResource(R.string.delete_button), color = PriorityHigh)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryPendingDelete = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }

    Column {
        Text(
            text = stringResource(R.string.categories),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // "All" pill button
            item {
                FilterChip(
                    selected = selectedId == null,
                    onClick = { onSelectId(null) },
                    label = { Text(stringResource(R.string.all_tasks)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SlateLight,
                        selectedLabelColor = NeonCyan,
                        containerColor = SlateDark,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selectedId == null) NeonCyan else BorderColor)
                )
            }

            // Real dynamic categories
            items(categories, key = { it.id }) { cat ->
                val isSelected = selectedId == cat.id
                val colorHex = remember(cat.colorHex) { safeParseColor(cat.colorHex) }

                InputChip(
                    selected = isSelected,
                    onClick = { onSelectId(cat.id) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(cat.iconName),
                                contentDescription = "",
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else colorHex
                            )
                            val catDisplayName = when (cat.name) {
                                "Work" -> stringResource(R.string.category_work)
                                "Personal" -> stringResource(R.string.category_personal)
                                "Shopping" -> stringResource(R.string.category_shopping)
                                "Health" -> stringResource(R.string.category_health)
                                "Study" -> stringResource(R.string.category_study)
                                "Finance" -> stringResource(R.string.category_finance)
                                else -> cat.name
                            }
                            Text(catDisplayName)
                        }
                    },
                    trailingIcon = if (onDeleteCategory != null) {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.delete_tag_title),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { categoryPendingDelete = cat },
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextSecondary
                            )
                        }
                    } else null,
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = colorHex,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = SlateDark,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (isSelected) colorHex else BorderColor)
                )
            }

            // Quick add category pill button
            item {
                FilterChip(
                    selected = false,
                    onClick = onAddCategoryClick,
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(stringResource(R.string.new_tag))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SlateDark,
                        labelColor = NeonPurple
                    ),
                    border = BorderStroke(1.dp, BorderColor)
                )
            }
        }
    }
}

@Composable
fun PriorityFilterStrip(
    selectedPriorityId: Int?,
    onPrioritySelected: (Int?) -> Unit
) {
    val priorities = listOf(
        Triple(0, "Low", PriorityLow),
        Triple(1, "Medium", PriorityMedium),
        Triple(2, "High", PriorityHigh)
    )

    Column {
        Text(
            text = stringResource(R.string.priority_level),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            priorities.forEach { (pId, name, color) ->
                val isSelected = selectedPriorityId == pId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) color.copy(alpha = 0.15f) else SlateDark)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) color else BorderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (isSelected) onPrioritySelected(null) else onPrioritySelected(pId)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                        val displayName = when (name) {
                            "Low" -> stringResource(R.string.priority_low)
                            "Medium" -> stringResource(R.string.priority_medium)
                            "High" -> stringResource(R.string.priority_high)
                            else -> name
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

data class ConfettiParticle(
    val id: Int,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val maxAlpha: Float,
    val rotation: Float,
    val rotationSpeed: Float
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoCard(
    todo: TodoItem,
    category: Category?,
    subTasks: List<SubTask>,
    modifier: Modifier = Modifier,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit,
    onEdit: () -> Unit,
    onToggleSubTask: (SubTask) -> Unit,
    onAddSubTask: (String) -> Unit,
    onDeleteSubTask: (SubTask) -> Unit,
    onDeleteSessionRecord: (Int) -> Unit,
    onSmartSuggest: (onComplete: () -> Unit) -> Unit = {}
) {

    val highLabel = stringResource(R.string.priority_high_label)
    val mediumLabel = stringResource(R.string.priority_medium_label)
    val lowLabel = stringResource(R.string.priority_low_label)

    // Resolve beautiful task palette based on priority to match NoteCard pattern
    val taskPalette = remember(todo.priority, highLabel, mediumLabel, lowLabel) {
        when (todo.priority) {
            2 -> NotesCardColorPalette(
                label = highLabel,
                primaryColor = PriorityHigh,
                secondaryColor = Color(0xFFF43F5E),
                glowColor = PriorityHigh.copy(alpha = 0.18f)
            )

            1 -> NotesCardColorPalette(
                label = mediumLabel,
                primaryColor = PriorityMedium,
                secondaryColor = Color(0xFFD97706),
                glowColor = PriorityMedium.copy(alpha = 0.18f)
            )

            else -> NotesCardColorPalette(
                label = lowLabel,
                primaryColor = PriorityLow,
                secondaryColor = Color(0xFF059669),
                glowColor = PriorityLow.copy(alpha = 0.18f)
            )
        }
    }

    val isNearingDeadline = remember(todo.dueDate, todo.isCompleted) {
        val now = System.currentTimeMillis()
        todo.dueDate != null && !todo.isCompleted && (todo.dueDate - now) in 0L..(24 * 60 * 60 * 1000L)
    }

    val borderPulseAlpha = if (isNearingDeadline) {
        val infiniteTransition = rememberInfiniteTransition(label = "nearing_deadline_pulse")
        val pulseAnim by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        pulseAnim
    } else {
        0.25f
    }

    var isExpanded by remember { mutableStateOf(false) }
    var isSessionsExpanded by remember { mutableStateOf(false) }
    val completedCount = subTasks.count { it.isCompleted }
    val totalCount = subTasks.size

    val dbRecords = remember(todo.recordedTimes) {
        val now = java.util.Calendar.getInstance()
        now.set(java.util.Calendar.HOUR_OF_DAY, 0)
        now.set(java.util.Calendar.MINUTE, 0)
        now.set(java.util.Calendar.SECOND, 0)
        now.set(java.util.Calendar.MILLISECOND, 0)
        val startOfToday = now.timeInMillis
        RecordedTimeItem.parseList(todo.recordedTimes).filter { it.timestamp >= startOfToday }
    }

    val todayPomodoroCycles = remember(dbRecords) {
        dbRecords.filter { it.isPomodoro }.sumOf { it.value.toInt() }
    }
    val todayElapsedTime = remember(dbRecords) {
        dbRecords.filter { !it.isPomodoro }.sumOf { it.value }
    }

    val interactionSource =
        remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_scale"
    )

    var showConfetti by remember { mutableStateOf(false) }
    val confettiProgress = remember { Animatable(0f) }

    val checkScale by animateFloatAsState(
        targetValue = if (todo.isCompleted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkmark_scale"
    )
    val checkboxBgColor = if (todo.isCompleted) NeonCyan.copy(alpha = 0.2f) else Color.Transparent
    val checkboxBorderColor = if (todo.isCompleted) NeonCyan else TextSecondary

    var lastCompletedState by remember { mutableStateOf(todo.isCompleted) }
    LaunchedEffect(todo.isCompleted) {
        if (todo.isCompleted && !lastCompletedState) {
            showConfetti = true
            confettiProgress.snapTo(0f)
            confettiProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = LinearOutSlowInEasing)
            )
            showConfetti = false
        }
        lastCompletedState = todo.isCompleted
    }

    val particles = remember(todo.isCompleted) {
        if (todo.isCompleted) {
            val colors = listOf(
                NeonCyan,
                NeonPurple,
                Color(0xFFFF4081),
                Color(0xFFFFD700),
                Color(0xFF00E676)
            )
            List(12) {
                val angle = Random.nextFloat() * 2 * Math.PI
                val speed = Random.nextFloat() * 120f + 60f
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()
                ConfettiParticle(
                    id = it,
                    vx = cosA * speed,
                    vy = sinA * speed,
                    color = colors.random(),
                    size = Random.nextFloat() * 8f + 6f,
                    maxAlpha = Random.nextFloat() * 0.4f + 0.6f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = Random.nextFloat() * 240f - 120f
                )
            }
        } else {
            emptyList()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 11.dp else if (isNearingDeadline) 7.dp else 5.dp,
                shape = RoundedCornerShape(22.dp),
                clip = false,
                ambientColor = if (isSelected) NeonCyan else if (isNearingDeadline) taskPalette.primaryColor else Color.Black,
                spotColor = if (isSelected) NeonCyan else if (isNearingDeadline) taskPalette.primaryColor else Color.Black
            )
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    if (isMultiSelectMode) {
                        onSelect()
                    } else {
                        onShowDetails()
                    }
                },
                onLongClick = onLongClick
            )
            .testTag("task_item_${todo.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else if (isNearingDeadline) 1.5.dp else 1.2.dp,
            brush = if (isSelected) {
                Brush.linearGradient(listOf(NeonCyan, NeonBlue))
            } else if (isNearingDeadline) {
                Brush.linearGradient(
                    listOf(
                        PriorityMedium.copy(alpha = borderPulseAlpha),
                        BorderColor.copy(alpha = 0.2f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        taskPalette.primaryColor.copy(alpha = 0.6f),
                        BorderColor.copy(alpha = 0.2f)
                    )
                )
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) NeonCyan.copy(alpha = 0.12f) else Color.Transparent
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(taskPalette.glowColor, Color.Transparent),
                        radius = 240f
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Elegant vertical accent bar on left edge (similar to NoteCard)
                if (!todo.isCompleted) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 14.dp)
                            .width(5.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        taskPalette.primaryColor,
                                        taskPalette.secondaryColor
                                    )
                                )
                            )
                    )
                }
                // Main content container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            start = if (!todo.isCompleted) 10.dp else 14.dp,
                            top = 14.dp,
                            end = 14.dp,
                            bottom = 14.dp
                        )
                ) {
                    val editInteractionSource = remember { MutableInteractionSource() }
                    val editPressed by editInteractionSource.collectIsPressedAsState()
                    val editScale by animateFloatAsState(
                        targetValue = if (editPressed) 0.86f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "edit_press_scale"
                    )

                    val deleteInteractionSource = remember { MutableInteractionSource() }
                    val deletePressed by deleteInteractionSource.collectIsPressedAsState()
                    val deleteScale by animateFloatAsState(
                        targetValue = if (deletePressed) 0.86f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "delete_press_scale"
                    )

                    val expandInteractionSource = remember { MutableInteractionSource() }
                    val expandPressed by expandInteractionSource.collectIsPressedAsState()
                    val expandScale by animateFloatAsState(
                        targetValue = if (expandPressed) 0.86f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "expand_press_scale"
                    )

                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "expand_rotation"
                    )

                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

                    // Content Row (Title, Checkbox, etc.)
                    // 52.dp space on the absolute left ensures exactly 12.dp padding from the 40.dp actions capsule
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                                    // Radio Checkbox with beautiful states and confetti animation
                                    Box(
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(checkboxBgColor)
                                                .border(
                                                    width = 2.dp,
                                                    color = checkboxBorderColor,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    if (isMultiSelectMode) {
                                                        onSelect()
                                                    } else {
                                                        onToggleComplete()
                                                    }
                                                }
                                                .testTag("todo_checkbox_${todo.id}")
                                        ) {
                                            if (checkScale > 0f) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Completed mark",
                                                    tint = NeonCyan,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .scale(checkScale)
                                                )
                                            }
                                        }

                                        if (showConfetti && particles.isNotEmpty()) {
                                            Canvas(
                                                modifier = Modifier
                                                    .size(24.dp)
                                            ) {
                                                val drawScope = this
                                                val center = Offset(
                                                    drawScope.size.width / 2f,
                                                    drawScope.size.height / 2f
                                                )
                                                val p = confettiProgress.value
                                                val fade = 1f - p

                                                particles.forEach { particle ->
                                                    val x = center.x + particle.vx * p
                                                    val y = center.y + particle.vy * p + 45f * p * p

                                                    val alpha =
                                                        (fade * particle.maxAlpha).coerceIn(0f, 1f)
                                                    val currentRotation =
                                                        particle.rotation + particle.rotationSpeed * p

                                                    drawScope.rotate(
                                                        currentRotation,
                                                        pivot = Offset(x, y)
                                                    ) {
                                                        when (particle.id % 3) {
                                                            0 -> { // Circle
                                                                drawCircle(
                                                                    color = particle.color,
                                                                    radius = particle.size * (1f - p * 0.4f),
                                                                    center = Offset(x, y),
                                                                    alpha = alpha
                                                                )
                                                            }

                                                            1 -> { // Square
                                                                val s =
                                                                    particle.size * (1f - p * 0.4f)
                                                                drawRect(
                                                                    color = particle.color,
                                                                    topLeft = Offset(
                                                                        x - s / 2,
                                                                        y - s / 2
                                                                    ),
                                                                    size = Size(
                                                                        s,
                                                                        s
                                                                    ),
                                                                    alpha = alpha
                                                                )
                                                            }

                                                            else -> { // Spark / Line
                                                                val s =
                                                                    particle.size * (1f - p * 0.4f)
                                                                drawLine(
                                                                    color = particle.color,
                                                                    start = Offset(x - s, y),
                                                                    end = Offset(x + s, y),
                                                                    strokeWidth = 2.dp.toPx(),
                                                                    alpha = alpha
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Title + description (fillMaxWidth)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = todo.title,
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null
                                            ),
                                            color = if (todo.isCompleted) TextSecondary else TextPrimary
                                        )

                                        if (todo.description.isNotBlank()) {
                                            Text(
                                                text = todo.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 2.dp)
                                            )
                                        }

                                        // Metadata row: Cateogory label, due date label, reminders alarm
                                        @OptIn(ExperimentalLayoutApi::class)
                                        androidx.compose.foundation.layout.FlowRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp, bottom = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Category pill inside task card
                                            if (category != null) {
                                                val catColor = remember(category.colorHex) {
                                                    safeParseColor(category.colorHex)
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(catColor.copy(alpha = 0.12f))
                                                        .padding(
                                                            horizontal = 8.dp,
                                                            vertical = 4.dp
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = getCategoryIcon(category.iconName),
                                                        contentDescription = "",
                                                        tint = catColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    val catDisplayName = when (category.name) {
                                                        "Work" -> stringResource(R.string.category_work)
                                                        "Personal" -> stringResource(R.string.category_personal)
                                                        "Shopping" -> stringResource(R.string.category_shopping)
                                                        "Health" -> stringResource(R.string.category_health)
                                                        "Study" -> stringResource(R.string.category_study)
                                                        "Finance" -> stringResource(R.string.category_finance)
                                                        else -> category.name
                                                    }
                                                    Text(
                                                        text = catDisplayName,
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            fontWeight = FontWeight.Bold
                                                        ),
                                                        color = catColor
                                                    )
                                                }
                                            }
                                            // Due date alert
                                            if (todo.dueDate != null || todo.isDaily) {
                                                val dueStr = remember(todo.dueDate, todo.hasTime, todo.isDaily) {
                                                    val isPersian = getSystemLocale().language == "fa"
                                                    if (todo.isDaily) {
                                                        if (isPersian) "امروز" else "Today"
                                                    } else if (todo.dueDate != null) {
                                                        if (isPersian) {
                                                            val cal = Calendar.getInstance()
                                                                .apply { timeInMillis = todo.dueDate }
                                                            val jd = JalaliCalendarHelper.gregorianToJalali(
                                                                cal.get(Calendar.YEAR),
                                                                cal.get(Calendar.MONTH) + 1,
                                                                cal.get(Calendar.DAY_OF_MONTH)
                                                            )
                                                            val monthName = JalaliCalendarHelper.JalaliDate.getMonthName(jd.month)
                                                            val dayStr = JalaliCalendarHelper.toPersianDigits(jd.day.toString())
                                                            if (todo.hasTime) {
                                                                val timeSdf = SimpleDateFormat("h:mm a", getSystemLocale())
                                                                val timeStr = JalaliCalendarHelper.toPersianDigits(timeSdf.format(Date(todo.dueDate)))
                                                                "$dayStr $monthName، $timeStr"
                                                            } else {
                                                                "$dayStr $monthName"
                                                            }
                                                        } else {
                                                            val formatStr = if (todo.hasTime) "MMM d • h:mm a" else "MMM d, yyyy"
                                                            val sdf = SimpleDateFormat(formatStr, getSystemLocale())
                                                            sdf.format(Date(todo.dueDate))
                                                        }
                                                    } else ""
                                                }
                                                val dueColor = if (todo.dueDate != null && todo.dueDate < System.currentTimeMillis() && !todo.isCompleted) {
                                                    PriorityHigh
                                                } else if (isNearingDeadline) {
                                                    PriorityMedium
                                                } else {
                                                    NeonCyan
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(dueColor.copy(alpha = 0.12f))
                                                        .border(0.5.dp, dueColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarToday,
                                                        contentDescription = "Due date",
                                                        tint = dueColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = dueStr,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = dueColor
                                                    )
                                                }

                                                // Beautiful warning badge for nearing deadlines
                                                if (isNearingDeadline) {
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(PriorityMedium.copy(alpha = 0.12f))
                                                            .border(
                                                                0.5.dp,
                                                                PriorityMedium.copy(alpha = 0.3f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                            ),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            4.dp
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = "Nearing deadline alert",
                                                            tint = PriorityMedium,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = stringResource(R.string.due_soon),
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = PriorityMedium
                                                        )
                                                    }
                                                }
                                            }

                                            // Alarm indicator
                                            if (todo.hasReminder && !todo.isCompleted) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(NeonPurple.copy(alpha = 0.12f))
                                                        .border(0.5.dp, NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Alarm,
                                                        contentDescription = "Reminder configured",
                                                        tint = NeonPurple,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            // Daily indicator
                                            if (todo.isDaily) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(NeonCyan.copy(alpha = 0.12f))
                                                        .border(0.5.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Daily task",
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Redesigned Focus Stats (Assigned Times)
                                        if (todayPomodoroCycles > 0 || todayElapsedTime > 0L) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (todayPomodoroCycles > 0) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            6.dp
                                                        ),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(NeonPurple.copy(alpha = 0.12f))
                                                            .border(
                                                                1.dp,
                                                                NeonPurple.copy(alpha = 0.3f),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                            )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Timer,
                                                            contentDescription = null,
                                                            tint = NeonPurple,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "${todayPomodoroCycles} cycle${if (todayPomodoroCycles > 1) "s" else ""}",
                                                            color = TextPrimary,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                if (todayElapsedTime > 0L) {
                                                    val totalMs = todayElapsedTime
                                                    val hrs = totalMs / 3600000
                                                    val mins = (totalMs % 3600000) / 60000
                                                    val secs = (totalMs % 60000) / 1000
                                                    val timeStr = if (hrs > 0) {
                                                        "${hrs}h ${mins}m"
                                                    } else if (mins > 0) {
                                                        "${mins}m"
                                                    } else {
                                                        "${secs}s"
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            6.dp
                                                        ),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(NeonCyan.copy(alpha = 0.12f))
                                                            .border(
                                                                1.dp,
                                                                NeonCyan.copy(alpha = 0.3f),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                            )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.HourglassBottom,
                                                            contentDescription = null,
                                                            tint = NeonCyan,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = timeStr,
                                                            color = TextPrimary,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                SlateLight.copy(alpha = 0.25f),
                                                SlateDark.copy(alpha = 0.75f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            ) {
                                // 1. Tactile Edit Button with Cyan Halo
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = editScale
                                            scaleY = editScale
                                        }
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    NeonCyan.copy(alpha = 0.14f),
                                                    Color.Transparent
                                                ),
                                                radius = 60f
                                            )
                                        )
                                        .clickable(
                                            interactionSource = editInteractionSource,
                                            indication = null,
                                            onClick = onEdit
                                        )
                                        .testTag("edit_todo_button_${todo.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(SlateLight.copy(alpha = 0.45f))
                                            .border(
                                                BorderStroke(
                                                    1.dp,
                                                    NeonCyan.copy(alpha = 0.35f)
                                                ), CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit_task),
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // 2. Tactile Delete Button with Crimson Glow
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = deleteScale
                                            scaleY = deleteScale
                                        }
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    PriorityHigh.copy(alpha = 0.18f),
                                                    Color.Transparent
                                                ),
                                                radius = 60f
                                            )
                                        )
                                        .clickable(
                                            interactionSource = deleteInteractionSource,
                                            indication = null,
                                            onClick = onDelete
                                        )
                                        .testTag("delete_todo_button_${todo.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(SlateLight.copy(alpha = 0.45f))
                                            .border(
                                                BorderStroke(
                                                    1.dp,
                                                    PriorityHigh.copy(alpha = 0.35f)
                                                ), CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete task",
                                            tint = PriorityHigh,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // 3. Tactile Smooth Rotating Expand/Collapse Button
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            scaleX = expandScale
                                            scaleY = expandScale
                                        }
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    taskPalette.primaryColor.copy(alpha = 0.14f),
                                                    Color.Transparent
                                                ),
                                                radius = 60f
                                            )
                                        )
                                        .clickable(
                                            interactionSource = expandInteractionSource,
                                            indication = null,
                                            onClick = { isExpanded = !isExpanded }
                                        )
                                        .testTag("expand_subtasks_button_${todo.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(SlateLight.copy(alpha = 0.45f))
                                            .border(
                                                BorderStroke(
                                                    width = 1.dp,
                                                    color = if (totalCount > 0) taskPalette.primaryColor.copy(
                                                        alpha = 0.5f
                                                    ) else BorderColor.copy(alpha = 0.35f)
                                                ),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle subtasks",
                                            tint = if (totalCount > 0) taskPalette.primaryColor else TextSecondary,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .graphicsLayer {
                                                    rotationZ = rotationAngle
                                                }
                                        )
                                    }
                                }
                            }
                    }
                }
            }

            if (dbRecords.isNotEmpty()) {
                Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    1.dp,
                                    BorderColor.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSessionsExpanded = !isSessionsExpanded }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = "Focus History",
                                    tint = if (isSessionsExpanded) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.focus_session_history),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    val sessionsText = if (dbRecords.size == 1) {
                                        stringResource(
                                            R.string.session_records_count,
                                            dbRecords.size
                                        )
                                    } else {
                                        stringResource(
                                            R.string.session_records_count_plural,
                                            dbRecords.size
                                        )
                                    }
                                    Text(
                                        text = sessionsText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = if (isSessionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isSessionsExpanded) "Collapse" else "Expand",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isSessionsExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                                ) {
                                    HorizontalDivider(
                                        color = BorderColor.copy(alpha = 0.3f),
                                        thickness = 1.dp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    val totalPomos = dbRecords.filter { it.isPomodoro }
                                        .sumOf { it.value.toInt() }
                                    val totalStopwatchMs =
                                        dbRecords.filter { !it.isPomodoro }.sumOf { it.value }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SlateDark.copy(alpha = 0.6f))
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = NeonPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(R.string.pomodoro_label),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            val cyclesText = if (totalPomos == 1) {
                                                stringResource(
                                                    R.string.pomodoro_cycles_count,
                                                    totalPomos
                                                )
                                            } else {
                                                stringResource(
                                                    R.string.pomodoro_cycles_count_plural,
                                                    totalPomos
                                                )
                                            }
                                            Text(
                                                text = cyclesText,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SlateDark.copy(alpha = 0.6f))
                                                .padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.HourglassBottom,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(R.string.stopwatch_label),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                            val totalHrs = totalStopwatchMs / 3600000
                                            val totalMins = (totalStopwatchMs % 3600000) / 60000
                                            val totalSecs = (totalStopwatchMs % 60000) / 1000
                                            val stopwatchStr = if (totalHrs > 0) {
                                                "${totalHrs}h ${totalMins}m"
                                            } else if (totalMins > 0) {
                                                "${totalMins}m"
                                            } else {
                                                "${totalSecs}s"
                                            }
                                            Text(
                                                text = stopwatchStr,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        dbRecords.forEachIndexed { idx, rec ->
                                            val hrs = rec.value / 3600000
                                            val mins = (rec.value % 3600000) / 60000
                                            val secs = (rec.value % 60000) / 1000

                                            val durationStr = if (rec.isPomodoro) {
                                                "${rec.value} cycle${if (rec.value > 1) "s" else ""}"
                                            } else {
                                                if (hrs > 0) {
                                                    "${hrs}h ${mins}m"
                                                } else if (mins > 0) {
                                                    "${mins}m"
                                                } else {
                                                    "${secs}s"
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(SlateDark.copy(alpha = 0.5f))
                                                    .border(
                                                        1.dp,
                                                        BorderColor.copy(alpha = 0.15f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(
                                                            if (rec.isPomodoro) NeonPurple.copy(
                                                                alpha = 0.15f
                                                            ) else NeonCyan.copy(alpha = 0.15f),
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (rec.isPomodoro) Icons.Default.Timer else Icons.Default.HourglassBottom,
                                                        contentDescription = null,
                                                        tint = if (rec.isPomodoro) NeonPurple else NeonCyan,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    val typeLabel =
                                                        if (rec.isPomodoro) stringResource(R.string.pomodoro_label) else stringResource(
                                                            R.string.stopwatch_label
                                                        )
                                                    Text(
                                                        text = stringResource(R.string.session_format, idx + 1, typeLabel),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val isPersianLog =
                                                        getSystemLocale().language == "fa"
                                                    val formattedDate =
                                                        remember(rec.timestamp, isPersianLog) {
                                                            try {
                                                                if (isPersianLog) {
                                                                    JalaliCalendarHelper.formatJalali(
                                                                        rec.timestamp,
                                                                        includeTime = true,
                                                                        includeYear = true
                                                                    )
                                                                } else {
                                                                    val date = Date(rec.timestamp)
                                                                    val format = SimpleDateFormat(
                                                                        "MMM dd, yyyy • h:mm a",
                                                                        getSystemLocale()
                                                                    )
                                                                    format.format(date)
                                                                }
                                                            } catch (_: Exception) {
                                                                ""
                                                            }
                                                        }
                                                    if (formattedDate.isNotEmpty()) {
                                                        Text(
                                                            text = formattedDate,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = durationStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (rec.isPomodoro) NeonPurple else NeonCyan,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )

                                                IconButton(
                                                    onClick = { onDeleteSessionRecord(idx) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Delete record",
                                                        tint = TextSecondary.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }



                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark)
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 4.dp)
                    ) {
                        HorizontalDivider(
                            color = BorderColor.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (subTasks.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_subtasks),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        } else {
                            subTasks.forEach { subTask ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onToggleSubTask(subTask) },
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (subTask.isCompleted) NeonCyan.copy(
                                                        alpha = 0.15f
                                                    ) else Color.Transparent
                                                )
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (subTask.isCompleted) NeonCyan else TextSecondary,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            if (subTask.isCompleted) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Completed mark",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = subTask.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null
                                            ),
                                            color = if (subTask.isCompleted) TextSecondary else TextPrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteSubTask(subTask) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete subtask",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var newSubtaskTitle by remember { mutableStateOf("") }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newSubtaskTitle,
                                onValueChange = { newSubtaskTitle = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp, max = 50.dp)
                                    .testTag("add_subtask_input_${todo.id}"),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.add_subtask_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SlateLight,
                                    unfocusedContainerColor = SlateLight
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )

                            Button(
                                onClick = {
                                    if (newSubtaskTitle.isNotBlank()) {
                                        onAddSubTask(newSubtaskTitle.trim())
                                        newSubtaskTitle = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.add_subtask_btn),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var isGenerating by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                if (!isGenerating) {
                                    isGenerating = true
                                    onSmartSuggest { isGenerating = false }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                            border = BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.generating_status),
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = stringResource(R.string.smart_suggest),
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.smart_suggest),
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (totalCount > 0) {
                    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
                    val remainingCount = totalCount - completedCount
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 16.dp, top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.subtasks_completed, completedCount, totalCount),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (remainingCount > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.subtasks_remaining, remainingCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "All done",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.subtasks_all_done),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NeonCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = NeonCyan,
                            trackColor = TextPrimary.copy(alpha = 0.25f)
                        )
                    }
                }
            }
        }
}


@Composable
fun EmptyStateView(
    hasFilters: Boolean,
    onClearFilters: () -> Unit,
    onCreateTaskClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")

    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_y"
    )

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Compact modern illustration
        Box(
            modifier = Modifier
                .size(100.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. Dynamic Glowing Halo Background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(
                        scaleX = scaleAnim,
                        scaleY = scaleAnim
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                NeonCyan.copy(alpha = 0.22f),
                                NeonPurple.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 2. Glassmorphic Floating Base Card
            Card(
                modifier = Modifier
                    .size(56.dp, 68.dp)
                    .graphicsLayer(
                        translationY = floatAnim * 0.5f,
                        rotationZ = floatAnim * 0.1f
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = SlateDark.copy(alpha = 1.0f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BorderColor,
                            NeonCyan.copy(alpha = 0.25f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Header line
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonCyan, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp, 3.dp)
                                .background(
                                    TextPrimary.copy(alpha = 0.35f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    // Task row 1
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    NeonPurple.copy(alpha = 0.25f),
                                    RoundedCornerShape(2.dp)
                                )
                                .border(0.5.dp, NeonPurple, RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(6.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp, 2.dp)
                                .background(
                                    TextPrimary.copy(alpha = 0.18f),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }

                    // Task row 2
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Transparent, RoundedCornerShape(2.dp))
                                .border(
                                    1.dp,
                                    TextMuted.copy(alpha = 0.7f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp, 2.dp)
                                .background(
                                    TextPrimary.copy(alpha = 0.18f),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                }
            }

            // 3. Floating Accent Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
                    .graphicsLayer(
                        translationY = -floatAnim * 0.3f,
                        rotationZ = -floatAnim * 0.15f
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (hasFilters) listOf(
                                NeonPurple,
                                NeonBlue
                            ) else listOf(NeonCyan, NeonBlue)
                        ),
                        shape = CircleShape
                    )
                    .border(1.dp, SlateLight.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasFilters) Icons.Default.FilterListOff else Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    tint = ObsidianBlack,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title text
        Text(
            text = if (hasFilters) stringResource(R.string.no_matching_title) else stringResource(
                R.string.task_list_empty
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag("empty_state_title")
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Description text
        Text(
            text = if (hasFilters) stringResource(R.string.no_matching_desc) else stringResource(
                R.string.task_list_empty_desc
            ),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .padding(horizontal = 16.dp)
                .testTag("empty_state_desc")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action CTA
        if (hasFilters) {
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlateLight,
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterListOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.reset_filters),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else if (onCreateTaskClick != null) {
            Button(
                onClick = onCreateTaskClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier
                    .widthIn(min = 200.dp)
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.create_task),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}


@Composable
fun TaskDetailsDialog(
    todo: TodoItem,
    category: Category?,
    subTasks: List<SubTask>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSubTask: (SubTask) -> Unit
) {
    val isPersian = getSystemLocale().language == "fa"
    val priorityColor = when (todo.priority) {
        2 -> PriorityHigh
        1 -> PriorityMedium
        else -> PriorityLow
    }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.task_details_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = NeonCyan
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Title
                    Text(
                        text = todo.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary
                    )

                    // Description Area (Scrollable within the main scroll)
                    if (todo.description.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 310.dp, max = 600.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SlateDark.copy(alpha = 0.5f))
                                .border(
                                    1.dp,
                                    BorderColor.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = todo.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    }

                    @Composable
                    fun ChipFlowLayout(
                        modifier: Modifier = Modifier,
                        horizontalSpacing: Dp = 10.dp,
                        verticalSpacing: Dp = 10.dp,
                        content: @Composable () -> Unit
                    ) {
                        Layout(
                            content = content,
                            modifier = modifier
                        ) { measurables, constraints ->
                            val hSpacingPx = horizontalSpacing.roundToPx()
                            val vSpacingPx = verticalSpacing.roundToPx()

                            val rows =
                                mutableListOf<List<Placeable>>()
                            val rowHeights = mutableListOf<Int>()

                            var currentRow =
                                mutableListOf<Placeable>()
                            var currentRowWidth = 0
                            var currentRowMaxHeight = 0

                            for (measurable in measurables) {
                                val placeable = measurable.measure(
                                    constraints.copy(
                                        minWidth = 0,
                                        minHeight = 0
                                    )
                                )
                                val isOverflow =
                                    currentRow.isNotEmpty() && (currentRowWidth + hSpacingPx + placeable.width > constraints.maxWidth)
                                if (isOverflow) {
                                    rows.add(currentRow)
                                    rowHeights.add(currentRowMaxHeight)
                                    currentRow = mutableListOf(placeable)
                                    currentRowWidth = placeable.width
                                    currentRowMaxHeight = placeable.height
                                } else {
                                    if (currentRow.isNotEmpty()) {
                                        currentRowWidth += hSpacingPx
                                    }
                                    currentRow.add(placeable)
                                    currentRowWidth += placeable.width
                                    currentRowMaxHeight =
                                        maxOf(currentRowMaxHeight, placeable.height)
                                }
                            }
                            if (currentRow.isNotEmpty()) {
                                rows.add(currentRow)
                                rowHeights.add(currentRowMaxHeight)
                            }

                            val totalHeight =
                                if (rowHeights.isEmpty()) 0 else rowHeights.sum() + (rowHeights.size - 1) * vSpacingPx
                            val totalWidth = constraints.maxWidth

                            layout(totalWidth, totalHeight) {
                                var y = 0
                                for (i in rows.indices) {
                                    val row = rows[i]
                                    val rowHeight = rowHeights[i]
                                    var x = 0
                                    for (placeable in row) {
                                        placeable.placeRelative(
                                            x,
                                            y + (rowHeight - placeable.height) / 2
                                        )
                                        x += placeable.width + hSpacingPx
                                    }
                                    y += rowHeight + vSpacingPx
                                }
                            }
                        }
                    }

// Metadata Badges Chips FlowRow
                    ChipFlowLayout(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalSpacing = 10.dp,
                        verticalSpacing = 10.dp
                    ) {
                        // Category (Tags)
                        if (category != null) {
                            val catColor = safeParseColor(category.colorHex)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(catColor.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        catColor.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(category.iconName),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = catColor
                                )
                            }
                        }

                        // Priority
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(priorityColor.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    priorityColor.copy(alpha = 0.3f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = priorityColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (todo.priority) {
                                    2 -> stringResource(R.string.priority_high)
                                    1 -> stringResource(R.string.priority_medium)
                                    else -> stringResource(R.string.priority_low)
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = priorityColor
                            )
                        }

                        // Due Date Chip
                        if (todo.dueDate != null || todo.isDaily) {
                            val dueStr = remember(todo.dueDate, todo.isDaily, todo.hasTime) {
                                if (todo.isDaily) {
                                    if (getSystemLocale().language == "fa") "امروز" else "Today"
                                } else if (todo.dueDate != null) {
                                    if (isPersian) {
                                        val cal = Calendar.getInstance()
                                            .apply { timeInMillis = todo.dueDate }
                                        val jd = JalaliCalendarHelper.gregorianToJalali(
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH) + 1,
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        )
                                        val monthName =
                                            JalaliCalendarHelper.JalaliDate.getMonthName(jd.month)
                                        val dayStr =
                                            JalaliCalendarHelper.toPersianDigits(jd.day.toString())
                                        if (todo.hasTime) {
                                            val timeSdf =
                                                SimpleDateFormat("h:mm a", getSystemLocale())
                                            val timeStr = JalaliCalendarHelper.toPersianDigits(
                                                timeSdf.format(Date(todo.dueDate))
                                            )
                                            "$dayStr $monthName، $timeStr"
                                        } else {
                                            "$dayStr $monthName"
                                        }
                                    } else {
                                        val formatStr =
                                            if (todo.hasTime) "MMM d • h:mm a" else "MMM d, yyyy"
                                        val sdf =
                                            SimpleDateFormat(formatStr, getSystemLocale())
                                        sdf.format(Date(todo.dueDate))
                                    }
                                } else ""
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonPurple.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        NeonPurple.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = dueStr,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = NeonPurple
                                )
                            }
                        }

                        // Repeatable Status Chip
                        if (todo.isDaily) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonCyan.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        NeonCyan.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.repeatable_status),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = NeonCyan
                                )
                            }
                        }

                        // Reminder Status Chip
                        if (todo.hasReminder || todo.reminderTime != null) {
                            val reminderText = remember(todo.reminderTime) {
                                val labelStr = if (isPersian) "یادآور" else "Reminder"
                                if (todo.reminderTime != null) {
                                    val formattedTime = if (isPersian) {
                                        val timeSdf =
                                            SimpleDateFormat("h:mm a", getSystemLocale())
                                        JalaliCalendarHelper.toPersianDigits(
                                            timeSdf.format(
                                                Date(todo.reminderTime)
                                            )
                                        )
                                    } else {
                                        SimpleDateFormat(
                                            "h:mm a",
                                            getSystemLocale()
                                        ).format(Date(todo.reminderTime))
                                    }
                                    "$labelStr: $formattedTime"
                                } else {
                                    labelStr
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PriorityMedium.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        PriorityMedium.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = PriorityMedium,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = reminderText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = PriorityMedium
                                )
                            }
                        }

                        // Repeat Reminder Daily Chip
                        if (todo.isReminderRepeating) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonPurple.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        NeonPurple.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventRepeat,
                                    contentDescription = null,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.repeat_reminder),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = NeonPurple
                                )
                            }
                        }
                    }

                    // Subtasks (Checklist)
                    if (subTasks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(R.string.subtask_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = NeonPurple,
                                fontWeight = FontWeight.Bold
                            )
                            subTasks.forEach { subTask ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SlateDark)
                                        .clickable { onToggleSubTask(subTask) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (subTask.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (subTask.isCompleted) NeonCyan else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = subTask.title,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else null
                                        ),
                                        color = if (subTask.isCompleted) TextSecondary else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Focus History (if any)
                    val dbRecords =
                        RecordedTimeItem.parseList(todo.recordedTimes)
                    if (dbRecords.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(R.string.focus_session_history),
                                style = MaterialTheme.typography.titleSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                            dbRecords.forEachIndexed { idx, rec ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SlateDark)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (rec.isPomodoro) Icons.Default.Timer else Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = if (rec.isPomodoro) NeonPurple else NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(
                                                R.string.session_label,
                                                idx + 1
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        val dateStr = if (isPersian) {
                                            JalaliCalendarHelper.formatJalali(
                                                rec.timestamp,
                                                includeTime = true,
                                                includeYear = true
                                            )
                                        } else {
                                            SimpleDateFormat(
                                                "MMM d, yyyy • h:mm a",
                                                getSystemLocale()
                                            ).format(Date(rec.timestamp))
                                        }
                                        Text(
                                            dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        border = BorderStroke(1.5.dp, PriorityHigh.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PriorityHigh),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.action_delete),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.edit_task),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun NoteDetailsDialog(
    note: NoteItem,
    category: Category?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val resolvedPalette = resolveNotePalette(note.colorIndex)
    val palette = remember(resolvedPalette) { resolvedPalette }

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianBlack)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.note_details_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = palette.primaryColor
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title
                    Text(
                        text = note.title.ifEmpty { if (getSystemLocale().language == "fa") "بدون عنوان" else "Untitled" },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary
                    )

                    if (category != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getCategoryIcon(category.iconName),
                                contentDescription = category.name,
                                tint = android.graphics.Color.parseColor(category.colorHex).let { Color(it) },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val catDisplayName = when (category.name) {
                                "Work" -> stringResource(R.string.category_work)
                                "Personal" -> stringResource(R.string.category_personal)
                                "Shopping" -> stringResource(R.string.category_shopping)
                                "Health" -> stringResource(R.string.category_health)
                                "Study" -> stringResource(R.string.category_study)
                                "Finance" -> stringResource(R.string.category_finance)
                                else -> category.name
                            }
                            Text(
                                text = catDisplayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large Note Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.primaryColor.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                palette.primaryColor.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                                color = TextPrimary
                            )
                        }
                    }

                    // Timestamp footer
                    Spacer(modifier = Modifier.height(12.dp))
                    val isPersianNote = getSystemLocale().language == "fa"
                    val dateStr = if (isPersianNote) {
                        JalaliCalendarHelper.formatJalali(
                            note.createdAt,
                            includeTime = true,
                            includeYear = true
                        )
                    } else {
                        SimpleDateFormat(
                            "MMMM d, yyyy • h:mm a",
                            getSystemLocale()
                        ).format(Date(note.createdAt))
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.End)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        border = BorderStroke(1.5.dp, PriorityHigh.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PriorityHigh),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.action_delete),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onEdit,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.edit_note),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    todoToEdit: TodoItem? = null,
    categories: List<Category>,
    viewModel: TodoViewModel,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        desc: String,
        dueDate: Long?,
        priority: Int,
        categoryId: Int?,
        hasReminder: Boolean,
        reminderTime: Long?,
        isDaily: Boolean,
        hasTime: Boolean,
        isReminderRepeating: Boolean,
        associatedPackageName: String?,
        trackingMode: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(todoToEdit?.title ?: "") }
    var desc by remember { mutableStateOf(todoToEdit?.description ?: "") }
    var priority by remember {
        mutableIntStateOf(
            todoToEdit?.priority ?: 1
        )
    } // 0=Low, 1=Med, 2=High
    var selectedCategoryId by remember { mutableStateOf(todoToEdit?.categoryId) }
    var hasReminder by remember { mutableStateOf(todoToEdit?.hasReminder ?: false) }
    var isDaily by remember { mutableStateOf(todoToEdit?.isDaily ?: true) }
    var isReminderRepeating by remember { mutableStateOf(todoToEdit?.isReminderRepeating ?: false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var associatedPackageName by remember { mutableStateOf(todoToEdit?.associatedPackageName) }
    var trackingMode by remember { mutableStateOf(todoToEdit?.trackingMode ?: "Strict") }

    val isPersian = getSystemLocale().language == "fa"
    val calendar = remember {
        Calendar.getInstance().apply {
            todoToEdit?.dueDate?.let { timeInMillis = it }
        }
    }
    var selectedDate by remember {
        mutableStateOf(
            if (todoToEdit != null) {
                todoToEdit.dueDate?.let { Date(it) }
            } else {
                Date()
            }
        )
    }
    var showJalaliDatePicker by remember { mutableStateOf(false) }
    var selectedTimeStr by remember {
        mutableStateOf(
            if (todoToEdit?.dueDate != null && todoToEdit.hasTime) {
                val sdf = SimpleDateFormat("h:mm a", getSystemLocale())
                val formatted = sdf.format(Date(todoToEdit.dueDate))
                if (isPersian) JalaliCalendarHelper.toPersianDigits(formatted) else formatted
            } else ""
        )
    }

    val context = LocalContext.current

    var isListening by remember { mutableStateOf(false) }
    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSuggestingCategory by remember { mutableStateOf(false) }
    var suggestCategoryError by remember { mutableStateOf<String?>(null) }
    var isSuggestingTaskDetails by remember { mutableStateOf(false) }
    var suggestTaskDetailsError by remember { mutableStateOf<String?>(null) }
    val speechManager = remember { SpeechRecognizerManager(context) }
    val commandParser = remember { VoiceCommandParser() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceErrorMessage = null
            speechManager.startListening(
                onResult = { text ->
                    val parsed = commandParser.parse(text)
                    title = parsed.title
                    parsed.priority?.let { priority = it }
                },
                onError = { error ->
                    voiceErrorMessage = error
                },
                onListeningStateChange = { listening ->
                    isListening = listening
                }
            )
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.voice_permission_rationale),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.stopListening()
        }
    }

    val toggleVoiceInput = {
        if (isListening) {
            speechManager.stopListening()
            isListening = false
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                voiceErrorMessage = null
                speechManager.startListening(
                    onResult = { text ->
                        val parsed = commandParser.parse(text)
                        title = parsed.title
                        parsed.priority?.let { priority = it }
                    },
                    onError = { error ->
                        voiceErrorMessage = error
                    },
                    onListeningStateChange = { listening ->
                        isListening = listening
                    }
                )
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    
    if (showTimePicker) {
        val initialHour = calendar.get(Calendar.HOUR_OF_DAY)
        val initialMinute = calendar.get(Calendar.MINUTE)
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = isPersian
        )
        
        AppAlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val sdf = java.text.SimpleDateFormat("h:mm a", getSystemLocale())
                    val formatted = sdf.format(calendar.time)
                    selectedTimeStr = if (isPersian) JalaliCalendarHelper.toPersianDigits(formatted) else formatted
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.set_time),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    if (showJalaliDatePicker) {
        JalaliDatePickerDialog(
            initialDate = selectedDate,
            onDismissResult = { showJalaliDatePicker = false },
            onDateSelectedResult = { date ->
                selectedDate = date
                calendar.time = date
                showJalaliDatePicker = false
            }
        )
    }

    val currentLocalContext = LocalContext.current
    val currentLayoutDirection = LocalLayoutDirection.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (todoToEdit != null) stringResource(R.string.edit_task) else stringResource(
                                R.string.new_flow_task
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = TextSecondary
                            )
                        }
                    }

                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = {
                            Text(
                                stringResource(R.string.task_title_label),
                                color = TextSecondary
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_task_title_input"),
                        trailingIcon = {
                            IconButton(
                                onClick = { toggleVoiceInput() },
                                modifier = Modifier.testTag("voice_add_button")
                            ) {
                                if (isListening) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = stringResource(R.string.voice_add_btn_desc),
                                        tint = PriorityHigh,
                                        modifier = Modifier
                                            .scale(micScale)
                                            .drawBehind {
                                                drawCircle(
                                                    color = PriorityHigh.copy(alpha = 0.2f * (1f - (micScale - 1f) / 0.3f)),
                                                    radius = size.minDimension * micScale * 0.6f
                                                )
                                            }
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MicNone,
                                        contentDescription = stringResource(R.string.voice_add_btn_desc),
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
                    )

                    // Voice Feedback Panel
                    AnimatedVisibility(
                        visible = isListening || voiceErrorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (voiceErrorMessage != null) PriorityHigh.copy(
                                        alpha = 0.1f
                                    ) else NeonCyan.copy(alpha = 0.1f)
                                )
                                .border(
                                    1.dp,
                                    if (voiceErrorMessage != null) PriorityHigh.copy(alpha = 0.3f) else NeonCyan.copy(
                                        alpha = 0.3f
                                    ),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isListening) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(NeonCyan, CircleShape)
                                                .scale(micScale)
                                        )
                                        Text(
                                            text = stringResource(R.string.voice_listening_status),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = NeonCyan
                                        )
                                    } else if (voiceErrorMessage != null) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = PriorityHigh,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.voice_error_prefix,
                                                voiceErrorMessage!!
                                            ),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = PriorityHigh
                                        )
                                    }
                                }

                                if (isListening) {
                                    Text(
                                        text = stringResource(R.string.voice_add_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Description Input
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = {
                            Text(
                                stringResource(R.string.notes_description_label),
                                color = TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 3,
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )

                    if (todoToEdit != null && (todoToEdit.pomodoroCycles > 0 || todoToEdit.elapsedTime > 0)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateDark)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (todoToEdit.pomodoroCycles > 0) {
                                Column {
                                    Text(
                                        stringResource(R.string.pomodoro_cycles),
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        "${todoToEdit.pomodoroCycles}",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            if (todoToEdit.elapsedTime > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        stringResource(R.string.elapsed_time),
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    val totalMs = todoToEdit.elapsedTime
                                    val hrs = totalMs / 3600000
                                    val mins = (totalMs % 3600000) / 60000
                                    val secs = (totalMs % 60000) / 1000
                                    val ms = totalMs % 1000
                                    val timeStr = String.format(
                                        getSystemLocale(),
                                        "%02d:%02d:%02d.%03d",
                                        hrs,
                                        mins,
                                        secs,
                                        ms
                                    )
                                    Text(
                                        timeStr,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    // Category selection grid
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.select_tag_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (categories.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SlateDark)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.no_tags_saved),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories) { cat ->
                                    val isSelected = selectedCategoryId == cat.id
                                    val catColor =
                                        remember(cat.colorHex) { safeParseColor(cat.colorHex) }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) catColor else SlateDark)
                                            .clickable {
                                                selectedCategoryId =
                                                    if (isSelected) null else cat.id
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(cat.iconName),
                                                contentDescription = "",
                                                modifier = Modifier.size(12.dp),
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else catColor
                                            )
                                            val catDisplayName = when (cat.name) {
                                                "Work" -> stringResource(R.string.category_work)
                                                "Personal" -> stringResource(R.string.category_personal)
                                                "Shopping" -> stringResource(R.string.category_shopping)
                                                "Health" -> stringResource(R.string.category_health)
                                                "Study" -> stringResource(R.string.category_study)
                                                "Finance" -> stringResource(R.string.category_finance)
                                                else -> cat.name
                                            }
                                            Text(
                                                text = catDisplayName,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Priority Selection row
                    Column {
                        Text(
                            stringResource(R.string.flow_priority_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val priorityLabels = listOf(
                                stringResource(R.string.priority_low),
                                stringResource(R.string.priority_medium),
                                stringResource(R.string.priority_high)
                            )
                            priorityLabels.forEachIndexed { index, pr ->
                                val isSelected = priority == index
                                val color = when (index) {
                                    2 -> PriorityHigh
                                    1 -> PriorityMedium
                                    else -> PriorityLow
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.2f) else SlateDark)
                                        .border(
                                            1.dp,
                                            if (isSelected) color else BorderColor,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { priority = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pr,
                                        color = if (isSelected) color else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                                        // Due date options picker rows
                    AnimatedVisibility(
                        visible = !isDaily || hasReminder,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateDark, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.schedule_title),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }

                                if (selectedDate != null || selectedTimeStr.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            selectedDate = null
                                            selectedTimeStr = ""
                                        },
                                        contentPadding = PaddingValues(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        ),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.clear_button),
                                                tint = PriorityHigh,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = stringResource(R.string.clear_button),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = PriorityHigh
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!isDaily) {
                                    // Date select button
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selectedDate != null) NeonCyan.copy(alpha = 0.18f) else SlateLight)
                                            .border(
                                                1.dp,
                                                if (selectedDate != null) NeonCyan else BorderColor,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                if (isPersian) {
                                                    showJalaliDatePicker = true
                                                } else {
                                                    val act = context.findActivity() ?: context
                                                    try {
                                                        DatePickerDialog(
                                                            act,
                                                            { _, year, month, dayOfMonth ->
                                                                calendar.set(Calendar.YEAR, year)
                                                                calendar.set(Calendar.MONTH, month)
                                                                calendar.set(
                                                                    Calendar.DAY_OF_MONTH,
                                                                    dayOfMonth
                                                                )
                                                                selectedDate = calendar.time
                                                            },
                                                            calendar.get(Calendar.YEAR),
                                                            calendar.get(Calendar.MONTH),
                                                            calendar.get(Calendar.DAY_OF_MONTH)
                                                        ).show()
                                                    } catch (e: Throwable) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = "",
                                                tint = if (selectedDate != null) NeonCyan else TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            val label = if (selectedDate != null) {
                                                if (isPersian) {
                                                    val cal = Calendar.getInstance()
                                                        .apply { time = selectedDate!! }
                                                    val jd = JalaliCalendarHelper.gregorianToJalali(
                                                        cal.get(Calendar.YEAR),
                                                        cal.get(Calendar.MONTH) + 1,
                                                        cal.get(Calendar.DAY_OF_MONTH)
                                                    )
                                                    val monthName =
                                                        JalaliCalendarHelper.JalaliDate.getMonthName(jd.month)
                                                    val dayStr =
                                                        JalaliCalendarHelper.toPersianDigits(jd.day.toString())
                                                    "$dayStr $monthName"
                                                } else {
                                                    SimpleDateFormat(
                                                        "MMM d",
                                                        getSystemLocale()
                                                    ).format(selectedDate!!)
                                                }
                                            } else stringResource(R.string.set_date)
                                            Text(
                                                text = label,
                                                color = if (selectedDate != null) TextPrimary else TextSecondary,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Time select button
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selectedTimeStr.isNotEmpty()) NeonPurple.copy(
                                                alpha = 0.18f
                                            ) else SlateLight
                                        )
                                        .border(
                                            1.dp,
                                            if (selectedTimeStr.isNotEmpty()) NeonPurple else BorderColor,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            showTimePicker = true
                                        }
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = "",
                                            tint = if (selectedTimeStr.isNotEmpty()) NeonPurple else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        val label =
                                            selectedTimeStr.ifEmpty { stringResource(R.string.set_time) }
                                        Text(
                                            text = label,
                                            color = if (selectedTimeStr.isNotEmpty()) TextPrimary else TextSecondary,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Smart Tracking (App Association)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AppShortcut,
                                contentDescription = "",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Smart Auto-Tracking (Optional)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedTextField(
                            value = associatedPackageName ?: "",
                            onValueChange = { associatedPackageName = it.ifEmpty { null } },
                            placeholder = { Text("e.g. com.instagram.android", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonCyan
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Auto-start timer when you open this app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        if (associatedPackageName?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tracking Mode:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateLight)
                                        .padding(4.dp)
                                ) {
                                    val modes = listOf("Strict", "Creative")
                                    modes.forEach { mode ->
                                        val isSelected = trackingMode == mode
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) NeonCyan else Color.Transparent)
                                                .clickable { trackingMode = mode }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = mode,
                                                color = if (isSelected) SlateDark else TextSecondary,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Daily task switch row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.daily_task_reset_notice),
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = isDaily,
                            onCheckedChange = { isDaily = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SlateDark
                            )
                        )
                    }

                    // Reminder notification switch row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.create_alarm_reminder),
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SlateDark
                            )
                        )
                    }

                    if (isDaily && hasReminder) {
                        // Repeat reminder switch row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EventRepeat,
                                    contentDescription = "",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.repeat_reminder),
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = isReminderRepeating,
                                onCheckedChange = { isReminderRepeating = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonCyan,
                                    checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SlateDark
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Actions Cancel/Save row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    // Compile trigger times
                                    val duetime =
                                        if (selectedDate != null) calendar.timeInMillis else null
                                    val alarmtime =
                                        if (hasReminder && duetime != null) duetime else null
                                    val hasTime = selectedTimeStr.isNotEmpty()
                                    onConfirm(
                                        title,
                                        desc,
                                        duetime,
                                        priority,
                                        selectedCategoryId,
                                        hasReminder,
                                        alarmtime,
                                        isDaily,
                                        hasTime,
                                        isReminderRepeating,
                                        associatedPackageName,
                                        trackingMode
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_add_task_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            enabled = title.isNotBlank()
                        ) {
                            Text(
                                if (todoToEdit != null) stringResource(R.string.save_changes) else stringResource(
                                    R.string.create
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    fun AddCategoryDialog(
        onDismiss: () -> Unit,
        onConfirm: (name: String, colorHex: String, iconName: String) -> Unit
    ) {
        var name by remember { mutableStateOf("") }
        var selectedColorIndex by remember { mutableIntStateOf(0) }
        var selectedIconIndex by remember { mutableIntStateOf(0) }

        val presetColors = listOf(
            "#0A84FF", // Blue
            "#BF5AF2", // Purple
            "#FF9F0A", // Orange
            "#FF453A", // Red
            "#64D2FF", // Teal
            "#30D158", // Green
            "#FFD60A", // Yellow
            "#FF64B4"  // Pink
        )

        val presetIcons = listOf(
            "Work",
            "Person",
            "ShoppingCart",
            "Favorite",
            "Book",
            "AttachMoney",
            "DirectionsRun",
            "Brush"
        )

        val currentColor = remember(selectedColorIndex) {
            safeParseColor(presetColors[selectedColorIndex])
        }

        AppDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        clip = false,
                        ambientColor = currentColor,
                        spotColor = currentColor
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(currentColor, BorderColor.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    currentColor.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                radius = 400f
                            )
                        )
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header section with an elegant label/tag icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(currentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = currentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.new_tag_category),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = TextPrimary
                        )
                    }

                    // 1. Dynamic Live Preview Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .border(
                                BorderStroke(1.dp, BorderColor.copy(alpha = 0.2f)),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.live_preview),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(currentColor.copy(alpha = 0.12f))
                                .border(
                                    BorderStroke(1.2.dp, currentColor.copy(alpha = 0.5f)),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(currentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(presetIcons[selectedIconIndex]),
                                    contentDescription = null,
                                    tint = ObsidianBlack,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = name.ifBlank { stringResource(R.string.category_tag_placeholder) },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.customize_look_feel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // 2. Category Name Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= 15) name = it },
                            placeholder = {
                                Text(
                                    stringResource(R.string.category_example_placeholder),
                                    color = TextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Label,
                                    contentDescription = null,
                                    tint = currentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (name.isNotEmpty()) {
                                    IconButton(onClick = { name = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.clear_text_desc),
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = currentColor,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SlateDark.copy(alpha = 0.4f),
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${name.length} / 15",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (name.length == 15) currentColor else TextSecondary
                            )
                        }
                    }

                    // 3. Color Selection Grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.pick_palette_color),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetColors.chunked(4).forEachIndexed { rowIdx, rowColors ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowColors.forEachIndexed { colIdx, hex ->
                                        val idx = rowIdx * 4 + colIdx
                                        val color = safeParseColor(hex)
                                        val isSelected = selectedColorIndex == idx
                                        val sizeScale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.15f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            label = "color_scale"
                                        )

                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .graphicsLayer {
                                                        scaleX = sizeScale
                                                        scaleY = sizeScale
                                                    }
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                color,
                                                                color.copy(alpha = 0.7f)
                                                            )
                                                        )
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) Color.White else BorderColor.copy(
                                                            alpha = 0.3f
                                                        ),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { selectedColorIndex = idx }
                                                    .testTag("category_color_picker_$idx"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = stringResource(R.string.selected_content_description),
                                                        tint = ObsidianBlack,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Vector Symbol Selector
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.pick_category_symbol),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetIcons.chunked(4).forEachIndexed { rowIdx, rowIcons ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowIcons.forEachIndexed { colIdx, icon ->
                                        val idx = rowIdx * 4 + colIdx
                                        val isSelected = selectedIconIndex == idx
                                        val sizeScale by animateFloatAsState(
                                            targetValue = if (isSelected) 1.12f else 1f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            label = "icon_scale"
                                        )

                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .graphicsLayer {
                                                        scaleX = sizeScale
                                                        scaleY = sizeScale
                                                    }
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (isSelected) currentColor.copy(alpha = 0.2f) else SlateDark.copy(
                                                            alpha = 0.6f
                                                        )
                                                    )
                                                    .border(
                                                        width = 1.2.dp,
                                                        color = if (isSelected) currentColor else BorderColor.copy(
                                                            alpha = 0.3f
                                                        ),
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable { selectedIconIndex = idx }
                                                    .testTag("category_icon_picker_$idx"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIcon(icon),
                                                    contentDescription = "",
                                                    tint = if (isSelected) currentColor else TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Dynamic Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.2.dp, BorderColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    onConfirm(
                                        name,
                                        presetColors[selectedColorIndex],
                                        presetIcons[selectedIconIndex]
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = currentColor,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = currentColor.copy(alpha = 0.35f)
                            ),
                            enabled = name.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.add_tag),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                    }
                }
            }
        }
    }
}


    @Composable
    fun AddNoteScreen(
        noteToEdit: NoteItem?,
        categories: List<Category>,
        onDismiss: () -> Unit,
        onConfirm: (title: String, content: String, colorIndex: Int, categoryId: Int?) -> Unit,
        onAddCategoryClick: () -> Unit,
        onDeleteCategory: ((Category) -> Unit)? = null
    ) {
        var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
        var content by remember { mutableStateOf(noteToEdit?.content ?: "") }
        var selectedColorIndex by remember { mutableStateOf(noteToEdit?.colorIndex ?: 0) }
        var selectedCategoryId by remember { mutableStateOf(noteToEdit?.categoryId) }
        var isCustomColor by remember { mutableStateOf(selectedColorIndex < 0) }
        var customHue by remember {
            mutableStateOf(
                if (selectedColorIndex < 0) {
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(selectedColorIndex, hsv)
                    hsv[0]
                } else 180f
            )
        }


        val currentLocalContext = LocalContext.current
        val currentLayoutDirection = LocalLayoutDirection.current

        androidx.activity.compose.BackHandler(onBack = onDismiss)

        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .systemBarsPadding()
                .imePadding(),
            colors = CardDefaults.cardColors(containerColor = ObsidianBlack),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (noteToEdit != null) stringResource(R.string.edit_note) else stringResource(
                                        R.string.new_note
                                    ),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonCyan
                                )
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = TextSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            TextField(
                                value = title,
                                onValueChange = { title = it },
                                label = {
                                    Text(
                                        stringResource(R.string.note_title_label),
                                        color = TextSecondary
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            )

                            HorizontalDivider(
                                color = BorderColor,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            TextField(
                                value = content,
                                onValueChange = { content = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.note_placeholder),
                                        color = TextSecondary
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Beautiful Color Customization Row (Fixed at Bottom)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.card_theme_color),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val notesPalette = getNotesPaletteList()
                                notesPalette.forEachIndexed { index, palette ->
                                    val isSelected = !isCustomColor && selectedColorIndex == index
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        palette.primaryColor,
                                                        palette.secondaryColor
                                                    )
                                                )
                                            )
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                isCustomColor = false
                                                selectedColorIndex = index
                                            }
                                            .testTag("color_picker_circle_$index"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = stringResource(R.string.selected_content_description),
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Add "Custom" color circle
                                val isCustomSelected = isCustomColor
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                colors = listOf(
                                                    Color.Red,
                                                    Color.Yellow,
                                                    Color.Green,
                                                    Color.Cyan,
                                                    Color.Blue,
                                                    Color.Magenta,
                                                    Color.Red
                                                )
                                            )
                                        )
                                        .border(
                                            width = if (isCustomSelected) 3.dp else 1.dp,
                                            color = if (isCustomSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            isCustomColor = true
                                            selectedColorIndex = android.graphics.Color.HSVToColor(
                                                floatArrayOf(
                                                    customHue,
                                                    1f,
                                                    1f
                                                )
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCustomSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected Custom Color",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            if (isCustomColor) {
                                Slider(
                                    value = customHue,
                                    onValueChange = {
                                        customHue = it
                                        selectedColorIndex = android.graphics.Color.HSVToColor(
                                            floatArrayOf(
                                                it,
                                                1f,
                                                1f
                                            )
                                        )
                                    },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(
                                            android.graphics.Color.HSVToColor(
                                                floatArrayOf(customHue, 1f, 1f)
                                            )
                                        ),
                                        activeTrackColor = Color(
                                            android.graphics.Color.HSVToColor(
                                                floatArrayOf(customHue, 1f, 1f)
                                            )
                                        )
                                    )
                                )
                            }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            CategoryRowSelector(
                                categories = categories,
                                selectedId = selectedCategoryId,
                                onSelectId = { selectedCategoryId = it },
                                onAddCategoryClick = onAddCategoryClick,
                                onDeleteCategory = onDeleteCategory
                            )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(
                                onClick = { onConfirm(title, content, selectedColorIndex, selectedCategoryId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (noteToEdit != null) stringResource(R.string.save_note) else stringResource(R.string.add_note),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
        }
    }

fun Context.findActivity(): android.app.Activity? {
    var cur = this
    while (cur is ContextWrapper) {
        if (cur is android.app.Activity) {
            return cur
        }
        cur = cur.baseContext
    }
    return null
}
