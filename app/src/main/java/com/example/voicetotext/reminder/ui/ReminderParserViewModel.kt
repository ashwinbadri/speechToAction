package com.example.voicetotext.reminder.ui

import androidx.lifecycle.ViewModel
import com.example.voicetotext.reminder.domain.ReminderIntent
import com.example.voicetotext.reminder.domain.ReminderParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReminderParserViewModel(
    private val parser: ReminderParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReminderParserUiState())
    val uiState: StateFlow<ReminderParserUiState> = _uiState.asStateFlow()

    fun onInputChanged(value: String) {
        _uiState.update { currentState ->
            currentState.copy(input = value)
        }
    }

    fun onParseClicked() {
        val parsedIntent = parser.parse(_uiState.value.input)
        _uiState.update { currentState ->
            currentState.copy(outputJson = parsedIntent.toJson())
        }
    }

    fun onClearClicked() {
        _uiState.value = ReminderParserUiState(
            input = "",
            outputJson = ReminderIntent.empty().toJson()
        )
    }
}
