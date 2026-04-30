package com.example.voicetotext.reminder.ui

import com.example.voicetotext.reminder.domain.ReminderIntent
import com.example.voicetotext.reminder.domain.ReminderParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderParserViewModelTest {

    @Test
    fun `onMicTapped switches ui into listening mode`() {
        val viewModel = ReminderParserViewModel(parser = FakeReminderParser())

        viewModel.onMicTapped()

        assertEquals(VoiceActionMode.Listening, viewModel.uiState.value.mode)
        assertEquals("Listening…", viewModel.uiState.value.transcript)
    }

    @Test
    fun `onDemoTranscriptReceived updates transcript and output json`() {
        val parserResult = ReminderIntent(
            title = "timer pasta",
            datetime = null,
            confidence = 0.9
        )
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(result = parserResult)
        )

        viewModel.onDemoTranscriptReceived()

        assertEquals(VoiceActionMode.Processing, viewModel.uiState.value.mode)
        assertEquals("Set a timer for 10 minutes for pasta", viewModel.uiState.value.transcript)
        assertEquals(parserResult.toJson(), viewModel.uiState.value.outputJson)
    }

    @Test
    fun `onResetClicked restores idle state`() {
        val viewModel = ReminderParserViewModel(parser = FakeReminderParser())

        viewModel.onMicTapped()
        viewModel.onResetClicked()

        assertEquals(VoiceActionMode.Idle, viewModel.uiState.value.mode)
        assertEquals("", viewModel.uiState.value.transcript)
        assertEquals(ReminderIntent.empty().toJson(), viewModel.uiState.value.outputJson)
    }

    private class FakeReminderParser(
        private val result: ReminderIntent = ReminderIntent.empty()
    ) : ReminderParser {
        override fun parse(input: String): ReminderIntent = result
    }
}
