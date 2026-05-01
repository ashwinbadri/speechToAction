package com.example.voicetotext.reminder.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.voicetotext.action.data.OnDevicePromptModel
import com.example.voicetotext.action.data.PromptModelStatus
import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionExecutor
import com.example.voicetotext.action.domain.VoiceActionParser
import com.example.voicetotext.speech.domain.SpeechRecognizer
import com.example.voicetotext.ui.theme.VoiceToTextTheme

@Composable
fun ReminderParserRoute(
    parser: VoiceActionParser,
    executor: VoiceActionExecutor,
    speechRecognizer: SpeechRecognizer,
    promptModel: OnDevicePromptModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ReminderParserViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ReminderParserViewModel(
                    parser = parser,
                    executor = executor,
                    speechRecognizer = speechRecognizer,
                    promptModel = promptModel
                )
            }
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val modelStatus by viewModel.promptModelStatus.collectAsStateWithLifecycle()
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onMicrophonePermissionUpdated(isGranted)
    }

    LaunchedEffect(context) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onMicrophonePermissionUpdated(isGranted)
    }

    ReminderParserScreen(
        uiState = uiState,
        modelStatus = modelStatus,
        onMicTapped = viewModel::onMicTapped,
        onRequestMicrophonePermission = {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onActionResolved = viewModel::onActionResolved,
        onRunActionClicked = viewModel::onRunActionClicked,
        onResetClicked = viewModel::onResetClicked,
        modifier = modifier
    )
}

@Composable
fun ReminderParserScreen(
    uiState: ReminderParserUiState,
    modelStatus: PromptModelStatus,
    onMicTapped: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onActionResolved: () -> Unit,
    onRunActionClicked: () -> Unit,
    onResetClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF6F6F2)
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "A focused voice action flow for timers and alarms. Tap the mic, speak naturally, then confirm the action.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4C4C4C)
                    )

                    ModelStatusCard(status = modelStatus)

                    VoiceCaptureCard(
                        hasMicrophonePermission = uiState.hasMicrophonePermission,
                        mode = uiState.mode,
                        transcript = uiState.transcript,
                        onMicTapped = onMicTapped,
                        onRequestMicrophonePermission = onRequestMicrophonePermission,
                        onActionResolved = onActionResolved
                    )

                    val canRun = uiState.hasMicrophonePermission &&
                        uiState.mode == VoiceActionMode.Idle &&
                        uiState.transcript.isNotBlank() &&
                        uiState.lastAction != null &&
                        uiState.lastAction !is VoiceAction.Unknown &&
                        uiState.executionResult == null

                    ActionPreviewCard(
                        title = uiState.resolvedActionTitle,
                        subtitle = uiState.resolvedActionSubtitle,
                        isReady = canRun,
                        executionResult = uiState.executionResult
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onRunActionClicked,
                            enabled = canRun,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Run Action")
                        }

                        OutlinedButton(
                            onClick = onResetClicked,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset")
                        }
                    }

                    DebugCard(output = uiState.outputJson)
                }
            }
        }
    }
}

@Composable
private fun ModelStatusCard(status: PromptModelStatus) {
    val title = when (status) {
        PromptModelStatus.Checking -> "Model: Checking device readiness"
        PromptModelStatus.Downloading -> "Model: Downloading Gemini Nano"
        PromptModelStatus.Ready -> "Model: Ready on-device"
        PromptModelStatus.Unavailable -> "Model: Unavailable, using fallback"
    }
    val subtitle = when (status) {
        PromptModelStatus.Checking -> "We’re checking whether on-device prompting is ready on this device."
        PromptModelStatus.Downloading -> "The app can still run with the fallback parser while the model downloads."
        PromptModelStatus.Ready -> "Prompt resolution is ready to parse voice actions locally on this device."
        PromptModelStatus.Unavailable -> "This device is using the deterministic timer parser until Gemini Nano is available."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF3))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF05603A)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF344054)
            )
        }
    }
}

@Composable
private fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F766E), Color(0xFF115E59))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Voice Time Actions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Hands-free timers and alarms, resolved on-device and ready to run.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun VoiceCaptureCard(
    hasMicrophonePermission: Boolean,
    mode: VoiceActionMode,
    transcript: String,
    onMicTapped: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onActionResolved: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasMicrophonePermission) {
                PermissionPromptCard(onRequestMicrophonePermission = onRequestMicrophonePermission)
                return@Column
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onMicTapped,
                    enabled = mode == VoiceActionMode.Idle,
                    modifier = Modifier.size(88.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Mic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = when (mode) {
                    VoiceActionMode.Idle -> "Tap to speak"
                    VoiceActionMode.Listening -> "Listening"
                    VoiceActionMode.Processing -> "Processing"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color(0xFFD9E2EC),
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Text(
                    text = if (transcript.isBlank()) "Your transcript will appear here." else transcript,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
            }

            if (mode == VoiceActionMode.Processing) {
                Button(
                    onClick = onActionResolved,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resolve Action")
                }
            }
        }
    }
}

@Composable
private fun PermissionPromptCard(
    onRequestMicrophonePermission: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        modifier = Modifier.border(
            width = 1.dp,
            color = Color(0xFFD9E2EC),
            shape = RoundedCornerShape(16.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Microphone access needed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
            Text(
                text = "Allow microphone access to capture speech on-device and turn it into time actions.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4B5563)
            )
            Button(
                onClick = onRequestMicrophonePermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Microphone")
            }
        }
    }
}

@Composable
private fun ActionPreviewCard(
    title: String,
    subtitle: String,
    isReady: Boolean,
    executionResult: com.example.voicetotext.action.domain.ExecutionResult?
) {
    val cardColor = when (executionResult) {
        is com.example.voicetotext.action.domain.ExecutionResult.Success -> Color(0xFFECFDF3)
        is com.example.voicetotext.action.domain.ExecutionResult.Failure -> Color(0xFFFFF1F2)
        null -> Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resolved Action",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4B5563)
            )

            Spacer(modifier = Modifier.height(4.dp))

            val statusText = when (executionResult) {
                is com.example.voicetotext.action.domain.ExecutionResult.Success ->
                    "Action launched. Check the Clock app to confirm."
                is com.example.voicetotext.action.domain.ExecutionResult.Failure ->
                    "Failed: ${executionResult.reason}"
                null -> if (isReady) "Ready to execute once you confirm." else
                    "We’ll show the final action here after voice capture finishes."
            }
            val statusColor = when (executionResult) {
                is com.example.voicetotext.action.domain.ExecutionResult.Success -> Color(0xFF05603A)
                is com.example.voicetotext.action.domain.ExecutionResult.Failure -> Color(0xFF9B1C1C)
                null -> Color(0xFF6B7280)
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }
    }
}

@Composable
private fun DebugCard(output: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFD))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Debug Details",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475467)
            )
            Text(
                text = "Raw parser output stays tucked away so the primary experience remains action-first.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF667085)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
            ) {
                Text(
                    text = output,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE5EEF7)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderParserScreenPreview() {
    VoiceToTextTheme {
        ReminderParserScreen(
            uiState = ReminderParserUiState(
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
            onMicTapped = {},
            onRequestMicrophonePermission = {},
            onActionResolved = {},
            onRunActionClicked = {},
            onResetClicked = {}
        )
    }
}
