package com.example.voicetotext.voiceaction.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicetotext.action.data.OnDevicePromptModel
import com.example.voicetotext.action.data.PromptModelStatus
import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionExecutor
import com.example.voicetotext.action.domain.VoiceActionParser
import com.example.voicetotext.core.logging.AppLogger
import com.example.voicetotext.speech.domain.SpeechRecognitionEvent
import com.example.voicetotext.speech.domain.SpeechRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class VoiceActionViewModel @Inject constructor(
    private val parser: VoiceActionParser,
    private val executor: VoiceActionExecutor,
    private val speechRecognizer: SpeechRecognizer,
    private val promptModel: OnDevicePromptModel
) : ViewModel() {

    companion object {
        private const val TAG = "VoiceActionFlow"
    }

    val promptModelStatus: StateFlow<PromptModelStatus> = promptModel.status

    private val _uiState = MutableStateFlow(
        VoiceActionUiState(outputJson = VoiceAction.empty().toJson())
    )
    val uiState: StateFlow<VoiceActionUiState> = _uiState.asStateFlow()

    init {
        AppLogger.d(TAG, "ViewModel initialized")
        viewModelScope.launch { promptModel.prefetch() }
        speechRecognizer.setListener(::onSpeechRecognitionEvent)
    }

    fun onMicrophonePermissionUpdated(isGranted: Boolean) {
        AppLogger.d(TAG, "Microphone permission updated: granted=$isGranted")
        _uiState.update { currentState ->
            currentState.copy(
                hasMicrophonePermission = isGranted,
                resolvedActionTitle = if (isGranted) {
                    if (currentState.transcript.isBlank()) "No action yet" else currentState.resolvedActionTitle
                } else {
                    "Microphone access required"
                },
                resolvedActionSubtitle = if (isGranted) {
                    if (currentState.transcript.isBlank()) {
                        "Tap the mic and say something like “set a timer for 10 minutes”."
                    } else {
                        currentState.resolvedActionSubtitle
                    }
                } else {
                    "Allow microphone access so voice capture can run fully on-device."
                }
            )
        }
    }

    fun onMicTapped() {
        AppLogger.d(
            TAG,
            "Mic tapped permission=${_uiState.value.hasMicrophonePermission} mode=${_uiState.value.mode}"
        )
        if (!_uiState.value.hasMicrophonePermission || _uiState.value.mode != VoiceActionMode.Idle) {
            AppLogger.d(TAG, "Ignoring mic tap because flow is not ready")
            return
        }

        speechRecognizer.startListening()
    }

    fun onActionResolved() {
        if (!_uiState.value.hasMicrophonePermission) return

        val transcript = _uiState.value.transcript
        AppLogger.d(TAG, "Manual action resolve requested transcript=$transcript")
        parseTranscript(transcript, setModeToProcessing = false)
    }

    fun onRunActionClicked() {
        val action = _uiState.value.lastAction ?: return
        if (action is VoiceAction.Unknown) return

        AppLogger.d(TAG, "Run action clicked action=${action.javaClass.simpleName}")
        val result = executor.execute(action)
        AppLogger.d(TAG, "Execution result=${result.javaClass.simpleName}")
        _uiState.update { it.copy(executionResult = result) }
    }

    fun onResetClicked() {
        AppLogger.d(TAG, "Reset clicked")
        speechRecognizer.stopListening()
        _uiState.value = VoiceActionUiState(
            hasMicrophonePermission = _uiState.value.hasMicrophonePermission,
            resolvedActionTitle = if (_uiState.value.hasMicrophonePermission) {
                "No action yet"
            } else {
                "Microphone access required"
            },
            resolvedActionSubtitle = if (_uiState.value.hasMicrophonePermission) {
                "Tap the mic and say something like “set a timer for 10 minutes”."
            } else {
                "Allow microphone access so voice capture can run fully on-device."
            },
            outputJson = VoiceAction.empty().toJson(),
            lastAction = null,
            executionResult = null
        )
    }

    private fun onSpeechRecognitionEvent(event: SpeechRecognitionEvent) {
        AppLogger.d(TAG, "Speech event received: ${event.javaClass.simpleName}")
        when (event) {
            SpeechRecognitionEvent.Ready -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        mode = VoiceActionMode.Listening,
                        transcript = if (currentState.transcript.isBlank()) "Listening…" else currentState.transcript,
                        resolvedActionTitle = "Microphone ready",
                        resolvedActionSubtitle = "Start speaking whenever you're ready."
                    )
                }
            }

            SpeechRecognitionEvent.Listening -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        mode = VoiceActionMode.Listening,
                        transcript = "Listening…",
                        resolvedActionTitle = "Waiting for your voice",
                        resolvedActionSubtitle = "Speak naturally. We’ll resolve the action on-device."
                    )
                }
            }

            is SpeechRecognitionEvent.PartialResult -> {
                AppLogger.d(TAG, "Partial transcript=${event.transcript}")
                _uiState.update { currentState ->
                    currentState.copy(transcript = event.transcript)
                }
            }

            is SpeechRecognitionEvent.FinalResult -> {
                AppLogger.d(TAG, "Final transcript=${event.transcript}")
                _uiState.update { currentState ->
                    currentState.copy(
                        transcript = event.transcript,
                        mode = VoiceActionMode.Processing,
                        resolvedActionTitle = "Processing request",
                        resolvedActionSubtitle = "Resolving the action and parameters from your speech…"
                    )
                }
                parseTranscript(event.transcript, setModeToProcessing = true)
            }

            is SpeechRecognitionEvent.Error -> {
                AppLogger.w(TAG, "Speech error=${event.message}")
                _uiState.update { currentState ->
                    currentState.copy(
                        mode = VoiceActionMode.Idle,
                        resolvedActionTitle = "Speech error",
                        resolvedActionSubtitle = event.message
                    )
                }
            }
        }
    }

    private fun parseTranscript(
        transcript: String,
        setModeToProcessing: Boolean
    ) {
        viewModelScope.launch {
            AppLogger.d(
                TAG,
                "Parsing transcript modeToProcessing=$setModeToProcessing transcript=$transcript"
            )
            val parsedAction = parser.parse(transcript)
            AppLogger.d(
                TAG,
                "Parsed action=${parsedAction.javaClass.simpleName} json=${parsedAction.toJson()}"
            )
            _uiState.update { currentState ->
                currentState.copy(
                    mode = VoiceActionMode.Idle,
                    lastAction = parsedAction,
                    resolvedActionTitle = parsedAction.actionTitle(),
                    resolvedActionSubtitle = parsedAction.actionSubtitle(),
                    outputJson = parsedAction.toJson()
                )
            }
        }
    }
}
