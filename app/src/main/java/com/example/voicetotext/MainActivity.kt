package com.example.voicetotext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicetotext.action.data.LlmVoiceActionParser
import com.example.voicetotext.action.data.MlKitPromptModel
import com.example.voicetotext.action.data.TimerVoiceActionParser
import com.example.voicetotext.reminder.ui.ReminderParserRoute
import com.example.voicetotext.speech.data.AndroidSpeechRecognizer
import com.example.voicetotext.ui.theme.VoiceToTextTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var speechRecognizer: AndroidSpeechRecognizer
    private lateinit var promptModel: MlKitPromptModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val fallbackParser = TimerVoiceActionParser()
        promptModel = MlKitPromptModel()
        val parser = LlmVoiceActionParser(
            promptModel = promptModel,
            fallbackParser = fallbackParser
        )
        speechRecognizer = AndroidSpeechRecognizer(applicationContext)
        lifecycleScope.launch {
            promptModel.prefetch()
        }

        setContent {
            val modelStatus by promptModel.status.collectAsStateWithLifecycle()
            VoiceToTextTheme {
                ReminderParserRoute(
                    parser = parser,
                    speechRecognizer = speechRecognizer,
                    modelStatus = modelStatus
                )
            }
        }
    }

    override fun onDestroy() {
        promptModel.close()
        speechRecognizer.destroy()
        super.onDestroy()
    }
}
