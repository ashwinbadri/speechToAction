package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser
import com.example.voicetotext.core.logging.AppLogger

class LlmVoiceActionParser(
    private val promptModel: OnDevicePromptModel,
    private val fallbackParser: VoiceActionParser
) : VoiceActionParser {

    companion object {
        private const val TAG = "VoiceActionLlm"
    }

    override suspend fun parse(input: String): VoiceAction {
        val normalizedInput = input.trim()
        if (normalizedInput.isBlank()) {
            AppLogger.d(TAG, "Skipping parse because transcript is blank")
            return VoiceAction.empty()
        }

        if (!promptModel.isAvailable()) {
            AppLogger.d(TAG, "Prompt model unavailable. Falling back to deterministic parser")
            return fallbackParser.parse(normalizedInput)
        }

        val prompt = buildPrompt(normalizedInput)
        AppLogger.d(TAG, "Sending transcript to prompt model transcript=$normalizedInput")
        val rawResponse = promptModel.generate(prompt)
        if (rawResponse == null) {
            AppLogger.w(TAG, "Prompt model returned null. Falling back to deterministic parser")
            return fallbackParser.parse(normalizedInput)
        }

        AppLogger.d(TAG, "Prompt model raw response=$rawResponse")
        val parsedAction = VoiceActionJsonParser.parse(rawResponse)
        if (parsedAction == null) {
            AppLogger.w(TAG, "Prompt response could not be parsed. Falling back to deterministic parser")
            return fallbackParser.parse(normalizedInput)
        }

        AppLogger.d(TAG, "Prompt parser resolved action=${parsedAction.javaClass.simpleName}")
        return parsedAction
    }

    private fun buildPrompt(transcript: String): String {
        return """
You are an on-device intent parser for an Android voice clock app.

Return JSON only. No markdown, no explanation.

Supported intents:
- SET_TIMER  (user wants a countdown timer)
- SET_ALARM  (user wants an alarm at a specific time of day)
- UNKNOWN_ACTION

Schema:
{
  "intent": string,
  "duration_seconds": number or null,
  "hour": number or null,
  "minute": number or null,
  "meridiem": "AM" or "PM" or null,
  "timezone": string or null,
  "label": string or null,
  "confidence": number
}

Rules:
- SET_TIMER: user says "set a timer", "start a countdown", "remind me in X minutes", etc.
  Set duration_seconds to the total seconds. Set hour, minute, meridiem, and timezone to null.
- SET_ALARM: user says "set an alarm", "wake me up at", "alarm for X AM/PM", "remind me at X",
  "set a reminder for X PM", "remember to do Y at Z", etc. Any request anchored to a clock time.
  Set hour as the 12-hour clock value (1–12) and minute (0–59). Set meridiem to "AM" or "PM".
  Set duration_seconds to null.
  Examples: "7:30 AM" → hour=7, minute=30, meridiem="AM"
            "9 PM"   → hour=9,  minute=0,  meridiem="PM"
            "7 PM"   → hour=7,  minute=0,  meridiem="PM"
            "noon"   → hour=12, minute=0,  meridiem="PM"
            "midnight" → hour=12, minute=0, meridiem="AM"
- timezone: if the user mentions a timezone, output its IANA zone ID (e.g., "America/New_York", "Europe/London", "Asia/Kolkata"). Use null if no timezone is mentioned.
  Examples: "EST" → "America/New_York". "PST" → "America/Los_Angeles". "IST" → "Asia/Kolkata". "GMT" → "GMT".
- label: the task or purpose the user stated (e.g. "call my friend", "take medicine"). Strip filler words like "to", "for", "called". Use null if none.
- confidence: 0.0–1.0 reflecting how certain you are.
- If unclear, return UNKNOWN_ACTION with all other fields null.

Input: "$transcript"
        """.trimIndent()
    }
}
