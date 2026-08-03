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
import com.goldenminute.catalyst.data.NoteItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import androidx.glance.GlanceTheme

class NotesWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = TodoDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val repository = TodoRepository(database.todoDao(), database.categoryDao(), database.noteDao())
        
        // Fetch snapshot of notes
        val notes = repository.allNotes.first()
        val recentNotes = notes.take(8)
        
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
                
                // Breakpoints
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
                        // ---- 1. COMPACT ROW DISPLAY ----
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
                                Text("📝", style = TextStyle(fontSize = 18.sp))
                            }
                            
                            Spacer(modifier = GlanceModifier.width(12.dp))
                            
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = "Notes Shelf",
                                    style = TextStyle(color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                )
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = if (recentNotes.isEmpty()) "No notes yet" else "${recentNotes.size} recent notes",
                                    style = TextStyle(color = textMuted, fontSize = 12.sp)
                                )
                            }
                        }
                    } else if (isWideLayout) {
                        // ---- 2. WIDE STAGGERED GRID-LIKE DISPLAY ----
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
                                    text = "NOTES BOARD",
                                    style = TextStyle(color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                )
                            }
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "${recentNotes.size} total",
                                style = TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        
                        if (recentNotes.isEmpty()) {
                            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No notes saved yet. 📝", style = TextStyle(color = textMuted, fontSize = 14.sp))
                            }
                        } else {
                            val maxNotesToShow = ((height - 40.dp) / 72.dp).toInt().coerceAtLeast(1).coerceAtMost(4) * 2
                            val displayNotes = recentNotes.take(maxNotesToShow)
                            
                            val col1Notes = displayNotes.filterIndexed { idx, _ -> idx % 2 == 0 }
                            val col2Notes = displayNotes.filterIndexed { idx, _ -> idx % 2 == 1 }
                            
                            Row(modifier = GlanceModifier.fillMaxSize()) {
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    col1Notes.forEach { note ->
                                        NoteItemCard(note, cardBg, textPrimary, textMuted, isCompactWidth, height)
                                    }
                                }
                                
                                Spacer(modifier = GlanceModifier.width(12.dp))
                                
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    col2Notes.forEach { note ->
                                        NoteItemCard(note, cardBg, textPrimary, textMuted, isCompactWidth, height)
                                    }
                                }
                            }
                        }
                    } else {
                        // ---- 3. STANDARD PORTRAIT LIST DISPLAY ----
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
                                    text = "NOTES",
                                    style = TextStyle(color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.width(8.dp))
                            
                            if (!isCompactWidth) {
                                Text(
                                    text = "Recent Boards",
                                    style = TextStyle(color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            
                            Text(
                                text = "${recentNotes.size} count",
                                style = TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        
                        if (recentNotes.isEmpty()) {
                            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📝", style = TextStyle(fontSize = 28.sp))
                                    Spacer(modifier = GlanceModifier.height(8.dp))
                                    Text(
                                        text = "No notes",
                                        style = TextStyle(color = textMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        } else {
                            val availableHeight = height - 48.dp
                            val maxItems = (availableHeight / 64.dp).toInt().coerceAtLeast(1).coerceAtMost(6)
                            val displayNotes = recentNotes.take(maxItems)
                            
                            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                                items(displayNotes) { note ->
                                    NoteItemCard(note, cardBg, textPrimary, textMuted, isCompactWidth, height)
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
private fun NoteItemCard(
    note: NoteItem,
    cardBg: ColorProvider,
    textPrimary: ColorProvider,
    textMuted: ColorProvider,
    isCompactWidth: Boolean,
    widgetHeight: Dp
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(cardBg)
            .cornerRadius(14.dp)
            .padding(12.dp)
    ) {
        // Suppress or avoid UnobservableLocale error
        val context = androidx.glance.LocalContext.current
        val isFa = context.resources.configuration.locale.language == "fa"
        Text(
            text = note.title.ifEmpty { if (isFa) "بدون عنوان" else "Untitled" },
            style = TextStyle(
                color = textPrimary, 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        
        // Show note text content preview if space is sufficient
        val showPreview = !isCompactWidth && widgetHeight > 160.dp && note.content.isNotEmpty()
        if (showPreview) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = note.content,
                style = TextStyle(color = textMuted, fontSize = 11.sp),
                maxLines = if (widgetHeight > 240.dp) 3 else 1
            )
        }
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
