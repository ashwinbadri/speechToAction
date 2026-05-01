package com.example.voicetotext.action.domain

fun interface VoiceActionParser {
    suspend fun parse(input: String): VoiceAction
}
