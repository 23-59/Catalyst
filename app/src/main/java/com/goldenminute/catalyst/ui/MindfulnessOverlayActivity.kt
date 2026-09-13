package com.goldenminute.catalyst.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.goldenminute.catalyst.service.SmartTrackerService
import com.goldenminute.catalyst.ui.theme.MyApplicationTheme

class MindfulnessOverlayActivity : ComponentActivity() {
    private var extraPackageName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SmartTrackerService.isOverlayShowing = true
        SmartTrackerService.overlayShownTime = System.currentTimeMillis()
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
        // If user pressed home, switched apps, or overlay lost focus, dismiss cleanly
        dismissOverlay()
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

        // This is a transparent activity that behaves as an overlay
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
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
                    onDismiss = {
                        dismissOverlay()
                    },
                    onInspiration = {
                        // Set cooldown for this specific app
                        SmartTrackerService.setMindfulnessCooldown(extraPackageName, 15 * 60 * 1000L)
                        // Also set the global cooldown for task-linked behavior
                        TimerStateManager.mindfulnessCooldownUntil = System.currentTimeMillis() + 15 * 60 * 1000L
                        // Resume if there was an active task
                        if (TimerStateManager.activeTaskId.value != -1) {
                            TimerStateManager.autoStartTask(TimerStateManager.activeTaskId.value)
                        }
                        dismissOverlay()
                    },
                    onDoomscrolling = {
                        SmartTrackerService.onDoomscrollingMarked(applicationContext, extraPackageName)
                        // Reset distracting time so the warning fires again after another 5 min
                        SmartTrackerService.resetDistractingTimeForApp(extraPackageName)
                        dismissOverlay()
                    }
                )
            }
        }
    }

    private fun dismissOverlay() {
        SmartTrackerService.onMindfulnessDismissed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Safety net: always clear the flag when the activity is destroyed
        SmartTrackerService.onMindfulnessDismissed()
    }
}
