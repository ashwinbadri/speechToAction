package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser
import java.util.Locale

class AlarmVoiceActionParser : VoiceActionParser {

    override suspend fun parse(input: String): VoiceAction {
        val normalized = input.trim()
        if (normalized.isBlank()) return VoiceAction.empty()

        val lowered = normalized.lowercase(Locale.US)
        if (!isLikelyAlarmIntent(lowered)) return VoiceAction.Unknown(confidence = 0.1)

        val (timeResult, afterIndex) = parseTime(lowered)
            ?: return VoiceAction.Unknown(confidence = 0.3)

        val label = extractLabel(normalized, afterIndex)

        val sourceZone = AlarmTimeZoneConverter.detectInText(lowered)
        val (localHour, localMinute) = if (sourceZone != null) {
            AlarmTimeZoneConverter.convertToDeviceLocal(timeResult.hour, timeResult.minute, sourceZone)
        } else {
            timeResult.hour to timeResult.minute
        }

        val confidence = when {
            label != null && alarmExplicitKeywordRegex.containsMatchIn(lowered) -> 0.95
            label != null -> 0.90
            alarmExplicitKeywordRegex.containsMatchIn(lowered) -> 0.85
            else -> 0.75
        }

        return VoiceAction.SetAlarm(
            hour = localHour,
            minute = localMinute,
            label = label,
            confidence = confidence
        )
    }

    private fun isLikelyAlarmIntent(lowered: String): Boolean =
        alarmKeywordRegex.containsMatchIn(lowered)

    private data class TimeResult(val hour: Int, val minute: Int)

    private fun parseTime(lowered: String): Pair<TimeResult, Int>? {
        // Named times before numeric patterns so "noon" / "midnight" win unconditionally.
        noonRegex.find(lowered)?.let { return TimeResult(12, 0) to it.range.last + 1 }
        midnightRegex.find(lowered)?.let { return TimeResult(0, 0) to it.range.last + 1 }

        // "7:30 AM" / "7:30 PM" — most specific first.
        timeWithMinutesAndMeridiemRegex.find(lowered)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@let
            val minute = match.groupValues[2].toIntOrNull() ?: return@let
            if (minute > 59) return@let
            val hour24 = to24Hour(hour, match.groupValues[3]) ?: return@let
            return TimeResult(hour24, minute) to match.range.last + 1
        }

        // "7 AM" / "7 PM"
        hourOnlyWithMeridiemRegex.find(lowered)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@let
            val hour24 = to24Hour(hour, match.groupValues[2]) ?: return@let
            return TimeResult(hour24, 0) to match.range.last + 1
        }

        // "14:30" — bare colon form (24-hour or contextually unambiguous).
        timeWithMinutesRegex.find(lowered)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@let
            val minute = match.groupValues[2].toIntOrNull() ?: return@let
            if (hour > 23 || minute > 59) return@let
            return TimeResult(hour, minute) to match.range.last + 1
        }

        // "7 o'clock"
        oclockRegex.find(lowered)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@let
            if (hour > 23) return@let
            return TimeResult(hour, 0) to match.range.last + 1
        }

        return null
    }

    // afterIndex is the end of the time match in lowered, which is the same length as normalized.
    private fun extractLabel(normalized: String, afterIndex: Int): String? {
        if (afterIndex >= normalized.length) return null
        val remaining = normalized.substring(afterIndex).trim()
        return labelRegex.find(remaining)
            ?.groupValues?.get(1)
            ?.trim()
            ?.ifBlank { null }
    }

    private fun to24Hour(hour: Int, meridiem: String): Int? {
        if (hour < 1 || hour > 12) return null
        return when {
            meridiem.startsWith("a") -> if (hour == 12) 0 else hour
            meridiem.startsWith("p") -> if (hour == 12) 12 else hour + 12
            else -> null
        }
    }

    companion object {
        private val alarmKeywordRegex = Regex("""\b(alarm|wake me up|wake up)\b""")
        private val alarmExplicitKeywordRegex = Regex("""\b(alarm|set an alarm|set alarm)\b""")

        private val noonRegex = Regex("""\bnoon\b""")
        private val midnightRegex = Regex("""\bmidnight\b""")

        private val timeWithMinutesAndMeridiemRegex = Regex("""(\d{1,2}):(\d{2})\s*(am|pm)""")
        private val hourOnlyWithMeridiemRegex = Regex("""(\d{1,2})\s*(am|pm)""")
        private val timeWithMinutesRegex = Regex("""(\d{1,2}):(\d{2})""")
        private val oclockRegex = Regex("""(\d{1,2})\s*o'?clock""")

        private val labelRegex = Regex(
            """(?:for|called|named|labeled)\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
    }
}
