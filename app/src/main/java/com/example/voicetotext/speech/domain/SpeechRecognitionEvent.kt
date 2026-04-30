package com.example.voicetotext.speech.domain

sealed interface SpeechRecognitionEvent {
    data object Ready : SpeechRecognitionEvent
    data object Listening : SpeechRecognitionEvent
    data class PartialResult(val transcript: String) : SpeechRecognitionEvent
    data class FinalResult(val transcript: String) : SpeechRecognitionEvent
    data class Error(val message: String) : SpeechRecognitionEvent
}
