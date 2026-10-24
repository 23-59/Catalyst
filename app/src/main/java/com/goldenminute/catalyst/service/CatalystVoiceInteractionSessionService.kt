package com.goldenminute.catalyst.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.goldenminute.catalyst.ui.QuickHighPriorityTasksActivity

class CatalystVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return CatalystVoiceInteractionSession(this)
    }
}

class CatalystVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, flags: Int) {
        super.onShow(args, flags)
        hide()
        if (PowerButtonShortcutManager.currentMode == "long_press") {
            PowerButtonShortcutManager.launchQuickTasks(context)
        }
    }
}
