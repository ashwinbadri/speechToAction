package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.coroutines.runBlocking

class TimerVoiceActionParserTest {

    private val parser = TimerVoiceActionParser()

    @Test
    fun `parse returns unknown action when input is blank`() {
        val result = runBlocking { parser.parse("   ") }

        assertEquals(VoiceAction.Unknown(confidence = 0.0), result)
    }

    @Test
    fun `parse extracts timer duration and label`() {
        val result = runBlocking { parser.parse("Set a timer for 10 minutes for pasta") }

        result as VoiceAction.SetTimer
        assertEquals(600, result.durationSeconds)
        assertEquals("pasta", result.label)
        assertEquals(1.0, result.confidence, 0.0)
    }

    @Test
    fun `parse supports multiple duration units`() {
        val result = runBlocking { parser.parse("Set a timer for 1 hour 15 minutes") }

        result as VoiceAction.SetTimer
        assertEquals(4500, result.durationSeconds)
        assertNull(result.label)
        assertEquals(0.9, result.confidence, 0.0)
    }

    @Test
    fun `parse supports wake me phrasing without timer keyword`() {
        val result = runBlocking { parser.parse("Wake me in 20 minutes") }

        result as VoiceAction.SetTimer
        assertEquals(1200, result.durationSeconds)
        assertNull(result.label)
        assertEquals(0.85, result.confidence, 0.0)
    }

    @Test
    fun `parse supports reminder style phrasing with task label`() {
        val result = runBlocking { parser.parse("Remind me in 15 minutes to check the oven") }

        result as VoiceAction.SetTimer
        assertEquals(900, result.durationSeconds)
        assertEquals("check the oven", result.label)
        assertEquals(0.95, result.confidence, 0.0)
    }

    @Test
    fun `parse supports timer label when timer comes after duration`() {
        val result = runBlocking { parser.parse("Start 5 minutes timer for tea") }

        result as VoiceAction.SetTimer
        assertEquals(300, result.durationSeconds)
        assertEquals("tea", result.label)
        assertEquals(1.0, result.confidence, 0.0)
    }

    @Test
    fun `parse returns unknown for duration without timer intent`() {
        val result = runBlocking { parser.parse("I worked for 10 minutes on math") }

        assertEquals(VoiceAction.Unknown(confidence = 0.3), result)
    }
}
