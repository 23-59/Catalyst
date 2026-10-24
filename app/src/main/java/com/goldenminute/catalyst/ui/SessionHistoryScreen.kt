package com.goldenminute.catalyst.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.data.RecordedTimeItem
import com.goldenminute.catalyst.data.TodoItem
import com.goldenminute.catalyst.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryPeriodFilter {
    ALL, TODAY, THIS_WEEK
}

data class SessionRecordIndexed(
    val originalIndex: Int,
    val record: RecordedTimeItem
)

/**
 * Dedicated full-screen Session History view for a task's recorded focus sessions.
 * Displays all sessions grouped by day, provides time period filters, summary stats, and session deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    todo: TodoItem,
    onBack: () -> Unit,
    onDeleteRecord: (Int) -> Unit, // original index of record in todo.recordedTimes
    isPersian: Boolean = false,
    viewModel: TodoViewModel? = null
) {
    val currentContext = androidx.compose.ui.platform.LocalContext.current
    val currentLocaleLang = currentContext.resources.configuration.locales[0].language
    val effectiveIsPersian = isPersian || currentLocaleLang == "fa" || Locale.getDefault().language == "fa"

    // Parse all records and keep track of original indices
    val allRecords = remember(todo.recordedTimes) {
        RecordedTimeItem.parseList(todo.recordedTimes).mapIndexed { index, record ->
            SessionRecordIndexed(index, record)
        }
    }

    // Filter selection (Default to ALL so user sees all recorded history)
    var selectedFilter by remember { mutableStateOf(HistoryPeriodFilter.ALL) }

    // Time boundaries
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val startOfYesterday = remember(startOfToday) {
        Calendar.getInstance().apply {
            timeInMillis = startOfToday
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
    }

    val startOfWeek = remember(effectiveIsPersian) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val firstDay = if (effectiveIsPersian) Calendar.SATURDAY else Calendar.MONDAY
            val currentDay = get(Calendar.DAY_OF_WEEK)
            val diff = (currentDay - firstDay + 7) % 7
            add(Calendar.DAY_OF_YEAR, -diff)
        }.timeInMillis
    }

    // Filter records based on selected tab/chip
    val filteredRecords = remember(allRecords, selectedFilter, startOfToday, startOfWeek) {
        when (selectedFilter) {
            HistoryPeriodFilter.ALL -> allRecords
            HistoryPeriodFilter.TODAY -> allRecords.filter { it.record.timestamp >= startOfToday }
            HistoryPeriodFilter.THIS_WEEK -> allRecords.filter { it.record.timestamp >= startOfWeek }
        }
    }

    // Calculate stats based on filtered records
    val totalStopwatchMs = remember(filteredRecords) {
        filteredRecords.filter { !it.record.isPomodoro }.sumOf { it.record.value }
    }
    val totalPomodoroCycles = remember(filteredRecords) {
        filteredRecords.filter { it.record.isPomodoro }.sumOf { it.record.value.toInt() }
    }
    val totalSessionsCount = remember(filteredRecords) {
        filteredRecords.size
    }
    val longestSessionMs = remember(filteredRecords) {
        filteredRecords.filter { !it.record.isPomodoro }.maxOfOrNull { it.record.value } ?: 0L
    }

    // Group records by calendar day (startOfDayMs) descending
    val groupedRecordsByDay = remember(filteredRecords) {
        filteredRecords
            .sortedByDescending { it.record.timestamp }
            .groupBy { item ->
                Calendar.getInstance().apply {
                    timeInMillis = item.record.timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
    }

    val appPackage = remember(todo.associatedPackageNames) {
        todo.associatedPackageNames
    }

    val doomscrollRecords = viewModel?.doomscrollRecords?.collectAsState()?.value ?: emptyList()

    val daysWithDoomscroll = remember(doomscrollRecords, appPackage) {
        if (appPackage.isNullOrEmpty()) emptyList()
        else {
            val matching = doomscrollRecords.filter {
                appPackage.contains(it.packageName, ignoreCase = true) || it.packageName.contains("instagram", ignoreCase = true)
            }
            matching.groupBy { rec ->
                Calendar.getInstance().apply {
                    timeInMillis = rec.timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }.keys.toList()
        }
    }

    val canRestoreFromDoomscroll = remember(allRecords, daysWithDoomscroll) {
        daysWithDoomscroll.size >= 2 &&
            allRecords.count { !it.record.isPomodoro } <= 2 &&
            allRecords.any { !it.record.isPomodoro && it.record.value >= 10 * 60 * 1000L }
    }

    var recordToSplit by remember { mutableStateOf<SessionRecordIndexed?>(null) }
    var splitDaysCount by remember { mutableStateOf(3) }

    val unifiedSections = remember(filteredRecords) {
        val map = mutableMapOf<String, Long>()
        for (item in filteredRecords) {
            for ((sec, time) in item.record.appSections) {
                map[sec] = (map[sec] ?: 0L) + time
            }
        }
        if (map.keys.any { it != "Unknown" && it != "General" }) {
            map.filterKeys { it != "Unknown" && it != "General" }
        } else {
            map.filterKeys { it != "Unknown" }
        }
    }

    // Delete confirmation
    var recordToDelete by remember { mutableStateOf<SessionRecordIndexed?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = todo.title.ifBlank { stringResource(R.string.session_history_title) },
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.session_history_title),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (allRecords.any { !it.record.isPomodoro && it.record.value >= 10 * 60 * 1000L }) {
                        IconButton(
                            onClick = {
                                val largest = allRecords.filter { !it.record.isPomodoro }.maxByOrNull { it.record.value }
                                if (largest != null) {
                                    recordToSplit = largest
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                contentDescription = stringResource(R.string.split_session_title),
                                tint = NeonCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Summary stat cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val focusTimeFormatted = when {
                        totalStopwatchMs > 0 && totalPomodoroCycles > 0 -> {
                            "${formatDurationShort(totalStopwatchMs, effectiveIsPersian)} + ${if (effectiveIsPersian) "${JalaliCalendarHelper.toPersianDigits(totalPomodoroCycles.toString())} دور" else "$totalPomodoroCycles c"}"
                        }
                        totalStopwatchMs > 0 -> formatDurationShort(totalStopwatchMs, effectiveIsPersian)
                        totalPomodoroCycles > 0 -> if (effectiveIsPersian) "${JalaliCalendarHelper.toPersianDigits(totalPomodoroCycles.toString())} دور" else "$totalPomodoroCycles cycles"
                        else -> "0s"
                    }

                    SummaryStatCard(
                        label = stringResource(R.string.today_focus_time),
                        value = focusTimeFormatted,
                        subtitle = when (selectedFilter) {
                            HistoryPeriodFilter.ALL -> stringResource(R.string.session_history_total_all)
                            HistoryPeriodFilter.TODAY -> stringResource(R.string.session_history_total_today)
                            HistoryPeriodFilter.THIS_WEEK -> stringResource(R.string.session_history_total_week)
                        },
                        color = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = stringResource(R.string.today_sessions_count),
                        value = if (effectiveIsPersian) JalaliCalendarHelper.toPersianDigits(totalSessionsCount.toString()) else totalSessionsCount.toString(),
                        subtitle = if (totalSessionsCount == 1) {
                            if (effectiveIsPersian) "۱ جلسه" else "1 session"
                        } else {
                            if (effectiveIsPersian) "${JalaliCalendarHelper.toPersianDigits(totalSessionsCount.toString())} جلسه" else "$totalSessionsCount sessions"
                        },
                        color = NeonPurple,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatCard(
                        label = stringResource(R.string.longest_session),
                        value = if (longestSessionMs > 0) formatDurationShort(longestSessionMs, effectiveIsPersian) else if (totalPomodoroCycles > 0) (if (effectiveIsPersian) "پومودورو" else "Pomodoro") else "-",
                        subtitle = stringResource(R.string.best_record),
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 1.5 Reconstruct Banner (if multiple days of activity detected for a combined session)
            if (canRestoreFromDoomscroll) {
                item(key = "reconstruct_activity_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateDark.copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.reconstruct_daily_sessions),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.reconstruct_daily_sessions_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel?.reconstructSessionsFromActivity(todo.id) { success, count ->
                                        if (success) {
                                            val msg = currentContext.getString(R.string.reconstruct_success, count)
                                            android.widget.Toast.makeText(currentContext, msg, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = ObsidianBlack
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.reconstruct_daily_sessions),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Filter chips row (All / Today / This Week)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryFilterChip(
                        label = stringResource(R.string.session_history_filter_all),
                        count = allRecords.size,
                        isSelected = selectedFilter == HistoryPeriodFilter.ALL,
                        isPersian = effectiveIsPersian,
                        onClick = { selectedFilter = HistoryPeriodFilter.ALL },
                        modifier = Modifier.weight(1f)
                    )
                    HistoryFilterChip(
                        label = stringResource(R.string.session_history_total_today),
                        count = allRecords.count { it.record.timestamp >= startOfToday },
                        isSelected = selectedFilter == HistoryPeriodFilter.TODAY,
                        isPersian = effectiveIsPersian,
                        onClick = { selectedFilter = HistoryPeriodFilter.TODAY },
                        modifier = Modifier.weight(1f)
                    )
                    HistoryFilterChip(
                        label = stringResource(R.string.session_history_total_week),
                        count = allRecords.count { it.record.timestamp >= startOfWeek },
                        isSelected = selectedFilter == HistoryPeriodFilter.THIS_WEEK,
                        isPersian = effectiveIsPersian,
                        onClick = { selectedFilter = HistoryPeriodFilter.THIS_WEEK },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2.5 Unified App Sections Card (if any recorded sections)
            if (unifiedSections.isNotEmpty()) {
                item(key = "unified_app_sections_card") {
                    UnifiedSectionsView(
                        sections = unifiedSections,
                        packageName = appPackage,
                        isPersian = effectiveIsPersian
                    )
                }
            }

            // 3. List of sessions or Empty State
            if (filteredRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(SlateDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                text = when {
                                    allRecords.isEmpty() -> stringResource(R.string.session_history_empty)
                                    selectedFilter == HistoryPeriodFilter.TODAY -> stringResource(R.string.session_history_today_empty)
                                    selectedFilter == HistoryPeriodFilter.THIS_WEEK -> stringResource(R.string.session_history_week_empty)
                                    else -> stringResource(R.string.session_history_empty)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                            if (allRecords.isNotEmpty() && selectedFilter != HistoryPeriodFilter.ALL) {
                                TextButton(
                                    onClick = { selectedFilter = HistoryPeriodFilter.ALL },
                                    colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                                ) {
                                    Text(
                                        text = stringResource(R.string.view_all_sessions),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Render Day Groups
                groupedRecordsByDay.forEach { (dayStartMs, daySessions) ->
                    // Day Header
                    item(key = "header_$dayStartMs") {
                        val dayLabel = when (dayStartMs) {
                            startOfToday -> {
                                if (effectiveIsPersian) {
                                    "امروز، " + JalaliCalendarHelper.formatJalali(dayStartMs, includeTime = false, includeYear = true)
                                } else {
                                    "Today, " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dayStartMs))
                                }
                            }
                            startOfYesterday -> {
                                if (effectiveIsPersian) {
                                    "دیروز، " + JalaliCalendarHelper.formatJalali(dayStartMs, includeTime = false, includeYear = true)
                                } else {
                                    "Yesterday, " + SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dayStartMs))
                                }
                            }
                            else -> {
                                if (effectiveIsPersian) {
                                    JalaliCalendarHelper.formatJalali(dayStartMs, includeTime = false, includeYear = true, includeWeekday = true)
                                } else {
                                    SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(dayStartMs))
                                }
                            }
                        }

                        val dayStopwatchMs = daySessions.filter { !it.record.isPomodoro }.sumOf { it.record.value }
                        val dayPomodoros = daySessions.filter { it.record.isPomodoro }.sumOf { it.record.value.toInt() }
                        val dayTotalFormatted = when {
                            dayStopwatchMs > 0 && dayPomodoros > 0 -> {
                                "${formatDurationShort(dayStopwatchMs, effectiveIsPersian)} + ${if (effectiveIsPersian) "${JalaliCalendarHelper.toPersianDigits(dayPomodoros.toString())} دور" else "$dayPomodoros c"}"
                            }
                            dayStopwatchMs > 0 -> formatDurationShort(dayStopwatchMs, effectiveIsPersian)
                            dayPomodoros > 0 -> if (effectiveIsPersian) "${JalaliCalendarHelper.toPersianDigits(dayPomodoros.toString())} دور" else "$dayPomodoros cycles"
                            else -> "0s"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = dayTotalFormatted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
                    }

                    // Sessions inside this day
                    items(
                        items = daySessions,
                        key = { "${it.originalIndex}_${it.record.timestamp}" }
                    ) { item ->
                        SessionRecordCard(
                            sessionItem = item,
                            isPersian = effectiveIsPersian,
                            onDelete = { recordToDelete = item },
                            onSplit = { recordToSplit = item },
                            packageName = appPackage
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (recordToDelete != null) {
        AppAlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = {
                Text(
                    stringResource(R.string.delete_session),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    stringResource(R.string.delete_session_confirm),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    recordToDelete?.let { onDeleteRecord(it.originalIndex) }
                    recordToDelete = null
                }) {
                    Text(stringResource(R.string.action_delete), color = PriorityHigh)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SlateDark,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Split session dialog
    if (recordToSplit != null) {
        val targetItem = recordToSplit!!
        val targetRecord = targetItem.record
        AppAlertDialog(
            onDismissRequest = { recordToSplit = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        stringResource(R.string.split_session_title),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        stringResource(R.string.split_session_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Total session duration card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SlateLight.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.longest_session),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                formatDuration(targetRecord.value, effectiveIsPersian),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }
                    }

                    // Days selector
                    Text(
                        stringResource(R.string.split_days_count, splitDaysCount).let {
                            if (effectiveIsPersian) JalaliCalendarHelper.toPersianDigits(it) else it
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(2, 3, 4, 5, 7).forEach { days ->
                            val isSelected = (splitDaysCount == days)
                            val label = if (effectiveIsPersian) "${JalaliCalendarHelper.toPersianDigits(days.toString())} روز" else "${days}d"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { splitDaysCount = days },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else SlateDark,
                                border = BorderStroke(1.dp, if (isSelected) NeonCyan else BorderColor.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) NeonCyan else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Live preview
                    val previewTime = formatDuration(targetRecord.value / splitDaysCount, effectiveIsPersian)
                    Text(
                        stringResource(R.string.split_daily_preview, previewTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonPurple,
                        fontWeight = FontWeight.Medium
                    )

                    // Optional: Reconstruct from activity history if available
                    if (daysWithDoomscroll.size >= 2) {
                        OutlinedButton(
                            onClick = {
                                viewModel?.reconstructSessionsFromActivity(todo.id) { success, count ->
                                    if (success) {
                                        val msg = currentContext.getString(R.string.reconstruct_success, count)
                                        android.widget.Toast.makeText(currentContext, msg, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                                recordToSplit = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.split_from_activity, daysWithDoomscroll.size).let {
                                    if (effectiveIsPersian) JalaliCalendarHelper.toPersianDigits(it) else it
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val recIndex = targetItem.originalIndex
                        viewModel?.splitSessionIntoDays(todo.id, recIndex, splitDaysCount) { success ->
                            if (success) {
                                val msg = currentContext.getString(R.string.split_success, splitDaysCount)
                                android.widget.Toast.makeText(currentContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        recordToSplit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        stringResource(R.string.split_action),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToSplit = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = SlateDark,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    isPersian: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val countStr = if (isPersian) JalaliCalendarHelper.toPersianDigits(count.toString()) else count.toString()
    val bgColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else SlateDark.copy(alpha = 0.6f)
    val borderColor = if (isSelected) NeonCyan else BorderColor.copy(alpha = 0.2f)
    val textColor = if (isSelected) NeonCyan else TextSecondary

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.22f) else SlateLight.copy(alpha = 0.5f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SessionRecordCard(
    sessionItem: SessionRecordIndexed,
    isPersian: Boolean,
    onDelete: () -> Unit,
    onSplit: () -> Unit = {},
    packageName: String? = null
) {
    val record = sessionItem.record
    val isPomodoro = record.isPomodoro
    val themeColor = if (isPomodoro) NeonPurple else NeonCyan

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateLight.copy(alpha = 0.4f)),
        border = BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: time + duration + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(themeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPomodoro) Icons.Default.Timer else Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        val sessionTitle = stringResource(R.string.session_label, sessionItem.originalIndex + 1)
                        Text(
                            text = sessionTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        val timeStr = if (isPersian) {
                            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            JalaliCalendarHelper.toPersianDigits(timeSdf.format(Date(record.timestamp)))
                        } else {
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(record.timestamp))
                        }
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val durationText = if (isPomodoro) {
                        if (isPersian) {
                            "${JalaliCalendarHelper.toPersianDigits(record.value.toString())} دور پومودورو"
                        } else {
                            "${record.value} Pomodoro cycle${if (record.value > 1) "s" else ""}"
                        }
                    } else {
                        formatDuration(record.value, isPersian)
                    }

                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )

                    if (!isPomodoro && record.value >= 5 * 60 * 1000L) {
                        IconButton(
                            onClick = onSplit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                contentDescription = stringResource(R.string.split_action),
                                tint = NeonCyan.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Section breakdown (if any)
            if (record.appSections.isNotEmpty()) {
                val totalSectionTime = record.appSections.values.sum().coerceAtLeast(1L)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Horizontal segmented bar
                    val sectionColors = remember(record.appSections) {
                        record.appSections.keys.mapIndexed { index, _ ->
                            sectionColor(index)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        record.appSections.entries.forEachIndexed { index, (_, time) ->
                            val fraction = (time.toFloat() / totalSectionTime).coerceIn(0.01f, 1f)
                            Box(
                                modifier = Modifier
                                    .weight(fraction)
                                    .fillMaxHeight()
                                    .background(sectionColors[index])
                            )
                        }
                    }

                    // Section labels (FlowRow ensures chips wrap cleanly instead of squeezing characters)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        record.appSections.entries.forEachIndexed { index, (section, time) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(sectionColors[index])
                                )
                                val sectionLabel = localizeSectionName(section, isPersian)
                                Text(
                                    text = "$sectionLabel ${formatDurationShort(time, isPersian)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun localizeSectionName(section: String, isPersian: Boolean): String {
    return com.goldenminute.catalyst.service.SmartTrackerService.localizeSectionName(section, isPersian)
}

