package com.example.voicetotext.voiceaction.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.example.voicetotext.action.domain.ExecutionResult
import com.example.voicetotext.action.domain.VoiceAction
import com.example.voicetotext.action.domain.VoiceActionExecutor
import com.example.voicetotext.action.domain.VoiceActionParser
import com.example.voicetotext.speech.domain.SpeechRecognizer
import com.example.voicetotext.ui.theme.VoiceToTextTheme

private val AppBackground = Color(0xFFF5EFE5)
private val Sand = Color(0xFFFFFBF5)
private val Ink = Color(0xFF1E293B)
private val Slate = Color(0xFF52606D)
private val Sage = Color(0xFFE7F7F0)
private val SageText = Color(0xFF0F6C4E)
private val Coral = Color(0xFFEE6C4D)
private val CoralDark = Color(0xFFB84B32)
private val Sky = Color(0xFFE8F1FF)
private val SkyBorder = Color(0xFFBED2F6)
private val Rose = Color(0xFFFFF1F2)
private val RoseText = Color(0xFFB42318)

@Composable
fun VoiceActionRoute(
    parser: VoiceActionParser,
    executor: VoiceActionExecutor,
    speechRecognizer: SpeechRecognizer,
    promptModel: OnDevicePromptModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: VoiceActionViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                VoiceActionViewModel(
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

    VoiceActionScreen(
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
fun VoiceActionScreen(
    uiState: VoiceActionUiState,
    modelStatus: PromptModelStatus,
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    StatusPill(status = modelStatus)
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
                        onRunActionClicked = onRunActionClicked,
                        onResetClicked = onResetClicked
                    )
                    DebugCard(output = uiState.outputJson)
                }
            }
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
                    colors = listOf(Color(0xFF103B36), Color(0xFF1B5B52), Color(0xFF2F7A6D))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Voice Time Actions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Speak one clear phrase, see the action resolve on-device, then launch it instantly.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun StatusPill(status: PromptModelStatus) {
    val label = when (status) {
        PromptModelStatus.Checking -> "Checking on-device model"
        PromptModelStatus.Downloading -> "Downloading Gemini Nano"
        PromptModelStatus.Ready -> "On-device model ready"
        PromptModelStatus.Unavailable -> "Using local fallback parser"
    }
    val detail = when (status) {
        PromptModelStatus.Checking -> "We are verifying whether Gemini Nano is ready on this device."
        PromptModelStatus.Downloading -> "Voice actions still work while the model downloads in the background."
        PromptModelStatus.Ready -> "Prompt-based action resolution is available locally."
        PromptModelStatus.Unavailable -> "This device is running the deterministic parser path."
    }
    val background = when (status) {
        PromptModelStatus.Unavailable -> Color(0xFFFFF7ED)
        else -> Sage
    }
    val accent = when (status) {
        PromptModelStatus.Unavailable -> Color(0xFFB45309)
        else -> SageText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Ink.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun HeroVoiceCard(
    hasMicrophonePermission: Boolean,
    mode: VoiceActionMode,
    transcript: String,
    onMicTapped: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onActionResolved: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Sand)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "One tap. One phrase. One action.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Say things like “set a timer for 10 minutes for pasta” or “wake me in 20 minutes.”",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
                textAlign = TextAlign.Center
            )

            if (!hasMicrophonePermission) {
                PermissionPromptCard(onRequestMicrophonePermission = onRequestMicrophonePermission)
                return@Column
            }

            MicHeroButton(
                mode = mode,
                onMicTapped = onMicTapped
            )

            Text(
                text = when (mode) {
                    VoiceActionMode.Idle -> "Tap the mic to start"
                    VoiceActionMode.Listening -> "Listening for your request"
                    VoiceActionMode.Processing -> "Turning speech into an action"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                textAlign = TextAlign.Center
            )

            TranscriptPanel(
                transcript = transcript,
                mode = mode
            )

            AnimatedVisibility(visible = mode == VoiceActionMode.Processing) {
                OutlinedButton(
                    onClick = onActionResolved,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resolve Again")
                }
            }
        }
    }
}

@Composable
private fun MicHeroButton(
    mode: VoiceActionMode,
    onMicTapped: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (mode == VoiceActionMode.Listening) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = if (mode == VoiceActionMode.Listening) 0.34f else 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val heroColor = when (mode) {
        VoiceActionMode.Idle -> Coral
        VoiceActionMode.Listening -> Color(0xFFDB5B57)
        VoiceActionMode.Processing -> Color(0xFF355C7D)
    }

    Box(
        modifier = Modifier
            .size(210.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(188.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(heroColor)
        )
        Surface(
            modifier = Modifier
                .size(142.dp)
                .clip(CircleShape)
                .clickable(enabled = mode == VoiceActionMode.Idle, onClick = onMicTapped),
            shape = CircleShape,
            color = heroColor,
            shadowElevation = 12.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = when (mode) {
                        VoiceActionMode.Idle -> "MIC"
                        VoiceActionMode.Listening -> "LIVE"
                        VoiceActionMode.Processing -> "AI"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TranscriptPanel(
    transcript: String,
    mode: VoiceActionMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Sky)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Live transcript",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Text(
                text = if (transcript.isBlank()) {
                    when (mode) {
                        VoiceActionMode.Idle -> "Your spoken phrase will appear here."
                        VoiceActionMode.Listening -> "Listening for speech..."
                        VoiceActionMode.Processing -> "Finalizing transcript..."
                    }
                } else {
                    transcript
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Ink
            )
        }
    }
}

@Composable
private fun PermissionPromptCard(
    onRequestMicrophonePermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Microphone access needed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = "Allow microphone access so the app can capture speech locally and turn it into time actions.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate
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
    executionResult: ExecutionResult?
) {
    val cardColor = when (executionResult) {
        is ExecutionResult.Success -> Sage
        is ExecutionResult.Failure -> Rose
        null -> Color.White
    }
    val borderColor = when (executionResult) {
        is ExecutionResult.Success -> Color(0xFFA9E2C4)
        is ExecutionResult.Failure -> Color(0xFFF5C2C7)
        null -> SkyBorder
    }
    val statusText = when (executionResult) {
        is ExecutionResult.Success -> "Timer intent launched. Check the Clock app."
        is ExecutionResult.Failure -> "Couldn't launch action: ${executionResult.reason}"
        null -> if (isReady) "Ready to run now." else "Resolved output will appear here after capture."
    }
    val statusColor = when (executionResult) {
        is ExecutionResult.Success -> SageText
        is ExecutionResult.Failure -> RoseText
        null -> Slate
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resolved action",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Slate
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Slate
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

@Composable
private fun ActionButtons(
    canRun: Boolean,
    onRunActionClicked: () -> Unit,
    onResetClicked: () -> Unit
) {
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
}

@Composable
private fun DebugCard(output: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFCFD))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Technical details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                    Text(
                        text = "Raw parser output for debugging and demos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate
                    )
                }
                Text(
                    text = if (expanded) "Hide" else "Show",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CoralDark
                )
            }

            AnimatedVisibility(visible = expanded) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18212F))
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
            onMicTapped = {},
            onRequestMicrophonePermission = {},
            onActionResolved = {},
            onRunActionClicked = {},
            onResetClicked = {}
        )
    }
}
