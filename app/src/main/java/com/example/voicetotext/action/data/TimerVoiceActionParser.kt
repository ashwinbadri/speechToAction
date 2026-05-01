package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser
import java.util.Locale

class TimerVoiceActionParser : VoiceActionParser {

    override suspend fun parse(input: String): VoiceAction {
        val normalizedInput = input.trim()
        if (normalizedInput.isBlank()) return VoiceAction.empty()

        val loweredInput = normalizedInput.lowercase(Locale.US)
        if (!loweredInput.contains("timer")) {
            return VoiceAction.Unknown(confidence = 0.1)
        }

        val durationMatches = durationRegex.findAll(normalizedInput).toList()
        val durationSeconds = durationMatches.sumOf { matchResult ->
            matchResult.groupValues[1].toInt() * unitToSeconds(matchResult.groupValues[2])
        }

        if (durationMatches.isEmpty() || durationSeconds <= 0) {
            return VoiceAction.Unknown(confidence = 0.2)
        }

        val lastDurationMatch = durationMatches.last()
        val label = normalizedInput
            .substring(lastDurationMatch.range.last + 1)
            .trim()
            .removePrefix("for ")
            .removePrefix("called ")
            .removePrefix("named ")
            .trim()
            .trimEnd('.', '!', '?')
            .takeIf { it.isNotBlank() }

        return VoiceAction.SetTimer(
            durationSeconds = durationSeconds,
            label = label,
            confidence = if (label == null) 0.9 else 1.0
        )
    }

    private fun unitToSeconds(unit: String): Int {
        return when (unit.lowercase(Locale.US)) {
            "hour", "hours", "hr", "hrs" -> 3600
            "minute", "minutes", "min", "mins" -> 60
            "second", "seconds", "sec", "secs" -> 1
            else -> 0
        }
    }

    companion object {
        private val durationRegex = Regex(
            pattern = """(\d+)\s*(hours?|hrs?|minutes?|mins?|seconds?|secs?)""",
            option = RegexOption.IGNORE_CASE
        )
    }
}
