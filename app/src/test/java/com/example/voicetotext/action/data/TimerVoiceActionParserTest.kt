package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerVoiceActionParserTest {

    private val parser = TimerVoiceActionParser()

    @Test
    fun `parse returns unknown action when input is blank`() {
        val result = parser.parse("   ")

        assertEquals(VoiceAction.Unknown(confidence = 0.0), result)
    }

    @Test
    fun `parse extracts timer duration and label`() {
        val result = parser.parse("Set a timer for 10 minutes for pasta")

        result as VoiceAction.SetTimer
        assertEquals(600, result.durationSeconds)
        assertEquals("pasta", result.label)
        assertEquals(1.0, result.confidence, 0.0)
    }

    @Test
    fun `parse supports multiple duration units`() {
        val result = parser.parse("Set a timer for 1 hour 15 minutes")

        result as VoiceAction.SetTimer
        assertEquals(4500, result.durationSeconds)
        assertNull(result.label)
        assertEquals(0.9, result.confidence, 0.0)
    }
}
