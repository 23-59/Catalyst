package com.goldenminute.catalyst.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.goldenminute.catalyst.data.TodoDatabase
import com.goldenminute.catalyst.ui.TimerStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmartTrackerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastPackageName = ""
    private var isCurrentlyChecking = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("SmartTracker", "Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // Fast tracking check on window state changed
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (packageName != lastPackageName) {
                lastPackageName = packageName
                checkAppAssociation(packageName)
            }
        }

        // Mindfulness check on content changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (packageName == lastPackageName && !isCurrentlyChecking) {
                isCurrentlyChecking = true
                scope.launch {
                    performMindfulnessCheck(rootInActiveWindow)
                    isCurrentlyChecking = false
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d("SmartTracker", "Accessibility Service Interrupted")
    }

    private fun checkAppAssociation(packageName: String) {
        scope.launch {
            val db = TodoDatabase.getDatabase(applicationContext, scope)
            val allTodos = db.todoDao().getAllTodos().first()
            
            val linkedTodo = allTodos.find { it.associatedPackageName == packageName && !it.isCompleted }
            
            if (linkedTodo != null) {
                // We found a task for this app! Start the timer automatically.
                Log.d("SmartTracker", "Starting timer for task ${linkedTodo.id}")
                TimerStateManager.autoStartTask(linkedTodo.id)
            } else {
                // If it's not a Catalyst related app, pause timer if it was running.
                if (packageName != "com.goldenminute.catalyst" && TimerStateManager.isTimerRunning.value) {
                    Log.d("SmartTracker", "User switched away from linked app. Pausing timer.")
                    TimerStateManager.autoPauseTask(idle = false)
                }
            }
        }
    }

    private suspend fun performMindfulnessCheck(node: AccessibilityNodeInfo?) {
        if (node == null) return
        
        // Example for Instagram
        if (lastPackageName == "com.instagram.android") {
            // Check if the timer is running and tracking mode is Strict
            val activeTaskId = TimerStateManager.activeTaskId.value
            if (activeTaskId != -1 && TimerStateManager.isTimerRunning.value) {
                val db = TodoDatabase.getDatabase(applicationContext, scope)
                val activeTodo = db.todoDao().getAllTodos().first().find { it.id == activeTaskId }
                
                if (activeTodo != null && activeTodo.trackingMode == "Strict") {
                    val isDoomscrolling = scanNodesForText(node, listOf("Reels", "Explore"))
                    if (isDoomscrolling) {
                        Log.d("SmartTracker", "Doomscrolling detected in strict mode. Pausing timer.")
                        // Pause the timer and flag as idle
                        TimerStateManager.autoPauseTask(idle = true)
                        
                        // Launch Mindfulness Check Intent (In a real app, this would start a dialog activity)
                        val intent = Intent(applicationContext, com.goldenminute.catalyst.MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("SHOW_MINDFULNESS_CHECK", true)
                        startActivity(intent)
                    }
                }
            }
        }
        node.recycle()
    }

    private fun scanNodesForText(node: AccessibilityNodeInfo?, keywords: List<String>): Boolean {
        if (node == null) return false
        if (node.text != null) {
            val nodeText = node.text.toString().lowercase()
            for (keyword in keywords) {
                if (nodeText.contains(keyword.lowercase())) {
                    return true
                }
            }
        }
        for (i in 0 until node.childCount) {
            if (scanNodesForText(node.getChild(i), keywords)) {
                return true
            }
        }
        return false
    }
}
