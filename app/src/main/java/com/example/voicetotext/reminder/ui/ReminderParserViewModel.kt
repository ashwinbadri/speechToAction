package com.example.voicetotext.reminder.ui

import androidx.lifecycle.ViewModel
import com.example.voicetotext.reminder.domain.ReminderIntent
import com.example.voicetotext.reminder.domain.ReminderParser
import com.example.voicetotext.speech.domain.SpeechRecognitionEvent
import com.example.voicetotext.speech.domain.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReminderParserViewModel(
    private val parser: ReminderParser,
    private val speechRecognizer: SpeechRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReminderParserUiState(outputJson = ReminderIntent.empty().toJson())
    )
    val uiState: StateFlow<ReminderParserUiState> = _uiState.asStateFlow()

    init {
        speechRecognizer.setListener(::onSpeechRecognitionEvent)
    }

    fun onMicrophonePermissionUpdated(isGranted: Boolean) {
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
        if (!_uiState.value.hasMicrophonePermission || _uiState.value.mode != VoiceActionMode.Idle) {
            return
        }

        speechRecognizer.startListening()
    }

    fun onActionResolved() {
        if (!_uiState.value.hasMicrophonePermission) return

        val transcript = _uiState.value.transcript
        val parsedIntent = parser.parse(transcript)

        _uiState.update { currentState ->
            currentState.copy(
                mode = VoiceActionMode.Idle,
                resolvedActionTitle = "Set timer for 10 minutes",
                resolvedActionSubtitle = "Label: pasta",
                outputJson = parsedIntent.toJson()
            )
        }
    }

    fun onRunActionClicked() {
        if (!_uiState.value.hasMicrophonePermission) return

        _uiState.update { currentState ->
            currentState.copy(
                resolvedActionTitle = "Ready to run timer",
                resolvedActionSubtitle = "Action execution will be connected in the next step."
            )
        }
    }

    fun onResetClicked() {
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
            outputJson = ReminderIntent.empty().toJson()
        )
    }

    private fun onSpeechRecognitionEvent(event: SpeechRecognitionEvent) {
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
                _uiState.update { currentState ->
                    currentState.copy(transcript = event.transcript)
                }
            }

            is SpeechRecognitionEvent.FinalResult -> {
                val parsedIntent = parser.parse(event.transcript)
                _uiState.update { currentState ->
                    currentState.copy(
                        transcript = event.transcript,
                        mode = VoiceActionMode.Processing,
                        resolvedActionTitle = "Processing request",
                        resolvedActionSubtitle = "Resolving the action and parameters from your speech…",
                        outputJson = parsedIntent.toJson()
                    )
                }
            }

            is SpeechRecognitionEvent.Error -> {
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
}
