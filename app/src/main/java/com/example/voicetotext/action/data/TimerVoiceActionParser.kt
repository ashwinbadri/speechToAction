package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser
import java.util.Locale

class TimerVoiceActionParser : VoiceActionParser {

    override suspend fun parse(input: String): VoiceAction {
        val normalizedInput = input.trim()
        if (normalizedInput.isBlank()) return VoiceAction.empty()

        val durationMatches = durationRegex.findAll(normalizedInput).toList()
        val durationSeconds = durationMatches.sumOf { matchResult ->
            matchResult.groupValues[1].toInt() * unitToSeconds(matchResult.groupValues[2])
        }

        if (durationMatches.isEmpty() || durationSeconds <= 0) {
            return VoiceAction.Unknown(confidence = 0.2)
        }

        val loweredInput = normalizedInput.lowercase(Locale.US)
        if (!isLikelyTimerIntent(loweredInput)) {
            return VoiceAction.Unknown(confidence = 0.3)
        }

        val lastDurationMatch = durationMatches.last()
        val label = extractLabel(normalizedInput, lastDurationMatch.range.last + 1)
        val confidence = when {
            label != null && loweredInput.contains("timer") -> 1.0
            label != null -> 0.95
            loweredInput.contains("timer") || loweredInput.contains("countdown") || loweredInput.contains("count down") -> 0.9
            else -> 0.85
        }

        return VoiceAction.SetTimer(
            durationSeconds = durationSeconds,
            label = label,
            confidence = confidence
        )
    }

    private fun isLikelyTimerIntent(loweredInput: String): Boolean {
        if (timerKeywordRegex.containsMatchIn(loweredInput)) return true
        if (wakeMeRegex.containsMatchIn(loweredInput)) return true
        if (remindMeRegex.containsMatchIn(loweredInput)) return true
        if (setOrStartRegex.containsMatchIn(loweredInput)) return true
        return false
    }

    private fun extractLabel(input: String, startIndex: Int): String? {
        val trailingText = input
            .substring(startIndex)
            .trim()
            .replace(labelPreambleRegex, "")
            .trim()
            .trimEnd('.', '!', '?')

        if (trailingText.isBlank()) return null
        if (trailingText.equals("timer", ignoreCase = true)) return null

        return trailingText
            .replace(timerOnlyPrefixRegex, "")
            .replace(labelPreambleRegex, "")
            .trim()
            .takeIf { it.isNotBlank() }
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
        private val timerKeywordRegex = Regex("""\b(timer|countdown|count down)\b""")
        private val wakeMeRegex = Regex("""\bwake me\b""")
        private val remindMeRegex = Regex("""\bremind me\b""")
        private val setOrStartRegex = Regex("""\b(set|start|run)\b""")
        private val labelPreambleRegex = Regex("""^(for|to|called|named)\s+""", RegexOption.IGNORE_CASE)
        private val timerOnlyPrefixRegex = Regex("""^(a\s+)?timer\s+""", RegexOption.IGNORE_CASE)
    }
}
