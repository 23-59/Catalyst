package com.goldenminute.catalyst.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.data.*
import com.goldenminute.catalyst.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Composable
fun DailyDigestCard(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit,
    todos: List<TodoItem>,
    notes: List<NoteItem>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var digestText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    val userGeminiApiKey by viewModel.userGeminiApiKey.collectAsState()
    val userAiDigestionPrompt by viewModel.userAiDigestionPrompt.collectAsState()

    val langInstruction = stringResource(R.string.daily_digest_prompt_language)
    val genericError = stringResource(R.string.daily_digest_error)
    val noDataMsg = stringResource(R.string.daily_digest_no_data)
    val proxyError = stringResource(R.string.error_403_proxy)

    LaunchedEffect(Unit) {
        val todayTodos = todos.filter { 
            // Simple filtering: In a real app we'd check if it's due today, 
            // but for this prototype we'll just send pending tasks.
            !it.isCompleted 
        }.take(10) // Limit to 10 so we don't blow up token limits
        
        val recentNotes = notes.take(5)

        val now = java.util.Calendar.getInstance().timeInMillis
        val dailyGoal = viewModel.dailyFocusGoalHours.value
        val appContext = "Current Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now))}, Daily Focus Goal: $dailyGoal hours"

        val tasksData = todayTodos.joinToString("\n") { 
            val priorityStr = when (it.priority) { 0 -> "Low"; 1 -> "Medium"; 2 -> "High"; else -> "Unknown" }
            val dueStr = it.dueDate?.let { d -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(d)) } ?: "None"
            "- Task: ${it.title} | Priority: $priorityStr | Due: $dueStr | Daily: ${it.isDaily} | Desc: ${it.description.take(50)}"
        }
        val notesData = recentNotes.joinToString("\n") { 
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.createdAt))
            "- ${it.title} (Created: $dateStr): ${it.content.take(100)}" 
        }

        var prompt = userAiDigestionPrompt
        prompt = prompt.replace("{APP_CONTEXT}", appContext)
        prompt = prompt.replace("{TASKS_DATA}", tasksData)
        prompt = prompt.replace("{NOTES_DATA}", notesData)
        
        prompt = "$prompt\n\nSystem Instruction: $langInstruction"

        try {
            val response = withContext(Dispatchers.IO) {
                val apiKey = userGeminiApiKey.takeIf { it.isNotEmpty() } ?: com.goldenminute.catalyst.BuildConfig.GEMINI_API_KEY
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
                RetrofitClient.service.generateContent(apiKey, request)
            }
            
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: noDataMsg
            digestText = text
        } catch (e: HttpException) {
            if (e.code() == 403) {
                errorMessage = proxyError
            } else {
                errorMessage = "$genericError (${e.localizedMessage})"
            }
        } catch (e: Exception) {
            errorMessage = "$genericError (${e.localizedMessage})"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SlateDark)
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = NeonPurple
                )
                Text(
                    text = stringResource(R.string.daily_digest_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_desc),
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SlateLight.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = NeonPurple)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.daily_digest_generating),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = digestText,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.daily_digest_powered_by),
            color = TextSecondary.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
