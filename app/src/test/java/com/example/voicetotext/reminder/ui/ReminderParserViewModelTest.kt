package com.example.voicetotext.reminder.ui

import com.example.voicetotext.reminder.domain.ReminderIntent
import com.example.voicetotext.reminder.domain.ReminderParser
import com.example.voicetotext.speech.data.FakeSpeechRecognizer
import com.example.voicetotext.speech.domain.SpeechRecognitionEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderParserViewModelTest {

    @Test
    fun `onMicrophonePermissionUpdated stores granted state`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(),
            speechRecognizer = FakeSpeechRecognizer()
        )

        viewModel.onMicrophonePermissionUpdated(true)

        assertEquals(true, viewModel.uiState.value.hasMicrophonePermission)
    }

    @Test
    fun `onMicTapped switches ui into listening mode`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(),
            speechRecognizer = FakeSpeechRecognizer()
        )
        viewModel.onMicrophonePermissionUpdated(true)

        viewModel.onMicTapped()

        assertEquals(VoiceActionMode.Listening, viewModel.uiState.value.mode)
        assertEquals("Listening…", viewModel.uiState.value.transcript)
    }

    @Test
    fun `onMicTapped does nothing when permission is missing`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(),
            speechRecognizer = FakeSpeechRecognizer()
        )

        viewModel.onMicTapped()

        assertEquals(VoiceActionMode.Idle, viewModel.uiState.value.mode)
        assertEquals(false, viewModel.uiState.value.hasMicrophonePermission)
    }

    @Test
    fun `onDemoTranscriptReceived updates transcript and output json`() {
        val parserResult = ReminderIntent(
            title = "timer pasta",
            datetime = null,
            confidence = 0.9
        )
        val speechRecognizer = FakeSpeechRecognizer()
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(result = parserResult),
            speechRecognizer = speechRecognizer
        )
        viewModel.onMicrophonePermissionUpdated(true)

        viewModel.onDemoTranscriptReceived()

        assertEquals(VoiceActionMode.Processing, viewModel.uiState.value.mode)
        assertEquals("Set a timer for 10 minutes for pasta", viewModel.uiState.value.transcript)
        assertEquals(parserResult.toJson(), viewModel.uiState.value.outputJson)
    }

    @Test
    fun `onResetClicked restores idle state`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(),
            speechRecognizer = FakeSpeechRecognizer()
        )
        viewModel.onMicrophonePermissionUpdated(true)

        viewModel.onMicTapped()
        viewModel.onResetClicked()

        assertEquals(VoiceActionMode.Idle, viewModel.uiState.value.mode)
        assertEquals("", viewModel.uiState.value.transcript)
        assertEquals(true, viewModel.uiState.value.hasMicrophonePermission)
        assertEquals(ReminderIntent.empty().toJson(), viewModel.uiState.value.outputJson)
    }

    @Test
    fun `speech partial result updates transcript`() {
        val speechRecognizer = FakeSpeechRecognizer()
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(),
            speechRecognizer = speechRecognizer
        )

        speechRecognizer.emit(SpeechRecognitionEvent.PartialResult("set a timer"))

        assertEquals("set a timer", viewModel.uiState.value.transcript)
    }

    private class FakeReminderParser(
        private val result: ReminderIntent = ReminderIntent.empty()
    ) : ReminderParser {
        override fun parse(input: String): ReminderIntent = result
    }
}
