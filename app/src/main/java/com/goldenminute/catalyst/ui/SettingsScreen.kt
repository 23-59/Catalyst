package com.goldenminute.catalyst.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goldenminute.catalyst.R
import androidx.compose.ui.graphics.asImageBitmap
import com.goldenminute.catalyst.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    viewModel: TodoViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp = 130.dp,
    isThemeAnimating: Boolean = false
) {
    val currentLang by viewModel.appLanguage.collectAsState()
    val currentTheme by viewModel.appTheme.collectAsState()
    val savedCustomThemes by viewModel.savedCustomThemes.collectAsState()
    val customThemeColor by viewModel.customThemeColor.collectAsState()
    val userGeminiApiKey by viewModel.userGeminiApiKey.collectAsState()
    val userAiHubPrompt by viewModel.userAiHubPrompt.collectAsState()
    val userAiDigestionPrompt by viewModel.userAiDigestionPrompt.collectAsState()
    val deletedPredefinedThemes by viewModel.deletedPredefinedThemes.collectAsState()

    var showAddThemeDialog by remember { mutableStateOf(false) }
    var newThemeName by remember { mutableStateOf("") }
    var newThemeHue by remember { mutableStateOf(180f) }
    var newThemeSat by remember { mutableStateOf(1f) }
    var newThemeVal by remember { mutableStateOf(1f) }
    var isNewThemeLightMode by remember { mutableStateOf(false) }
    var isNewThemeApplyToBg by remember { mutableStateOf(true) }
    var themeToDelete by remember { mutableStateOf<String?>(null) }

    var hueValue by remember(customThemeColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(customThemeColor, hsv)
        mutableStateOf(hsv[0])
    }
    
    var geminiKeyInput by remember { mutableStateOf(userGeminiApiKey) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var selectedExportTab by remember { mutableStateOf(0) }
    
    // Track selected IDs for export
    var selectedExportCategories by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedExportTasks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedExportSubTasks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedExportNotes by remember { mutableStateOf<Set<Int>>(emptySet()) }

    // When dialog opens, we can pre-populate with all IDs if we want to "Select All" by default,
    // or just leave them empty based on user preference. Let's pre-populate when they click Export.

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            if (outputStream != null) {
                viewModel.exportBackup(
                    outputStream, 
                    selectedExportCategories, 
                    selectedExportTasks, 
                    selectedExportSubTasks, 
                    selectedExportNotes
                ) { success ->
                    val msg = if (success) context.getString(R.string.backup_exported_success) else context.getString(R.string.export_failed)
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirmDialog = true
        }
    }

    val animatedPrimaryColor = NeonCyan
    val animatedSecondaryColor = NeonPurple
    val animatedTextPrimary = TextPrimary
    val animatedTextSecondary = TextSecondary
    val animatedBorderColor = BorderColor

    val themeRevealState = LocalThemeRevealState.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            
            // Gemini API Key Section
            Text(
                text = stringResource(R.string.gemini_api_key),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = animatedTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            var isEditingApiKey by remember(userGeminiApiKey) { mutableStateOf(userGeminiApiKey.isEmpty()) }
            val apiKeySavedMsg = stringResource(R.string.api_key_saved)

            if (!isEditingApiKey) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(animatedPrimaryColor.copy(alpha = 0.1f))
                        .border(1.dp, animatedPrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "API Key Set",
                            tint = animatedPrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.api_key_configured),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = animatedTextPrimary
                        )
                    }
                    Button(
                        onClick = { isEditingApiKey = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = animatedPrimaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.edit_button), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                OutlinedTextField(
                    value = geminiKeyInput,
                    onValueChange = { geminiKeyInput = it },
                    placeholder = { Text(stringResource(R.string.enter_gemini_api_key), color = animatedTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = animatedPrimaryColor,
                        unfocusedBorderColor = animatedBorderColor,
                        focusedTextColor = animatedTextPrimary,
                        unfocusedTextColor = animatedTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            viewModel.setGeminiApiKey(geminiKeyInput)
                            android.widget.Toast.makeText(context, apiKeySavedMsg, android.widget.Toast.LENGTH_SHORT).show()
                            isEditingApiKey = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = animatedPrimaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.save_key), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    if (userGeminiApiKey.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { 
                                isEditingApiKey = false
                                geminiKeyInput = userGeminiApiKey
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = animatedTextPrimary)
                        ) {
                            Text(stringResource(R.string.cancel_button), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Custom AI Prompts Section
            Text(
                text = stringResource(R.string.custom_ai_prompts),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = animatedTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            var isEditingAiHubPrompt by remember { mutableStateOf(false) }
            var aiHubPromptInput by remember(userAiHubPrompt) { mutableStateOf(userAiHubPrompt) }
            
            Text(stringResource(R.string.intelligence_hub_prompt), color = animatedTextSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            if (!isEditingAiHubPrompt) {
                OutlinedTextField(
                    value = userAiHubPrompt,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = animatedBorderColor,
                        unfocusedBorderColor = animatedBorderColor,
                        focusedTextColor = animatedTextPrimary,
                        unfocusedTextColor = animatedTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { isEditingAiHubPrompt = true },
                    colors = ButtonDefaults.buttonColors(containerColor = animatedPrimaryColor.copy(alpha=0.1f), contentColor = animatedPrimaryColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) { Text(stringResource(R.string.edit_button), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
            } else {
                OutlinedTextField(
                    value = aiHubPromptInput,
                    onValueChange = { aiHubPromptInput = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = animatedPrimaryColor,
                        unfocusedBorderColor = animatedBorderColor,
                        focusedTextColor = animatedTextPrimary,
                        unfocusedTextColor = animatedTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            viewModel.setAiHubPrompt(aiHubPromptInput)
                            isEditingAiHubPrompt = false 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = animatedPrimaryColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(stringResource(R.string.save_key), fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { 
                            isEditingAiHubPrompt = false
                            aiHubPromptInput = userAiHubPrompt
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = animatedTextPrimary)
                    ) { Text(stringResource(R.string.cancel_button), fontWeight = FontWeight.Bold) }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            var isEditingAiDigestionPrompt by remember { mutableStateOf(false) }
            var aiDigestionPromptInput by remember(userAiDigestionPrompt) { mutableStateOf(userAiDigestionPrompt) }
            
            Text(stringResource(R.string.daily_digest_prompt), color = animatedTextSecondary, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            if (!isEditingAiDigestionPrompt) {
                OutlinedTextField(
                    value = userAiDigestionPrompt,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = animatedBorderColor,
                        unfocusedBorderColor = animatedBorderColor,
                        focusedTextColor = animatedTextPrimary,
                        unfocusedTextColor = animatedTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { isEditingAiDigestionPrompt = true },
                    colors = ButtonDefaults.buttonColors(containerColor = animatedPrimaryColor.copy(alpha=0.1f), contentColor = animatedPrimaryColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) { Text(stringResource(R.string.edit_button), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
            } else {
                OutlinedTextField(
                    value = aiDigestionPromptInput,
                    onValueChange = { aiDigestionPromptInput = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = animatedPrimaryColor,
                        unfocusedBorderColor = animatedBorderColor,
                        focusedTextColor = animatedTextPrimary,
                        unfocusedTextColor = animatedTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { 
                            viewModel.setAiDigestionPrompt(aiDigestionPromptInput)
                            isEditingAiDigestionPrompt = false 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = animatedPrimaryColor, contentColor = MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(8.dp),
                    ) { Text(stringResource(R.string.save_key), fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { 
                            isEditingAiDigestionPrompt = false
                            aiDigestionPromptInput = userAiDigestionPrompt
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = animatedTextPrimary)
                    ) { Text(stringResource(R.string.cancel_button), fontWeight = FontWeight.Bold) }
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))

            // Daily Focus Goal Section
            val dailyFocusGoalHours by viewModel.dailyFocusGoalHours.collectAsState()
            Text(
                text = stringResource(R.string.daily_focus_goal),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = animatedTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val hoursStr = if (dailyFocusGoalHours % 1f == 0f) "${dailyFocusGoalHours.toInt()}" else "$dailyFocusGoalHours"
            Text(
                text = stringResource(R.string.current_target_format, "$hoursStr ${stringResource(R.string.hours_unit)}"),
                style = MaterialTheme.typography.bodySmall,
                color = animatedPrimaryColor
            )
            Spacer(modifier = Modifier.height(10.dp))
            Slider(
                value = dailyFocusGoalHours,
                onValueChange = { viewModel.setDailyFocusGoalHours((it * 2).toInt() / 2f) },
                valueRange = 0.5f..12.0f,
                colors = SliderDefaults.colors(
                    thumbColor = animatedPrimaryColor,
                    activeTrackColor = animatedPrimaryColor,
                    inactiveTrackColor = SlateDark
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(1.0f, 2.0f, 3.0f, 4.0f, 6.0f, 8.0f).forEach { preset ->
                    val isSelected = dailyFocusGoalHours == preset
                    Button(
                        onClick = { viewModel.setDailyFocusGoalHours(preset) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) animatedPrimaryColor else SlateDark,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else animatedTextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, if (isSelected) animatedPrimaryColor else animatedBorderColor)
                    ) {
                        Text(
                            text = stringResource(R.string.hours_short, preset.toInt()),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else animatedTextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Distracting Apps Section
            Text(
                text = stringResource(R.string.distracting_apps_section),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = animatedTextPrimary
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.manage_distracting_apps),
                style = MaterialTheme.typography.bodySmall,
                color = animatedTextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            val distractingApps by viewModel.distractingApps.collectAsState()
            var installedApps by remember { mutableStateOf<List<com.goldenminute.catalyst.ui.AppInfo>>(emptyList()) }
            val context = androidx.compose.ui.platform.LocalContext.current

            LaunchedEffect(Unit) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val pm = context.packageManager
                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    }
                    val apps = pm.queryIntentActivities(intent, 0).map { resolveInfo ->
                        val iconDrawable = resolveInfo.loadIcon(pm)
                        val bitmap = if (iconDrawable is android.graphics.drawable.BitmapDrawable) {
                            iconDrawable.bitmap
                        } else if (iconDrawable.intrinsicWidth > 0 && iconDrawable.intrinsicHeight > 0) {
                            val bmp = android.graphics.Bitmap.createBitmap(
                                iconDrawable.intrinsicWidth, 
                                iconDrawable.intrinsicHeight, 
                                android.graphics.Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bmp)
                            iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
                            iconDrawable.draw(canvas)
                            bmp
                        } else null
                        
                        com.goldenminute.catalyst.ui.AppInfo(
                            resolveInfo.loadLabel(pm).toString(), 
                            resolveInfo.activityInfo.packageName, 
                            bitmap?.asImageBitmap()
                        )
                    }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
                    installedApps = apps
                }
            }

            var showAppSelector by remember { mutableStateOf(false) }

            Button(
                onClick = { showAppSelector = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, animatedBorderColor)
            ) {
                Text(text = stringResource(R.string.distracting_apps_section), color = animatedTextPrimary, fontWeight = FontWeight.Bold)
            }

            if (showAppSelector) {
                AppDialog(onDismissRequest = { showAppSelector = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 500.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.distracting_apps_section), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(installedApps) { app ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            val current = distractingApps.toMutableSet()
                                            if (current.contains(app.packageName)) {
                                                current.remove(app.packageName)
                                            } else {
                                                current.add(app.packageName)
                                            }
                                            viewModel.setDistractingApps(current)
                                        }.padding(vertical = 8.dp)
                                    ) {
                                        Checkbox(
                                            checked = distractingApps.contains(app.packageName),
                                            onCheckedChange = { chk ->
                                                val current = distractingApps.toMutableSet()
                                                if (chk) current.add(app.packageName) else current.remove(app.packageName)
                                                viewModel.setDistractingApps(current)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = animatedPrimaryColor)
                                        )
                                        
                                        if (app.icon != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = app.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp).padding(end = 8.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(32.dp).padding(end = 8.dp))
                                        }
                                        
                                        Text(app.name, color = TextPrimary, maxLines = 1)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showAppSelector = false }) {
                                    Text(stringResource(R.string.ok), color = animatedPrimaryColor)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val doomscrollingAlertEnabled by viewModel.doomscrollingAlertEnabled.collectAsState()
            val totalDoomscrollVideos by viewModel.totalDoomscrollVideosCount.collectAsState()
            val totalDoomscrollDurationMs by viewModel.totalDoomscrollDurationMs.collectAsState()
            val isPersian = java.util.Locale.getDefault().language == "fa"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.setDoomscrollingAlertEnabled(!doomscrollingAlertEnabled)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                border = BorderStroke(1.dp, animatedBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.doomscrolling_alert_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = animatedTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.doomscrolling_alert_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = animatedTextSecondary
                            )
                        }
                        Switch(
                            checked = doomscrollingAlertEnabled,
                            onCheckedChange = { viewModel.setDoomscrollingAlertEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = animatedPrimaryColor
                            )
                        )
                    }

                    if (totalDoomscrollVideos > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SlateLight.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, animatedBorderColor.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val formattedTime = formatDurationShort(totalDoomscrollDurationMs, isPersian)
                                Text(
                                    text = stringResource(R.string.doomscrolling_stats_summary, totalDoomscrollVideos, formattedTime),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = animatedPrimaryColor
                                )
                                Text(
                                    text = stringResource(R.string.tab_insights),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = animatedTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Data / Backup Section
            Text(
                text = stringResource(R.string.data_backup),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = animatedTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showExportOptionsDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, animatedBorderColor)
                ) {
                    Text(text = stringResource(R.string.export_backup), color = animatedTextPrimary, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateDark),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, animatedBorderColor)
                ) {
                    Text(text = stringResource(R.string.import_backup), color = animatedTextPrimary, fontWeight = FontWeight.Bold)
                }
            }

            if (showExportOptionsDialog) {
                val allCategories by viewModel.categories.collectAsState()
                val allTodos by viewModel.allTodosList.collectAsState()
                val allSubTasks by viewModel.allSubTasks.collectAsState()
                val allNotes by viewModel.allNotes.collectAsState()

                AppDialog(onDismissRequest = { showExportOptionsDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ObsidianBlack,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 500.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.export_options_title), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.select_data_backup), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            val tabs = listOf(
                                stringResource(R.string.tab_tasks),
                                stringResource(R.string.tab_categories),
                                stringResource(R.string.tab_subtasks),
                                stringResource(R.string.tab_notes)
                            )
                            
                            TabRow(
                                selectedTabIndex = selectedExportTab,
                                containerColor = Color.Transparent,
                                contentColor = animatedPrimaryColor
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedExportTab == index,
                                        onClick = { selectedExportTab = index },
                                        text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Select All Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                val isAllSelected = when(selectedExportTab) {
                                    0 -> selectedExportTasks.size == allTodos.size && allTodos.isNotEmpty()
                                    1 -> selectedExportCategories.size == allCategories.size && allCategories.isNotEmpty()
                                    2 -> selectedExportSubTasks.size == allSubTasks.size && allSubTasks.isNotEmpty()
                                    else -> selectedExportNotes.size == allNotes.size && allNotes.isNotEmpty()
                                }
                                
                                TextButton(onClick = {
                                    when(selectedExportTab) {
                                        0 -> selectedExportTasks = if (isAllSelected) emptySet() else allTodos.map { it.id }.toSet()
                                        1 -> selectedExportCategories = if (isAllSelected) emptySet() else allCategories.map { it.id }.toSet()
                                        2 -> selectedExportSubTasks = if (isAllSelected) emptySet() else allSubTasks.map { it.id }.toSet()
                                        3 -> selectedExportNotes = if (isAllSelected) emptySet() else allNotes.map { it.id }.toSet()
                                    }
                                }) {
                                    Text(if (isAllSelected) stringResource(R.string.action_deselect_all) else stringResource(R.string.action_select_all), color = animatedPrimaryColor)
                                }
                            }

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                when(selectedExportTab) {
                                    0 -> items(allTodos) { todo ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Checkbox(
                                                checked = selectedExportTasks.contains(todo.id),
                                                onCheckedChange = { chk ->
                                                    selectedExportTasks = if (chk) selectedExportTasks + todo.id else selectedExportTasks - todo.id
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = animatedPrimaryColor)
                                            )
                                            Text(todo.title, color = TextPrimary, maxLines = 1)
                                        }
                                    }
                                    1 -> items(allCategories) { cat ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Checkbox(
                                                checked = selectedExportCategories.contains(cat.id),
                                                onCheckedChange = { chk ->
                                                    selectedExportCategories = if (chk) selectedExportCategories + cat.id else selectedExportCategories - cat.id
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = animatedPrimaryColor)
                                            )
                                            Text(cat.name, color = TextPrimary, maxLines = 1)
                                        }
                                    }
                                    2 -> items(allSubTasks) { sub ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Checkbox(
                                                checked = selectedExportSubTasks.contains(sub.id),
                                                onCheckedChange = { chk ->
                                                    selectedExportSubTasks = if (chk) selectedExportSubTasks + sub.id else selectedExportSubTasks - sub.id
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = animatedPrimaryColor)
                                            )
                                            Text(sub.title, color = TextPrimary, maxLines = 1)
                                        }
                                    }
                                    3 -> items(allNotes) { note ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Checkbox(
                                                checked = selectedExportNotes.contains(note.id),
                                                onCheckedChange = { chk ->
                                                    selectedExportNotes = if (chk) selectedExportNotes + note.id else selectedExportNotes - note.id
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = animatedPrimaryColor)
                                            )
                                            Text(note.title.ifEmpty { if (getSystemLocale().language == "fa") "بدون عنوان" else "Untitled" }, color = TextPrimary, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { showExportOptionsDialog = false }) {
                                    Text(stringResource(R.string.cancel), color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                val hasSelection = selectedExportTasks.isNotEmpty() || selectedExportCategories.isNotEmpty() || selectedExportSubTasks.isNotEmpty() || selectedExportNotes.isNotEmpty()
                                Button(
                                    onClick = {
                                        showExportOptionsDialog = false
                                        exportLauncher.launch("hypertask_backup.json")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = animatedPrimaryColor),
                                    enabled = hasSelection
                                ) {
                                    Text(stringResource(R.string.proceed), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            if (showImportConfirmDialog) {
                AppAlertDialog(
                    onDismissRequest = { 
                        showImportConfirmDialog = false 
                        pendingImportUri = null
                    },
                    containerColor = ObsidianBlack,
                    title = {
                        Text(stringResource(R.string.restore_backup_title), color = TextPrimary, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(stringResource(R.string.restore_backup_warning), color = TextSecondary)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                pendingImportUri?.let { uri ->
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    if (inputStream != null) {
                                        viewModel.importBackup(inputStream) { success ->
                                            val msg = if (success) context.getString(R.string.backup_imported_success) else context.getString(R.string.import_failed)
                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                showImportConfirmDialog = false
                                pendingImportUri = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PriorityHigh)
                        ) {
                            Text(stringResource(R.string.overwrite_data), color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showImportConfirmDialog = false 
                                pendingImportUri = null
                            }
                        ) {
                            Text(stringResource(R.string.cancel), color = TextSecondary)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Language Section
            Text(
                text = stringResource(R.string.language_section),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = animatedTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val langEnName = stringResource(R.string.lang_en)
                val langFaName = stringResource(R.string.lang_fa)
                val languages = remember(langEnName, langFaName) {
                    listOf(
                        Pair("en", langEnName),
                        Pair("fa", langFaName)
                    )
                }

                languages.forEach { (langCode, langName) ->
                    val isSelected = currentLang == langCode

                    val animUnselectedBtnBg = SlateDark
                    val animBtnBg = if (isSelected) animatedPrimaryColor else animUnselectedBtnBg
                    val animBtnBorder = if (isSelected) animatedPrimaryColor else animatedBorderColor
                    val animTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else animatedTextPrimary

                    Button(
                        onClick = { viewModel.setLanguage(langCode) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("lang_${langCode}_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = animBtnBg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, animBtnBorder)
                    ) {
                        Text(
                            text = langName,
                            color = animTextColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Theme Section
            Text(
                text = stringResource(R.string.theme_section),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = animatedTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            val cosmicDarkThemeName = stringResource(R.string.theme_cosmic_dark)
            val slateLightThemeName = stringResource(R.string.theme_slate_light)
            val oceanDarkThemeName = stringResource(R.string.theme_ocean_dark)
            val forestDarkThemeName = stringResource(R.string.theme_forest_dark)
            val amberDarkThemeName = stringResource(R.string.theme_amber_dark)
            val roseDarkThemeName = stringResource(R.string.theme_rose_dark)
            val monochromeDarkThemeName = stringResource(R.string.theme_monochrome_dark)

            val defaultThemes = remember(
                deletedPredefinedThemes,
                cosmicDarkThemeName,
                slateLightThemeName,
                oceanDarkThemeName,
                forestDarkThemeName,
                amberDarkThemeName,
                roseDarkThemeName,
                monochromeDarkThemeName
            ) {
                listOf(
                    Triple("cosmic_dark", cosmicDarkThemeName, Color(0xFF00ADB5)),
                    Triple("slate_light", slateLightThemeName, Color(0xFF2563EB)),
                    Triple("ocean_dark", oceanDarkThemeName, Color(0xFF00E5FF)),
                    Triple("forest_dark", forestDarkThemeName, Color(0xFF2DDA8A)),
                    Triple("amber_dark", amberDarkThemeName, Color(0xFFFF9F1C)),
                    Triple("rose_dark", roseDarkThemeName, Color(0xFFFF4D6D)),
                    Triple("monochrome_dark", monochromeDarkThemeName, Color(0xFFFFFFFF))
                ).filter { it.first !in deletedPredefinedThemes }
            }
            
            val themes = remember(defaultThemes, savedCustomThemes) {
                defaultThemes + savedCustomThemes.map { Triple(it.id, it.name, Color(it.color)) }
            }

            val columns = 4
            val rows = (themes.size + columns - 1) / columns

            Column(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (j in 0 until columns) {
                            val index = i * columns + j
                            if (index < themes.size) {
                                val (themeId, themeName, colorAccent) = themes[index]
                                key(themeId) {
                                    val isSelected = currentTheme == themeId
                                    val isLight = themeId == "slate_light"

                                    val scale by animateFloatAsState(
                                        targetValue = if (isSelected) 1.12f else 1f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                        label = "themeScale_$themeId"
                                    )
                                    val circleBorderWidth by animateDpAsState(
                                        targetValue = if (isSelected) 3.dp else 1.5.dp,
                                        animationSpec = tween(300),
                                        label = "themeBorder_$themeId"
                                    )

                                Box(modifier = Modifier.weight(1f)) {
                                    var layoutCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { layoutCoordinates = it }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = { localOffset ->
                                                        if (currentTheme != themeId) {
                                                            val globalOffset = layoutCoordinates?.localToWindow(localOffset) ?: Offset.Zero
                                                            val targetPalette = com.goldenminute.catalyst.ui.theme.getThemePalette(themeId, viewModel.customThemeColor.value, viewModel.customThemeApplyToBg.value)
                                                            themeRevealState.isAnimating = true
                                                            coroutineScope.launch {
                                                                delay(60)
                                                                themeRevealState.requestReveal(globalOffset)
                                                                viewModel.setTheme(themeId)
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp)
                                            .testTag("theme_${themeId}_button"),
                                    ) {
                                    Box(
                                        modifier = Modifier
                                            .scale(scale)
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        colorAccent,
                                                        colorAccent.copy(alpha = 0.6f)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = circleBorderWidth,
                                                color = if (isSelected) animatedPrimaryColor else (if (isLight) Color.White.copy(alpha = 0.8f) else animatedBorderColor),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val checkmarkTransitionState = remember {
                                            MutableTransitionState(isSelected).apply {
                                                targetState = isSelected
                                            }
                                        }
                                        LaunchedEffect(isSelected) {
                                            checkmarkTransitionState.targetState = isSelected
                                        }

                                        androidx.compose.animation.AnimatedVisibility(
                                            visibleState = checkmarkTransitionState,
                                            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                                            exit = scaleOut() + fadeOut()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = themeName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isSelected) animatedTextPrimary else animatedTextSecondary,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        minLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                
                                
                                    if (themes.size > 1) {
                                        IconButton(
                                            onClick = { themeToDelete = themeId },
                                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.delete_button), modifier = Modifier.size(16.dp), tint = animatedTextSecondary)
                                        }
                                    }
                                }
                            }
                        } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            AnimatedVisibility(
                visible = !isThemeAnimating,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 600, easing = LinearOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(50))
            ) {
                Button(
                    onClick = { 
                        newThemeName = ""
                        newThemeHue = 260f
                        newThemeSat = 1f
                        newThemeVal = 1f
                        isNewThemeLightMode = false
                        isNewThemeApplyToBg = true
                        showAddThemeDialog = true 
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, animatedPrimaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.add_custom_theme), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Created by 23 : 59",
                style = MaterialTheme.typography.bodyMedium,
                color = animatedTextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }

    if (showAddThemeDialog) {
        AppDialog(onDismissRequest = { showAddThemeDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(1.dp, animatedBorderColor),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_custom_theme),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = animatedTextPrimary
                    )

                    OutlinedTextField(
                        value = newThemeName,
                        onValueChange = { newThemeName = it },
                        placeholder = { Text(stringResource(R.string.theme_name_placeholder), color = animatedTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = animatedPrimaryColor,
                            unfocusedBorderColor = animatedBorderColor,
                            focusedTextColor = animatedTextPrimary,
                            unfocusedTextColor = animatedTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    val previewColor = android.graphics.Color.HSVToColor(floatArrayOf(newThemeHue, newThemeSat, newThemeVal))
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(previewColor))
                            .border(2.dp, animatedBorderColor, CircleShape)
                    )

                    Text(
                        text = stringResource(R.string.custom_theme_hue),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = animatedTextPrimary
                    )

                    HsvColorPicker(
                        hue = newThemeHue,
                        saturation = newThemeSat,
                        value = newThemeVal,
                        onHueChange = { newThemeHue = it },
                        onSaturationChange = { newThemeSat = it },
                        onValueChange = { newThemeVal = it }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isNewThemeLightMode = !isNewThemeLightMode }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.light_mode_theme),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = animatedTextPrimary
                        )
                        Switch(
                            checked = isNewThemeLightMode,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = animatedPrimaryColor
                            )
                        )
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddThemeDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = animatedTextPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (newThemeName.isNotBlank()) {
                                    viewModel.addCustomTheme(newThemeName, previewColor, isNewThemeLightMode, isNewThemeApplyToBg)
                                    showAddThemeDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = animatedPrimaryColor),
                            shape = RoundedCornerShape(12.dp),
                            enabled = newThemeName.isNotBlank()
                        ) {
                            Text(stringResource(R.string.save_btn), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    themeToDelete?.let { themeIdToDelete ->
        AppDialog(onDismissRequest = { themeToDelete = null }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(1.dp, animatedBorderColor),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_theme_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = animatedTextPrimary
                    )
                    Text(
                        text = stringResource(R.string.delete_theme_confirm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = animatedTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { themeToDelete = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = animatedTextPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                viewModel.deleteTheme(themeIdToDelete)
                                themeToDelete = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PriorityHigh),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.delete_button), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HsvColorPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Saturation/Value Picker Area
        var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        if (size.width > 0 && size.height > 0) {
                            val sat = (change.position.x / size.width).coerceIn(0f, 1f)
                            val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            onSaturationChange(sat)
                            onValueChange(v)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && size.height > 0) {
                            val sat = (offset.x / size.width).coerceIn(0f, 1f)
                            val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            onSaturationChange(sat)
                            onValueChange(v)
                        }
                    }
                }
                .onSizeChanged { size = it }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                drawRect(color = hueColor)
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.Transparent)
                    )
                )
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
                
                if (size.width > 0 && size.height > 0) {
                    val cx = saturation * size.width
                    val cy = (1f - value) * size.height
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = Color.Black,
                        radius = 8.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
        
        // Hue Slider
        Slider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                activeTrackColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            )
        )
    }
}

