package com.goldenminute.catalyst.ui

import androidx.compose.ui.res.stringResource
import com.goldenminute.catalyst.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import com.goldenminute.catalyst.data.TodoItem
import com.goldenminute.catalyst.data.Category
import com.goldenminute.catalyst.data.RecordedTimeItem
import com.goldenminute.catalyst.ui.theme.*
import java.text.SimpleDateFormat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.draw.scale
import java.util.Locale
import java.util.Date
import java.util.Calendar
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalLocale

@Composable
fun ProgressScreen(
    viewModel: TodoViewModel,
    todos: List<TodoItem>,
    categories: List<Category> = emptyList(),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("Insights") }

    BackHandler(enabled = selectedTab != "Insights") {
        selectedTab = "Insights"
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 10.dp)
            .padding(horizontal = 12.dp)
    ) {
        // Top App Bar/Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .shadow(2.dp, RoundedCornerShape(14.dp))
                    .background(SlateDark, RoundedCornerShape(14.dp))
                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        onBackClick()
                    }
                    .testTag("progress_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go Back",
                    tint = TextPrimary
                )
            }
            
            Text(
                text = stringResource(R.string.analytics_timeline_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                maxLines = 1
            )
        }

        // Premium Sliding Tabs Selector
        val tabIndex = if (selectedTab == "Insights") 0 else 1
        val animatedTabOffset by animateFloatAsState(
            targetValue = tabIndex.toFloat(),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "tabOffset"
        )
        val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        val isRtl = layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(52.dp)
                .background(SlateDark, RoundedCornerShape(26.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(26.dp))
        ) {
            // Sliding highlight capsule
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        val sign = if (isRtl) -1f else 1f
                        translationX = size.width * animatedTabOffset * sign
                    }
                    .padding(4.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.2f),
                                NeonPurple.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))),
                        RoundedCornerShape(22.dp)
                    )
            )
            
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf("Insights", "History")
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabLabel = if (tab == "Insights") stringResource(R.string.tab_insights) else stringResource(R.string.tab_history)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(26.dp))
                            .clickable { selectedTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabLabel,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
        
        // Tab Content Display
        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == "Insights") {
                InsightsContent(viewModel, todos, categories)
            } else {
                HistoryContent(todos, categories)
            }
        }
    }
}

