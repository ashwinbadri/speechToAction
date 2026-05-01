package com.example.voicetotext.action.data

import kotlinx.coroutines.flow.StateFlow

interface OnDevicePromptModel {
    val status: StateFlow<PromptModelStatus>

    suspend fun isAvailable(): Boolean

    suspend fun prefetch()

    suspend fun generate(prompt: String): String?

    fun close()
}
