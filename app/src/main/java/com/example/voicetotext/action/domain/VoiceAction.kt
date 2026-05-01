package com.example.voicetotext.action.domain

import java.util.Locale

sealed interface VoiceAction {
    val confidence: Double

    fun actionTitle(): String

    fun actionSubtitle(): String

    fun toJson(): String

    data class SetTimer(
        val durationSeconds: Int,
        val label: String?,
        override val confidence: Double
    ) : VoiceAction {
        override fun actionTitle(): String = “Set timer for ${durationSeconds.toDurationLabel()}”

        override fun actionSubtitle(): String {
            return if (label.isNullOrBlank()) “No label” else “Label: $label”
        }

        override fun toJson(): String {
            return buildString {
                appendLine(“{“)
                appendLine(“  \”intent\”: \”SET_TIMER\”,”)
                appendLine(“  \”duration_seconds\”: $durationSeconds,”)
                appendLine(“  \”label\”: ${label?.let { “\”${escape(it)}\”” } ?: “null”},”)
                appendLine(“  \”confidence\”: ${formatConfidence(confidence)}”)
                append(“}”)
            }
        }
    }

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String?,
        override val confidence: Double
    ) : VoiceAction {
        override fun actionTitle(): String = “Set alarm for ${hour.toHour12()}:${minute.toString().padStart(2, ‘0’)} ${hour.toAmPm()}”

        override fun actionSubtitle(): String {
            return if (label.isNullOrBlank()) “No label” else “Label: $label”
        }

        override fun toJson(): String {
            return buildString {
                appendLine(“{“)
                appendLine(“  \”intent\”: \”SET_ALARM\”,”)
                appendLine(“  \”hour\”: $hour,”)
                appendLine(“  \”minute\”: $minute,”)
                appendLine(“  \”label\”: ${label?.let { “\”${escape(it)}\”” } ?: “null”},”)
                appendLine(“  \”confidence\”: ${formatConfidence(confidence)}”)
                append(“}”)
            }
        }
    }

    data class Unknown(
        override val confidence: Double
    ) : VoiceAction {
        override fun actionTitle(): String = “Couldn’t parse action”

        override fun actionSubtitle(): String = “Try saying “set a timer for 10 minutes for pasta” or “set an alarm for 7 AM”.”

        override fun toJson(): String {
            return buildString {
                appendLine(“{“)
                appendLine(“  \”intent\”: \”UNKNOWN_ACTION\”,”)
                appendLine(“  \”duration_seconds\”: null,”)
                appendLine(“  \”label\”: null,”)
                appendLine(“  \”confidence\”: ${formatConfidence(confidence)}”)
                append(“}”)
            }
        }
    }

    companion object {
        fun empty(): VoiceAction = Unknown(confidence = 0.0)

        private fun formatConfidence(value: Double): String = String.format(Locale.US, “%.1f”, value)

        internal fun escape(value: String): String {
            return value.replace(“\\”, “\\\\”).replace(“\””, “\\\””)
        }

        private fun Int.toDurationLabel(): String {
            val hours = this / 3600
            val minutes = (this % 3600) / 60
            val seconds = this % 60
            val parts = buildList {
                if (hours > 0) add(“$hours ${if (hours == 1) “hour” else “hours”}”)
                if (minutes > 0) add(“$minutes ${if (minutes == 1) “minute” else “minutes”}”)
                if (seconds > 0 || isEmpty()) add(“$seconds ${if (seconds == 1) “second” else “seconds”}”)
            }
            return parts.joinToString(“ “)
        }

        private fun Int.toHour12(): String = when (this) {
            0, 12 -> “12”
            in 1..11 -> this.toString()
            else -> (this - 12).toString()
        }

        private fun Int.toAmPm(): String = if (this < 12) “AM” else “PM”
    }
}
