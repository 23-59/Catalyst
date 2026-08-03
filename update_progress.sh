#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/ProgressScreen.kt
package com.example.ui

import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.data.TodoItem
import com.example.data.Category
import com.example.data.RecordedTimeItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.testTag

@Composable
fun ProgressScreen(
    viewModel: TodoViewModel,
    todos: List<TodoItem>,
    categories: List<Category> = emptyList(),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("Insights") }
    
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
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .shadow(1.dp, RoundedCornerShape(14.dp))
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
            Column {
                Text(
                    text = "Insights & History",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = TextPrimary
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(56.dp)
                .background(SlateDark, RoundedCornerShape(28.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(28.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("Insights", "History")
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isSelected) Brush.horizontalGradient(listOf(NeonCyan.copy(alpha=0.2f), NeonPurple.copy(alpha=0.2f))) else SolidColor(Color.Transparent)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            brush = if (isSelected) Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)) else SolidColor(Color.Transparent),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable { selectedTab = tab },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        
        if (selectedTab == "Insights") {
            InsightsContent(viewModel, todos, categories)
        } else {
            HistoryContent(todos)
        }
    }
}

@Composable
fun InsightsContent(viewModel: TodoViewModel, todos: List<TodoItem>, categories: List<Category>) {
    var selectedPeriod by remember { mutableStateOf("Yesterday") }
    val periods = listOf("Overall", "Yesterday", "Last Week", "Last Month")

    val periodStats = remember(todos, selectedPeriod) {
        val now = Calendar.getInstance()
        val startOfToday = now.clone() as Calendar
        startOfToday.set(Calendar.HOUR_OF_DAY, 0)
        startOfToday.set(Calendar.MINUTE, 0)
        startOfToday.set(Calendar.SECOND, 0)
        startOfToday.set(Calendar.MILLISECOND, 0)
        val startOfTodayMs = startOfToday.timeInMillis

        val (startMs, endMs) = when (selectedPeriod) {
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

        val filteredTodos = if (selectedPeriod == "Overall") todos else todos.filter {
            (it.completedDate != null && it.completedDate in startMs..endMs) ||
            (it.dueDate != null && it.dueDate in startMs..endMs) ||
            (!it.isCompleted && it.dueDate == null)
        }

        val totalTasks = filteredTodos.size
        val completedTasks = filteredTodos.count { it.isCompleted && (selectedPeriod == "Overall" || (it.completedDate != null && it.completedDate in startMs..endMs)) }
        val pendingTasks = totalTasks - completedTasks
        val highPriority = filteredTodos.count { !it.isCompleted && it.priority == 2 }
        val completionRatio = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
        
        mapOf(
            "total" to totalTasks,
            "completed" to completedTasks,
            "pending" to pendingTasks,
            "highPriority" to highPriority,
            "ratio" to completionRatio
        )
    }

    val totalTasks = periodStats["total"] as Int
    val completedTasks = periodStats["completed"] as Int
    val pendingTasks = periodStats["pending"] as Int
    val highPriority = periodStats["highPriority"] as Int
    val completionRatio = periodStats["ratio"] as Float

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(completionRatio) {
        animatedProgress.animateTo(
            targetValue = completionRatio,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }
    
    // AI Insights State
    val aiInsight by viewModel.aiInsight.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var isAiEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Period Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            periods.forEach { period ->
                val isSelected = selectedPeriod == period
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPeriod = period },
                    label = { Text(period) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SlateDark,
                        labelColor = TextSecondary,
                        selectedContainerColor = NeonPurple.copy(alpha = 0.2f),
                        selectedLabelColor = NeonPurple
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = BorderColor,
                        selectedBorderColor = NeonPurple
                    )
                )
            }
        }

        // Overall Progress Circle
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp).padding(top = 16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawArc(
                    color = SlateDark,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
                // Progress Arc
                drawArc(
                    brush = Brush.sweepGradient(listOf(NeonCyan, NeonPurple, NeonCyan)),
                    startAngle = -90f,
                    sweepAngle = animatedProgress.value * 360f,
                    useCenter = false,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${(animatedProgress.value * 100).toInt()}%",
                    color = TextPrimary,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$selectedPeriod Progress",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InsightMiniCard("Completed", completedTasks.toString(), Icons.Default.CheckCircle, NeonPurple, Modifier.weight(1f))
            InsightMiniCard("Success", "${(completionRatio * 100).toInt()}%", Icons.Default.TrendingUp, NeonCyan, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InsightMiniCard("Pending", pendingTasks.toString(), Icons.Default.Schedule, TextSecondary, Modifier.weight(1f))
            InsightMiniCard("High Priority", highPriority.toString(), Icons.Default.PriorityHigh, PriorityHigh, Modifier.weight(1f))
        }
        
        // AI Insights Toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDark, RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Progress Analysis", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = isAiEnabled,
                        onCheckedChange = { 
                            isAiEnabled = it
                            if (it) {
                                viewModel.fetchAiInsight(todos, categories, selectedPeriod)
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
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isAiLoading) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Text(
                            text = aiInsight ?: "No insights generated.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Activity by Hour Chart
        val hourlyData = remember(todos) {
            val hours = IntArray(24) { 0 }
            todos.forEach { todo ->
                val records = RecordedTimeItem.parseList(todo.recordedTimes)
                records.forEach { record ->
                    val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hours[hour]++
                }
            }
            hours.toList()
        }
        
        if (hourlyData.any { it > 0 }) {
            ChartBox(
                title = "Activity by Hour of Day",
                labels = listOf("0h", "4h", "8h", "12h", "16h", "20h", "24h"),
                values = hourlyData.map { it.toFloat() },
                isHourly = true
            )
        }
        
        // Recursive Tasks Averages
        val recursiveAverages = remember(todos) {
            todos.filter { it.isDaily }.map { todo ->
                val records = RecordedTimeItem.parseList(todo.recordedTimes)
                val totalTime = todo.elapsedTime
                val pomodoros = todo.pomodoroCycles
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Recursive Tasks Averages",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )
                recursiveAverages.forEach { (title, averages) ->
                    val (avgTime, avgPomodoros) = averages
                    val avgTimeStr = String.format("%02d:%02d", avgTime / 60000, (avgTime % 60000) / 1000)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(SlateDark, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$avgTimeStr avg time", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Text("$avgPomodoros avg pomodoros", color = NeonCyan, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun InsightMiniCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SlateDark, RoundedCornerShape(20.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ChartBox(title: String, labels: List<String>, values: List<Float>, isHourly: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(SlateDark, RoundedCornerShape(24.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(title, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
                
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridLines = 4
                        val lineSpacing = size.height / gridLines
                        for (i in 0..gridLines) {
                            drawLine(
                                color = BorderColor.copy(alpha = 0.5f),
                                start = Offset(0f, i * lineSpacing),
                                end = Offset(size.width, i * lineSpacing),
                                strokeWidth = 1f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                        
                        val maxVal = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                        val barSpacing = size.width / (values.size + 1)
                        
                        values.forEachIndexed { index, value ->
                            val x = barSpacing * (index + 1)
                            val valHeight = value / maxVal
                            val barHeight = size.height * valHeight
                            
                            val barWidth = if (isHourly) 8f else 24f
                            
                            // Bar background
                            drawRoundRect(
                                color = BorderColor,
                                topLeft = Offset(x - (barWidth/2), 0f),
                                size = Size(barWidth, size.height),
                                cornerRadius = CornerRadius(barWidth/2)
                            )
                            
                            // Bar foreground
                            if (barHeight > 0) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(listOf(NeonCyan, NeonPurple)),
                                    topLeft = Offset(x - (barWidth/2), size.height - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(barWidth/2)
                                )
                            }
                        }
                    }
                }
                
                // Labels
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { label ->
                        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(name, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Text(ratioText, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(8.dp).background(BorderColor, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.value)
                    .fillMaxHeight()
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun HistoryContent(todos: List<TodoItem>) {
    val grouped = remember(todos) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        todos.groupBy { 
            if (it.dueDate != null) sdf.format(Date(it.dueDate)) else "No Date"
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        
        if (grouped.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = BorderColor, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.no_history_available), color = TextSecondary)
                }
            }
        } else {
            grouped.forEach { (dateStr, items) ->
                Text(
                    text = dateStr, 
                    color = NeonCyan, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateDark, RoundedCornerShape(24.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        .padding(vertical = 8.dp)
                ) {
                    Column {
                        items.forEachIndexed { index, item ->
                            HistoryItem(item)
                            if (index < items.size - 1) {
                                HorizontalDivider(color = BorderColor, thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun HistoryItem(item: TodoItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (item.isCompleted) NeonPurple.copy(alpha=0.15f) else BorderColor.copy(alpha=0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, 
                contentDescription = null,
                tint = if (item.isCompleted) NeonPurple else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            if (item.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
INNER_EOF
