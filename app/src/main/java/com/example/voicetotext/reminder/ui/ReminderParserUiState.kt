package com.example.voicetotext.reminder.ui

data class ReminderParserUiState(
    val hasMicrophonePermission: Boolean = false,
    val mode: VoiceActionMode = VoiceActionMode.Idle,
    val transcript: String = "",
    val resolvedActionTitle: String = "Microphone access required",
    val resolvedActionSubtitle: String = "Allow microphone access so voice capture can run fully on-device.",
    val outputJson: String = ""
)

enum class VoiceActionMode {
    Idle,
    Listening,
    Processing
}
