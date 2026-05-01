package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LlmVoiceActionParserTest {

    @Test
    fun `parse returns empty action for blank input without calling the model`() {
        val model = FakeOnDevicePromptModel(available = true, response = "should not be called")
        val parser = LlmVoiceActionParser(promptModel = model, fallbackParser = FakeParser())

        val result = runBlocking { parser.parse("   ") }

        assertEquals(VoiceAction.empty(), result)
        assertEquals(0, model.generateCallCount)
    }

    @Test
    fun `parse uses fallback when model is unavailable`() {
        val fallbackAction = VoiceAction.SetTimer(300, null, 0.9)
        val parser = LlmVoiceActionParser(
            promptModel = FakeOnDevicePromptModel(available = false),
            fallbackParser = FakeParser(fallbackAction)
        )

        val result = runBlocking { parser.parse("set a timer for 5 minutes") }

        assertEquals(fallbackAction, result)
    }

    @Test
    fun `parse uses fallback when model returns null`() {
        val fallbackAction = VoiceAction.SetTimer(600, "pasta", 0.9)
        val parser = LlmVoiceActionParser(
            promptModel = FakeOnDevicePromptModel(available = true, response = null),
            fallbackParser = FakeParser(fallbackAction)
        )

        val result = runBlocking { parser.parse("set a timer for 10 minutes for pasta") }

        assertEquals(fallbackAction, result)
    }

    @Test
    fun `parse uses fallback when model returns unparseable response`() {
        val fallbackAction = VoiceAction.SetTimer(300, null, 0.8)
        val parser = LlmVoiceActionParser(
            promptModel = FakeOnDevicePromptModel(available = true, response = "not valid json"),
            fallbackParser = FakeParser(fallbackAction)
        )

        val result = runBlocking { parser.parse("set a timer for 5 minutes") }

        assertEquals(fallbackAction, result)
    }

    @Test
    fun `parse returns model action when response is valid`() {
        val json = """
            {
              "intent": "SET_TIMER",
              "duration_seconds": 600,
              "hour": null,
              "minute": null,
              "meridiem": null,
              "timezone": null,
              "label": "pasta",
              "confidence": 0.95
            }
        """.trimIndent()
        val parser = LlmVoiceActionParser(
            promptModel = FakeOnDevicePromptModel(available = true, response = json),
            fallbackParser = FakeParser()
        )

        val result = runBlocking { parser.parse("set a timer for 10 minutes for pasta") }

        assertEquals(VoiceAction.SetTimer(durationSeconds = 600, label = "pasta", confidence = 0.95), result)
    }

    @Test
    fun `parse returns model alarm action with meridiem correction`() {
        // Simulates Gemini Nano outputting 12h hour=7 for "7 PM" — meridiem corrects it
        val json = """
            {
              "intent": "SET_ALARM",
              "duration_seconds": null,
              "hour": 7,
              "minute": 0,
              "meridiem": "PM",
              "timezone": null,
              "label": "call my friend",
              "confidence": 0.9
            }
        """.trimIndent()
        val parser = LlmVoiceActionParser(
            promptModel = FakeOnDevicePromptModel(available = true, response = json),
            fallbackParser = FakeParser()
        )

        val result = runBlocking { parser.parse("remind me at 7 PM to call my friend") }

        result as VoiceAction.SetAlarm
        assertEquals(19, result.hour)
        assertEquals("call my friend", result.label)
    }

    @Test
    fun `parse enriches model alarm with transcript meridiem and fallback label`() {
        val json = """
            {
              "intent": "SET_ALARM",
              "duration_seconds": null,
              "hour": 5,
              "minute": 0,
              "meridiem": null,
              "timezone": null,
              "label": null,
              "confidence": 0.82
            }
        """.trimIndent()
        val fallbackAction = VoiceAction.SetAlarm(
            hour = 17,
            minute = 0,
            label = "doing task",
            confidence = 0.9
        )
        val parser = LlmVoiceActionParser(
            promptModel = FakeOnDevicePromptModel(available = true, response = json),
            fallbackParser = FakeParser(fallbackAction)
        )

        val result = runBlocking {
            parser.parse("Set alarm to remind me about doing task at 5 PM today")
        }

        result as VoiceAction.SetAlarm
        assertEquals(17, result.hour)
        assertEquals("doing task", result.label)
    }

    private class FakeParser(
        private val result: VoiceAction = VoiceAction.Unknown(0.0)
    ) : VoiceActionParser {
        override suspend fun parse(input: String): VoiceAction = result
    }

    private class FakeOnDevicePromptModel(
        private val available: Boolean,
        private val response: String? = null
    ) : OnDevicePromptModel {
        var generateCallCount = 0
            private set

        override val status: StateFlow<PromptModelStatus> =
            MutableStateFlow(PromptModelStatus.Ready)

        override suspend fun isAvailable(): Boolean = available
        override suspend fun prefetch() {}
        override suspend fun generate(prompt: String): String? {
            generateCallCount++
            return response
        }
        override fun close() {}
    }
}
