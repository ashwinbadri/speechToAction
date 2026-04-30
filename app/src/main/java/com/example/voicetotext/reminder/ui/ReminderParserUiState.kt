package com.example.voicetotext.reminder.ui

data class ReminderParserUiState(
    val mode: VoiceActionMode = VoiceActionMode.Idle,
    val transcript: String = "",
    val resolvedActionTitle: String = "No action yet",
    val resolvedActionSubtitle: String = "Tap the mic and say something like “set a timer for 10 minutes”.",
    val outputJson: String = ""
)

enum class VoiceActionMode {
    Idle,
    Listening,
    Processing
}
