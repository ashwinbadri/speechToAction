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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.voicetotext.action.data.PromptModelStatus
import com.example.voicetotext.action.domain.ExecutionResult
import com.example.voicetotext.action.domain.VoiceAction
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
    isDebug: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: VoiceActionViewModel = hiltViewModel()
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
        isDebug = isDebug,
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
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
                    FollowUpCard(
                        lastAction = uiState.lastAction,
                        executionResult = uiState.executionResult,
                        onResetClicked = onResetClicked
                    )
                    ActionButtons(
                        canRun = canRun,
                        executionResult = uiState.executionResult,
                        onRunActionClicked = onRunActionClicked,
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
private fun StatusPill(
    status: PromptModelStatus,
    isDebug: Boolean
) {
    val label = when (status) {
        PromptModelStatus.Checking -> "Checking on-device model"
        PromptModelStatus.Downloading -> "Downloading Gemini Nano"
        PromptModelStatus.Ready -> "On-device model ready"
        PromptModelStatus.Unavailable -> if (isDebug) {
            "Using local fallback parser"
        } else {
            "On-device model unavailable"
        }
    }
    val detail = when (status) {
        PromptModelStatus.Checking -> "We are verifying whether Gemini Nano is ready on this device."
        PromptModelStatus.Downloading -> "Voice actions still work while the model downloads in the background."
        PromptModelStatus.Ready -> "Prompt-based action resolution is available locally."
        PromptModelStatus.Unavailable -> if (isDebug) {
            "This device is running the deterministic parser path."
        } else {
            "The enhanced model path is not available on this device right now."
        }
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
    val orbitSweep by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitSweep"
    )
    val waveformPhase by pulseTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveformPhase"
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
        if (mode == VoiceActionMode.Listening) {
            Canvas(
                modifier = Modifier
                    .size(204.dp)
                    .alpha(0.75f)
            ) {
                drawArc(
                    color = Color.White.copy(alpha = 0.45f),
                    startAngle = orbitSweep,
                    sweepAngle = 110f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 10.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.2f),
                    startAngle = orbitSweep + 170f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
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
                if (mode == VoiceActionMode.Listening) {
                    RecordingWaveform(phase = waveformPhase)
                } else {
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
}

@Composable
private fun RecordingWaveform(phase: Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WaveBar(height = 28.dp, scale = phase * 0.92f)
        WaveBar(height = 46.dp, scale = phase)
        WaveBar(height = 34.dp, scale = phase * 0.96f)
        WaveBar(height = 22.dp, scale = phase * 0.88f)
    }
}

@Composable
private fun WaveBar(
    height: androidx.compose.ui.unit.Dp,
    scale: Float
) {
    Box(
        modifier = Modifier
            .size(width = 10.dp, height = height)
            .scale(scaleX = 1f, scaleY = scale)
            .clip(RoundedCornerShape(100))
            .background(Color.White)
    )
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
    val permissionTransition = rememberInfiniteTransition(label = "permissionPrompt")
    val haloScale by permissionTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )
    val haloAlpha by permissionTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )
    val sparkleFloat by permissionTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleFloat"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(172.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .scale(haloScale)
                        .alpha(haloAlpha)
                        .clip(CircleShape)
                        .background(Color(0xFFFBD4C9))
                )
                Canvas(
                    modifier = Modifier
                        .size(164.dp)
                        .alpha(0.7f)
                ) {
                    drawCircle(
                        color = Color(0xFFF4A261),
                        radius = size.minDimension / 2.35f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFDE6D8),
                        radius = size.minDimension / 2.9f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12.dp.toPx())
                    )
                }
                Surface(
                    modifier = Modifier.size(108.dp),
                    shape = CircleShape,
                    color = Coral,
                    shadowElevation = 10.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "MIC",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = "✦",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 22.dp, end = 18.dp)
                        .alpha(0.8f)
                        .scale(1f + (sparkleFloat / 60f)),
                    color = Color(0xFFFFB703),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "✦",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 18.dp, bottom = 22.dp)
                        .alpha(0.65f)
                        .scale(0.9f - (sparkleFloat / 100f)),
                    color = Color(0xFFFFD166),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = "Turn on the mic to unlock the magic",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Voice Time Actions works best when you can just speak naturally. Enable microphone access and we’ll handle the rest on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You’ll be able to set timers and alarms hands-free in one tap.",
                style = MaterialTheme.typography.bodySmall,
                color = CoralDark,
                textAlign = TextAlign.Center
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
private fun FollowUpCard(
    lastAction: VoiceAction?,
    executionResult: ExecutionResult?,
    onResetClicked: () -> Unit
) {
    if (executionResult == null || lastAction == null || lastAction is VoiceAction.Unknown) return

    val title = when (executionResult) {
        is ExecutionResult.Success -> "Ready for the next action?"
        is ExecutionResult.Failure -> "Try a cleaner follow-up"
    }
    val subtitle = when (executionResult) {
        is ExecutionResult.Success -> when (lastAction) {
            is VoiceAction.SetTimer -> "Your timer was handed off to Clock. You can quickly set another timer or switch to an alarm."
            is VoiceAction.SetAlarm -> "Your alarm was handed off to Clock. You can set another alarm or jump back to a timer."
            is VoiceAction.Unknown -> ""
        }
        is ExecutionResult.Failure -> "The action didn’t launch, but your parsed request is still here. Reset and try the phrase again or simplify the wording."
    }
    val suggestions = when (executionResult) {
        is ExecutionResult.Success -> when (lastAction) {
            is VoiceAction.SetTimer -> listOf(
                "Try “set another timer for 5 minutes for tea.”",
                "Try “set an alarm for 7 AM tomorrow.”"
            )
            is VoiceAction.SetAlarm -> listOf(
                "Try “set another alarm for 6:30 AM.”",
                "Try “set a timer for 20 minutes for pasta.”"
            )
            is VoiceAction.Unknown -> emptyList()
        }
        is ExecutionResult.Failure -> listOf(
            "Keep the action phrase short and specific.",
            "Include a clear time like “10 minutes” or “7 AM”."
        )
    }
    val cardColor = when (executionResult) {
        is ExecutionResult.Success -> Color(0xFFF7FDF9)
        is ExecutionResult.Failure -> Color(0xFFFFFBFA)
    }
    val accent = when (executionResult) {
        is ExecutionResult.Success -> SageText
        is ExecutionResult.Failure -> RoseText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate
            )
            suggestions.forEach { suggestion ->
                Text(
                    text = "• $suggestion",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink
                )
            }
            Button(
                onClick = onResetClicked,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (executionResult) {
                        is ExecutionResult.Success -> "Set another action"
                        is ExecutionResult.Failure -> "Try again"
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    canRun: Boolean,
    executionResult: ExecutionResult?,
    onRunActionClicked: () -> Unit,
    onResetClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onRunActionClicked,
            enabled = canRun && executionResult == null,
            modifier = Modifier.weight(1f)
        ) {
            Text(if (executionResult == null) "Run Action" else "Action Sent")
        }
        OutlinedButton(
            onClick = onResetClicked,
            modifier = Modifier.weight(1f)
        ) {
            Text(if (executionResult == null) "Reset" else "Start Over")
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
            isDebug = true,
            onMicTapped = {},
            onRequestMicrophonePermission = {},
            onActionResolved = {},
            onRunActionClicked = {},
            onResetClicked = {}
        )
    }
}
