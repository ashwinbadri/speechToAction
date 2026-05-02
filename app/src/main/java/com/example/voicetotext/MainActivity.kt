package com.example.voicetotext

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.voicetotext.voiceaction.ui.VoiceActionRoute
import com.example.voicetotext.ui.theme.VoiceToTextTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val isDebugBuild by lazy {
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceToTextTheme {
                VoiceActionRoute(isDebug = isDebugBuild)
            }
        }
    }
}
