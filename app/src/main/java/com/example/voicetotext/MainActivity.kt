package com.example.voicetotext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.voicetotext.reminder.data.PlaceholderReminderParser
import com.example.voicetotext.reminder.ui.ReminderParserRoute
import com.example.voicetotext.speech.data.FakeSpeechRecognizer
import com.example.voicetotext.ui.theme.VoiceToTextTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val parser = PlaceholderReminderParser()
        val speechRecognizer = FakeSpeechRecognizer()

        setContent {
            VoiceToTextTheme {
                ReminderParserRoute(
                    parser = parser,
                    speechRecognizer = speechRecognizer
                )
            }
        }
    }
}
