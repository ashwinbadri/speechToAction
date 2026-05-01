package com.example.voicetotext.reminder.ui

import com.example.voicetotext.action.domain.ExecutionResult
import com.example.voicetotext.action.domain.VoiceAction

data class ReminderParserUiState(
    val hasMicrophonePermission: Boolean = false,
    val mode: VoiceActionMode = VoiceActionMode.Idle,
    val transcript: String = "",
    val resolvedActionTitle: String = "Microphone access required",
    val resolvedActionSubtitle: String = "Allow microphone access so voice capture can run fully on-device.",
    val outputJson: String = "",
    val lastAction: VoiceAction? = null,
    val executionResult: ExecutionResult? = null
)

enum class VoiceActionMode {
    Idle, Listening, Processing
}
