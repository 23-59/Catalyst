package com.goldenminute.catalyst.ui.nfc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.nfc.Tag
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.goldenminute.catalyst.LocalizedContent
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.appDataStore
import com.goldenminute.catalyst.nfc.*
import com.goldenminute.catalyst.ui.AppAlertDialog
import com.goldenminute.catalyst.ui.AppDialog
import com.goldenminute.catalyst.ui.TodoViewModel
import com.goldenminute.catalyst.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

@Composable
fun NfcCardSettingsSection(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isNfcSupported by remember { mutableStateOf(NfcManager.isNfcSupported(context)) }
    var isNfcEnabled by remember { mutableStateOf(NfcManager.isNfcEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNfcSupported = NfcManager.isNfcSupported(context)
                isNfcEnabled = NfcManager.isNfcEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showWriteDialog by remember { mutableStateOf(false) }
    var showReadDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.nfc_section_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SlateDark,
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Header Row: Icon, Title, and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isNfcSupported && isNfcEnabled) NeonCyan.copy(alpha = 0.15f) else BorderColor.copy(alpha = 0.2f),
                            border = BorderStroke(
                                1.dp,
                                if (isNfcSupported && isNfcEnabled) NeonCyan.copy(alpha = 0.4f) else BorderColor.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = if (isNfcSupported && isNfcEnabled) NeonCyan else TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.nfc_section_title),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when {
                                    !isNfcSupported -> stringResource(R.string.nfc_status_unsupported)
                                    !isNfcEnabled -> stringResource(R.string.nfc_status_disabled)
                                    else -> stringResource(R.string.nfc_section_subtitle)
                                },
                                color = when {
                                    !isNfcSupported -> TextSecondary
                                    !isNfcEnabled -> PriorityMedium
                                    else -> PriorityLow
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Status Pill or Enable Button
                    if (isNfcSupported && !isNfcEnabled) {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(NfcManager.getNfcSettingsIntent())
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PriorityMedium.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PriorityMedium.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = PriorityMedium,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.nfc_btn_enable),
                                    color = PriorityMedium,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isNfcSupported && isNfcEnabled) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PriorityLow.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, PriorityLow.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PriorityLow)
                                )
                                Text(
                                    text = stringResource(R.string.nfc_status_ready),
                                    color = PriorityLow,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.nfc_section_desc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Cards: Write and Read side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Write Tile
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianBlack,
                        border = BorderStroke(
                            1.dp,
                            if (isNfcSupported && isNfcEnabled) NeonCyan.copy(alpha = 0.45f) else BorderColor.copy(alpha = 0.3f)
                        ),
                        onClick = {
                            if (!isNfcEnabled && isNfcSupported) {
                                try { context.startActivity(NfcManager.getNfcSettingsIntent()) } catch (_: Exception) {}
                            } else {
                                showWriteDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = NeonCyan.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Nfc,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.nfc_btn_write),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.nfc_write_card_desc),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Read Tile
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianBlack,
                        border = BorderStroke(
                            1.dp,
                            if (isNfcSupported && isNfcEnabled) NeonPurple.copy(alpha = 0.45f) else BorderColor.copy(alpha = 0.3f)
                        ),
                        onClick = {
                            if (!isNfcEnabled && isNfcSupported) {
                                try { context.startActivity(NfcManager.getNfcSettingsIntent()) } catch (_: Exception) {}
                            } else {
                                showReadDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = NeonPurple.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = null,
                                            tint = NeonPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.nfc_btn_read),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.nfc_read_card_desc),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWriteDialog) {
        NfcWriteModal(
            viewModel = viewModel,
            onDismiss = { showWriteDialog = false }
        )
    }

    if (showReadDialog) {
        NfcReadModal(
            viewModel = viewModel,
            onDismiss = { showReadDialog = false }
        )
    }
}

private enum class WriteStep {
    SELECT_PAYLOAD,
    SCAN_AND_WRITE,
    SUCCESS,
    ERROR
}

