package com.example.voicetotext.voiceaction.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voicetotext.action.domain.ExecutionResult
import com.example.voicetotext.action.domain.VoiceAction

@Composable
internal fun ActionPreviewCard(
    title: String,
    subtitle: String,
    isReady: Boolean,
    executionResult: ExecutionResult?
) {
    val cardColor = when (executionResult) {
        is ExecutionResult.Success -> Sage
        is ExecutionResult.Failure -> Rose
        null -> androidx.compose.ui.graphics.Color.White
    }
    val borderColor = when (executionResult) {
        is ExecutionResult.Success -> androidx.compose.ui.graphics.Color(0xFFA9E2C4)
        is ExecutionResult.Failure -> androidx.compose.ui.graphics.Color(0xFFF5C2C7)
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resolved action",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Slate
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
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
internal fun FollowUpCard(
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
        is ExecutionResult.Success -> androidx.compose.ui.graphics.Color(0xFFF7FDF9)
        is ExecutionResult.Failure -> androidx.compose.ui.graphics.Color(0xFFFFFBFA)
    }
    val accent = when (executionResult) {
        is ExecutionResult.Success -> SageText
        is ExecutionResult.Failure -> RoseText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            suggestions.take(1).forEach { suggestion ->
                Text(
                    text = "• $suggestion",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
internal fun ActionButtons(
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
internal fun DebugCard(output: String) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFCFCFD))
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
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18212F))
                ) {
                    Text(
                        text = output,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = androidx.compose.ui.graphics.Color(0xFFE5EEF7)
                    )
                }
            }
        }
    }
}
