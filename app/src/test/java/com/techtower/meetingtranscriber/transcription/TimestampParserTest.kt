package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimestampParserTest {
    private val parser = TimestampParser()

    @Test
    fun parse_acceptsTextOnlyResponse() {
        val parsed = parser.parse("""{"text":"Meeting started at nine."}""")

        assertEquals("Meeting started at nine.", parsed.plainText)
        assertTrue(parsed.segments.isEmpty())
        assertTrue(parsed.words.isEmpty())
    }

    @Test
    fun parse_acceptsSegmentResponse() {
        val parsed = parser.parse(
            """
            {
              "text": "Hello team.",
              "segments": [
                {"start": 1.25, "end": 2.5, "text": "Hello team."}
              ],
              "usage": {"seconds": 3.0, "cost": 0.01}
            }
            """.trimIndent(),
        )

        assertEquals("Hello team.", parsed.plainText)
        assertEquals(1, parsed.segments.size)
        assertEquals(1250L, parsed.segments.first().startMillis)
        assertEquals(2500L, parsed.segments.first().endMillis)
        assertEquals(3.0, parsed.usage?.seconds ?: 0.0, 0.0)
        assertEquals(0.01, parsed.usage?.cost ?: 0.0, 0.0)
    }

    @Test
    fun parse_acceptsWordResponse() {
        val parsed = parser.parse(
            """
            {
              "text": "Hello team",
              "words": [
                {"start": 0.0, "end": 0.4, "word": "Hello"},
                {"start": 0.5, "end": 0.9, "word": "team"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(2, parsed.words.size)
        assertEquals("Hello", parsed.words.first().word)
        assertEquals(900L, parsed.words.last().endMillis)
    }
}
