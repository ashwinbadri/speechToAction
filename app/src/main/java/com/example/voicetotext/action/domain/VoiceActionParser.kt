package com.example.voicetotext.action.domain

fun interface VoiceActionParser {
    fun parse(input: String): VoiceAction
}
