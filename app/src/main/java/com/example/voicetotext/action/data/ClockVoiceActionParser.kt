package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionParser

class ClockVoiceActionParser(
    private val timerParser: VoiceActionParser = TimerVoiceActionParser(),
    private val alarmParser: VoiceActionParser = AlarmVoiceActionParser()
) : VoiceActionParser {

    override suspend fun parse(input: String): VoiceAction {
        val timerResult = timerParser.parse(input)
        if (timerResult !is VoiceAction.Unknown) return timerResult

        val alarmResult = alarmParser.parse(input)
        if (alarmResult !is VoiceAction.Unknown) return alarmResult

        return VoiceAction.Unknown(confidence = 0.0)
    }
}
