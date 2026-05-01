package com.example.voicetotext.reminder.ui

import com.example.voicetotext.action.data.OnDevicePromptModel
import com.example.voicetotext.action.data.PromptModelStatus
import com.example.voicetotext.action.domain.ExecutionResult
import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionExecutor
import com.example.voicetotext.action.domain.VoiceActionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.voicetotext.speech.data.FakeSpeechRecognizer
import com.example.voicetotext.speech.domain.SpeechRecognitionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderParserViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `onMicrophonePermissionUpdated stores granted state`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeVoiceActionParser(),
            executor = FakeVoiceActionExecutor(),
            speechRecognizer = FakeSpeechRecognizer(),
            promptModel = FakeOnDevicePromptModel()
        )

        viewModel.onMicrophonePermissionUpdated(true)

        assertEquals(true, viewModel.uiState.value.hasMicrophonePermission)
    }

    @Test
    fun `onMicTapped switches ui into listening mode`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeVoiceActionParser(),
            executor = FakeVoiceActionExecutor(),
            speechRecognizer = FakeSpeechRecognizer(),
            promptModel = FakeOnDevicePromptModel()
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
            executor = FakeVoiceActionExecutor(),
            speechRecognizer = FakeSpeechRecognizer(),
            promptModel = FakeOnDevicePromptModel()
        )

        viewModel.onMicTapped()

        assertEquals(VoiceActionMode.Idle, viewModel.uiState.value.mode)
        assertEquals(false, viewModel.uiState.value.hasMicrophonePermission)
    }

    @Test
    fun `speech final result updates transcript and output json`() = runTest {
        val parserResult = VoiceAction.SetTimer(
            durationSeconds = 600,
            label = "pasta",
            confidence = 0.9
        )
        val speechRecognizer = FakeSpeechRecognizer()
        val viewModel = ReminderParserViewModel(
            parser = FakeVoiceActionParser(result = parserResult),
            executor = FakeVoiceActionExecutor(),
            speechRecognizer = speechRecognizer,
            promptModel = FakeOnDevicePromptModel()
        )
        viewModel.onMicrophonePermissionUpdated(true)

        speechRecognizer.emit(
            SpeechRecognitionEvent.FinalResult("Set a timer for 10 minutes for pasta")
        )
        advanceUntilIdle()

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
            executor = FakeVoiceActionExecutor(),
            speechRecognizer = FakeSpeechRecognizer(),
            promptModel = FakeOnDevicePromptModel()
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
            executor = FakeVoiceActionExecutor(),
            speechRecognizer = speechRecognizer,
            promptModel = FakeOnDevicePromptModel()
        )

        speechRecognizer.emit(SpeechRecognitionEvent.PartialResult("set a timer"))

        assertEquals("set a timer", viewModel.uiState.value.transcript)
    }

    private class FakeVoiceActionParser(
        private val result: VoiceAction = VoiceAction.empty()
    ) : VoiceActionParser {
        override suspend fun parse(input: String): VoiceAction = result
    }

    private class FakeVoiceActionExecutor : VoiceActionExecutor {
        override fun execute(action: VoiceAction): ExecutionResult = ExecutionResult.Success
    }

    private class FakeOnDevicePromptModel : OnDevicePromptModel {
        override val status: StateFlow<PromptModelStatus> = MutableStateFlow(PromptModelStatus.Ready)
        override suspend fun isAvailable(): Boolean = false
        override suspend fun prefetch() {}
        override suspend fun generate(prompt: String): String? = null
        override fun close() {}
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
