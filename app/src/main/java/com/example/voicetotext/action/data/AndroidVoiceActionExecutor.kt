package com.example.voicetotext.action.data

import android.provider.AlarmClock
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.example.voicetotext.action.domain.ExecutionResult
import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionExecutor
import com.example.voicetotext.core.logging.AppLogger

class AndroidVoiceActionExecutor(private val context: Context) : VoiceActionExecutor {

    companion object {
        private const val TAG = "VoiceActionExecutor"
    }

    override fun execute(action: VoiceAction): ExecutionResult {
        AppLogger.d(TAG, "Executing action=${action.javaClass.simpleName}")
        return when (action) {
            is VoiceAction.SetTimer -> launchTimer(action)
            is VoiceAction.SetAlarm -> launchAlarm(action)
            is VoiceAction.Unknown -> ExecutionResult.Failure("No action to execute.")
        }
    }

    private fun launchTimer(action: VoiceAction.SetTimer): ExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, action.durationSeconds)
            if (!action.label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return launch(intent)
    }

    private fun launchAlarm(action: VoiceAction.SetAlarm): ExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
            if (!action.label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return launch(intent)
    }

    private fun launch(intent: Intent): ExecutionResult {
        return try {
            context.startActivity(intent)
            AppLogger.d(TAG, "Intent launched successfully action=${intent.action}")
            ExecutionResult.Success
        } catch (e: ActivityNotFoundException) {
            val reason = "No clock app found to handle this request."
            AppLogger.w(TAG, "ActivityNotFoundException: $reason")
            ExecutionResult.Failure(reason)
        }
    }
}
