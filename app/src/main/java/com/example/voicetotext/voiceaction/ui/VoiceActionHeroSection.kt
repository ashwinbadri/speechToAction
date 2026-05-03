package com.example.voicetotext.voiceaction.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.voicetotext.action.data.PromptModelStatus

@Composable
internal fun Header() {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Voice Time Actions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Speak once. Review. Launch.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.84f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatusPill(
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Text(
                text = when (status) {
                    PromptModelStatus.Checking -> "Checking"
                    PromptModelStatus.Downloading -> "Downloading"
                    PromptModelStatus.Ready -> "Ready"
                    PromptModelStatus.Unavailable -> "Fallback"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

@Composable
internal fun HeroVoiceCard(
    hasMicrophonePermission: Boolean,
    mode: VoiceActionMode,
    transcript: String,
    onMicTapped: () -> Unit,
    onRequestMicrophonePermission: () -> Unit,
    onActionResolved: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Sand)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "One tap. One phrase.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Try “set a timer for 10 minutes” or “wake me at 7 AM.”",
                style = MaterialTheme.typography.bodySmall,
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
                style = MaterialTheme.typography.bodyLarge,
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
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        if (mode == VoiceActionMode.Listening) {
            Canvas(
                modifier = Modifier
                    .size(174.dp)
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
                .size(160.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
                .clip(CircleShape)
                .background(heroColor)
        )
        Surface(
            modifier = Modifier
                .size(124.dp)
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
    height: Dp,
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Sky)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                style = MaterialTheme.typography.bodyMedium,
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
