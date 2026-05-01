package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmVoiceActionParserTest {

    private val parser = AlarmVoiceActionParser()

    @Test
    fun `parse returns unknown when input is blank`() {
        val result = runBlocking { parser.parse("   ") }
        assertEquals(VoiceAction.Unknown(confidence = 0.0), result)
    }

    @Test
    fun `parse returns unknown when no alarm intent is present`() {
        val result = runBlocking { parser.parse("Set a timer for 10 minutes") }
        result as VoiceAction.Unknown
    }

    @Test
    fun `parse extracts 12-hour time with minutes and AM`() {
        val result = runBlocking { parser.parse("Set an alarm for 7:30 AM") }
        result as VoiceAction.SetAlarm
        assertEquals(7, result.hour)
        assertEquals(30, result.minute)
        assertNull(result.label)
        assertEquals(0.85, result.confidence, 0.0)
    }

    @Test
    fun `parse converts PM hour to 24-hour format`() {
        val result = runBlocking { parser.parse("Set an alarm for 9 PM") }
        result as VoiceAction.SetAlarm
        assertEquals(21, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse converts 12 PM to hour 12`() {
        val result = runBlocking { parser.parse("Set alarm for 12 PM") }
        result as VoiceAction.SetAlarm
        assertEquals(12, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse converts 12 AM to hour 0`() {
        val result = runBlocking { parser.parse("Set alarm for 12 AM") }
        result as VoiceAction.SetAlarm
        assertEquals(0, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse resolves noon to 12 00`() {
        val result = runBlocking { parser.parse("Set alarm for noon") }
        result as VoiceAction.SetAlarm
        assertEquals(12, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse resolves midnight to 0 00`() {
        val result = runBlocking { parser.parse("Set alarm for midnight") }
        result as VoiceAction.SetAlarm
        assertEquals(0, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse supports wake me up phrasing`() {
        val result = runBlocking { parser.parse("Wake me up at 6 AM") }
        result as VoiceAction.SetAlarm
        assertEquals(6, result.hour)
        assertEquals(0, result.minute)
        assertEquals(0.75, result.confidence, 0.0)
    }

    @Test
    fun `parse supports bare 24-hour colon time`() {
        val result = runBlocking { parser.parse("Set alarm for 14:30") }
        result as VoiceAction.SetAlarm
        assertEquals(14, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun `parse supports o clock phrasing`() {
        val result = runBlocking { parser.parse("Set alarm for 7 o'clock") }
        result as VoiceAction.SetAlarm
        assertEquals(7, result.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun `parse extracts label from trailing for clause after time`() {
        val result = runBlocking { parser.parse("Set alarm for 7 AM for my morning run") }
        result as VoiceAction.SetAlarm
        assertEquals(7, result.hour)
        assertEquals("my morning run", result.label)
        assertEquals(0.95, result.confidence, 0.0)
    }

    @Test
    fun `parse extracts label from called clause`() {
        val result = runBlocking { parser.parse("Set an alarm for 8:30 AM called standup") }
        result as VoiceAction.SetAlarm
        assertEquals(8, result.hour)
        assertEquals(30, result.minute)
        assertEquals("standup", result.label)
    }

    @Test
    fun `parse returns unknown for time without alarm keyword`() {
        val result = runBlocking { parser.parse("Meet me at 7 PM") }
        result as VoiceAction.Unknown
    }

    // --- Timezone conversion ---

    @Test
    fun `parse converts EST time to device local time`() {
        val estZone = java.time.ZoneId.of("America/New_York")
        val deviceZone = java.time.ZoneId.systemDefault()

        val result = runBlocking { parser.parse("Set alarm for 9 AM EST") }
        result as VoiceAction.SetAlarm

        val expected = AlarmTimeZoneConverter.convertToDeviceLocal(9, 0, estZone)
        assertEquals(expected.first, result.hour)
        assertEquals(expected.second, result.minute)
    }

    @Test
    fun `parse converts PST time to device local time`() {
        val pstZone = java.time.ZoneId.of("America/Los_Angeles")

        val result = runBlocking { parser.parse("Wake me up at 6:30 AM PST") }
        result as VoiceAction.SetAlarm

        val expected = AlarmTimeZoneConverter.convertToDeviceLocal(6, 30, pstZone)
        assertEquals(expected.first, result.hour)
        assertEquals(expected.second, result.minute)
    }

    @Test
    fun `parse converts named timezone to device local time`() {
        val easternZone = java.time.ZoneId.of("America/New_York")

        val result = runBlocking { parser.parse("Set alarm for 8 AM Eastern") }
        result as VoiceAction.SetAlarm

        val expected = AlarmTimeZoneConverter.convertToDeviceLocal(8, 0, easternZone)
        assertEquals(expected.first, result.hour)
        assertEquals(expected.second, result.minute)
    }

    @Test
    fun `parse leaves time unchanged when no timezone is specified`() {
        val result = runBlocking { parser.parse("Set alarm for 7:00 AM") }
        result as VoiceAction.SetAlarm
        assertEquals(7, result.hour)
        assertEquals(0, result.minute)
    }
}
