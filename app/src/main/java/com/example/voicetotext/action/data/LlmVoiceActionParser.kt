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
  "label": string or null,
  "confidence": number
}

Rules:
- SET_TIMER: user says "set a timer", "start a countdown", "remind me in X minutes", etc.
  Set duration_seconds to the total seconds. Set hour and minute to null.
- SET_ALARM: user says "set an alarm", "wake me up at", "alarm for X AM/PM", etc.
  Set hour (0–23, 24-hour format) and minute (0–59). Set duration_seconds to null.
  Examples: "7:30 AM" → hour=7, minute=30. "9 PM" → hour=21, minute=0. "midnight" → hour=0, minute=0. "noon" → hour=12, minute=0.
- label: optional name the user gave ("for pasta", "called standup"). Omit filler words like "for" or "called". Use null if none.
- confidence: 0.0–1.0 reflecting how certain you are.
- If unclear, return UNKNOWN_ACTION with all other fields null.

Input: "$transcript"
        """.trimIndent()
    }
}
