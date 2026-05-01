package com.example.voicetotext.action.domain

interface VoiceActionExecutor {
    fun execute(action: VoiceAction): ExecutionResult
}

sealed class ExecutionResult {
    data object Success : ExecutionResult()
    data class Failure(val reason: String) : ExecutionResult()
}