@Composable
private fun NfcWriteModal(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var step by remember { mutableStateOf(WriteStep.SELECT_PAYLOAD) }
    var selectedMode by remember { mutableStateOf("quick") } // "quick", "custom", "full"
    var errorMessage by remember { mutableStateOf("") }

    val allCategories by viewModel.categories.collectAsState()
    val allTodos by viewModel.allTodosList.collectAsState()
    val allSubTasks by viewModel.allSubTasks.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()

    var selectedCategoryIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedTodoIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedSubTaskIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedNoteIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var customTab by remember { mutableStateOf(0) }

    // Calculate current payload
    val currentPayload = remember(selectedMode, selectedCategoryIds, selectedTodoIds, selectedSubTaskIds, selectedNoteIds, allTodos, allCategories, allSubTasks, allNotes) {
        when (selectedMode) {
            "quick" -> viewModel.buildQuickNfcPayload()
            "full" -> viewModel.buildCustomNfcPayload(
                selectedCategoryIds = allCategories.map { it.id }.toSet(),
                selectedTodoIds = allTodos.map { it.id }.toSet(),
                selectedSubTaskIds = allSubTasks.map { it.id }.toSet(),
                selectedNoteIds = allNotes.map { it.id }.toSet(),
                isFull = true
            )
            else -> viewModel.buildCustomNfcPayload(
                selectedCategoryIds = selectedCategoryIds,
                selectedTodoIds = selectedTodoIds,
                selectedSubTaskIds = selectedSubTaskIds,
                selectedNoteIds = selectedNoteIds,
                isFull = false
            )
        }
    }

    val payloadBytesSize = remember(currentPayload) {
        NfcManager.calculatePayloadSize(currentPayload)
    }

    var isWriting by remember { mutableStateOf(false) }

    // Attach NFC reader mode when on SCAN_AND_WRITE step
    DisposableEffect(step) {
        val activity = context.findActivity()
        if (step == WriteStep.SCAN_AND_WRITE && activity != null) {
            isWriting = false
            NfcManager.enableReaderMode(activity) { tag ->
                if (isWriting) return@enableReaderMode
                activity.runOnUiThread {
                    isWriting = true
                }
                val result = NfcManager.writeToTag(tag, currentPayload)
                activity.runOnUiThread {
                    isWriting = false
                    when (result) {
                        is NfcWriteResult.Success -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            step = WriteStep.SUCCESS
                        }
                        is NfcWriteResult.ReadOnlyTag -> {
                            errorMessage = context.getString(R.string.nfc_err_readonly)
                            step = WriteStep.ERROR
                        }
                        is NfcWriteResult.InsufficientSpace -> {
                            errorMessage = context.getString(
                                R.string.nfc_err_insufficient_space,
                                result.requiredBytes,
                                result.availableBytes
                            )
                            step = WriteStep.ERROR
                        }
                        is NfcWriteResult.TagLost -> {
                            errorMessage = context.getString(R.string.nfc_err_tag_lost)
                            step = WriteStep.ERROR
                        }
                        is NfcWriteResult.Error -> {
                            errorMessage = context.getString(R.string.nfc_err_general, result.message)
                            step = WriteStep.ERROR
                        }
                    }
                }
            }
        }
        onDispose {
            if (activity != null) {
                NfcManager.disableReaderMode(activity)
            }
        }
    }

    val appLanguage by context.appDataStore.data.collectAsState(initial = null)
    val lang = appLanguage?.get(DataStoreKeys.LANGUAGE) ?: if (Locale.getDefault().language == "fa") "fa" else "en"

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LocalizedContent(localeCode = lang) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianBlack,
                border = BorderStroke(1.2.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 480.dp)
                    .heightIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        WriteStep.SELECT_PAYLOAD -> {
                            Surface(
                                shape = CircleShape,
                                color = NeonCyan.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Nfc,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = stringResource(R.string.nfc_write_dialog_title),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(R.string.nfc_write_dialog_subtitle),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Mode 1: Quick Priorities
                            PayloadModeOption(
                                title = stringResource(R.string.nfc_mode_quick),
                                subtitle = stringResource(R.string.nfc_mode_quick_desc),
                                icon = Icons.Default.Bolt,
                                accentColor = NeonCyan,
                                isSelected = selectedMode == "quick",
                                onClick = { selectedMode = "quick" }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Mode 2: Custom Selection
                            PayloadModeOption(
                                title = stringResource(R.string.nfc_mode_custom),
                                subtitle = stringResource(R.string.nfc_mode_custom_desc),
                                icon = Icons.Default.Tune,
                                accentColor = NeonPurple,
                                isSelected = selectedMode == "custom",
                                onClick = {
                                    selectedMode = "custom"
                                    if (selectedTodoIds.isEmpty() && allTodos.isNotEmpty()) {
                                        selectedTodoIds = allTodos.filter { !it.isCompleted }.map { it.id }.toSet()
                                    }
                                }
                            )

                            // If Custom Mode selected, show mini picker
                            AnimatedVisibility(visible = selectedMode == "custom") {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = SlateDark,
                                        border = BorderStroke(1.dp, BorderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 160.dp, max = 220.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            val tabs = listOf(
                                                stringResource(R.string.tab_tasks),
                                                stringResource(R.string.tab_notes),
                                                stringResource(R.string.tab_categories)
                                            )
                                            TabRow(
                                                selectedTabIndex = customTab,
                                                containerColor = Color.Transparent,
                                                contentColor = NeonCyan
                                            ) {
                                                tabs.forEachIndexed { idx, t ->
                                                    Tab(
                                                        selected = customTab == idx,
                                                        onClick = { customTab = idx },
                                                        text = { Text(t, style = MaterialTheme.typography.labelSmall) }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LazyColumn(modifier = Modifier.weight(1f)) {
                                                when (customTab) {
                                                    0 -> items(allTodos) { todo ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    selectedTodoIds = if (selectedTodoIds.contains(todo.id)) selectedTodoIds - todo.id else selectedTodoIds + todo.id
                                                                }
                                                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                                        ) {
                                                            Checkbox(
                                                                checked = selectedTodoIds.contains(todo.id),
                                                                onCheckedChange = { chk ->
                                                                    selectedTodoIds = if (chk) selectedTodoIds + todo.id else selectedTodoIds - todo.id
                                                                },
                                                                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = todo.title,
                                                                color = TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                    1 -> items(allNotes) { note ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    selectedNoteIds = if (selectedNoteIds.contains(note.id)) selectedNoteIds - note.id else selectedNoteIds + note.id
                                                                }
                                                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                                        ) {
                                                            Checkbox(
                                                                checked = selectedNoteIds.contains(note.id),
                                                                onCheckedChange = { chk ->
                                                                    selectedNoteIds = if (chk) selectedNoteIds + note.id else selectedNoteIds - note.id
                                                                },
                                                                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = note.title.ifEmpty { "Untitled" },
                                                                color = TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                    2 -> items(allCategories) { cat ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    selectedCategoryIds = if (selectedCategoryIds.contains(cat.id)) selectedCategoryIds - cat.id else selectedCategoryIds + cat.id
                                                                }
                                                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                                        ) {
                                                            Checkbox(
                                                                checked = selectedCategoryIds.contains(cat.id),
                                                                onCheckedChange = { chk ->
                                                                    selectedCategoryIds = if (chk) selectedCategoryIds + cat.id else selectedCategoryIds - cat.id
                                                                },
                                                                colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = cat.name,
                                                                color = TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Mode 3: Full Backup
                            PayloadModeOption(
                                title = stringResource(R.string.nfc_mode_full),
                                subtitle = stringResource(R.string.nfc_mode_full_desc),
                                icon = Icons.Default.CloudSync,
                                accentColor = PriorityLow,
                                isSelected = selectedMode == "full",
                                onClick = { selectedMode = "full" }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Size badge & Compatibility info
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SlateDark,
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                imageVector = Icons.Default.DataUsage,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.nfc_data_size_label),
                                                color = TextPrimary,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = NeonCyan.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = stringResource(R.string.nfc_payload_size_badge, payloadBytesSize),
                                                color = NeonCyan,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.8.dp)

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = PriorityLow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = when {
                                                payloadBytesSize <= 144 -> stringResource(R.string.nfc_tag_fit_hint_ntag213)
                                                payloadBytesSize <= 504 -> stringResource(R.string.nfc_tag_fit_hint_ntag215)
                                                else -> stringResource(R.string.nfc_tag_fit_hint_ntag216)
                                            },
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text(stringResource(R.string.cancel), color = TextSecondary)
                                }

                                Button(
                                    onClick = { step = WriteStep.SCAN_AND_WRITE },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.4f).height(46.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.nfc_btn_continue_write),
                                        color = ObsidianBlack,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }

                        WriteStep.SCAN_AND_WRITE -> {
                            Text(
                                text = stringResource(R.string.nfc_step_scan_title),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            NfcRadarPulseAnimation(
                                isSuccess = false,
                                isError = false
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = stringResource(if (isWriting) R.string.nfc_writing_in_progress else R.string.nfc_step_scan_desc),
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.nfc_do_not_move),
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SlateDark,
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Text(
                                    text = stringResource(R.string.nfc_payload_size_badge, payloadBytesSize),
                                    color = NeonCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = { step = WriteStep.SELECT_PAYLOAD },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.fillMaxWidth(0.5f).height(44.dp)
                            ) {
                                Text(stringResource(R.string.back), color = TextSecondary)
                            }
                        }

                        WriteStep.SUCCESS -> {
                            NfcRadarPulseAnimation(
                                isSuccess = true,
                                isError = false
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = stringResource(R.string.nfc_write_success_title),
                                color = PriorityLow,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.nfc_write_success_desc),
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = PriorityLow),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(0.6f).height(46.dp)
                            ) {
                                Text(stringResource(R.string.close_button), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                            }
                        }

                        WriteStep.ERROR -> {
                            NfcRadarPulseAnimation(
                                isSuccess = false,
                                isError = true
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = stringResource(R.string.nfc_write_error_title),
                                color = PriorityHigh,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PriorityHigh.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text(stringResource(R.string.cancel), color = TextSecondary)
                                }

                                Button(
                                    onClick = { step = WriteStep.SCAN_AND_WRITE },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.2f).height(46.dp)
                                ) {
                                    Text(stringResource(R.string.nfc_retry), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class ReadStep {
    SCANNING,
    PREVIEW,
    ERROR
}

@Composable
private fun NfcReadModal(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var step by remember { mutableStateOf(ReadStep.SCANNING) }
    var readPayload by remember { mutableStateOf<NfcPayload?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    var isReading by remember { mutableStateOf(false) }

    DisposableEffect(step) {
        val activity = context.findActivity()
        if (step == ReadStep.SCANNING && activity != null) {
            isReading = false
            NfcManager.enableReaderMode(activity) { tag ->
                if (isReading) return@enableReaderMode
                activity.runOnUiThread {
                    isReading = true
                }
                val result = NfcManager.readFromTag(tag)
                activity.runOnUiThread {
                    isReading = false
                    when (result) {
                        is NfcReadResult.Success -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            readPayload = result.payload
                            step = ReadStep.PREVIEW
                        }
                        is NfcReadResult.EmptyTag -> {
                            errorMessage = context.getString(R.string.nfc_err_no_catalyst_data)
                            step = ReadStep.ERROR
                        }
                        is NfcReadResult.InvalidData -> {
                            errorMessage = context.getString(R.string.nfc_err_no_catalyst_data)
                            step = ReadStep.ERROR
                        }
                        is NfcReadResult.TagLost -> {
                            errorMessage = context.getString(R.string.nfc_err_tag_lost)
                            step = ReadStep.ERROR
                        }
                        is NfcReadResult.Error -> {
                            errorMessage = context.getString(R.string.nfc_err_general, result.message)
                            step = ReadStep.ERROR
                        }
                    }
                }
            }
        }
        onDispose {
            if (activity != null) {
                NfcManager.disableReaderMode(activity)
            }
        }
    }

    val appLanguage by context.appDataStore.data.collectAsState(initial = null)
    val lang = appLanguage?.get(DataStoreKeys.LANGUAGE) ?: if (Locale.getDefault().language == "fa") "fa" else "en"

    AppDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LocalizedContent(localeCode = lang) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianBlack,
                border = BorderStroke(1.2.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 480.dp)
                    .heightIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                when (step) {
                    ReadStep.SCANNING -> {
                        Text(
                            text = stringResource(R.string.nfc_read_dialog_title),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        NfcRadarPulseAnimation(
                            isSuccess = false,
                            isError = false
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = stringResource(if (isReading) R.string.nfc_reading_in_progress else R.string.nfc_tap_prompt_read),
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Text(stringResource(R.string.cancel), color = TextSecondary)
                        }
                    }

                    ReadStep.PREVIEW -> {
                        val payload = readPayload
                        if (payload != null) {
                            Text(
                                text = stringResource(R.string.nfc_preview_title),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Summary Badges Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PreviewBadge(
                                    text = stringResource(R.string.nfc_preview_tasks_count, payload.todos.size),
                                    color = NeonCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                PreviewBadge(
                                    text = stringResource(R.string.nfc_preview_notes_count, payload.notes.size),
                                    color = NeonPurple,
                                    modifier = Modifier.weight(1f)
                                )
                                PreviewBadge(
                                    text = stringResource(R.string.nfc_preview_categories_count, payload.categories.size),
                                    color = PriorityLow,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Items preview list
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SlateDark,
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 180.dp)
                            ) {
                                LazyColumn(modifier = Modifier.padding(12.dp)) {
                                    items(payload.todos) { todo ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (todo.priority) {
                                                            2 -> PriorityHigh
                                                            1 -> PriorityMedium
                                                            else -> PriorityLow
                                                        }
                                                    )
                                            )
                                            Text(
                                                text = todo.title,
                                                color = TextPrimary,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    items(payload.notes) { note ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = NeonPurple,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = note.title.ifEmpty { "Note" },
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action buttons: Merge vs Restore
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.importNfcPayload(payload, merge = true) { success ->
                                            val msg = if (success) context.getString(R.string.backup_imported_success) else context.getString(R.string.import_failed)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.nfc_action_merge),
                                        color = ObsidianBlack,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Button(
                                    onClick = { showRestoreConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().height(46.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.nfc_action_restore),
                                        color = PriorityHigh,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text(stringResource(R.string.cancel), color = TextSecondary)
                                }
                            }
                        }
                    }

                    ReadStep.ERROR -> {
                        NfcRadarPulseAnimation(
                            isSuccess = false,
                            isError = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = errorMessage,
                            color = PriorityHigh,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, BorderColor)
                            ) {
                                Text(stringResource(R.string.cancel), color = TextSecondary)
                            }

                            Button(
                                onClick = { step = ReadStep.SCANNING },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.nfc_retry), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (showRestoreConfirmDialog && readPayload != null) {
        AppAlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            containerColor = ObsidianBlack,
            title = {
                Text(
                    text = stringResource(R.string.nfc_restore_confirm_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.nfc_restore_confirm_desc),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        readPayload?.let { payload ->
                            viewModel.importNfcPayload(payload, merge = false) { success ->
                                val msg = if (success) context.getString(R.string.backup_imported_success) else context.getString(R.string.import_failed)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                        showRestoreConfirmDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityHigh)
                ) {
                    Text(stringResource(R.string.overwrite_data), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PayloadModeOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Bolt,
    accentColor: Color = NeonCyan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) SlateDark else ObsidianBlack,
        border = BorderStroke(
            1.2.dp,
            if (isSelected) accentColor else BorderColor.copy(alpha = 0.6f)
        ),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) accentColor.copy(alpha = 0.18f) else BorderColor.copy(alpha = 0.2f),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) accentColor.copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.3f)
                ),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) accentColor else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = TextSecondary.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
private fun PreviewBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NfcRadarPulseAnimation(
    modifier: Modifier = Modifier,
    isSuccess: Boolean = false,
    isError: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")

    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p1"
    )

    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p2"
    )

    val activeColor = when {
        isSuccess -> PriorityLow
        isError -> PriorityHigh
        else -> NeonCyan
    }

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!isSuccess && !isError) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = center
                val maxRadius = size.minDimension / 2f

                drawCircle(
                    color = activeColor.copy(alpha = (1f - pulse1).coerceIn(0f, 0.45f)),
                    radius = maxRadius * pulse1,
                    center = centerOffset,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                )

                drawCircle(
                    color = activeColor.copy(alpha = (1f - pulse2).coerceIn(0f, 0.45f)),
                    radius = maxRadius * pulse2,
                    center = centerOffset,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = activeColor.copy(alpha = 0.18f),
            border = BorderStroke(2.dp, activeColor.copy(alpha = 0.7f)),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isSuccess -> Icons.Default.Check
                        isError -> Icons.Default.Close
                        else -> Icons.Default.Nfc
                    },
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}
