package com.goldenminute.catalyst.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.appDataStore
import com.goldenminute.catalyst.ui.QuickHighPriorityTasksActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PowerButtonShortcutManager {

    private const val TAG = "PowerButtonShortcut"

    @Volatile
    var currentMode: String = "none" // "none", "double_press", "long_press"

    private var lastLaunchTime: Long = 0L

    /**
     * Called at application startup, boot completion, and service launch
     * to load saved preference from DataStore and apply component states.
     */
    fun init(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mode = appContext.appDataStore.data.first()[DataStoreKeys.POWER_BUTTON_SHORTCUT_MODE] ?: "none"
                currentMode = mode
                Log.d(TAG, "Initialized PowerButtonShortcutManager with mode: $mode")
                withContext(Dispatchers.Main) {
                    applyMode(appContext, mode)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing PowerButtonShortcutManager", e)
            }
        }
    }

    fun applyMode(context: Context, mode: String) {
        currentMode = mode
        val appContext = context.applicationContext
        when (mode) {
            "double_press" -> {
                setComponentEnabled(appContext, "com.goldenminute.catalyst.CameraShortcutAlias", true)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.AssistShortcutAlias", false)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.service.CatalystVoiceInteractionService", false)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.service.CatalystVoiceInteractionSessionService", false)
            }
            "long_press" -> {
                setComponentEnabled(appContext, "com.goldenminute.catalyst.CameraShortcutAlias", false)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.AssistShortcutAlias", true)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.service.CatalystVoiceInteractionService", true)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.service.CatalystVoiceInteractionSessionService", true)
            }
            else -> {
                setComponentEnabled(appContext, "com.goldenminute.catalyst.CameraShortcutAlias", false)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.AssistShortcutAlias", false)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.service.CatalystVoiceInteractionService", false)
                setComponentEnabled(appContext, "com.goldenminute.catalyst.service.CatalystVoiceInteractionSessionService", false)
            }
        }
    }

    fun setComponentEnabled(context: Context, className: String, enabled: Boolean) {
        try {
            val component = ComponentName(context, className)
            val targetState = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            val currentState = context.packageManager.getComponentEnabledSetting(component)
            if (currentState != targetState) {
                context.packageManager.setComponentEnabledSetting(
                    component,
                    targetState,
                    PackageManager.DONT_KILL_APP
                )
                Log.d(TAG, "Component $className enabled set to: $enabled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling component $className state to $enabled", e)
        }
    }

    fun enableCameraAlias(context: Context, enabled: Boolean) {
        setComponentEnabled(context, "com.goldenminute.catalyst.CameraShortcutAlias", enabled)
    }

    fun handleGlobalActionsEvent(accessibilityService: AccessibilityService) {
        if (currentMode != "long_press") return

        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 1500L) return

        Log.d(TAG, "Global Actions / Power Menu detected during long press mode. Dismissing and launching quick tasks.")

        // Dismiss system power menu
        try {
            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing global back action", e)
        }

        launchQuickTasks(accessibilityService)
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlayPermissionSettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.packageName)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {}
        }
    }

    fun openAssistantSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent("android.settings.VOICE_INPUT_SETTINGS"),
            Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {
                continue
            }
        }
    }

    /**
     * Opens the phone's native side button / power button settings page.
     * Uses manufacturer-specific component intents with comprehensive fallbacks.
     */
    fun openSideButtonSettings(context: Context) {
        val candidates = mutableListOf<Intent>()

        // 1. Samsung One UI specific intents
        candidates.add(Intent("com.samsung.android.settings.SIDE_KEY_SETTINGS"))
        candidates.add(Intent().apply {
            component = ComponentName(
                "com.android.settings",
                "com.android.settings.Settings\$SideKeySettingsActivity"
            )
        })
        candidates.add(Intent("android.intent.action.MAIN").apply {
            component = ComponentName("com.android.settings", "com.android.settings.SubSettings")
            putExtra(":settings:show_fragment", "com.samsung.android.settings.usefulfeature.SideKeySettings")
        })
        candidates.add(Intent().apply {
            component = ComponentName(
                "com.samsung.android.app.routines",
                "com.samsung.android.app.routines.ui.settings.SideKeySettingsActivity"
            )
        })
        candidates.add(Intent().apply {
            component = ComponentName(
                "com.android.settings",
                "com.android.settings.Settings\$UsefulFeatureMainActivity"
            )
        })

        // 2. Xiaomi / Redmi / Poco (MIUI / HyperOS)
        candidates.add(Intent().apply {
            component = ComponentName(
                "com.android.settings",
                "com.android.settings.KeyShortcutSettingsActivity"
            )
        })
        candidates.add(Intent().apply {
            component = ComponentName(
                "com.android.settings",
                "com.android.settings.gesture.GestureSettingsActivity"
            )
        })
        candidates.add(Intent("miui.intent.action.POWER_KEY_SETTINGS"))

        // 3. Android Gestures Settings (Pixel, Motorola, OnePlus, Nothing, standard Android)
        candidates.add(Intent("android.settings.GESTURE_SETTINGS"))
        candidates.add(Intent("com.android.settings.GESTURE_SETTINGS"))

        // 4. Default apps / Assistant settings
        candidates.add(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
        candidates.add(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))

        // 5. App details (to clear default camera if desired)
        candidates.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + context.packageName)
        })

        // 6. Generic settings fallback
        candidates.add(Intent(Settings.ACTION_SETTINGS))

        for (intent in candidates) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {
                continue
            }
        }

        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    fun launchQuickTasks(context: Context, force: Boolean = false) {
        if (!force && currentMode == "none") {
            Log.d(TAG, "launchQuickTasks aborted: currentMode is none")
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 1000L) {
            return // debounce
        }
        lastLaunchTime = now
        val appContext = context.applicationContext

        Log.d(TAG, "Executing launchQuickTasks (force=$force, mode=$currentMode)...")

        // 1. Wake the screen and CPU if needed
        try {
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "catalyst:quick_tasks_wakelock"
            )
            wakeLock?.acquire(3000L)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wakeLock", e)
        }

        // 2. Prepare target intent
        val intent = Intent(appContext, QuickHighPriorityTasksActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            if (force) {
                putExtra("is_preview", true)
            }
        }

        // 3. Direct startActivity from available context
        try {
            if (context is AccessibilityService) {
                context.startActivity(intent)
                Log.d(TAG, "Direct startActivity via AccessibilityService context")
            } else {
                appContext.startActivity(intent)
                Log.d(TAG, "Direct startActivity invoked")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct startActivity failed", e)
        }
    }
}
