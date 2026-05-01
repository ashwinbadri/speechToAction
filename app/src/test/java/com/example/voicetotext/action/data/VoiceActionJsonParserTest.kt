package com.example.voicetotext.action.data

import com.example.voicetotext.action.domain.VoiceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceActionJsonParserTest {

    @Test
    fun `parse returns timer action for valid timer json`() {
        val result = VoiceActionJsonParser.parse(
            """
            {
              "intent": "SET_TIMER",
              "duration_seconds": 600,
              "label": "pasta",
              "confidence": 0.95
            }
            """.trimIndent()
        )

        assertEquals(
            VoiceAction.SetTimer(
                durationSeconds = 600,
                label = "pasta",
                confidence = 0.95
            ),
            result
        )
    }

    @Test
    fun `parse ignores markdown fences`() {
        val result = VoiceActionJsonParser.parse(
            """
            ```json
            {
              "intent": "UNKNOWN_ACTION",
              "duration_seconds": null,
              "label": null,
              "confidence": 0.2
            }
            ```
            """.trimIndent()
        )

        assertEquals(VoiceAction.Unknown(confidence = 0.2), result)
    }

    @Test
    fun `parse returns null for invalid timer payload`() {
        val result = VoiceActionJsonParser.parse(
            """
            {
              "intent": "SET_TIMER",
              "duration_seconds": null,
              "label": "pasta",
              "confidence": 0.9
            }
            """.trimIndent()
        )

        assertNull(result)
    }
}
