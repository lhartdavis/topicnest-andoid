package com.techtower.meetingtranscriber.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenRouterKeyClientTest {
    @Test
    fun parseSuccess_marksKeyValidAndShowsRemainingLimit() {
        val result = OpenRouterKeyClient().parseSuccess(
            """
            {
              "data": {
                "label": "Meeting Transcriber test key",
                "limit_remaining": 12.5
              }
            }
            """.trimIndent(),
        )

        assertEquals(ApiKeyValidationStatus.VALID, result.status)
        assertEquals("Meeting Transcriber test key", result.label)
        assertEquals("Key works. Limit remaining: 12.50.", result.message)
    }

    @Test
    fun parseSuccess_handlesMissingLimit() {
        val result = OpenRouterKeyClient().parseSuccess("""{"data":{"label":"Meeting key"}}""")

        assertEquals(ApiKeyValidationStatus.VALID, result.status)
        assertEquals("Meeting key", result.label)
        assertEquals("Key works.", result.message)
    }
}
