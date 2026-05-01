package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceActionJsonParserTest {

    @Test
    fun `parse returns timer action for valid timer json`() {
        val result = VoiceActionJsonParser.parse(
            """
            {
              "intent": "SET_TIMER",
              "duration_seconds": 600,
              "label": "pasta",
              "confidence": 0.95
            }
            """.trimIndent()
        )

        assertEquals(
            VoiceAction.SetTimer(
                durationSeconds = 600,
                label = "pasta",
                confidence = 0.95
            ),
            result
        )
    }

    @Test
    fun `parse ignores markdown fences`() {
        val result = VoiceActionJsonParser.parse(
            """
            ```json
            {
              "intent": "UNKNOWN_ACTION",
              "duration_seconds": null,
              "label": null,
              "confidence": 0.2
            }
            ```
            """.trimIndent()
        )

        assertEquals(VoiceAction.Unknown(confidence = 0.2), result)
    }

    @Test
    fun `parse returns alarm action for valid alarm json`() {
        val result = VoiceActionJsonParser.parse(
            """
            {
              "intent": "SET_ALARM",
              "hour": 9,
              "minute": 30,
              "meridiem": "PM",
              "timezone": null,
              "label": "standup",
              "confidence": 0.95
            }
            """.trimIndent()
        )

        assertEquals(
            VoiceAction.SetAlarm(hour = 21, minute = 30, label = "standup", confidence = 0.95),
            result
        )
    }

    @Test
    fun `parse corrects 12-hour PM output from LLM to 24-hour`() {
        // LLM outputs hour=7 for "7 PM" despite 24h instructions — meridiem fixes it.
        val result = VoiceActionJsonParser.parse(
            """
            {
              "intent": "SET_ALARM",
              "hour": 7,
              "minute": 0,
              "meridiem": "PM",
              "timezone": null,
              "label": "call my friend",
              "confidence": 0.9
            }
            """.trimIndent()
        )

        result as VoiceAction.SetAlarm
        assertEquals(19, result.hour)
        assertEquals(0, result.minute)
        assertEquals("call my friend", result.label)
    }

    @Test
    fun `parse handles 12 AM as midnight`() {
        val result = VoiceActionJsonParser.parse(
            """
            {
              "intent": "SET_ALARM",
              "hour": 12,
              "minute": 0,
              "meridiem": "AM",
              "timezone": null,
              "label": null,
              "confidence": 0.9
            }
            """.trimIndent()
        )

        result as VoiceAction.SetAlarm
        assertEquals(0, result.hour)
    }

    @Test
    fun `parse accepts p dot m meridiem from model output`() {
        val result = VoiceActionJsonParser.parse(
            """
            {
              "intent": "SET_ALARM",
              "hour": 5,
              "minute": 0,
              "meridiem": "p.m.",
              "timezone": null,
              "label": "task",
              "confidence": 0.9
            }
            """.trimIndent()
        )

        result as VoiceAction.SetAlarm
        assertEquals(17, result.hour)
        assertEquals("task", result.label)
    }

    @Test
    fun `parse returns null for invalid timer payload`() {
        val result = VoiceActionJsonParser.parse(
            """
            {
              "intent": "SET_TIMER",
              "duration_seconds": null,
              "label": "pasta",
              "confidence": 0.9
            }
            """.trimIndent()
        )

        assertNull(result)
    }
}
