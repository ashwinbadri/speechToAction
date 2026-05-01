package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction

object VoiceActionJsonParser {

    fun parse(rawResponse: String): VoiceAction? {
        val normalized = rawResponse
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val intent = intentRegex.find(normalized)?.groupValues?.get(1) ?: return null
        val confidence = confidenceRegex.find(normalized)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

        return when (intent) {
            "SET_TIMER" -> {
                val durationSeconds = durationRegex.find(normalized)?.groupValues?.get(1)?.toIntOrNull() ?: return null
                if (durationSeconds <= 0) return null
                VoiceAction.SetTimer(
                    durationSeconds = durationSeconds,
                    label = parseLabel(normalized),
                    confidence = confidence
                )
            }

            "SET_ALARM" -> {
                val rawHour = hourRegex.find(normalized)?.groupValues?.get(1)?.toIntOrNull() ?: return null
                val minute = minuteRegex.find(normalized)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if (rawHour !in 0..23 || minute !in 0..59) return null

                // Convert 12-hour LLM output to 24-hour using the meridiem field.
                // Small on-device models often output hour=7 for "7 PM" despite 24h instructions,
                // so we do the conversion in code rather than trusting the model.
                val meridiem = meridiemRegex.find(normalized)?.groupValues?.get(1)
                val hour = applyMeridiem(rawHour, meridiem)

                val timezoneStr = timezoneRegex.find(normalized)?.groupValues?.get(1)
                val sourceZone = timezoneStr?.let { AlarmTimeZoneConverter.resolveZoneId(it) }
                val (localHour, localMinute) = if (sourceZone != null) {
                    AlarmTimeZoneConverter.convertToDeviceLocal(hour, minute, sourceZone)
                } else {
                    hour to minute
                }

                VoiceAction.SetAlarm(
                    hour = localHour,
                    minute = localMinute,
                    label = parseLabel(normalized),
                    confidence = confidence
                )
            }

            "UNKNOWN_ACTION" -> VoiceAction.Unknown(confidence = confidence)
            else -> null
        }
    }

    private fun parseLabel(normalized: String): String? {
        val rawLabelMatch = labelRegex.find(normalized)?.groupValues?.get(1)
        return when (rawLabelMatch) {
            null, "null" -> null
            else -> rawLabelMatch
                .removePrefix("\"")
                .removeSuffix("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim()
                .ifBlank { null }
        }
    }

    private fun applyMeridiem(hour: Int, meridiem: String?): Int {
        if (meridiem == null || hour > 12) return hour
        val normalizedMeridiem = meridiem.uppercase()
            .replace(".", "")
            .replace(" ", "")
        return when (normalizedMeridiem) {
            "PM" -> if (hour == 12) 12 else hour + 12
            "AM" -> if (hour == 12) 0 else hour
            else -> hour
        }
    }

    private val intentRegex = Regex(""""intent"\s*:\s*"([^"]+)"""")
    private val durationRegex = Regex(""""duration_seconds"\s*:\s*(\d+)""")
    private val hourRegex = Regex(""""hour"\s*:\s*(\d+)""")
    private val minuteRegex = Regex(""""minute"\s*:\s*(\d+)""")
    private val meridiemRegex = Regex(
        """"meridiem"\s*:\s*"(a\.?\s*m\.?|p\.?\s*m\.?)"""",
        RegexOption.IGNORE_CASE
    )
    private val timezoneRegex = Regex(""""timezone"\s*:\s*"([^"]+)"""")
    private val labelRegex = Regex(""""label"\s*:\s*(null|"([^"\\]|\\.)*")""")
    private val confidenceRegex = Regex(""""confidence"\s*:\s*([0-9]+(?:\.[0-9]+)?)""")
}
