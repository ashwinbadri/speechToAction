package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser

class ClockVoiceActionParser : VoiceActionParser {

    private val timerParser = TimerVoiceActionParser()
    private val alarmParser = AlarmVoiceActionParser()

    override suspend fun parse(input: String): VoiceAction {
        val timerResult = timerParser.parse(input)
        if (timerResult !is VoiceAction.Unknown) return timerResult

        val alarmResult = alarmParser.parse(input)
        if (alarmResult !is VoiceAction.Unknown) return alarmResult

        return VoiceAction.Unknown(confidence = 0.0)
    }
}
