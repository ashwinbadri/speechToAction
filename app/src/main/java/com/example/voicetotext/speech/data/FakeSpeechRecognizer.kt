package com.example.voicetotext.speech.data

import com.example.voicetotext.speech.domain.SpeechRecognitionEvent
import com.example.voicetotext.speech.domain.SpeechRecognizer

class FakeSpeechRecognizer : SpeechRecognizer {

    private var listener: ((SpeechRecognitionEvent) -> Unit)? = null
    var stopListeningCalled: Boolean = false
        private set

    override fun setListener(listener: (SpeechRecognitionEvent) -> Unit) {
        this.listener = listener
    }

    override fun startListening() {
        listener?.invoke(SpeechRecognitionEvent.Ready)
        listener?.invoke(SpeechRecognitionEvent.Listening)
    }

    override fun stopListening() {
        stopListeningCalled = true
    }

    override fun destroy() {
        // No-op in tests and previews.
    }

    fun emit(event: SpeechRecognitionEvent) {
        listener?.invoke(event)
    }
}