@Composable
fun InsightsContent(viewModel: TodoViewModel, todos: List<TodoItem>, categories: List<Category>) {
    var selectedPeriod by remember { mutableStateOf("Today") }
    val periods = listOf("Today", "Yesterday", "Last Week", "Last Month")

    val (startMs, endMs) = remember(selectedPeriod) {
        val now = Calendar.getInstance()
        val startOfToday = now.clone() as Calendar
        startOfToday.set(Calendar.HOUR_OF_DAY, 0)
        startOfToday.set(Calendar.MINUTE, 0)
        startOfToday.set(Calendar.SECOND, 0)
        startOfToday.set(Calendar.MILLISECOND, 0)
        val startOfTodayMs = startOfToday.timeInMillis

        when (selectedPeriod) {
            "Today" -> {
                val endOfTodayMs = Calendar.getInstance().apply { 
                    timeInMillis = startOfTodayMs
                    add(Calendar.DAY_OF_YEAR, 1)
                }.timeInMillis - 1
                startOfTodayMs to endOfTodayMs
            }
            "Yesterday" -> {
                val start = Calendar.getInstance().apply { timeInMillis = startOfTodayMs; add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
                start to (startOfTodayMs - 1)
            }
            "Last Week" -> {
                val start = Calendar.getInstance().apply { timeInMillis = startOfTodayMs; add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                start to (startOfTodayMs - 1)
            }
            "Last Month" -> {
                val start = Calendar.getInstance().apply { timeInMillis = startOfTodayMs; add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
                start to (startOfTodayMs - 1)
            }
            else -> 0L to Long.MAX_VALUE
        }
    }

    val periodStats = remember(todos, startMs, endMs) {

        // Include tasks that had any recorded focus activity in the period,
        // OR were completed/due in the period, OR are active high-priority
        val filteredTodos = todos.filter { todo ->
            val hasCompletedInRange = todo.completedDate != null && todo.completedDate in startMs..endMs
            val hasDueInRange = todo.dueDate != null && todo.dueDate in startMs..endMs
            val hasRecordedActivityInRange = RecordedTimeItem.parseList(todo.recordedTimes)
                .any { it.timestamp in startMs..endMs }
            val isActiveHighPriority = !todo.isCompleted && todo.priority == 2
            hasCompletedInRange || hasDueInRange || hasRecordedActivityInRange || isActiveHighPriority
        }

        val totalTasks = filteredTodos.size
        val completedTasks = filteredTodos.count { it.isCompleted && (it.completedDate != null && it.completedDate in startMs..endMs) }
        val pendingTasks = totalTasks - completedTasks
        val highPriority = filteredTodos.count { !it.isCompleted && it.priority == 2 }
        val completionRatio = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
        
        val hpTotal = filteredTodos.count { it.priority == 2 }
        val hpCompleted = filteredTodos.count { it.priority == 2 && it.isCompleted && (it.completedDate != null && it.completedDate in startMs..endMs) }
        val hpRatio = if (hpTotal > 0) hpCompleted.toFloat() / hpTotal else 0f

        mapOf(
            "total" to totalTasks,
            "completed" to completedTasks,
            "pending" to pendingTasks,
            "highPriority" to highPriority,
            "ratio" to completionRatio,
            "hpTotal" to hpTotal,
            "hpCompleted" to hpCompleted,
            "hpRatio" to hpRatio
        )
    }

    val totalTasks = periodStats["total"] as Int
    val completedTasks = periodStats["completed"] as Int
    val pendingTasks = periodStats["pending"] as Int
    val highPriority = periodStats["highPriority"] as Int
    val completionRatio = periodStats["ratio"] as Float

    // Animation values
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(completionRatio) {
        animatedProgress.animateTo(
            targetValue = completionRatio,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val highPriorityTotal = periodStats["hpTotal"] as Int
    val highPriorityCompleted = periodStats["hpCompleted"] as Int
    val highPriorityRatio = periodStats["hpRatio"] as Float

    val animatedHighPriorityProgress = remember { Animatable(0f) }
    LaunchedEffect(highPriorityRatio) {
        animatedHighPriorityProgress.animateTo(
            targetValue = highPriorityRatio,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    // Advanced dynamic metrics
    val streakDays = remember(todos) {
        val completedDates = todos.mapNotNull { it.completedDate }
        if (completedDates.isEmpty()) return@remember 0
        val sortedDays = completedDates.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.distinct().sortedDescending()

        if (sortedDays.isEmpty()) return@remember 0
        
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMs = todayCal.timeInMillis
        val yesterdayMs = todayCal.apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        
        if (sortedDays[0] != todayMs && sortedDays[0] != yesterdayMs) {
            return@remember 0
        }
        
        var currentStreak = 1
        var checkMs = sortedDays[0]
        for (i in 1 until sortedDays.size) {
            val prevDayCal = Calendar.getInstance().apply { 
                timeInMillis = checkMs
                add(Calendar.DAY_OF_YEAR, -1)
            }
            if (sortedDays[i] == prevDayCal.timeInMillis) {
                currentStreak++
                checkMs = sortedDays[i]
            } else {
                break
            }
        }
        currentStreak
    }

    // Free mode sessions
    val freeSessionsSerialized by viewModel.freeModeSessions.collectAsState()
    val freeSessionRecords = remember(freeSessionsSerialized) {
        RecordedTimeItem.parseList(freeSessionsSerialized)
    }

    val focusLengthMs by viewModel.focusLength.collectAsState()
    
    val totalElapsedTime = remember(todos, freeSessionRecords, startMs, endMs, focusLengthMs) {
        val taskTime = todos.sumOf { todo ->
            val records = RecordedTimeItem.parseList(todo.recordedTimes)
            records.filter { it.timestamp in startMs..endMs }.sumOf { 
                if (it.isPomodoro) it.value * focusLengthMs else it.value
            }
        }
        val freeTime = freeSessionRecords.filter { it.timestamp in startMs..endMs }.sumOf { 
            if (it.isPomodoro) it.value * focusLengthMs else it.value
        }
        taskTime + freeTime
    }
    val totalHours = totalElapsedTime / 3600000
    val totalMinutes = (totalElapsedTime % 3600000) / 60000
    val totalTimeStr = if (totalHours > 0) "${totalHours}h ${totalMinutes}m" else "${totalMinutes}m"

    val totalPomodoros = remember(todos, freeSessionRecords, startMs, endMs) {
        val taskPomodoros = todos.sumOf { todo ->
            val records = RecordedTimeItem.parseList(todo.recordedTimes)
            records.filter { it.isPomodoro && it.timestamp in startMs..endMs }.sumOf { it.value.toInt() }
        }
        val freePomodoros = freeSessionRecords.filter { it.isPomodoro && it.timestamp in startMs..endMs }.sumOf { it.value.toInt() }
        taskPomodoros + freePomodoros
    }
    
    // AI Insights State
    val aiInsight by viewModel.aiInsight.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var isAiEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Period Selector Caps Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            periods.forEach { period ->
                val isSelected = selectedPeriod == period
                val periodLabel = when (period) {
            "Today" -> stringResource(R.string.period_today)
            "Yesterday" -> stringResource(R.string.period_yesterday)
            "Last Week" -> stringResource(R.string.period_last_week)
            "Last Month" -> stringResource(R.string.period_last_month)
            else -> period
        }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) NeonPurple.copy(alpha = 0.25f) else SlateDark
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) NeonPurple else BorderColor
                            ),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { selectedPeriod = period }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = periodLabel,
                        color = if (isSelected) NeonPurple else TextSecondary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        // Hero Performance Double-Ring Dashboard Gauge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDark, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nested Dual Progress Ring Canvas
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(125.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidthOuter = 12.dp.toPx()
                            val strokeWidthInner = 8.dp.toPx()
                            
                            // Outer Track Background
                            val outerRadius = (size.minDimension - strokeWidthOuter) / 2
                            drawCircle(
                                color = BorderColor.copy(alpha = 0.3f),
                                radius = outerRadius,
                                style = Stroke(width = strokeWidthOuter)
                            )
                            
                            // Outer Progress Arc (Completion Rate)
                            drawArc(
                                brush = Brush.sweepGradient(listOf(NeonCyan, NeonPurple, NeonCyan)),
                                startAngle = -90f,
                                sweepAngle = animatedProgress.value * 360f,
                                useCenter = false,
                                topLeft = Offset((size.width - outerRadius * 2) / 2, (size.height - outerRadius * 2) / 2),
                                size = Size(outerRadius * 2, outerRadius * 2),
                                style = Stroke(width = strokeWidthOuter, cap = StrokeCap.Round)
                            )
                            
                            // Inner Track Background
                            val innerRadius = outerRadius - strokeWidthOuter - 6.dp.toPx()
                            drawCircle(
                                color = BorderColor.copy(alpha = 0.3f),
                                radius = innerRadius,
                                style = Stroke(width = strokeWidthInner)
                            )
                            
                            // Inner Progress Arc (High Priority Completion Ratio)
                            drawArc(
                                brush = Brush.linearGradient(listOf(PriorityHigh, PriorityMedium)),
                                startAngle = -90f,
                                sweepAngle = animatedHighPriorityProgress.value * 360f,
                                useCenter = false,
                                topLeft = Offset((size.width - innerRadius * 2) / 2, (size.height - innerRadius * 2) / 2),
                                size = Size(innerRadius * 2, innerRadius * 2),
                                style = Stroke(width = strokeWidthInner, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(animatedProgress.value * 100).toInt()}%",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                fontSize = 22.sp
                            )
                            Text(
                                text = stringResource(R.string.completed_status),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(NeonCyan, CircleShape))
                            Text(text = stringResource(R.string.outer_ring_legend), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(PriorityMedium, CircleShape))
                            Text(text = stringResource(R.string.inner_ring_legend), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                
                // Detailed performance metrics
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.performance_overview),
                        color = NeonCyan,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    
                    // Streak Item
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(PriorityMedium.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = "Streak",
                                tint = PriorityMedium,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (streakDays > 0) stringResource(R.string.day_streak, streakDays) else stringResource(R.string.no_active_streak),
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (streakDays > 0) stringResource(R.string.streak_sub_active) else stringResource(R.string.streak_sub_inactive),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    // Focus Elapsed Time Item
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(NeonCyan.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Focus Time",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = totalTimeStr,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = stringResource(R.string.total_logged_focus),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    // Pomodoros Item
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(NeonPurple.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Pomodoros",
                                tint = NeonPurple,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.sessions_count, totalPomodoros),
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = stringResource(R.string.pomodoro_cycles_label),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Visual Quad-Metric Grid (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumMetricCard(
                    title = stringResource(R.string.metric_tasks_done),
                    value = "$completedTasks/$totalTasks",
                    icon = Icons.Default.CheckCircle,
                    color = NeonPurple,
                    subtitle = stringResource(R.string.metric_tasks_done_sub),
                    modifier = Modifier.weight(1f)
                )
                PremiumMetricCard(
                    title = stringResource(R.string.metric_success_rate),
                    value = "${(completionRatio * 100).toInt()}%",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = NeonCyan,
                    subtitle = stringResource(R.string.metric_success_rate_sub),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumMetricCard(
                    title = stringResource(R.string.metric_remaining),
                    value = pendingTasks.toString(),
                    icon = Icons.Default.Schedule,
                    color = TextSecondary,
                    subtitle = stringResource(R.string.metric_remaining_sub),
                    modifier = Modifier.weight(1f)
                )
                PremiumMetricCard(
                    title = stringResource(R.string.metric_high_priority),
                    value = highPriority.toString(),
                    icon = Icons.Default.PriorityHigh,
                    color = PriorityHigh,
                    subtitle = stringResource(R.string.metric_high_priority_sub),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Daily Focus Goal Progress Card
        DailyFocusGoalCard(viewModel = viewModel, todos = todos, selectedPeriod = selectedPeriod, freeSessionRecords = freeSessionRecords)

        // Free Focus Sessions Card
        FreeFocusSessionsCard(freeSessionRecords = freeSessionRecords.filter { it.timestamp in startMs..endMs })

        // Category Distribution Progress Widgets
        CategoryDistributionSection(todos, categories, startMs, endMs)

        // AI Intelligence Hub (Combined AI Performance & Focus Efficiency)
        LaunchedEffect(isAiEnabled) {
            if (isAiEnabled && aiInsight == null && !isAiLoading) {
                viewModel.fetchUnifiedAiHubInsight(todos, categories, selectedPeriod)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDark, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Intelligence Hub",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.ai_performance_consulting),
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Switch(
                        checked = isAiEnabled,
                        onCheckedChange = { 
                            isAiEnabled = it
                            if (it) {
                                viewModel.fetchUnifiedAiHubInsight(todos, categories, selectedPeriod)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPurple,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SlateDark
                        )
                    )
                }
                
                if (isAiEnabled) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // --- AI Unified Insight ---
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.performance_overview), color = NeonCyan, style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isAiLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                            Text(
                                stringResource(R.string.ai_generating_advisory),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateLight.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            val displayInsight = when (aiInsight) {
                                "ERROR_API_KEY" -> stringResource(R.string.ai_api_key_missing)
                                "ERROR_403" -> stringResource(R.string.error_403_proxy)
                                else -> aiInsight ?: stringResource(R.string.ai_no_insights_yet)
                            }
                            AiInsightContent(displayInsight)
                        }
                    }
                }
            }
        }
        
        // Activity by Hour Analytics Chart
        val hourlyData = remember(todos, categories, freeSessionRecords, startMs, endMs) {
            val hours = Array(24) { mutableMapOf<Int?, Int>() }
            todos.forEach { todo ->
                val records = RecordedTimeItem.parseList(todo.recordedTimes)
                records.filter { it.timestamp in startMs..endMs }.forEach { record ->
                    val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hours[hour][todo.categoryId] = (hours[hour][todo.categoryId] ?: 0) + 1
                }
            }
            // Include free mode sessions as null-category entries
            freeSessionRecords.filter { it.timestamp in startMs..endMs }.forEach { record ->
                val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                hours[hour][null] = (hours[hour][null] ?: 0) + 1
            }
            hours.toList()
        }
        
        if (hourlyData.any { it.isNotEmpty() }) {
            HourlyCategoryChart(
                title = stringResource(R.string.hourly_category_density),
                labels = listOf("0h", "4h", "8h", "12h", "16h", "20h", "24h"),
                hourlyData = hourlyData,
                categories = categories
            )
        }
        
        // Recursive Tasks Trackers & Averages
        val recursiveAverages = remember(todos, startMs, endMs) {
            todos.filter { it.isDaily }.mapNotNull { todo ->
                val records = RecordedTimeItem.parseList(todo.recordedTimes).filter { it.timestamp in startMs..endMs }
                if (records.isEmpty()) return@mapNotNull null
                
                val totalTime = records.filter { !it.isPomodoro }.sumOf { it.value }
                val pomodoros = records.filter { it.isPomodoro }.sumOf { it.value.toInt() }
                val daysActive = records.map { 
                    val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                }.distinct().size
                
                val avgTime = if (daysActive > 0) totalTime / daysActive else 0L
                val avgPomodoros = if (daysActive > 0) pomodoros / daysActive else 0
                
                Pair(todo.title, Pair(avgTime, avgPomodoros))
            }
        }
        
        if (recursiveAverages.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.recursive_performance_standards),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                recursiveAverages.forEach { (title, averages) ->
                    val (avgTime, avgPomodoros) = averages
                    val avgTimeStr = String.format("%02d:%02d", avgTime / 60000, (avgTime % 60000) / 1000)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark, RoundedCornerShape(16.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.avg_focus_text, avgTimeStr),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = stringResource(R.string.avg_pomodoros_text, avgPomodoros),
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Doomscrolling Insights Section
        DoomscrollingInsightsCard(viewModel = viewModel)
    }
}

@Composable
fun AiInsightContent(insightText: String) {
    val lines = remember(insightText) {
        insightText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        lines.forEach { line ->
            if (line.startsWith("###") || line.startsWith("##") || line.startsWith("#")) {
                val headerText = line.replace("#", "").trim()
                Text(
                    text = headerText,
                    color = NeonCyan,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            } else if (line.startsWith("-") || line.startsWith("*") || line.startsWith("•")) {
                val bulletText = line.substring(1).trim().replace("**", "")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 2.dp)
                    )
                    Text(
                        text = bulletText,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                val cleanLine = line.replace("**", "")
                Text(
                    text = cleanLine,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun PremiumMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SlateDark, RoundedCornerShape(20.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun CategoryDistributionSection(todos: List<TodoItem>, categories: List<Category>, startMs: Long, endMs: Long) {
    val uncategorizedLabel = stringResource(R.string.uncategorized_label)
    val categoryStats = remember(todos, categories, uncategorizedLabel, startMs, endMs) {
        val filteredTodos = todos.filter { 
            (it.completedDate != null && it.completedDate in startMs..endMs) ||
            (it.dueDate != null && it.dueDate in startMs..endMs) ||
            (!it.isCompleted && it.priority == 2)
        }
        val stats = categories.map { cat ->
            val total = filteredTodos.count { it.categoryId == cat.id }
            val completed = filteredTodos.count { it.categoryId == cat.id && it.isCompleted && (it.completedDate != null && it.completedDate in startMs..endMs) }
            val progress = if (total > 0) completed.toFloat() / total else 0f
            Triple(cat.name, safeParseColor(cat.colorHex, NeonCyan), Pair("$completed/$total", progress))
        }
        val uncategorizedTotal = filteredTodos.count { it.categoryId == null }
        val uncategorizedCompleted = filteredTodos.count { it.categoryId == null && it.isCompleted && (it.completedDate != null && it.completedDate in startMs..endMs) }
        val uncategorizedProgress = if (uncategorizedTotal > 0) uncategorizedCompleted.toFloat() / uncategorizedTotal else 0f
        if (uncategorizedTotal > 0) {
            stats + Triple(uncategorizedLabel, TextSecondary, Pair("$uncategorizedCompleted/$uncategorizedTotal", uncategorizedProgress))
        } else stats
    }
    val hasTaskData = remember(todos, categoryStats) {
        todos.isNotEmpty() && categoryStats.any {
            (it.third.first.split("/").lastOrNull()?.toIntOrNull() ?: 0) > 0
        }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.category_progress_distribution),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDark, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!hasTaskData) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(SlateLight.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, BorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.no_category_progress_data),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.no_category_progress_desc),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    categoryStats.forEach { (name, color, pair) ->
                        CategoryDistributionItem(name, color, pair.first, pair.second)
                    }
                }
            }
        }
    }
}

@Composable
fun DoomscrollingInsightsCard(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val records by viewModel.doomscrollRecords.collectAsState()
    val totalVideos by viewModel.totalDoomscrollVideosCount.collectAsState()
    val totalDurationMs by viewModel.totalDoomscrollDurationMs.collectAsState()
    val isPersian = Locale.getDefault().language == "fa"
    var isExpanded by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = SlateDark,
            title = {
                Text(
                    text = stringResource(R.string.clear_history),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.clear_history_confirm),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDoomscrollRecords()
                        showClearConfirm = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.clear_action),
                        color = PriorityHigh,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = TextSecondary
                    )
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SlateDark, RoundedCornerShape(24.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(NeonPurple.copy(alpha = 0.25f), PriorityHigh.copy(alpha = 0.25f))
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(NeonPurple, PriorityHigh)),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartDisplay,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.doomscrolling_insights_title),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(R.string.doomscrolling_insights_desc),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (records.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.clear_history),
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Stat Metrics Row (3 stats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Videos
                val videoCountText = if (isPersian) JalaliCalendarHelper.toPersianDigits(totalVideos.toString()) else totalVideos.toString()
                DoomscrollStatCard(
                    title = stringResource(R.string.doomscrolling_total_videos),
                    value = videoCountText,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                // Total Time Lost
                val totalTimeText = formatDurationShort(totalDurationMs, isPersian)
                DoomscrollStatCard(
                    title = stringResource(R.string.doomscrolling_total_time),
                    value = totalTimeText,
                    accentColor = PriorityHigh,
                    modifier = Modifier.weight(1.2f)
                )

                // Avg Time per Video
                val avgMs = if (totalVideos > 0) totalDurationMs / totalVideos else 0L
                val avgTimeText = if (totalVideos > 0) formatDurationShort(avgMs, isPersian) else "-"
                DoomscrollStatCard(
                    title = stringResource(R.string.doomscrolling_avg_time),
                    value = avgTimeText,
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
            }

            // History / Records list
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateLight.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.doomscrolling_no_records),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateLight.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    // Toggle Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.doomscrolling_history_title),
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val context = LocalContext.current
                        val todayLabel = stringResource(R.string.period_today)
                        val yesterdayLabel = stringResource(R.string.period_yesterday)
                        val dayTotalLabel = stringResource(R.string.doomscrolling_day_total)

                        val groupedByDay = remember(records, isPersian) {
                            val sorted = records.sortedByDescending { it.timestamp }
                            val now = Calendar.getInstance()
                            val todayStart = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L

                            sorted.groupBy { record ->
                                when {
                                    record.timestamp >= todayStart -> todayLabel
                                    record.timestamp >= yesterdayStart -> yesterdayLabel
                                    else -> {
                                        if (isPersian) {
                                            val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                                            val jd = JalaliCalendarHelper.gregorianToJalali(
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH) + 1,
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            )
                                            val monthName = JalaliCalendarHelper.JalaliDate.getMonthName(jd.month)
                                            JalaliCalendarHelper.toPersianDigits("${jd.day}") + " $monthName"
                                        } else {
                                            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(record.timestamp))
                                        }
                                    }
                                }
                            }.toList()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            groupedByDay.forEach { (dayLabel, dayRecords) ->
                                val dayTotalMs = dayRecords.sumOf { it.durationMs }
                                val sessionsCount = dayRecords.size
                                val sessionsCountStr = stringResource(R.string.doomscrolling_sessions_count, sessionsCount)

                                // Day header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(NeonPurple, CircleShape)
                                        )
                                        Text(
                                            text = dayLabel,
                                            color = TextPrimary,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "·",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = if (isPersian) JalaliCalendarHelper.toPersianDigits(sessionsCountStr) else sessionsCountStr,
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = NeonPurple.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = formatDuration(dayTotalMs, isPersian),
                                            color = NeonPurple,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Individual records within the day
                                val timeOnlyFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                                dayRecords.forEachIndexed { index, record ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SlateDark.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val sessionNumber = dayRecords.size - index
                                            val numberStr = if (isPersian) "#${JalaliCalendarHelper.toPersianDigits(sessionNumber.toString())}" else "#$sessionNumber"
                                            Text(
                                                text = numberStr,
                                                color = NeonPurple,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            val timeStr = timeOnlyFormat.format(Date(record.timestamp))
                                            val formattedTime = if (isPersian) JalaliCalendarHelper.toPersianDigits(timeStr) else timeStr
                                            Text(
                                                text = formattedTime,
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = PriorityHigh.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = formatDuration(record.durationMs, isPersian),
                                                color = PriorityHigh,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoomscrollStatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SlateLight.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = accentColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CategoryDistributionItem(name: String, color: Color, ratioText: String, progress: Float) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = ratioText,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(BorderColor.copy(alpha = 0.2f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.value)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                color.copy(alpha = 0.8f),
                                color
                            )
                        ),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun HistoryContent(todos: List<TodoItem>, categories: List<Category>) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("All") } // "All", "Completed Only", "Pending Only", "High Priority"
    var selectedFilterCategory by remember { mutableStateOf<Int?>(null) } // Category ID

    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }
    val speechManager = remember { SpeechRecognizerManager(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceErrorMessage = null
            speechManager.startListening(
                onResult = { text ->
                    searchQuery = text
                },
                onError = { error ->
                    voiceErrorMessage = error
                },
                onListeningStateChange = { listening ->
                    isListening = listening
                }
            )
        } else {
            Toast.makeText(context, context.getString(R.string.voice_permission_rationale), Toast.LENGTH_LONG).show()
        }
    }

    val toggleVoiceSearch = {
        if (isListening) {
            speechManager.stopListening()
            isListening = false
        } else {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                voiceErrorMessage = null
                speechManager.startListening(
                    onResult = { text ->
                        searchQuery = text
                    },
                    onError = { error ->
                        voiceErrorMessage = error
                    },
                    onListeningStateChange = { listening ->
                        isListening = listening
                    }
                )
            } else {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    DisposableEffect(speechManager) {
        onDispose {
            speechManager.stopListening()
        }
    }

    val filteredHistory = remember(todos, searchQuery, filterMode, selectedFilterCategory) {
        var list = todos
        
        // 1. Text Query Filter
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // 2. Completion Status & Priority Mode Filter
        list = when (filterMode) {
            "Completed Only" -> list.filter { it.isCompleted }
            "Pending Only" -> list.filter { !it.isCompleted }
            "High Priority" -> list.filter { it.priority == 2 }
            else -> list
        }
        
        // 3. Category Filter
        if (selectedFilterCategory != null) {
            list = list.filter { it.categoryId == selectedFilterCategory }
        }
        
        list
    }

    val isPersian = java.util.Locale.getDefault().language == "fa"
    val noDateStr = context.getString(R.string.no_date_history)
    val grouped = remember(filteredHistory, isPersian, noDateStr) {
        filteredHistory.groupBy {
            val dateMs = it.completedDate ?: it.dueDate
            if (dateMs != null) {
                if (isPersian) {
                    JalaliCalendarHelper.formatJalali(dateMs, includeTime = false, includeYear = false, includeWeekday = true)
                } else {
                    SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date(dateMs))
                }
            } else noDateStr
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search and Filter Controls Header
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Search Bar Text Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_timeline_placeholder), color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                        IconButton(onClick = toggleVoiceSearch) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = stringResource(R.string.voice_search_desc),
                                tint = if (isListening) NeonPurple else TextSecondary,
                                modifier = if (isListening) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "historySearchPulse")
                                    val scale by infiniteTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 1.3f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "historySearchMicScale"
                                    )
                                    Modifier.scale(scale)
                                } else Modifier
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SlateDark,
                    unfocusedContainerColor = SlateDark,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = BorderColor,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Status Filter Caps Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modes = listOf("All", "Completed Only", "Pending Only", "High Priority")
                modes.forEach { mode ->
                    val isSelected = filterMode == mode
                    val modeLabel = when (mode) {
                        "All" -> stringResource(R.string.tab_all)
                        "Completed Only" -> stringResource(R.string.filter_completed_only)
                        "Pending Only" -> stringResource(R.string.filter_pending_only)
                        "High Priority" -> stringResource(R.string.filter_high_priority)
                        else -> mode
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else SlateDark)
                            .border(
                                BorderStroke(1.dp, if (isSelected) NeonCyan else BorderColor),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { filterMode = mode }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modeLabel,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        )
                    }
                }
            }

            // Categories Filter Caps Row
            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "All Categories" chip
                    val isAllCatsSelected = selectedFilterCategory == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isAllCatsSelected) NeonPurple.copy(alpha = 0.25f) else SlateDark)
                            .border(
                                BorderStroke(1.dp, if (isAllCatsSelected) NeonPurple else BorderColor),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilterCategory = null }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(TextSecondary, CircleShape))
                            Text(
                                stringResource(R.string.filter_all_categories),
                                color = if (isAllCatsSelected) NeonPurple else TextSecondary,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isAllCatsSelected) FontWeight.Bold else FontWeight.Normal)
                            )
                        }
                    }

                    // Individual category chips
                    categories.forEach { cat ->
                        val isCatSelected = selectedFilterCategory == cat.id
                        val catColor = safeParseColor(cat.colorHex, NeonPurple)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isCatSelected) catColor.copy(alpha = 0.25f) else SlateDark)
                                .border(
                                    BorderStroke(1.dp, if (isCatSelected) catColor else BorderColor),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedFilterCategory = if (isCatSelected) null else cat.id
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(catColor, CircleShape))
                                Text(
                                    text = cat.name,
                                    color = if (isCatSelected) catColor else TextSecondary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Chronological Timeline Journal
        if (grouped.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BorderColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_history_available),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.no_history_desc),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            grouped.forEach { (dateStr, items) ->
                // Date Header
                Text(
                    text = dateStr, 
                    color = NeonCyan, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                // Timeline Stack for this Date
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            verticalAlignment = Alignment.Top
                        ) {
                            // 1. Left Vertical Timeline Dot & Track Indicator
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                // Background vertical line connecting cards
                                // We hide the top part of the line for the first item of the day
                                // and the bottom part of the line for the last item of the day
                                // Actually, for simplicity and standard timeline look:
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(2.dp)
                                        .background(BorderColor.copy(alpha = 0.3f))
                                )
                                
                                // Timeline Node Circle
                                Box(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .size(24.dp)
                                        .background(
                                            if (item.isCompleted) NeonPurple.copy(alpha = 0.15f) else SlateDark,
                                            CircleShape
                                        )
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (item.isCompleted) NeonPurple else NeonCyan
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.isCompleted) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (item.isCompleted) NeonPurple else NeonCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // 2. Timeline Content Card
                            TimelineHistoryCard(item = item, categories = categories)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineHistoryCard(item: TodoItem, categories: List<Category>) {
    val cat = categories.find { it.id == item.categoryId }
    val catColor = if (cat != null) safeParseColor(cat.colorHex, Color.Gray) else Color.Transparent
    
    val priorityColor = when (item.priority) {
        2 -> PriorityHigh
        1 -> PriorityMedium
        else -> PriorityLow
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = 12.dp)
            .background(SlateDark, RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (item.priority == 2 && !item.isCompleted) PriorityHigh.copy(alpha = 0.35f) else BorderColor,
                RoundedCornerShape(18.dp)
            )
    ) {
        // Vertical Priority stripe indicator on the left edge (aligned perfectly using IntrinsicSize.Min)
        if (!item.isCompleted) {
            val prioritySecondaryColor = when (item.priority) {
                2 -> Color(0xFFF43F5E)
                1 -> Color(0xFFD97706)
                else -> Color(0xFF059669)
            }
            Box(
                modifier = Modifier
                    .padding(vertical = 14.dp)
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                priorityColor,
                                prioritySecondaryColor
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = if (!item.isCompleted) 13.dp else 18.dp,
                    top = 14.dp,
                    end = 18.dp,
                    bottom = 14.dp
                )
        ) {
            // Header Row (Category Name & Completion/Due Timestamp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Pill/Badge
                if (cat != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(catColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, catColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(catColor, CircleShape)
                        )
                        Text(
                            text = cat.name,
                            color = catColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(TextMuted.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, TextMuted.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(TextMuted, CircleShape)
                        )
                        Text(
                            text = stringResource(R.string.uncategorized_label),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Completion/Due Timestamp
                val dateMs = item.completedDate ?: item.dueDate
                if (dateMs != null) {
                    val isPersianTime = java.util.Locale.getDefault().language == "fa"
                    val timeStr = if (isPersianTime) {
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = dateMs }
                        val isAm = cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM
                        val amPmStr = if (isAm) "ق.ظ" else "ب.ظ"
                        val h = cal.get(java.util.Calendar.HOUR)
                        val hour = if (h == 0) 12 else h
                        val m = cal.get(java.util.Calendar.MINUTE)
                        JalaliCalendarHelper.toPersianDigits(String.format(java.util.Locale.US, "%d:%02d %s", hour, m, amPmStr))
                    } else {
                        val sdf = SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                        sdf.format(Date(dateMs))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (item.isCompleted) NeonPurple else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (item.isCompleted) stringResource(R.string.done_at_time, timeStr) else stringResource(R.string.due_at_time, timeStr),
                            color = if (item.isCompleted) NeonPurple else TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task title & Urgent star indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.priority == 2 && !item.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Urgent",
                        tint = PriorityHigh,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = item.title,
                    color = if (item.isCompleted) TextPrimary.copy(alpha = 0.6f) else TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp,
                        textDecoration = if (item.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Description with better line spacing
            if (item.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.description,
                    color = if (item.isCompleted) TextSecondary.copy(alpha = 0.6f) else TextSecondary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp
                    ),
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Badges Footer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Badge
                val priorityText: String
                val priorityBgColor: Color
                val priorityTextColor: Color
                val priorityIcon: androidx.compose.ui.graphics.vector.ImageVector

                when (item.priority) {
                    2 -> {
                        priorityText = stringResource(R.string.urgent)
                        priorityBgColor = PriorityHigh.copy(alpha = 0.15f)
                        priorityTextColor = PriorityHigh
                        priorityIcon = Icons.Default.PriorityHigh
                    }
                    1 -> {
                        priorityText = stringResource(R.string.priority_medium)
                        priorityBgColor = PriorityMedium.copy(alpha = 0.15f)
                        priorityTextColor = PriorityMedium
                        priorityIcon = Icons.Default.Flag
                    }
                    else -> {
                        priorityText = stringResource(R.string.normal_priority)
                        priorityBgColor = PriorityLow.copy(alpha = 0.15f)
                        priorityTextColor = PriorityLow
                        priorityIcon = Icons.Default.Flag
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(priorityBgColor)
                        .border(1.dp, priorityTextColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = priorityIcon,
                            contentDescription = null,
                            tint = priorityTextColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = priorityText,
                            color = priorityTextColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Focus Time Logging Badges
                if (item.elapsedTime > 0 || item.pomodoroCycles > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.pomodoroCycles > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPurple.copy(alpha = 0.12f))
                                    .border(1.dp, NeonPurple.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = NeonPurple,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.pomodoros_badge, item.pomodoroCycles),
                                        color = NeonPurple,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        if (item.elapsedTime >= 60000) {
                            val minutes = item.elapsedTime / 60000
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonCyan.copy(alpha = 0.12f))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.focused_time_badge, minutes),
                                        color = NeonCyan,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                // Daily/Repeat Badge
                if (item.isDaily) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.12f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.daily_badge),
                                color = NeonCyan,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyFocusGoalCard(viewModel: TodoViewModel, todos: List<TodoItem>, selectedPeriod: String = "Overall", freeSessionRecords: List<RecordedTimeItem> = emptyList()) {
    val dailyFocusGoalHours by viewModel.dailyFocusGoalHours.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    val periodTimeRange = remember(selectedPeriod) {
        val now = Calendar.getInstance()
        val startOfToday = now.clone() as Calendar
        startOfToday.set(Calendar.HOUR_OF_DAY, 0)
        startOfToday.set(Calendar.MINUTE, 0)
        startOfToday.set(Calendar.SECOND, 0)
        startOfToday.set(Calendar.MILLISECOND, 0)
        val startOfTodayMs = startOfToday.timeInMillis

        when (selectedPeriod) {
            "Yesterday" -> {
                val start = Calendar.getInstance().apply { timeInMillis = startOfTodayMs; add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
                Triple(start, startOfTodayMs - 1, 1)
            }
            "Last Week" -> {
                val start = Calendar.getInstance().apply { timeInMillis = startOfTodayMs; add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                Triple(start, Long.MAX_VALUE, 7)
            }
            "Last Month" -> {
                val start = Calendar.getInstance().apply { timeInMillis = startOfTodayMs; add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
                Triple(start, Long.MAX_VALUE, 30)
            }
            else -> { // Overall / Today
                Triple(startOfTodayMs, Long.MAX_VALUE, 1)
            }
        }
    }

    val (startMs, endMs, daysInPeriod) = periodTimeRange

    val focusLengthMs by viewModel.focusLength.collectAsState()

    val focusTimeMs = remember(todos, startMs, endMs, freeSessionRecords, focusLengthMs) {
        var totalMs = 0L
        todos.forEach { todo ->
            val records = RecordedTimeItem.parseList(todo.recordedTimes)
            val periodRecords = records.filter { it.timestamp in startMs..endMs }
            if (periodRecords.isNotEmpty()) {
                periodRecords.forEach { rec ->
                    if (rec.isPomodoro) {
                        // Each pomodoro cycle = focusLength duration
                        totalMs += rec.value * focusLengthMs
                    } else {
                        totalMs += rec.value
                    }
                }
            } else if (todo.elapsedTime > 0) {
                val checkTime = todo.completedDate ?: todo.dueDate ?: 0L
                if (checkTime in startMs..endMs) {
                    totalMs += todo.elapsedTime
                }
            }
        }
        // Include ALL free mode sessions (both stopwatch and pomodoro) in the period
        freeSessionRecords.filter { it.timestamp in startMs..endMs }.forEach { record ->
            if (record.isPomodoro) {
                totalMs += record.value * focusLengthMs
            } else {
                totalMs += record.value
            }
        }
        totalMs
    }

    val focusHours = focusTimeMs.toFloat() / (1000f * 60f * 60f)
    val effectiveTargetHours = dailyFocusGoalHours * daysInPeriod
    val progressFraction = if (effectiveTargetHours > 0f) (focusHours / effectiveTargetHours).coerceIn(0f, 1f) else 0f

    val animatedGoalProgress = remember { Animatable(0f) }
    LaunchedEffect(progressFraction) {
        animatedGoalProgress.animateTo(
            targetValue = progressFraction,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val loggedLabel = when (selectedPeriod) {
        "Yesterday" -> stringResource(R.string.logged_yesterday)
        "Last Week" -> stringResource(R.string.logged_last_week)
        "Last Month" -> stringResource(R.string.logged_last_month)
        else -> stringResource(R.string.logged_today)
    }

    if (showEditDialog) {
        var tempGoal by remember { mutableFloatStateOf(dailyFocusGoalHours) }
        AppAlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.set_daily_focus_goal),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${if (tempGoal % 1f == 0f) tempGoal.toInt().toString() else String.format(java.util.Locale.getDefault(), "%.1f", tempGoal)} ${stringResource(R.string.hours_unit)}",
                        color = NeonCyan,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Slider(
                        value = tempGoal,
                        onValueChange = { tempGoal = (it * 2).toInt() / 2f },
                        valueRange = 0.5f..12.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = SlateLight
                        )
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1.0f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f).chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { preset ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (tempGoal == preset) NeonCyan else SlateDark)
                                            .border(1.dp, if (tempGoal == preset) NeonCyan else BorderColor, RoundedCornerShape(8.dp))
                                            .clickable { tempGoal = preset }
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.hours_short, preset.toInt()),
                                            color = if (tempGoal == preset) ObsidianBlack else TextPrimary,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setDailyFocusGoalHours(tempGoal)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(stringResource(R.string.save_goal), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SlateDark,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateDark, RoundedCornerShape(24.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(NeonCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = stringResource(R.string.daily_focus_goal),
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.daily_focus_goal),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val targetStr = if (dailyFocusGoalHours % 1f == 0f) "${dailyFocusGoalHours.toInt()}h" else "${dailyFocusGoalHours}h"
                        Text(
                            text = stringResource(R.string.target_per_day, targetStr),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Surface(
                    onClick = { showEditDialog = true },
                    color = NeonCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Text(
                            text = stringResource(R.string.adjust_goal),
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Body Content Row: Circular Progress Ring + Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Circular Progress Ring Gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(115.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 11.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2

                        // Outer Track
                        drawCircle(
                            color = SlateLight.copy(alpha = 0.35f),
                            radius = radius,
                            style = Stroke(width = strokeWidth)
                        )

                        // Animated Sweep Arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    NeonCyan,
                                    NeonPurple,
                                    NeonCyan
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = animatedGoalProgress.value * 360f,
                            useCenter = false,
                            topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val percent = (progressFraction * 100).toInt()
                        Text(
                            text = "$percent%",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            fontSize = 22.sp
                        )
                        Text(
                            text = if (focusHours >= effectiveTargetHours) stringResource(R.string.goal_achieved) else stringResource(R.string.goal_label),
                            color = if (focusHours >= effectiveTargetHours) NeonCyan else TextSecondary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

                // Statistics Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val focusHoursInt = (focusTimeMs / 3600000).toInt()
                    val focusMinsInt = ((focusTimeMs % 3600000) / 60000).toInt()
                    val loggedStr = if (focusHoursInt > 0) "${focusHoursInt}h ${focusMinsInt}m" else "${focusMinsInt}m"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonCyan, CircleShape)
                        )
                        Column {
                            Text(loggedLabel, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(loggedStr, color = TextPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    val goalMinsTotal = (effectiveTargetHours * 60).toInt()
                    val goalHoursInt = goalMinsTotal / 60
                    val goalMinsInt = goalMinsTotal % 60
                    val goalStr = if (goalMinsInt > 0) "${goalHoursInt}h ${goalMinsInt}m" else "${goalHoursInt}h"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeonPurple, CircleShape)
                        )
                        Column {
                            Text(stringResource(R.string.target_goal), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(goalStr, color = TextPrimary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    val remainingMs = ((effectiveTargetHours * 3600000) - focusTimeMs).coerceAtLeast(0f).toLong()
                    val remHoursInt = (remainingMs / 3600000).toInt()
                    val remMinsInt = ((remainingMs % 3600000) / 60000).toInt()
                    val remainingStr = if (remainingMs <= 0) {
                        stringResource(R.string.goal_achieved_status)
                    } else {
                        val leftTimeStr = if (remHoursInt > 0) "${remHoursInt}h ${remMinsInt}m" else "${remMinsInt}m"
                        stringResource(R.string.time_left_status, leftTimeStr)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (remainingMs <= 0) NeonCyan else PriorityMedium, CircleShape)
                        )
                        Column {
                            Text(stringResource(R.string.status_label), color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                            Text(remainingStr, color = if (remainingMs <= 0) NeonCyan else PriorityMedium, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Quick Preset Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.quick_presets),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1.0f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f).chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { preset ->
                                val isSelected = dailyFocusGoalHours == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) NeonCyan else SlateLight.copy(alpha = 0.3f))
                                        .border(1.dp, if (isSelected) NeonCyan else BorderColor, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setDailyFocusGoalHours(preset) }
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.hours_short, preset.toInt()),
                                        color = if (isSelected) ObsidianBlack else TextPrimary,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
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
fun FreeFocusSessionsCard(freeSessionRecords: List<RecordedTimeItem>) {
    val stopwatchRecords = freeSessionRecords.filter { !it.isPomodoro }
    val pomodoroRecords = freeSessionRecords.filter { it.isPomodoro }
    
    val totalFreeTime = stopwatchRecords.sumOf { it.value }
    val totalFreePomodoros = pomodoroRecords.sumOf { it.value.toInt() }
    
    val freeTimeHrs = totalFreeTime / 3600000
    val freeTimeMins = (totalFreeTime % 3600000) / 60000
    val freeTimeSecs = (totalFreeTime % 60000) / 1000
    val freeTimeStr = when {
        freeTimeHrs > 0 -> "${freeTimeHrs}h ${freeTimeMins}m"
        freeTimeMins > 0 -> "${freeTimeMins}m ${freeTimeSecs}s"
        else -> "${freeTimeSecs}s"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.free_focus_sessions),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDark, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            if (freeSessionRecords.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(SlateLight.copy(alpha = 0.5f), CircleShape)
                            .border(1.dp, BorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.no_free_sessions_yet),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.no_free_sessions_desc),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Summary row with totals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Free Focus Time chip
                        if (totalFreeTime > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassBottom,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.free_focus_total_time),
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = freeTimeStr,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        // Free Pomodoro Cycles chip
                        if (totalFreePomodoros > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(NeonPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(1.dp, NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = null,
                                            tint = NeonPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.free_pomodoro_cycles),
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$totalFreePomodoros",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // Session count header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.tab_history),
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val sessionsText = if (freeSessionRecords.size == 1) {
                            stringResource(R.string.free_sessions_count, freeSessionRecords.size)
                        } else {
                            stringResource(R.string.free_sessions_count_plural, freeSessionRecords.size)
                        }
                        Text(
                            text = sessionsText,
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    // Recent sessions list (last 5)
                    val recentSessions = freeSessionRecords.reversed().take(5)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentSessions.forEachIndexed { index, record ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateLight.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.free_session_label, freeSessionRecords.size - index),
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
                                    color = if (record.isPomodoro) NeonPurple else NeonCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
