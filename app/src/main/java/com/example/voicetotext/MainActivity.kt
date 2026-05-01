package com.example.voicetotext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.voicetotext.action.data.AlarmVoiceActionParser
import com.example.voicetotext.action.data.AndroidVoiceActionExecutor
import com.example.voicetotext.action.data.ClockVoiceActionParser
import com.example.voicetotext.action.data.LlmVoiceActionParser
import com.example.voicetotext.action.data.MlKitPromptModel
import com.example.voicetotext.action.data.TimerVoiceActionParser
import com.example.voicetotext.reminder.ui.ReminderParserRoute
import com.example.voicetotext.speech.data.AndroidSpeechRecognizer
import com.example.voicetotext.ui.theme.VoiceToTextTheme

// Manual wiring mirrors AppModule exactly.
// Switch to @AndroidEntryPoint + hiltViewModel() once a Hilt release
// supports AGP 9.x (BaseExtension was removed in AGP 9.0).
class MainActivity : ComponentActivity() {

    private val promptModel by lazy { MlKitPromptModel() }
    private val speechRecognizer by lazy { AndroidSpeechRecognizer(applicationContext) }
    private val executor by lazy { AndroidVoiceActionExecutor(applicationContext) }
    private val parser by lazy {
        LlmVoiceActionParser(
            promptModel = promptModel,
            fallbackParser = ClockVoiceActionParser(
                timerParser = TimerVoiceActionParser(),
                alarmParser = AlarmVoiceActionParser()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceToTextTheme {
                ReminderParserRoute(
                    parser = parser,
                    executor = executor,
                    speechRecognizer = speechRecognizer,
                    promptModel = promptModel
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
