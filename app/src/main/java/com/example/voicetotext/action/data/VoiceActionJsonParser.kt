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
                val hour = hourRegex.find(normalized)?.groupValues?.get(1)?.toIntOrNull() ?: return null
                val minute = minuteRegex.find(normalized)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if (hour !in 0..23 || minute !in 0..59) return null

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

    private val intentRegex = Regex(""""intent"\s*:\s*"([^"]+)"""")
    private val durationRegex = Regex(""""duration_seconds"\s*:\s*(\d+)""")
    private val hourRegex = Regex(""""hour"\s*:\s*(\d+)""")
    private val minuteRegex = Regex(""""minute"\s*:\s*(\d+)""")
    private val timezoneRegex = Regex(""""timezone"\s*:\s*"([^"]+)"""")
    private val labelRegex = Regex(""""label"\s*:\s*(null|"([^"\\]|\\.)*")""")
    private val confidenceRegex = Regex(""""confidence"\s*:\s*([0-9]+(?:\.[0-9]+)?)""")
}
