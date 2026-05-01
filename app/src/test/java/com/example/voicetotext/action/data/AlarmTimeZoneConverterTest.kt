package com.example.voicetotext.action.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class AlarmTimeZoneConverterTest {

    // --- convertToDeviceLocal ---

    @Test
    fun `convertToDeviceLocal converts UTC to IST correctly`() {
        // IST = UTC+5:30, no DST — always deterministic
        val utc = ZoneId.of("UTC")
        val ist = ZoneId.of("Asia/Kolkata")
        val (hour, minute) = AlarmTimeZoneConverter.convertToDeviceLocal(9, 0, utc, ist)
        assertEquals(14, hour)
        assertEquals(30, minute)
    }

    @Test
    fun `convertToDeviceLocal handles minute carry across hour boundary`() {
        // UTC 23:00 → IST 04:30 next day — only hour and minute matter for alarm
        val utc = ZoneId.of("UTC")
        val ist = ZoneId.of("Asia/Kolkata")
        val (hour, minute) = AlarmTimeZoneConverter.convertToDeviceLocal(23, 0, utc, ist)
        assertEquals(4, hour)
        assertEquals(30, minute)
    }

    @Test
    fun `convertToDeviceLocal returns unchanged when source and device zones are equal`() {
        val utc = ZoneId.of("UTC")
        val (hour, minute) = AlarmTimeZoneConverter.convertToDeviceLocal(9, 30, utc, utc)
        assertEquals(9, hour)
        assertEquals(30, minute)
    }

    @Test
    fun `convertToDeviceLocal converts UTC to Japan time correctly`() {
        // JST = UTC+9, no DST
        val utc = ZoneId.of("UTC")
        val jst = ZoneId.of("Asia/Tokyo")
        val (hour, minute) = AlarmTimeZoneConverter.convertToDeviceLocal(10, 15, utc, jst)
        assertEquals(19, hour)
        assertEquals(15, minute)
    }

    // --- detectInText ---

    @Test
    fun `detectInText recognizes EST abbreviation`() {
        val zone = AlarmTimeZoneConverter.detectInText("set alarm for 7 am est")
        assertEquals(ZoneId.of("America/New_York"), zone)
    }

    @Test
    fun `detectInText recognizes Eastern region name`() {
        val zone = AlarmTimeZoneConverter.detectInText("alarm for 8 am eastern")
        assertEquals(ZoneId.of("America/New_York"), zone)
    }

    @Test
    fun `detectInText recognizes IST abbreviation`() {
        val zone = AlarmTimeZoneConverter.detectInText("remind me at 9 pm ist")
        assertEquals(ZoneId.of("Asia/Kolkata"), zone)
    }

    @Test
    fun `detectInText returns null when no timezone is mentioned`() {
        val zone = AlarmTimeZoneConverter.detectInText("set alarm for 7 am")
        assertNull(zone)
    }

    @Test
    fun `detectInText does not match timezone token inside a longer word`() {
        // "estimate" contains "est" but should not match EST timezone
        val zone = AlarmTimeZoneConverter.detectInText("my best estimate is 7 am")
        assertNull(zone)
    }

    // --- resolveZoneId ---

    @Test
    fun `resolveZoneId handles known abbreviation`() {
        assertEquals(ZoneId.of("America/Los_Angeles"), AlarmTimeZoneConverter.resolveZoneId("pst"))
    }

    @Test
    fun `resolveZoneId handles IANA format returned by LLM`() {
        assertEquals(ZoneId.of("America/New_York"), AlarmTimeZoneConverter.resolveZoneId("America/New_York"))
    }

    @Test
    fun `resolveZoneId is case-insensitive for abbreviations`() {
        assertEquals(ZoneId.of("America/New_York"), AlarmTimeZoneConverter.resolveZoneId("EST"))
    }

    @Test
    fun `resolveZoneId returns null for unknown input`() {
        assertNull(AlarmTimeZoneConverter.resolveZoneId("XYZ"))
    }
}
