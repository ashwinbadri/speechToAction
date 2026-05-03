package com.example.voicetotext.voiceaction.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.voicetotext.action.data.PromptModelStatus
import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.ui.theme.VoiceToTextTheme

@Composable
fun VoiceActionScreen(
    uiState: VoiceActionUiState,
    modelStatus: PromptModelStatus,
    isDebug: Boolean,
    onMicTapped: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onActionResolved: () -> Unit,
    onRunActionClicked: () -> Unit,
    onResetClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canRun = uiState.hasMicrophonePermission &&
        uiState.mode == VoiceActionMode.Idle &&
        uiState.transcript.isNotBlank() &&
        uiState.lastAction != null &&
        uiState.lastAction !is VoiceAction.Unknown &&
        uiState.executionResult == null

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Scaffold(containerColor = Color.Transparent) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Header()
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatusPill(status = modelStatus, isDebug = isDebug)
                    HeroVoiceCard(
                        hasMicrophonePermission = uiState.hasMicrophonePermission,
                        mode = uiState.mode,
                        transcript = uiState.transcript,
                        onMicTapped = onMicTapped,
                        onRequestMicrophonePermission = onRequestMicrophonePermission,
                        onActionResolved = onActionResolved
                    )
                    ActionPreviewCard(
                        title = uiState.resolvedActionTitle,
                        subtitle = uiState.resolvedActionSubtitle,
                        isReady = canRun,
                        executionResult = uiState.executionResult
                    )
                    ActionButtons(
                        canRun = canRun,
                        executionResult = uiState.executionResult,
                        onRunActionClicked = onRunActionClicked,
                        onResetClicked = onResetClicked
                    )
                    FollowUpCard(
                        lastAction = uiState.lastAction,
                        executionResult = uiState.executionResult,
                        onResetClicked = onResetClicked
                    )
                    if (isDebug) {
                        DebugCard(output = uiState.outputJson)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceActionScreenPreview() {
    VoiceToTextTheme {
        VoiceActionScreen(
            uiState = VoiceActionUiState(
                hasMicrophonePermission = true,
                transcript = "Set a timer for 10 minutes for pasta",
                resolvedActionTitle = "Set timer for 10 minutes",
                resolvedActionSubtitle = "Label: pasta",
                outputJson = VoiceAction.SetTimer(
                    durationSeconds = 600,
                    label = "pasta",
                    confidence = 1.0
                ).toJson()
            ),
            modelStatus = PromptModelStatus.Ready,
            isDebug = true,
            onMicTapped = {},
            onRequestMicrophonePermission = {},
            onActionResolved = {},
            onRunActionClicked = {},
            onResetClicked = {}
        )
    }
}
