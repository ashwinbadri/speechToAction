package com.example.voicetotext.action.data

import android.os.Build
import com.example.voicetotext.core.logging.AppLogger
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

class MlKitPromptModel(
    private val generativeModel: GenerativeModel = Generation.getClient()
) : OnDevicePromptModel {

    companion object {
        private const val TAG = "VoiceActionLlm"
    }

    private val _status = MutableStateFlow(PromptModelStatus.Checking)
    override val status: StateFlow<PromptModelStatus> = _status

    override suspend fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            _status.value = PromptModelStatus.Unavailable
            AppLogger.w(TAG, "Prompt model unavailable because SDK is below 26")
            return false
        }
        return try {
            val status = generativeModel.checkStatus()
            val isAvailable = status == FeatureStatus.AVAILABLE
            _status.value = when (status) {
                FeatureStatus.AVAILABLE -> PromptModelStatus.Ready
                FeatureStatus.DOWNLOADING,
                FeatureStatus.DOWNLOADABLE -> PromptModelStatus.Downloading
                else -> PromptModelStatus.Unavailable
            }
            AppLogger.d(TAG, "Prompt model status=${status.asLogLabel()} available=$isAvailable")
            isAvailable
        } catch (exception: GenAiException) {
            _status.value = PromptModelStatus.Unavailable
            AppLogger.w(TAG, "Prompt model availability check failed", exception)
            false
        }
    }

    override suspend fun prefetch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            _status.value = PromptModelStatus.Unavailable
            AppLogger.w(TAG, "Skipping prompt prefetch because SDK is below 26")
            return
        }

        try {
            when (val status = generativeModel.checkStatus()) {
                FeatureStatus.AVAILABLE -> {
                    _status.value = PromptModelStatus.Ready
                    AppLogger.d(TAG, "Prompt model already available on app launch")
                }

                FeatureStatus.DOWNLOADABLE -> {
                    _status.value = PromptModelStatus.Downloading
                    AppLogger.d(TAG, "Prompt model downloadable. Starting Gemini Nano download")
                    generativeModel.download().collect { downloadStatus ->
                        when (downloadStatus) {
                            is DownloadStatus.DownloadStarted -> {
                                _status.value = PromptModelStatus.Downloading
                                AppLogger.d(
                                    TAG,
                                    "Gemini Nano download started bytes=${downloadStatus.bytesToDownload}"
                                )
                            }

                            is DownloadStatus.DownloadProgress -> {
                                _status.value = PromptModelStatus.Downloading
                                AppLogger.d(
                                    TAG,
                                    "Gemini Nano download progress bytes=${downloadStatus.totalBytesDownloaded}"
                                )
                            }

                            DownloadStatus.DownloadCompleted -> {
                                _status.value = PromptModelStatus.Ready
                                AppLogger.d(TAG, "Gemini Nano download completed")
                            }

                            is DownloadStatus.DownloadFailed -> {
                                _status.value = PromptModelStatus.Unavailable
                                AppLogger.w(
                                    TAG,
                                    "Gemini Nano download failed: ${downloadStatus.e.message}",
                                    downloadStatus.e
                                )
                            }
                        }
                    }
                }

                FeatureStatus.DOWNLOADING -> {
                    _status.value = PromptModelStatus.Downloading
                    AppLogger.d(TAG, "Prompt model already downloading on app launch")
                }

                FeatureStatus.UNAVAILABLE -> {
                    _status.value = PromptModelStatus.Unavailable
                    AppLogger.w(
                        TAG,
                        "Prompt model unavailable on this device or configuration is not ready yet"
                    )
                }

                else -> {
                    _status.value = PromptModelStatus.Unavailable
                    AppLogger.w(TAG, "Unexpected prompt model status=${status.asLogLabel()}")
                }
            }
        } catch (exception: GenAiException) {
            _status.value = PromptModelStatus.Unavailable
            AppLogger.w(TAG, "Prompt model prefetch failed", exception)
        }
    }

    override suspend fun generate(prompt: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            AppLogger.w(TAG, "Skipping generation because SDK is below 26")
            return null
        }
        return try {
            AppLogger.d(TAG, "Generating prompt response")
            val result = generativeModel.generateContent(prompt).extractText()?.trim()
            AppLogger.d(TAG, "Prompt response extracted=${!result.isNullOrBlank()}")
            result
        } catch (exception: GenAiException) {
            AppLogger.w(TAG, "Prompt generation failed", exception)
            null
        }
    }

    override fun close() {
        AppLogger.d(TAG, "Closing prompt model")
        generativeModel.close()
    }

    private fun Any.extractText(): String? {
        val directText = javaClass.methods
            .firstOrNull { it.name == "getText" && it.parameterCount == 0 }
            ?.invoke(this) as? String
        if (!directText.isNullOrBlank()) {
            AppLogger.d(TAG, "Using direct text field from prompt response")
            return directText
        }

        val candidates = javaClass.methods
            .firstOrNull { it.name == "getCandidates" && it.parameterCount == 0 }
            ?.invoke(this) as? List<*>

        val candidateText = candidates
            ?.firstOrNull()
            ?.let { candidate ->
                candidate.javaClass.methods
                    .firstOrNull { it.name == "getText" && it.parameterCount == 0 }
                    ?.invoke(candidate) as? String
            }

        if (!candidateText.isNullOrBlank()) {
            AppLogger.d(TAG, "Using candidate text field from prompt response")
        }
        return candidateText?.takeIf { it.isNotBlank() }
    }

    private fun Int.asLogLabel(): String {
        return when (this) {
            FeatureStatus.UNAVAILABLE -> "UNAVAILABLE"
            FeatureStatus.DOWNLOADABLE -> "DOWNLOADABLE"
            FeatureStatus.DOWNLOADING -> "DOWNLOADING"
            FeatureStatus.AVAILABLE -> "AVAILABLE"
            else -> toString()
        }
    }
}
