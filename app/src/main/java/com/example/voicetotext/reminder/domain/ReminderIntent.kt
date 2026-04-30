package com.example.voicetotext.reminder.domain

import java.util.Locale

data class ReminderIntent(
    val intent: String = CREATE_REMINDER,
    val title: String?,
    val datetime: String?,
    val confidence: Double
) {
    fun toJson(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"intent\": \"${escape(intent)}\",")
            appendLine("  \"title\": ${title?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"datetime\": ${datetime?.let { "\"${escape(it)}\"" } ?: "null"},")
            appendLine("  \"confidence\": ${confidence.formatConfidence()}")
            append("}")
        }
    }

    private fun escape(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun Double.formatConfidence(): String = String.format(Locale.US, "%.1f", this)

    companion object {
        const val CREATE_REMINDER = "CREATE_REMINDER"

        fun empty(): ReminderIntent = ReminderIntent(
            title = null,
            datetime = null,
            confidence = 0.0
        )
    }
}
