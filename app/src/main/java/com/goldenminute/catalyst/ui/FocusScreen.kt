package com.goldenminute.catalyst.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.goldenminute.catalyst.R
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenminute.catalyst.data.TodoItem
import com.goldenminute.catalyst.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen(
    viewModel: TodoViewModel,
    innerPadding: PaddingValues,
    onViewAllSessions: (TodoItem) -> Unit = {}
) {
    val todos by viewModel.allTodosList.collectAsState()
    val incompleteTodos = todos.filter { !it.isCompleted }
    val selectedTodo by viewModel.selectedFocusTodo.collectAsState()
    val isIdleMode by viewModel.isIdleMode.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val isPomodoro by viewModel.isPomodoro.collectAsState()
    val pomodoroMode by viewModel.pomodoroMode.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()
    val timeElapsed by viewModel.timeElapsed.collectAsState()
    val focusLength by viewModel.focusLength.collectAsState()
    val shortBreakLength by viewModel.shortBreakLength.collectAsState()
    val longBreakLength by viewModel.longBreakLength.collectAsState()
    val pomodoroCycle by viewModel.pomodoroCycle.collectAsState()
    var sessionRecords by remember { mutableStateOf(emptyList<com.goldenminute.catalyst.data.RecordedTimeItem>()) }
    var lastRecordedTimeElapsed by remember { mutableLongStateOf(0L) }
    var isAssignedToTask by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val freeModeSessionsStr by viewModel.freeModeSessions.collectAsState()

    // POST_NOTIFICATIONS permission launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently; timer still starts */ }

    fun startTimerWithPermissionCheck() {
        val isAppAssigned = selectedTodo != null && !selectedTodo!!.associatedPackageNames.isNullOrEmpty()
        if (isAppAssigned) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.timer_app_assigned_blocked),
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.isTimerRunning.value = true
    }

    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTodo?.id, isTimerRunning) {
        if (isTimerRunning && selectedTodo != null && !selectedTodo!!.associatedPackageNames.isNullOrEmpty()) {
            viewModel.isTimerRunning.value = false
            TimerStateManager.autoPauseAndSaveTask(selectedTodo!!.id)
        }
    }

    LaunchedEffect(selectedTodo?.id) {
        sessionRecords = emptyList()
        lastRecordedTimeElapsed = 0L
        isAssignedToTask = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding(), bottom = 0.dp)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Pomodoro / Stopwatch Redesign
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pomodoro Option Button
            val pomodoroSelected = isPomodoro
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (pomodoroSelected) NeonCyan.copy(alpha = 0.15f) else SlateDark)
                    .border(
                        width = if (pomodoroSelected) 1.5.dp else 1.dp,
                        color = if (pomodoroSelected) NeonCyan else BorderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        if (!isPomodoro) {
                            viewModel.isPomodoro.value = true
                            viewModel.setPomodoroMode("Focus")
                            lastRecordedTimeElapsed = 0L
                        }
                    }
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Pomodoro Timer Icon",
                        tint = if (pomodoroSelected) NeonCyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.tab_pomodoro),
                        color = if (pomodoroSelected) TextPrimary else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Stopwatch Option Button
            val stopwatchSelected = !isPomodoro
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (stopwatchSelected) NeonPurple.copy(alpha = 0.15f) else SlateDark)
                    .border(
                        width = if (stopwatchSelected) 1.5.dp else 1.dp,
                        color = if (stopwatchSelected) NeonPurple else BorderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        if (isPomodoro) {
                            viewModel.isPomodoro.value = false
                            viewModel.isTimerRunning.value = false
                            viewModel.timeElapsed.value = 0L
                            lastRecordedTimeElapsed = 0L
                        }
                    }
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = "Stopwatch Icon",
                        tint = if (stopwatchSelected) NeonPurple else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.tab_stopwatch),
                        color = if (stopwatchSelected) TextPrimary else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isPomodoro) {
                IconButton(
                    onClick = { showSettingsDialog = true },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timer Display
        val displayTimeMillis = if (isPomodoro) timeLeft else timeElapsed
        val totalSeconds = if (isPomodoro) (displayTimeMillis + 999) / 1000 else displayTimeMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = if (isPomodoro) totalSeconds / 60 else (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val milliseconds = displayTimeMillis % 1000
        val progress = if (isPomodoro) {
            val totalDuration = when (pomodoroMode) {
                "Focus" -> focusLength.toFloat()
                "Short Break" -> shortBreakLength.toFloat()
                "Long Break" -> longBreakLength.toFloat()
                else -> focusLength.toFloat()
            }
            if (totalDuration > 0) timeLeft.toFloat() / totalDuration else 0f
        } else {
            0f
        }

        val animatedProgress by animateFloatAsState(targetValue = progress, label = "timer_progress")

        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPomodoro) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SlateDark,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    drawArc(
                        color = if (pomodoroMode != "Focus") NeonPurple else NeonCyan,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isPomodoro && hours > 0) {
                    Text(
                        text = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                } else if (!isPomodoro) {
                    Text(
                        text = String.format("%02d:%02d.%03d", minutes, seconds, milliseconds),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                } else {
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isPomodoro) { if (pomodoroMode != "Focus") "$pomodoroMode" else stringResource(R.string.focus_cycle_text, pomodoroCycle + 1) } else stringResource(R.string.elapsed_time),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // --- REDESIGNED RECORDS SECTION ---
        val dbRecords = if (selectedTodo != null) {
            selectedTodo!!.recordedTimes.let { com.goldenminute.catalyst.data.RecordedTimeItem.parseList(it) }
        } else if (isIdleMode) {
            com.goldenminute.catalyst.data.RecordedTimeItem.parseList(freeModeSessionsStr)
        } else {
            emptyList()
        }
        val now = java.util.Calendar.getInstance()
        val startOfToday = now.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfToday = now.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis

        val activeDbRecords = dbRecords.filter { record -> 
            record.isPomodoro == isPomodoro && record.timestamp in startOfToday..endOfToday 
        }
        
        if (selectedTodo != null || isIdleMode) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isPomodoro) NeonPurple.copy(alpha = 0.3f) else NeonCyan.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isPomodoro) Icons.Default.Timer else Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = if (isPomodoro) NeonPurple else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isPomodoro) stringResource(R.string.focus_history_pomodoro) else stringResource(R.string.focus_history_stopwatch),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (activeDbRecords.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val sessionsText = if (activeDbRecords.size == 1) {
                                    stringResource(R.string.session_records_count, activeDbRecords.size)
                                } else {
                                    stringResource(R.string.session_records_count_plural, activeDbRecords.size)
                                }
                                Text(
                                    text = sessionsText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                if (selectedTodo != null) {
                                    TextButton(
                                        onClick = { selectedTodo?.let { onViewAllSessions(it) } },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.view_all_sessions),
                                            color = if (isPomodoro) NeonPurple else NeonCyan,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (activeDbRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPomodoro) stringResource(R.string.no_pomodoro_history) else stringResource(R.string.no_stopwatch_history),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    } else {
                        // Display all records
                        val recentRecords = activeDbRecords.reversed()
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentRecords.forEachIndexed { index, record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateLight.copy(alpha = 0.3f))
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.session_label, activeDbRecords.size - index),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val isPersianRecord = Locale.getDefault().language == "fa"
                                        val recordDateStr = if (isPersianRecord) {
                                            JalaliCalendarHelper.formatJalali(record.timestamp, includeTime = true, includeYear = false)
                                        } else {
                                            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(record.timestamp))
                                        }
                                        Text(
                                            text = recordDateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    
                                    Text(
                                        text = if (record.isPomodoro) {
                                            "${record.value} cycle"
                                        } else {
                                            val hrs = record.value / 3600000
                                            val mins = (record.value % 3600000) / 60000
                                            val secs = (record.value % 60000) / 1000
                                            val ms = record.value % 1000
                                            if (hrs > 0) {
                                                String.format(Locale.getDefault(), "%dh %dm %ds", hrs, mins, secs)
                                            } else if (mins > 0) {
                                                String.format(Locale.getDefault(), "%dm %ds", mins, secs)
                                            } else {
                                                String.format(Locale.getDefault(), "%.1fs", record.value / 1000f)
                                            }
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPomodoro) NeonPurple else NeonCyan
                                    )
                                }
                            }
                            
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Space-age Focus Control Capsule/Dock
        val infiniteTransition = rememberInfiniteTransition(label = "timer_glow")
        val glowPulse by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )

        val capsuleColor = if (isTimerRunning) NeonCyan else MaterialTheme.colorScheme.primary.copy(0.5f)
        val capsuleAlpha = if (isTimerRunning) (glowPulse * 0.5f).coerceIn(0.3f, 0.9f) else 1f

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .widthIn(max = 350.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = if (isTimerRunning) (20.dp * glowPulse) else 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = if (isTimerRunning) NeonCyan.copy(alpha = 0.15f * glowPulse) else TextPrimary.copy(alpha = 0.1f),
                    spotColor = if (isTimerRunning) NeonCyan.copy(alpha = 0.3f * glowPulse) else TextPrimary.copy(alpha = 0.2f)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SlateDark,
                            SlateDark.copy(alpha = 0.85f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = capsuleColor.copy(alpha = if (isTimerRunning) capsuleAlpha else 0.5f)
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Reset Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SlateLight)
                        .border(BorderStroke(1.dp, BorderColor), CircleShape)
                        .clickable {
                            if (isPomodoro) {
                                viewModel.resetCurrentMode()
                                lastRecordedTimeElapsed = 0L
                            } else {
                                viewModel.isTimerRunning.value = false
                                viewModel.timeElapsed.value = 0L
                                lastRecordedTimeElapsed = 0L
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Save Session Button
                val isRecordEnabled = (selectedTodo != null || isIdleMode) && (!isPomodoro || pomodoroMode != "Focus")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isRecordEnabled) SlateLight else SlateLight.copy(alpha = 0.3f))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isRecordEnabled) BorderColor else BorderColor.copy(alpha = 0.5f)
                            ),
                            CircleShape
                        )
                        .clickable(enabled = isRecordEnabled) {
                            if (selectedTodo != null) {
                                if (isPomodoro) {
                                    val newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                                        isPomodoro = true,
                                        value = 1L,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    viewModel.addSessionRecords(selectedTodo!!.id, listOf(newRecord))
                                } else {
                                    val duration = timeElapsed
                                    if (duration > 60_000L) {
                                        val newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                                            isPomodoro = false,
                                            value = duration,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        viewModel.addSessionRecords(selectedTodo!!.id, listOf(newRecord))
                                        viewModel.isTimerRunning.value = false
                                        viewModel.timeElapsed.value = 0L
                                        lastRecordedTimeElapsed = 0L
                                    } else {
                                        viewModel.isTimerRunning.value = false
                                        viewModel.timeElapsed.value = 0L
                                        lastRecordedTimeElapsed = 0L
                                        android.widget.Toast.makeText(context, context.getString(R.string.session_too_short_not_saved), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else if (isIdleMode) {
                                if (isPomodoro) {
                                    val newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                                        isPomodoro = true,
                                        value = 1L,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    viewModel.addFreeModeFocusSession(newRecord)
                                    android.widget.Toast.makeText(context, context.getString(R.string.free_focus_session_saved), android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    val duration = timeElapsed
                                    if (duration > 60_000L) {
                                        val newRecord = com.goldenminute.catalyst.data.RecordedTimeItem(
                                            isPomodoro = false,
                                            value = duration,
                                            timestamp = System.currentTimeMillis()
                                        )
                                        viewModel.addFreeModeFocusSession(newRecord)
                                        android.widget.Toast.makeText(context, context.getString(R.string.free_focus_session_saved), android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, context.getString(R.string.session_too_short_not_saved), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                viewModel.isTimerRunning.value = false
                                viewModel.timeElapsed.value = 0L
                                lastRecordedTimeElapsed = 0L
                            }
                        }
                ) {
                    Icon(
                        imageVector = if (isIdleMode) Icons.Default.Stop else Icons.Default.Save,
                        contentDescription = if (isIdleMode) "Stop Session" else "Record & Save Session",
                        tint = if (isRecordEnabled) TextPrimary else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 3. Play / Pause Button (The Centerpiece with breathing glow, exact 52.dp layout footprint)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer(clip = false)
                ) {
                    if (isTimerRunning) {
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.45f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "center_pulse_scale"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "center_pulse_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                    alpha = pulseAlpha
                                    clip = false
                                }
                                .background(TextPrimary, CircleShape)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = if (isTimerRunning) TextPrimary else SlateLight,
                                shape = CircleShape
                            )
                            .border(
                                BorderStroke(1.dp, if (isTimerRunning) Color.Transparent else BorderColor),
                                CircleShape
                            )
                            .clickable {
                                if (!isTimerRunning) {
                                    startTimerWithPermissionCheck()
                                } else {
                                    viewModel.isTimerRunning.value = false
                                }
                            }
                    ) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isTimerRunning) "Pause" else "Start",
                            tint = if (isTimerRunning) SlateDark else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 4. Complete Task Button
                val isCompleteEnabled = selectedTodo != null
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isCompleteEnabled) SlateLight else SlateLight.copy(alpha = 0.3f))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isCompleteEnabled) BorderColor else BorderColor.copy(alpha = 0.5f)
                            ),
                            CircleShape
                        )
                        .clickable(enabled = isCompleteEnabled) {
                            if (selectedTodo != null && !selectedTodo!!.isCompleted) {
                                viewModel.toggleTodoCompleted(selectedTodo!!.id, !selectedTodo!!.isCompleted)
                                viewModel.isTimerRunning.value = false
                                TimerStateManager.activeTaskId.value = -1
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete Task",
                        tint = if (isCompleteEnabled) TextPrimary else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Task Selection
        Text(
            text = stringResource(R.string.select_a_task_to_focus_on),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (incompleteTodos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_tasks_to_get_focus_on),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                // Idle State Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isIdleMode) NeonCyan.copy(alpha = 0.2f) else SlateDark)
                        .clickable { 
                            TimerStateManager.isIdleMode.value = true
                            TimerStateManager.activeTaskId.value = -1
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isIdleMode) {
                        Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.idle_state_focus),
                        color = if (isIdleMode) NeonCyan else TextPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Tasks
                incompleteTodos.forEach { item ->
                    val isSelected = selectedTodo?.id == item.id
                    val isAppLinked = !item.associatedPackageNames.isNullOrBlank() && item.associatedPackageNames.split(",").any { it.isNotBlank() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else SlateDark)
                            .clickable(enabled = !isAppLinked) { 
                                TimerStateManager.activeTaskId.value = item.id
                                TimerStateManager.isIdleMode.value = false
                                TimerStateManager.isPomodoro.value = (item.timerMode == "Pomodoro")
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isAppLinked) TextSecondary.copy(alpha = 0.35f) else TextSecondary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = if (isSelected) NeonCyan else if (isAppLinked) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (isAppLinked) {
                                Text(
                                    text = stringResource(R.string.auto_starts_with_app),
                                    color = TextSecondary.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

        if (showSettingsDialog) {
            AppAlertDialog(
                onDismissRequest = { 
                    viewModel.resetCurrentMode()
                    showSettingsDialog = false 
                },
                title = { Text(stringResource(R.string.timer_settings_title), color = TextPrimary) },
                text = {
                    Column {
                        Text(stringResource(R.string.focus_length_min), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Slider(
                            value = (focusLength / (60 * 1000f)),
                            onValueChange = { viewModel.setFocusLength((it.toInt() * 60 * 1000).toLong()) },
                            valueRange = 1f..60f,
                            steps = 59
                        )
                        Text(stringResource(R.string.minutes_short, (focusLength / (60 * 1000)).toInt()), fontSize = 12.sp, color = TextSecondary)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.short_break_min), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Slider(
                            value = (shortBreakLength / (60 * 1000f)),
                            onValueChange = { viewModel.setShortBreakLength((it.toInt() * 60 * 1000).toLong()) },
                            valueRange = 1f..30f,
                            steps = 29
                        )
                        Text(stringResource(R.string.minutes_short, (shortBreakLength / (60 * 1000)).toInt()), fontSize = 12.sp, color = TextSecondary)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.long_break_min), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Slider(
                            value = (longBreakLength / (60 * 1000f)),
                            onValueChange = { viewModel.setLongBreakLength((it.toInt() * 60 * 1000).toLong()) },
                            valueRange = 5f..60f,
                            steps = 55
                        )
                        Text(stringResource(R.string.minutes_short, (longBreakLength / (60 * 1000)).toInt()), fontSize = 12.sp, color = TextSecondary)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        viewModel.resetCurrentMode()
                        showSettingsDialog = false 
                    }) {
                        Text(stringResource(R.string.close_button), color = NeonCyan)
                    }
                },
                containerColor = SlateDark
            )
        }

        Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding() + 16.dp))
    }
}
