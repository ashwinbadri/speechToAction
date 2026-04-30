package com.example.voicetotext.reminder.data

import com.example.voicetotext.reminder.domain.ReminderIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceholderReminderParserTest {

    private val parser = PlaceholderReminderParser()

    @Test
    fun `parse returns empty reminder intent when input is blank`() {
        val result = parser.parse("   ")

        assertEquals(ReminderIntent.CREATE_REMINDER, result.intent)
        assertNull(result.title)
        assertNull(result.datetime)
        assertEquals(0.0, result.confidence, 0.0)
    }

    @Test
    fun `parse trims input and maps it to title`() {
        val result = parser.parse("  call mom tomorrow  ")

        assertEquals("call mom tomorrow", result.title)
        assertNull(result.datetime)
        assertEquals(0.2, result.confidence, 0.0)
    }
}
