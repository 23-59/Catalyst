package com.goldenminute.catalyst.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.goldenminute.catalyst.MainActivity
import androidx.compose.ui.graphics.Color
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.width
import com.goldenminute.catalyst.data.TodoDatabase
import com.goldenminute.catalyst.data.TodoRepository
import com.goldenminute.catalyst.data.TodoItem
import com.goldenminute.catalyst.data.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.GlanceTheme

class TasksWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = TodoDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val repository = TodoRepository(database.todoDao(), database.categoryDao(), database.noteDao())
        
        // Fetch snapshot of categories and tasks
        val todos = repository.allTodos.first()
        val categories = repository.allCategories.first()
        val categoryMap = categories.associateBy { it.id }
        val pendingTodos = todos.filter { !it.isCompleted }
        
        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                val width = size.width
                val height = size.height
                
                // Design Tokens from GlanceTheme
                val bg = GlanceTheme.colors.surface
                val cardBg = GlanceTheme.colors.surfaceVariant
                val primaryColor = GlanceTheme.colors.primary
                val onPrimary = GlanceTheme.colors.onPrimary
                val textPrimary = GlanceTheme.colors.onSurface
                val textMuted = GlanceTheme.colors.onSurfaceVariant
                
                // Determine sizing breakpoints
                val isCompactHeight = height < 120.dp
                val isCompactWidth = width < 150.dp
                val isWideLayout = width >= 280.dp
                
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .appWidgetBackground()
                        .background(bg)
                        .cornerRadius(24.dp)
                        .padding(if (isCompactWidth) 12.dp else 16.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    if (isCompactHeight) {
                        // ---- 1. EXTREMELY COMPACT HEIGHT LAYOUT ----
                        Row(
                            modifier = GlanceModifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .background(primaryColor)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .cornerRadius(12.dp)
                            ) {
                                Text("🎯", style = TextStyle(fontSize = 18.sp))
                            }
                            
                            Spacer(modifier = GlanceModifier.width(12.dp))
                            
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "Pending Flow",
                                    style = TextStyle(color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = if (pendingTodos.isEmpty()) "All caught up! 🎉" else "${pendingTodos.size} tasks remaining",
                                    style = TextStyle(color = textMuted, fontSize = 12.sp)
                                )
                            }
                        }
                    } else if (isWideLayout) {
                        // ---- 2. SPLIT WIDE LAYOUT ----
                        Row(modifier = GlanceModifier.fillMaxSize()) {
                            Column(
                                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .background(primaryColor)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .cornerRadius(8.dp)
                                ) {
                                    Text(
                                        text = "HYPERTASK",
                                        style = TextStyle(color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(12.dp))
                                Text(
                                    text = "Your Day",
                                    style = TextStyle(color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = if (pendingTodos.isEmpty()) "All clear! 🎉" else "${pendingTodos.size} tasks left",
                                    style = TextStyle(color = textMuted, fontSize = 12.sp)
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.width(16.dp))
                            
                            Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
                                if (pendingTodos.isEmpty()) {
                                    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("All tasks complete! ✨", style = TextStyle(color = textMuted, fontSize = 13.sp))
                                    }
                                } else {
                                    val maxItems = ((height - 24.dp) / 48.dp).toInt().coerceAtLeast(1).coerceAtMost(6)
                                    val itemsToDraw = pendingTodos.take(maxItems)
                                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                                        items(itemsToDraw) { todo ->
                                            TaskItemRow(todo, categoryMap, cardBg, textPrimary, textMuted, isCompactWidth, height)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ---- 3. STANDARD PORTRAIT LAYOUT ----
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = GlanceModifier
                                    .background(primaryColor)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .cornerRadius(8.dp)
                            ) {
                                Text(
                                    text = "TASKS",
                                    style = TextStyle(color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            
                            if (!isCompactWidth) {
                                Text(
                                    text = "HyperTask",
                                    style = TextStyle(color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            
                            Text(
                                text = "${pendingTodos.size} left",
                                style = TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        
                        if (pendingTodos.isEmpty()) {
                            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎉", style = TextStyle(fontSize = 28.sp))
                                    Spacer(modifier = GlanceModifier.height(8.dp))
                                    Text(
                                        text = "Focus clear",
                                        style = TextStyle(color = textMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        } else {
                            val availableHeight = height - 48.dp
                            val maxItems = (availableHeight / 52.dp).toInt().coerceAtLeast(1).coerceAtMost(8)
                            val itemsToDraw = pendingTodos.take(maxItems)
                            
                            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                                items(itemsToDraw) { todo ->
                                    TaskItemRow(todo, categoryMap, cardBg, textPrimary, textMuted, isCompactWidth, height)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TaskItemRow(
    todo: TodoItem,
    categoryMap: Map<Int, Category>,
    cardBg: androidx.glance.unit.ColorProvider,
    textPrimary: androidx.glance.unit.ColorProvider,
    textMuted: androidx.glance.unit.ColorProvider,
    isCompactWidth: Boolean,
    widgetHeight: Dp
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(cardBg)
            .cornerRadius(14.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Priority Color Stripe
        val priorityColor = when (todo.priority) {
            2 -> Color(0xFFFF5252) // High -> Neon Red
            1 -> Color(0xFFFFC107) // Medium -> Soft Yellow
            else -> Color(0xFF64FFDA) // Low -> Cyan/Teal
        }
        
        Box(
            modifier = GlanceModifier
                .width(4.dp)
                .height(16.dp)
                .background(priorityColor)
                .cornerRadius(4.dp)
        ) {}
        
        Spacer(modifier = GlanceModifier.width(6.dp))
        
        // Tap area to check task
        Box(
            modifier = GlanceModifier
                .width(32.dp)
                .height(32.dp)
                .clickable(
                    actionRunCallback<CompleteTaskAction>(
                        actionParametersOf(CompleteTaskAction.TaskIdKey to todo.id)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "○",
                style = TextStyle(
                    color = ColorProvider(priorityColor),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = GlanceModifier.width(6.dp))
        
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = todo.title,
                style = TextStyle(
                    color = textPrimary, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            
            // Render subtext if room allows
            val showSubtext = !isCompactWidth && widgetHeight > 180.dp && (todo.description.isNotEmpty() || todo.categoryId != null)
            if (showSubtext) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                val categoryName = todo.categoryId?.let { categoryMap[it]?.name }
                val subText = when {
                    categoryName != null && todo.description.isNotEmpty() -> "$categoryName • ${todo.description}"
                    categoryName != null -> categoryName
                    else -> todo.description
                }
                Text(
                    text = subText,
                    style = TextStyle(color = textMuted, fontSize = 11.sp),
                    maxLines = 1
                )
            }
        }
    }
}

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val database = TodoDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val repository = TodoRepository(database.todoDao(), database.categoryDao(), database.noteDao())
        
        // Mark task completed in DB
        repository.updateCompletionStatus(taskId, isCompleted = true, completedDate = System.currentTimeMillis())
        
        // Force immediate refresh of this widget
        TasksWidget().update(context, glanceId)
    }

    companion object {
        val TaskIdKey = ActionParameters.Key<Int>("task_id")
    }
}

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasksWidget()
}
