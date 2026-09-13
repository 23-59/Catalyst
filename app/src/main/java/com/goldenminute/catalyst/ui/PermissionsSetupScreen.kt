package com.goldenminute.catalyst.ui

import android.Manifest
import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.goldenminute.catalyst.R
import com.goldenminute.catalyst.service.SmartTrackerService
import com.goldenminute.catalyst.ui.theme.*

data class PermissionItemState(
    val id: String,
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
    val isGranted: Boolean,
    val onGrant: () -> Unit
)

@Composable
fun PermissionsSetupScreen(
    viewModel: TodoViewModel,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State refresh trigger on activity resume
    var refreshCounter by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshCounter++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission state checks
    val isNotificationsGranted = remember(refreshCounter) { checkNotificationsPermission(context) }
    val isMicGranted = remember(refreshCounter) { checkMicPermission(context) }
    val isExactAlarmGranted = remember(refreshCounter) { checkExactAlarmPermission(context) }
    val isOverlayGranted = remember(refreshCounter) { checkOverlayPermission(context) }
    val isAccessibilityGranted = remember(refreshCounter) { checkAccessibilityService(context) }
    val isBatteryIgnored = remember(refreshCounter) { checkBatteryOptimization(context) }

    // Multi-permission runtime launcher
    var isSequentialFlowActive by remember { mutableStateOf(false) }

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshCounter++
        if (isSequentialFlowActive) {
            // Move to next step in sequential flow
            triggerNextUngrantedSpecialPermission(
                context = context,
                isExactAlarm = checkExactAlarmPermission(context),
                isOverlay = checkOverlayPermission(context),
                isAccessibility = checkAccessibilityService(context),
                isBattery = checkBatteryOptimization(context)
            )
        }
    }

    fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationsGranted) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!isMicGranted) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionsToRequest.isNotEmpty()) {
            runtimeLauncher.launch(permissionsToRequest.toTypedArray())
        } else if (isSequentialFlowActive) {
            triggerNextUngrantedSpecialPermission(
                context = context,
                isExactAlarm = isExactAlarmGranted,
                isOverlay = isOverlayGranted,
                isAccessibility = isAccessibilityGranted,
                isBattery = isBatteryIgnored
            )
        }
    }

    fun startSequentialGrantFlow() {
        isSequentialFlowActive = true
        val hasUngrantedRuntime = (!isNotificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) || !isMicGranted
        if (hasUngrantedRuntime) {
            requestRuntimePermissions()
        } else {
            triggerNextUngrantedSpecialPermission(
                context = context,
                isExactAlarm = isExactAlarmGranted,
                isOverlay = isOverlayGranted,
                isAccessibility = isAccessibilityGranted,
                isBattery = isBatteryIgnored
            )
        }
    }

    val allGranted = isNotificationsGranted && isMicGranted && isExactAlarmGranted && isOverlayGranted && isAccessibilityGranted && isBatteryIgnored

    val permissionItems = remember(
        isNotificationsGranted,
        isMicGranted,
        isExactAlarmGranted,
        isOverlayGranted,
        isAccessibilityGranted,
        isBatteryIgnored
    ) {
        listOf(
            PermissionItemState(
                id = "notifications",
                icon = Icons.Default.Notifications,
                titleRes = R.string.perm_notifications_title,
                descRes = R.string.perm_notifications_desc,
                isGranted = isNotificationsGranted,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        runtimeLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    } else {
                        openAppNotificationSettings(context)
                    }
                }
            ),
            PermissionItemState(
                id = "microphone",
                icon = Icons.Default.Mic,
                titleRes = R.string.perm_microphone_title,
                descRes = R.string.perm_microphone_desc,
                isGranted = isMicGranted,
                onGrant = {
                    runtimeLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            ),
            PermissionItemState(
                id = "overlay",
                icon = Icons.Default.Layers,
                titleRes = R.string.perm_overlay_title,
                descRes = R.string.perm_overlay_desc,
                isGranted = isOverlayGranted,
                onGrant = {
                    openOverlaySettings(context)
                }
            ),
            PermissionItemState(
                id = "accessibility",
                icon = Icons.Default.TrackChanges,
                titleRes = R.string.perm_accessibility_title,
                descRes = R.string.perm_accessibility_desc,
                isGranted = isAccessibilityGranted,
                onGrant = {
                    openAccessibilitySettings(context)
                }
            ),
            PermissionItemState(
                id = "exact_alarms",
                icon = Icons.Default.Alarm,
                titleRes = R.string.perm_exact_alarms_title,
                descRes = R.string.perm_exact_alarms_desc,
                isGranted = isExactAlarmGranted,
                onGrant = {
                    openExactAlarmSettings(context)
                }
            ),
            PermissionItemState(
                id = "battery",
                icon = Icons.Default.Bolt,
                titleRes = R.string.perm_battery_title,
                descRes = R.string.perm_battery_desc,
                isGranted = isBatteryIgnored,
                onGrant = {
                    openBatteryOptimizationSettings(context)
                }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        AnimatedCosmicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with title and skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.permissions_setup_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.permissions_setup_subtitle),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }

                TextButton(
                    onClick = onFinished,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.skip_for_now),
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable list of permissions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                permissionItems.forEach { item ->
                    PermissionCard(
                        item = item
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Bottom CTA Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = {
                        if (allGranted) {
                            onFinished()
                        } else {
                            startSequentialGrantFlow()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (allGranted) {
                                    Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF10B981)))
                                } else {
                                    Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (allGranted) {
                                    stringResource(R.string.continue_to_app)
                                } else {
                                    stringResource(R.string.grant_all_permissions)
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                imageVector = if (allGranted) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    item: PermissionItemState
) {
    val cardBg = if (item.isGranted) SlateDark.copy(alpha = 0.9f) else SlateLight.copy(alpha = 0.85f)
    val borderColor = if (item.isGranted) Color(0xFF10B981).copy(alpha = 0.5f) else BorderColor.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (item.isGranted) Color(0xFF10B981).copy(alpha = 0.15f)
                    else NeonCyan.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.isGranted) Color(0xFF10B981) else NeonCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.titleRes),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(item.descRes),
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Action / Status Badge
        if (item.isGranted) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.permission_granted),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        } else {
            Button(
                onClick = item.onGrant,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.2f),
                    contentColor = NeonCyan
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = stringResource(R.string.grant_permission),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission Check & Intent Helpers
// ─────────────────────────────────────────────────────────────────────────────

fun checkNotificationsPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

fun checkMicPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

fun checkExactAlarmPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else {
        true
    }
}

fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun checkAccessibilityService(context: Context): Boolean {
    // 1. Direct in-memory running check
    if (SmartTrackerService.isRunning) return true

    // 2. Official AccessibilityManager enabled services check
    try {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
        val enabledList = am?.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        if (enabledList != null && enabledList.any { info ->
            info.resolveInfo?.serviceInfo?.packageName == context.packageName ||
            (info.id != null && info.id.contains(context.packageName, ignoreCase = true) && info.id.contains("SmartTrackerService", ignoreCase = true))
        }) {
            return true
        }
    } catch (_: Exception) {}

    // 3. Settings.Secure check with flexible matching
    try {
        val expectedComponentName = ComponentName(context, SmartTrackerService::class.java).flattenToString()
        val expectedShortComponentName = ComponentName(context, SmartTrackerService::class.java).flattenToShortString()
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
        stringColonSplitter.setString(enabledServicesSetting)
        while (stringColonSplitter.hasNext()) {
            val componentNameString = stringColonSplitter.next()
            if (componentNameString.equals(expectedComponentName, ignoreCase = true) ||
                componentNameString.equals(expectedShortComponentName, ignoreCase = true) ||
                (componentNameString.contains(context.packageName, ignoreCase = true) && componentNameString.contains("SmartTrackerService", ignoreCase = true))
            ) {
                return true
            }
        }
    } catch (_: Exception) {}

    return false
}

fun checkBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

fun triggerNextUngrantedSpecialPermission(
    context: Context,
    isExactAlarm: Boolean,
    isOverlay: Boolean,
    isAccessibility: Boolean,
    isBattery: Boolean
) {
    when {
        !isOverlay -> openOverlaySettings(context)
        !isAccessibility -> openAccessibilitySettings(context)
        !isExactAlarm -> openExactAlarmSettings(context)
        !isBattery -> openBatteryOptimizationSettings(context)
    }
}

fun openOverlaySettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}

fun openBatteryOptimizationSettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }
}

fun openAppNotificationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
