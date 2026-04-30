package com.example.voicetotext.reminder.ui

import com.example.voicetotext.reminder.domain.ReminderIntent

data class ReminderParserUiState(
    val input: String = "",
    val outputJson: String = ReminderIntent.empty().toJson()
)
