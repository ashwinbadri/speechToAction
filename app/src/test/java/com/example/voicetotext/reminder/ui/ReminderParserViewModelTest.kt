package com.example.voicetotext.reminder.ui

import com.example.voicetotext.reminder.domain.ReminderIntent
import com.example.voicetotext.reminder.domain.ReminderParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderParserViewModelTest {

    @Test
    fun `onInputChanged updates input state`() {
        val viewModel = ReminderParserViewModel(parser = FakeReminderParser())

        viewModel.onInputChanged("buy groceries")

        assertEquals("buy groceries", viewModel.uiState.value.input)
    }

    @Test
    fun `onParseClicked updates output json from parser result`() {
        val parserResult = ReminderIntent(
            title = "call mom",
            datetime = "2026-05-01T19:00:00",
            confidence = 0.9
        )
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(result = parserResult)
        )

        viewModel.onInputChanged("Remind me to call mom tomorrow at 7pm")
        viewModel.onParseClicked()

        assertEquals(parserResult.toJson(), viewModel.uiState.value.outputJson)
    }

    @Test
    fun `onClearClicked resets input and output`() {
        val viewModel = ReminderParserViewModel(
            parser = FakeReminderParser(
                result = ReminderIntent(
                    title = "call mom",
                    datetime = null,
                    confidence = 0.2
                )
            )
        )

        viewModel.onInputChanged("call mom")
        viewModel.onParseClicked()
        viewModel.onClearClicked()

        assertEquals("", viewModel.uiState.value.input)
        assertEquals(ReminderIntent.empty().toJson(), viewModel.uiState.value.outputJson)
    }

    private class FakeReminderParser(
        private val result: ReminderIntent = ReminderIntent.empty()
    ) : ReminderParser {
        override fun parse(input: String): ReminderIntent = result
    }
}
