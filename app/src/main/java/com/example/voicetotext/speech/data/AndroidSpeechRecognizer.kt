package com.example.voicetotext.speech.data

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.voicetotext.speech.domain.SpeechRecognitionEvent

class AndroidSpeechRecognizer(
    private val context: Context
) : com.example.voicetotext.speech.domain.SpeechRecognizer {

    private var listener: ((SpeechRecognitionEvent) -> Unit)? = null

    override fun setListener(listener: (SpeechRecognitionEvent) -> Unit) {
        this.listener = listener
    }

    override fun startListening() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            listener?.invoke(
                SpeechRecognitionEvent.Error("Speech recognition is not available on this device.")
            )
            return
        }

        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
        )
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
    }

    override fun destroy() {
        speechRecognizer?.destroy()
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listener?.invoke(SpeechRecognitionEvent.Ready)
        }

        override fun onBeginningOfSpeech() {
            listener?.invoke(SpeechRecognitionEvent.Listening)
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio capture failed."
                SpeechRecognizer.ERROR_CLIENT -> "Speech recognizer was interrupted."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing."
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Offline speech recognition was unavailable."
                SpeechRecognizer.ERROR_NO_MATCH -> "I couldn't understand that. Try again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Try again."
                SpeechRecognizer.ERROR_SERVER -> "Speech recognition service returned an error."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                else -> "Speech recognition failed."
            }
            listener?.invoke(SpeechRecognitionEvent.Error(message))
        }

        override fun onResults(results: Bundle?) {
            val transcript = results.firstTranscript() ?: return
            listener?.invoke(SpeechRecognitionEvent.FinalResult(transcript))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val transcript = partialResults.firstTranscript() ?: return
            listener?.invoke(SpeechRecognitionEvent.PartialResult(transcript))
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun Bundle?.firstTranscript(): String? {
        return this
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private val speechRecognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        } else {
            null
        }
}
