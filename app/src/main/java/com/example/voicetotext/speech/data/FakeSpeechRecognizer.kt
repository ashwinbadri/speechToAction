package com.example.voicetotext.speech.data

import com.example.voicetotext.speech.domain.SpeechRecognitionEvent
import com.example.voicetotext.speech.domain.SpeechRecognizer

class FakeSpeechRecognizer : SpeechRecognizer {

    private var listener: ((SpeechRecognitionEvent) -> Unit)? = null

    override fun setListener(listener: (SpeechRecognitionEvent) -> Unit) {
        this.listener = listener
    }

    override fun startListening() {
        listener?.invoke(SpeechRecognitionEvent.Ready)
        listener?.invoke(SpeechRecognitionEvent.Listening)
    }

    override fun stopListening() {
        // No-op in the fake implementation until the Android recognizer is wired in.
    }

    fun emit(event: SpeechRecognitionEvent) {
        listener?.invoke(event)
    }
}
