package com.goldenminute.catalyst.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.goldenminute.catalyst.MainActivity
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.TodoDatabase
import com.goldenminute.catalyst.data.TodoItem
import com.goldenminute.catalyst.data.SubTask
import com.goldenminute.catalyst.data.appDataStore
import com.goldenminute.catalyst.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.util.Locale

class FloatingTaskBarService : Service() {

    companion object {
        const val ACTION_SHOW = "com.goldenminute.catalyst.action.SHOW_FLOATING_TASK"
        const val ACTION_CLOSE = "com.goldenminute.catalyst.action.CLOSE_FLOATING_TASK"
        const val EXTRA_TODO_ID = "extra_todo_id"

        private val _activeTaskId = MutableStateFlow<Int?>(null)
        val activeTaskId: StateFlow<Int?> = _activeTaskId.asStateFlow()

        fun isTaskActive(id: Int): Boolean = _activeTaskId.value == id

        fun show(context: Context, todoId: Int) {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(
                    context,
                    context.getString(R.string.floating_task_bar_permission_required),
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }

            val intent = Intent(context, FloatingTaskBarService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_TODO_ID, todoId)
            }
            context.startService(intent)
        }

        fun close(context: Context) {
            val intent = Intent(context, FloatingTaskBarService::class.java).apply {
                action = ACTION_CLOSE
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var containerView: FrameLayout? = null
    private var lifecycleOwner: FloatingOverlayLifecycleOwner? = null
    private var isViewAdded = false

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val currentTodoIdState = MutableStateFlow<Int?>(null)

    private lateinit var windowParams: WindowManager.LayoutParams

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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CLOSE -> {
                closeOverlay()
                return START_NOT_STICKY
            }
            ACTION_SHOW -> {
                val todoId = intent.getIntExtra(EXTRA_TODO_ID, -1)
                if (todoId != -1) {
                    currentTodoIdState.value = todoId
                    _activeTaskId.value = todoId
                    if (!isViewAdded) {
                        createAndAttachOverlay()
                    }
                } else {
                    closeOverlay()
                }
            }
            else -> {
                val todoId = intent?.getIntExtra(EXTRA_TODO_ID, -1) ?: -1
                if (todoId != -1) {
                    currentTodoIdState.value = todoId
                    _activeTaskId.value = todoId
                    if (!isViewAdded) {
                        createAndAttachOverlay()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun createAndAttachOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val density = displayMetrics.density

        val defaultWidth = (320 * density).toInt().coerceIn((260 * density).toInt(), screenWidth - (24 * density).toInt())
        val defaultHeight = (74 * density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            defaultWidth,
            defaultHeight,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - defaultWidth) / 2
            y = (screenHeight * 0.12f).toInt()
        }

        val lifecycle = FloatingOverlayLifecycleOwner().also { lifecycleOwner = it }
        val container = FrameLayout(this).also { containerView = it }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val isPersian = Locale.getDefault().language == "fa"
                val typography = getTypography(if (isPersian) Vazirmatn else androidx.compose.ui.text.font.FontFamily.Default)

                CompositionLocalProvider(
                    LocalLayoutDirection provides if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    MaterialTheme(typography = typography) {
                        FloatingTaskBarContent(
                            todoIdFlow = currentTodoIdState,
                            windowParams = windowParams,
                            updateWindow = {
                                try {
                                    windowManager?.updateViewLayout(container, windowParams)
                                } catch (_: Exception) {}
                            },
                            onClose = { closeOverlay() },
                            onOpenCatalyst = { todoId ->
                                val mainIntent = Intent(applicationContext, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("OPEN_TODO_ID", todoId)
                                }
                                applicationContext.startActivity(mainIntent)
                            }
                        )
                    }
                }
            }
        }

        container.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        container.setViewTreeLifecycleOwner(lifecycle)
        container.setViewTreeViewModelStoreOwner(lifecycle)
        container.setViewTreeSavedStateRegistryOwner(lifecycle)

        composeView.setViewTreeLifecycleOwner(lifecycle)
        composeView.setViewTreeViewModelStoreOwner(lifecycle)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycle)

        try {
            windowManager?.addView(container, windowParams)
            isViewAdded = true
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun closeOverlay() {
        if (isViewAdded && containerView != null) {
            try {
                windowManager?.removeView(containerView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isViewAdded = false
        }
        _activeTaskId.value = null
        currentTodoIdState.value = null
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeOverlay()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        serviceScope.cancel()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FloatingTaskBarContent(
    todoIdFlow: StateFlow<Int?>,
    windowParams: WindowManager.LayoutParams,
    updateWindow: () -> Unit,
    onClose: () -> Unit,
    onOpenCatalyst: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val isPersian = Locale.getDefault().language == "fa"

    val currentTodoId by todoIdFlow.collectAsState()
    val database = remember { TodoDatabase.getDatabase(context.applicationContext, coroutineScope) }

    var todoItem by remember { mutableStateOf<TodoItem?>(null) }
    var subtasks by remember { mutableStateOf<List<SubTask>>(emptyList()) }
    var isExpanded by remember { mutableStateOf(false) }

    val screenMetrics = context.resources.displayMetrics
    val screenWidth = screenMetrics.widthPixels
    val screenHeight = screenMetrics.heightPixels

    val minWidthPx = (240 * density).toInt()
    val maxWidthPx = (screenWidth - (20 * density)).toInt()
    val minHeightPx = (60 * density).toInt()
    val maxHeightPx = (screenHeight * 0.75f).toInt()

    // Observe active todo and its subtasks
    LaunchedEffect(currentTodoId) {
        val id = currentTodoId ?: return@LaunchedEffect
        val item = withContext(Dispatchers.IO) {
            database.todoDao().getTodoById(id)
        }
        todoItem = item
        if (item == null || item.isCompleted) {
            onClose()
            return@LaunchedEffect
        }

        database.todoDao().getSubTasksForTodo(id).collect { subs ->
            subtasks = subs
        }
    }

    val todo = todoItem ?: return

    val priorityColor = when (todo.priority) {
        2 -> PriorityHigh
        1 -> PriorityMedium
        else -> PriorityLow
    }

    // Drag touch state
    var dragInitX by remember { mutableIntStateOf(0) }
    var dragInitY by remember { mutableIntStateOf(0) }
    var dragTouchRawX by remember { mutableFloatStateOf(0f) }
    var dragTouchRawY by remember { mutableFloatStateOf(0f) }

    // Resize touch state
    var resizeInitW by remember { mutableIntStateOf(0) }
    var resizeInitH by remember { mutableIntStateOf(0) }
    var resizeTouchRawX by remember { mutableFloatStateOf(0f) }
    var resizeTouchRawY by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .border(
                border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianBlack.copy(alpha = 0.95f),
        shadowElevation = 10.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isExpanded) {
                // ==================== COMPACT BAR MODE ====================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Complete Task Checkbox Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                database.todoDao().updateCompletionStatus(todo.id, true, System.currentTimeMillis())
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.floating_task_bar_completed), Toast.LENGTH_SHORT).show()
                                    onClose()
                                }
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = stringResource(R.string.floating_task_bar_completed),
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Title & Subtask Count (Tap to open Catalyst)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenCatalyst(todo.id) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Text(
                                text = todo.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (subtasks.isNotEmpty()) {
                            val doneCount = subtasks.count { it.isCompleted }
                            Text(
                                text = "$doneCount/${subtasks.size} ${stringResource(R.string.subtask_title)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonPurple,
                                maxLines = 1
                            )
                        }
                    }

                    // Drag Pill Area
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(28.dp)
                            .height(36.dp)
                            .pointerInteropFilter { motionEvent ->
                                when (motionEvent.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        dragInitX = windowParams.x
                                        dragInitY = windowParams.y
                                        dragTouchRawX = motionEvent.rawX
                                        dragTouchRawY = motionEvent.rawY
                                        true
                                    }
                                    MotionEvent.ACTION_MOVE -> {
                                        val dx = (motionEvent.rawX - dragTouchRawX).toInt()
                                        val dy = (motionEvent.rawY - dragTouchRawY).toInt()
                                        windowParams.x = (dragInitX + dx).coerceIn(0, screenWidth - 100)
                                        windowParams.y = (dragInitY + dy).coerceIn(0, screenHeight - 100)
                                        updateWindow()
                                        true
                                    }
                                    else -> false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Expand to Box Button
                    IconButton(
                        onClick = {
                            isExpanded = true
                            windowParams.height = (230 * density).toInt().coerceIn(minHeightPx, maxHeightPx)
                            updateWindow()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.floating_task_bar_expand),
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Close Button on Floating Bar itself
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(SlateDark.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.floating_task_bar_close),
                            tint = TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            } else {
                // ==================== EXPANDED BOX MODE ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Header Row (Drag Area + Controls)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority Indicator Dot
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Drag Pill Handle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .pointerInteropFilter { motionEvent ->
                                    when (motionEvent.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            dragInitX = windowParams.x
                                            dragInitY = windowParams.y
                                            dragTouchRawX = motionEvent.rawX
                                            dragTouchRawY = motionEvent.rawY
                                            true
                                        }
                                        MotionEvent.ACTION_MOVE -> {
                                            val dx = (motionEvent.rawX - dragTouchRawX).toInt()
                                            val dy = (motionEvent.rawY - dragTouchRawY).toInt()
                                            windowParams.x = (dragInitX + dx).coerceIn(0, screenWidth - 100)
                                            windowParams.y = (dragInitY + dy).coerceIn(0, screenHeight - 100)
                                            updateWindow()
                                            true
                                        }
                                        else -> false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(38.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(TextSecondary.copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Collapse to Bar Button
                        IconButton(
                            onClick = {
                                isExpanded = false
                                windowParams.height = (74 * density).toInt()
                                updateWindow()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = stringResource(R.string.floating_task_bar_minimize),
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Close Button on Floating Bar itself
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(SlateDark.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.floating_task_bar_close),
                                tint = TextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Scrollable content in Box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Title + Quick Complete Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        database.todoDao().updateCompletionStatus(todo.id, true, System.currentTimeMillis())
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.floating_task_bar_completed), Toast.LENGTH_SHORT).show()
                                            onClose()
                                        }
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = stringResource(R.string.floating_task_bar_completed),
                                    tint = NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = todo.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Description preview if available
                        if (todo.description.isNotBlank()) {
                            Text(
                                text = todo.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 36.dp)
                            )
                        }

                        // Subtasks checklist
                        if (subtasks.isNotEmpty()) {
                            val completedCount = subtasks.count { it.isCompleted }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.subtask_title),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NeonPurple
                                )
                                Text(
                                    text = "$completedCount/${subtasks.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NeonPurple
                                )
                            }

                            subtasks.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlateDark.copy(alpha = 0.6f))
                                        .clickable {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                database.todoDao().updateSubTaskCompletion(sub.id, !sub.isCompleted)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (sub.isCompleted) NeonCyan else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = sub.title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else null
                                        ),
                                        color = if (sub.isCompleted) TextSecondary else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Open in Catalyst Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onOpenCatalyst(todo.id) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.floating_task_bar_open),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }
            }

            // Resizable Grip Handle (Bottom-Right / Bottom-Left corners)
            Box(
                modifier = Modifier
                    .align(if (isPersian) Alignment.BottomStart else Alignment.BottomEnd)
                    .size(28.dp)
                    .pointerInteropFilter { motionEvent ->
                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                resizeInitW = windowParams.width
                                resizeInitH = windowParams.height
                                resizeTouchRawX = motionEvent.rawX
                                resizeTouchRawY = motionEvent.rawY
                                true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val rawDx = (motionEvent.rawX - resizeTouchRawX).toInt()
                                val dx = if (isPersian) -rawDx else rawDx
                                val dy = (motionEvent.rawY - resizeTouchRawY).toInt()
                                windowParams.width = (resizeInitW + dx).coerceIn(minWidthPx, maxWidthPx)
                                windowParams.height = (resizeInitH + dy).coerceIn(minHeightPx, maxHeightPx)
                                updateWindow()
                                true
                            }
                            else -> false
                        }
                    },
                contentAlignment = if (isPersian) Alignment.BottomStart else Alignment.BottomEnd
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = null,
                    tint = NeonCyan.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(5.dp)
                        .size(11.dp)
                        .rotate(if (isPersian) 0f else 90f)
                )
            }
        }
    }
}
