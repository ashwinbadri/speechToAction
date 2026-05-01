package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockVoiceActionParserTest {

    private val parser = ClockVoiceActionParser()

    @Test
    fun `parse returns timer action for explicit timer input`() {
        val result = runBlocking { parser.parse("Set a timer for 10 minutes") }
        result as VoiceAction.SetTimer
        assertEquals(600, result.durationSeconds)
    }

    @Test
    fun `parse returns alarm action when timer returns unknown`() {
        val result = runBlocking { parser.parse("Set an alarm for 7 AM") }
        result as VoiceAction.SetAlarm
        assertEquals(7, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse returns unknown when neither parser matches`() {
        val result = runBlocking { parser.parse("play some music") }
        result as VoiceAction.Unknown
    }

    @Test
    fun `parse prefers timer over alarm for duration-based reminder`() {
        // "remind me in 5 minutes" has a relative duration — timer wins
        val result = runBlocking { parser.parse("Remind me in 5 minutes") }
        result as VoiceAction.SetTimer
        assertEquals(300, result.durationSeconds)
    }

    @Test
    fun `parse routes time-anchored reminder to alarm`() {
        // "remind me at 8 AM" has a clock time — alarm wins
        val result = runBlocking { parser.parse("Remind me at 8 AM to take medicine") }
        result as VoiceAction.SetAlarm
        assertEquals(8, result.hour)
    }
}
