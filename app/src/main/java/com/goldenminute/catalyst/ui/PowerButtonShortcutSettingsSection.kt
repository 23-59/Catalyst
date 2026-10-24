package com.goldenminute.catalyst.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.service.PowerButtonShortcutManager
import com.goldenminute.catalyst.service.SmartTrackerService
import com.goldenminute.catalyst.ui.theme.*

@Composable
fun PowerButtonShortcutSettingsSection(
    viewModel: TodoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentMode by viewModel.powerButtonShortcutMode.collectAsState()

    var isAccessibilityRunning by remember { mutableStateOf(SmartTrackerService.isRunning) }
    var hasOverlayPermission by remember { mutableStateOf(PowerButtonShortcutManager.hasOverlayPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityRunning = SmartTrackerService.isRunning
                hasOverlayPermission = PowerButtonShortcutManager.hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.power_button_shortcut_title),
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
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (currentMode) {
                                "double_press" -> NeonCyan.copy(alpha = 0.15f)
                                "long_press" -> PriorityHigh.copy(alpha = 0.15f)
                                else -> BorderColor.copy(alpha = 0.2f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when (currentMode) {
                                    "double_press" -> NeonCyan.copy(alpha = 0.4f)
                                    "long_press" -> PriorityHigh.copy(alpha = 0.4f)
                                    else -> BorderColor.copy(alpha = 0.4f)
                                }
                            ),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = when (currentMode) {
                                        "double_press" -> NeonCyan
                                        "long_press" -> PriorityHigh
                                        else -> TextSecondary
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = stringResource(R.string.power_button_shortcut_title),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (currentMode) {
                                    "double_press" -> stringResource(R.string.power_shortcut_double_press)
                                    "long_press" -> stringResource(R.string.power_shortcut_long_press)
                                    else -> stringResource(R.string.power_shortcut_off)
                                },
                                color = when (currentMode) {
                                    "double_press" -> NeonCyan
                                    "long_press" -> PriorityHigh
                                    else -> TextSecondary
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Active badge
                    if (currentMode != "none") {
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
                                    text = stringResource(R.string.power_shortcut_active),
                                    color = PriorityLow,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.power_button_shortcut_desc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Disabled
                ShortcutOptionRow(
                    title = stringResource(R.string.power_shortcut_off),
                    subtitle = stringResource(R.string.power_shortcut_off_desc),
                    isSelected = currentMode == "none",
                    onClick = {
                        viewModel.setPowerButtonShortcutMode("none")
                        PowerButtonShortcutManager.applyMode(context, "none")
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.power_shortcut_disabled_toast),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                if (currentMode == "none") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BorderColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.power_shortcut_disabled_info_title),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.power_shortcut_disabled_info_desc),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { PowerButtonShortcutManager.openAssistantSettings(context) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.power_shortcut_restore_assistant),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: Double Press
                ShortcutOptionRow(
                    title = stringResource(R.string.power_shortcut_double_press),
                    subtitle = stringResource(R.string.power_shortcut_double_press_desc),
                    isSelected = currentMode == "double_press",
                    accentColor = NeonCyan,
                    onClick = {
                        viewModel.setPowerButtonShortcutMode("double_press")
                        PowerButtonShortcutManager.applyMode(context, "double_press")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: Long Press
                ShortcutOptionRow(
                    title = stringResource(R.string.power_shortcut_long_press),
                    subtitle = stringResource(R.string.power_shortcut_long_press_desc),
                    isSelected = currentMode == "long_press",
                    accentColor = PriorityHigh,
                    onClick = {
                        viewModel.setPowerButtonShortcutMode("long_press")
                        PowerButtonShortcutManager.applyMode(context, "long_press")
                    }
                )

                // Overlay Permission Alert (Required for launching from background / lockscreen)
                if (currentMode != "none" && !hasOverlayPermission) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PriorityMedium.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, PriorityMedium.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = PriorityMedium,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.power_shortcut_overlay_permission_title),
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.power_shortcut_overlay_permission_desc),
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { PowerButtonShortcutManager.openOverlayPermissionSettings(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = PriorityMedium.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PriorityMedium.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PriorityMedium,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.power_shortcut_grant_permission),
                                        color = PriorityMedium,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Contextual guide for Double Press Mode
                AnimatedVisibility(visible = currentMode == "double_press") {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonCyan.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.power_shortcut_open_phone_settings_desc),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SlateDark,
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = stringResource(R.string.power_shortcut_hint_double_native),
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { PowerButtonShortcutManager.openSideButtonSettings(context) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.power_shortcut_open_phone_settings),
                                                    color = NeonCyan,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Contextual guide for Long Press Mode
                AnimatedVisibility(visible = currentMode == "long_press") {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PriorityHigh.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assistant,
                                        contentDescription = null,
                                        tint = PriorityHigh,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.power_shortcut_hint_long_native),
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { PowerButtonShortcutManager.openAssistantSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PriorityHigh.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.6f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Assistant,
                                            contentDescription = null,
                                            tint = PriorityHigh,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.power_shortcut_set_default_assistant),
                                            color = PriorityHigh,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (!isAccessibilityRunning) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(R.string.power_shortcut_accessibility_required),
                                        color = PriorityMedium,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                })
                                            } catch (_: Exception) {}
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PriorityMedium.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, PriorityMedium.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.power_shortcut_enable_accessibility),
                                            color = PriorityMedium,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick test preview button
                if (currentMode != "none") {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            PowerButtonShortcutManager.launchQuickTasks(context, force = true)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.power_shortcut_test_btn),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color = NeonCyan,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ObsidianBlack else SlateDark,
        border = BorderStroke(
            1.dp,
            if (isSelected) accentColor else BorderColor.copy(alpha = 0.5f)
        ),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = TextSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
