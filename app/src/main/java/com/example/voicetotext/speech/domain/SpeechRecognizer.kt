package com.example.voicetotext.speech.domain

interface SpeechRecognizer {
    fun setListener(listener: (SpeechRecognitionEvent) -> Unit)

    fun startListening()

    fun stopListening()
}
