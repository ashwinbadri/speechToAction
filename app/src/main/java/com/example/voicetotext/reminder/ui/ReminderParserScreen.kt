package com.example.voicetotext.reminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.voicetotext.reminder.data.PlaceholderReminderParser
import com.example.voicetotext.reminder.domain.ReminderParser
import com.example.voicetotext.ui.theme.VoiceToTextTheme

@Composable
fun ReminderParserRoute(
    parser: ReminderParser,
    modifier: Modifier = Modifier
) {
    val viewModel: ReminderParserViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ReminderParserViewModel(parser = parser)
            }
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReminderParserScreen(
        uiState = uiState,
        onInputChanged = viewModel::onInputChanged,
        onParseClicked = viewModel::onParseClicked,
        onClearClicked = viewModel::onClearClicked,
        modifier = modifier
    )
}

@Composable
fun ReminderParserScreen(
    uiState: ReminderParserUiState,
    onInputChanged: (String) -> Unit,
    onParseClicked: () -> Unit,
    onClearClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFFF6F6F2)
    ) {
        Scaffold(
            containerColor = Color.Transparent
        ) { innerPadding ->
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
                        text = "Try a sentence like “Remind me to call mom tomorrow at 7pm”.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF4C4C4C)
                    )

                    OutlinedTextField(
                        value = uiState.input,
                        onValueChange = onInputChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reminder request") },
                        placeholder = { Text("Type or paste a reminder sentence") },
                        minLines = 4,
                        shape = RoundedCornerShape(18.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onParseClicked,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Parse")
                        }

                        Button(
                            onClick = onClearClicked,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }
                    }

                    ResultCard(output = uiState.outputJson)
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
                    colors = listOf(Color(0xFF0F766E), Color(0xFF115E59))
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Voice Reminder Parser",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Turn a voice assistant sentence into structured JSON.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ResultCard(output: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "JSON Output",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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

            Text(
                text = "This step uses a placeholder parser so the MVVM structure is ready for real intent parsing next.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderParserScreenPreview() {
    VoiceToTextTheme {
        ReminderParserScreen(
            uiState = ReminderParserUiState(
                input = "Remind me to call mom tomorrow at 7pm"
            ),
            onInputChanged = {},
            onParseClicked = {},
            onClearClicked = {}
        )
    }
}
