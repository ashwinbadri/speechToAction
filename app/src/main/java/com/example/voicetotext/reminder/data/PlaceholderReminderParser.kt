package com.example.voicetotext.reminder.data

import com.example.voicetotext.reminder.domain.ReminderIntent
import com.example.voicetotext.reminder.domain.ReminderParser

class PlaceholderReminderParser : ReminderParser {
    override fun parse(input: String): ReminderIntent {
        val normalizedInput = input.trim()

        return if (normalizedInput.isBlank()) {
            ReminderIntent.empty()
        } else {
            ReminderIntent(
                title = normalizedInput,
                datetime = null,
                confidence = 0.2
            )
        }
    }
}
