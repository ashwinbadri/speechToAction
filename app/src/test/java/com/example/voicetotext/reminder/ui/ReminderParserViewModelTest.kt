package com.example.voicetotext.reminder.ui

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser
import com.example.voicetotext.speech.data.FakeSpeechRecognizer
import com.example.voicetotext.speech.domain.SpeechRecognitionEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderParserViewModelTest {

    @Test
    fun `onMicrophonePermissionUpdated stores granted state`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeVoiceActionParser(),
            speechRecognizer = FakeSpeechRecognizer()
        )

        viewModel.onMicrophonePermissionUpdated(true)

        assertEquals(true, viewModel.uiState.value.hasMicrophonePermission)
    }

    @Test
    fun `onMicTapped switches ui into listening mode`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeVoiceActionParser(),
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
            parser = FakeVoiceActionParser(),
            speechRecognizer = FakeSpeechRecognizer()
        )

        viewModel.onMicTapped()

        assertEquals(VoiceActionMode.Idle, viewModel.uiState.value.mode)
        assertEquals(false, viewModel.uiState.value.hasMicrophonePermission)
    }

    @Test
    fun `speech final result updates transcript and output json`() {
        val parserResult = VoiceAction.SetTimer(
            durationSeconds = 600,
            label = "pasta",
            confidence = 0.9
        )
        val speechRecognizer = FakeSpeechRecognizer()
        val viewModel = ReminderParserViewModel(
            parser = FakeVoiceActionParser(result = parserResult),
            speechRecognizer = speechRecognizer
        )
        viewModel.onMicrophonePermissionUpdated(true)

        speechRecognizer.emit(
            SpeechRecognitionEvent.FinalResult("Set a timer for 10 minutes for pasta")
        )

        assertEquals(VoiceActionMode.Processing, viewModel.uiState.value.mode)
        assertEquals("Set a timer for 10 minutes for pasta", viewModel.uiState.value.transcript)
        assertEquals("Set timer for 10 minutes", viewModel.uiState.value.resolvedActionTitle)
        assertEquals("Label: pasta", viewModel.uiState.value.resolvedActionSubtitle)
        assertEquals(parserResult.toJson(), viewModel.uiState.value.outputJson)
    }

    @Test
    fun `onResetClicked restores idle state`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeVoiceActionParser(),
            speechRecognizer = FakeSpeechRecognizer()
        )
        viewModel.onMicrophonePermissionUpdated(true)

        viewModel.onMicTapped()
        viewModel.onResetClicked()

        assertEquals(VoiceActionMode.Idle, viewModel.uiState.value.mode)
        assertEquals("", viewModel.uiState.value.transcript)
        assertEquals(true, viewModel.uiState.value.hasMicrophonePermission)
        assertEquals(VoiceAction.empty().toJson(), viewModel.uiState.value.outputJson)
    }

    @Test
    fun `speech partial result updates transcript`() {
        val speechRecognizer = FakeSpeechRecognizer()
        val viewModel = ReminderParserViewModel(
            parser = FakeVoiceActionParser(),
            speechRecognizer = speechRecognizer
        )

        speechRecognizer.emit(SpeechRecognitionEvent.PartialResult("set a timer"))

        assertEquals("set a timer", viewModel.uiState.value.transcript)
    }

    private class FakeVoiceActionParser(
        private val result: VoiceAction = VoiceAction.empty()
    ) : VoiceActionParser {
        override fun parse(input: String): VoiceAction = result
    }
}
