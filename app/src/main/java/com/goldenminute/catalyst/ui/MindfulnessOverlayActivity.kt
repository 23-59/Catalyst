package com.goldenminute.catalyst.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontFamily
import com.goldenminute.catalyst.LocalizedContent
import com.goldenminute.catalyst.ui.theme.Vazirmatn
import com.goldenminute.catalyst.ui.theme.getTypography
import com.goldenminute.catalyst.data.DataStoreKeys
import com.goldenminute.catalyst.data.appDataStore
import com.goldenminute.catalyst.service.SmartTrackerService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.lifecycleScope
import com.goldenminute.catalyst.data.TodoDatabase
import android.os.Build
import android.view.WindowManager
import java.util.Locale

class MindfulnessOverlayActivity : ComponentActivity() {
    companion object {
        private var instanceRef: java.lang.ref.WeakReference<MindfulnessOverlayActivity>? = null

        fun isShowing(): Boolean {
            val act = instanceRef?.get() ?: return false
            return !act.isFinishing && !act.isDestroyed
        }

        fun dismissIfShowing() {
            try {
                instanceRef?.get()?.dismissOverlay()
            } catch (_: Exception) {}
        }
    }

    private var extraPackageName: String = ""

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instanceRef = java.lang.ref.WeakReference(this)
        SmartTrackerService.isOverlayShowing = true
        SmartTrackerService.overlayShownTime = System.currentTimeMillis()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        SmartTrackerService.isOverlayShowing = true
        SmartTrackerService.overlayShownTime = System.currentTimeMillis()
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            SmartTrackerService.onMindfulnessDismissed()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            dismissOverlay()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop re-delivery — update extras but don't re-create
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appName = intent.getStringExtra("EXTRA_APP_NAME") ?: ""
        val sectionName = intent.getStringExtra("EXTRA_SECTION_NAME") ?: ""
        extraPackageName = intent.getStringExtra("EXTRA_PACKAGE_NAME") ?: ""
        val totalWastedMs = intent.getLongExtra("EXTRA_TOTAL_DOOMSCROLL_MS", 0L)
        val langFromIntent = intent.getStringExtra("EXTRA_LANGUAGE")
        val showHighPriorityTasksFromIntent = intent.getBooleanExtra("EXTRA_SHOW_HIGH_PRIORITY_TASKS", false)

        // This is a transparent activity that behaves as an overlay
        setContent {
            val prefs by applicationContext.appDataStore.data.collectAsState(initial = null)
            val lang = prefs?.get(DataStoreKeys.LANGUAGE)
                ?: langFromIntent
                ?: (if (Locale.getDefault().language == "fa") "fa" else "en")

            val showHighPriorityTasks = showHighPriorityTasksFromIntent

            val db = androidx.compose.runtime.remember { TodoDatabase.getDatabase(applicationContext, lifecycleScope) }
            val allTodos by db.todoDao().getAllTodos().collectAsState(initial = emptyList())
            val pendingTodos = androidx.compose.runtime.remember(allTodos) { allTodos.filter { !it.isCompleted } }
            val highPriorityTodos = androidx.compose.runtime.remember(pendingTodos) {
                val highs = pendingTodos.filter { it.priority == 2 }
                if (highs.isNotEmpty()) highs else pendingTodos.sortedByDescending { it.priority }
            }

            LocalizedContent(localeCode = lang) {
                val isRtl = lang == "fa"
                val font = if (isRtl) Vazirmatn else FontFamily.Default
                MaterialTheme(typography = getTypography(font)) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable(
                                    interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    dismissOverlay()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            MindfulnessCheckDialog(
                                appName = appName,
                                sectionName = sectionName,
                                languageCode = lang,
                                totalWastedMs = totalWastedMs,
                                showHighPriorityTasks = showHighPriorityTasks,
                                highPriorityTasks = highPriorityTodos.take(3),
                                onDismiss = {
                                    dismissOverlay()
                                },
                                onInspiration = {
                                    if (extraPackageName == "com.instagram.android" || sectionName == "Reels") {
                                        SmartTrackerService.onReelMarkedInspiration()
                                        SmartTrackerService.resetDistractingTimeForApp(extraPackageName)
                                        SmartTrackerService.setMindfulnessCooldown(extraPackageName, 15 * 60 * 1000L)
                                        dismissOverlay()
                                    } else {
                                        SmartTrackerService.resetDoomscrollSession()
                                        // Set cooldown for task-linked or other apps
                                        SmartTrackerService.setMindfulnessCooldown(extraPackageName, 15 * 60 * 1000L)
                                        TimerStateManager.mindfulnessCooldownUntil = System.currentTimeMillis() + 15 * 60 * 1000L
                                        dismissOverlay()
                                    }
                                },
                                onDoomscrolling = {
                                    SmartTrackerService.onDoomscrollingMarked(applicationContext, extraPackageName)
                                    SmartTrackerService.resetDistractingTimeForApp(extraPackageName)
                                    // Do NOT dismiss here; the dialog flips to show high priority tasks and the Got It button
                                },
                                onTaskSelected = { task ->
                                    TimerStateManager.autoStartTask(task.id)
                                    SmartTrackerService.resetDoomscrollSession()
                                    val launchIntent = packageManager.getLaunchIntentForPackage("com.goldenminute.catalyst")?.apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    }
                                    if (launchIntent != null) {
                                        startActivity(launchIntent)
                                    }
                                    dismissOverlay()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun dismissOverlay() {
        SmartTrackerService.onMindfulnessDismissed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instanceRef?.get() == this) {
            instanceRef = null
        }
        // Safety net: always clear the flag when the activity is destroyed
        SmartTrackerService.onMindfulnessDismissed()
    }
}
