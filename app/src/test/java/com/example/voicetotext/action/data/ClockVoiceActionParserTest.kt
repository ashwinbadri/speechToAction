package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockVoiceActionParserTest {

    private val timerAction = VoiceAction.SetTimer(durationSeconds = 300, label = null, confidence = 0.9)
    private val alarmAction = VoiceAction.SetAlarm(hour = 7, minute = 0, label = null, confidence = 0.9)
    private val unknownAction = VoiceAction.Unknown(confidence = 0.0)

    @Test
    fun `parse returns timer result when timer parser succeeds`() {
        val parser = clockParser(timer = timerAction, alarm = alarmAction)

        val result = runBlocking { parser.parse("anything") }

        assertEquals(timerAction, result)
    }

    @Test
    fun `parse falls through to alarm parser when timer returns unknown`() {
        val parser = clockParser(timer = unknownAction, alarm = alarmAction)

        val result = runBlocking { parser.parse("anything") }

        assertEquals(alarmAction, result)
    }

    @Test
    fun `parse returns unknown when both parsers return unknown`() {
        val parser = clockParser(timer = unknownAction, alarm = unknownAction)

        val result = runBlocking { parser.parse("anything") }

        assertEquals(VoiceAction.Unknown(confidence = 0.0), result)
    }

    @Test
    fun `parse does not call alarm parser when timer succeeds`() {
        var alarmCalled = false
        val parser = ClockVoiceActionParser(
            timerParser = FakeParser(timerAction),
            alarmParser = FakeParser(alarmAction) { alarmCalled = true }
        )

        runBlocking { parser.parse("anything") }

        assertEquals(false, alarmCalled)
    }

    private fun clockParser(timer: VoiceAction, alarm: VoiceAction) = ClockVoiceActionParser(
        timerParser = FakeParser(timer),
        alarmParser = FakeParser(alarm)
    )

    private class FakeParser(
        private val result: VoiceAction,
        private val onParseCalled: () -> Unit = {}
    ) : VoiceActionParser {
        override suspend fun parse(input: String): VoiceAction {
            onParseCalled()
            return result
        }
    }
}
