package com.example.voicetotext.di

import android.content.Context
import com.example.voicetotext.action.data.AlarmVoiceActionParser
import com.example.voicetotext.action.data.AndroidVoiceActionExecutor
import com.example.voicetotext.action.data.ClockVoiceActionParser
import com.example.voicetotext.action.data.LlmVoiceActionParser
import com.example.voicetotext.action.data.MlKitPromptModel
import com.example.voicetotext.action.data.OnDevicePromptModel
import com.example.voicetotext.action.data.TimerVoiceActionParser
import com.example.voicetotext.action.domain.VoiceActionExecutor
import com.example.voicetotext.action.domain.VoiceActionParser
import com.example.voicetotext.speech.data.AndroidSpeechRecognizer
import com.example.voicetotext.speech.domain.SpeechRecognizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOnDevicePromptModel(): OnDevicePromptModel = MlKitPromptModel()

    @Provides
    @Singleton
    @TimerParser
    fun provideTimerParser(): VoiceActionParser = TimerVoiceActionParser()

    @Provides
    @Singleton
    @AlarmParser
    fun provideAlarmParser(): VoiceActionParser = AlarmVoiceActionParser()

    @Provides
    @Singleton
    @FallbackParser
    fun provideFallbackParser(
        @TimerParser timerParser: VoiceActionParser,
        @AlarmParser alarmParser: VoiceActionParser
    ): VoiceActionParser = ClockVoiceActionParser(
        timerParser = timerParser,
        alarmParser = alarmParser
    )

    @Provides
    @Singleton
    fun provideVoiceActionParser(
        promptModel: OnDevicePromptModel,
        @FallbackParser fallbackParser: VoiceActionParser
    ): VoiceActionParser = LlmVoiceActionParser(
        promptModel = promptModel,
        fallbackParser = fallbackParser
    )

    @Provides
    @Singleton
    fun provideVoiceActionExecutor(
        @ApplicationContext context: Context
    ): VoiceActionExecutor = AndroidVoiceActionExecutor(context)

    @Provides
    @Singleton
    fun provideSpeechRecognizer(
        @ApplicationContext context: Context
    ): SpeechRecognizer = AndroidSpeechRecognizer(context)
}
