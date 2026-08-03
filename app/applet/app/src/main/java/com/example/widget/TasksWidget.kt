package com.example.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import androidx.compose.ui.graphics.Color
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import com.example.data.TodoDatabase
import com.example.data.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first

class TasksWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = TodoDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val repository = TodoRepository(database.todoDao(), database.categoryDao(), database.noteDao())
        
        val todos = repository.allTodos.first()
        val pendingTodos = todos.filter { !it.isCompleted }.take(5)
        
        provideContent {
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .background(Color(0xFF1E1E2C))
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Text(
                        text = "Pending Tasks",
                        style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    if (pendingTodos.isEmpty()) {
                        Text(
                            text = "All caught up!",
                            style = TextStyle(color = ColorProvider(Color.Gray))
                        )
                    } else {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(pendingTodos) { todo ->
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = GlanceModifier.padding(end = 8.dp)) {
                                        Text(text = "•", style = TextStyle(color = ColorProvider(Color(0xFFB983FF))))
                                    }
                                    Text(
                                        text = todo.title,
                                        style = TextStyle(color = ColorProvider(Color.White))
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

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasksWidget()
}
