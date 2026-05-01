package com.example.voicetotext.reminder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser
import com.example.voicetotext.core.logging.AppLogger
import com.example.voicetotext.speech.domain.SpeechRecognitionEvent
import com.example.voicetotext.speech.domain.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReminderParserViewModel(
    private val parser: VoiceActionParser,
    private val speechRecognizer: SpeechRecognizer
) : ViewModel() {

    companion object {
        private const val TAG = "VoiceActionFlow"
    }

    private val _uiState = MutableStateFlow(
        ReminderParserUiState(outputJson = VoiceAction.empty().toJson())
    )
    val uiState: StateFlow<ReminderParserUiState> = _uiState.asStateFlow()

    init {
        AppLogger.d(TAG, "ViewModel initialized")
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
        if (!_uiState.value.hasMicrophonePermission) return

        AppLogger.d(TAG, "Run action clicked")
        _uiState.update { currentState ->
            currentState.copy(
                resolvedActionTitle = "Ready to run timer",
                resolvedActionSubtitle = "Action execution will be connected in the next step."
            )
        }
    }

    fun onResetClicked() {
        AppLogger.d(TAG, "Reset clicked")
        speechRecognizer.stopListening()
        _uiState.value = ReminderParserUiState(
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
            outputJson = VoiceAction.empty().toJson()
        )
    }

    private fun onSpeechRecognitionEvent(event: SpeechRecognitionEvent) {
        AppLogger.d(TAG, "Speech event received: ${event.javaClass.simpleName}")
        when (event) {
            SpeechRecognitionEvent.Ready -> {
                _uiState.update { currentState ->
                    currentState.copy(
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
                    mode = if (setModeToProcessing) VoiceActionMode.Processing else VoiceActionMode.Idle,
                    resolvedActionTitle = parsedAction.actionTitle(),
                    resolvedActionSubtitle = parsedAction.actionSubtitle(),
                    outputJson = parsedAction.toJson()
                )
            }
        }
    }
}
