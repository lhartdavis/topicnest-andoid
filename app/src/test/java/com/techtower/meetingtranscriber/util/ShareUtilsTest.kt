package com.techtower.meetingtranscriber.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareUtilsTest {
    @Test
    fun formatShareText_putsNotesBeforeTranscript() {
        val text = formatShareText(
            notes = "Follow up with product.",
            transcript = "We reviewed launch timing.",
        )

        assertEquals(
            """
            Notes:
            Follow up with product.

            Transcript:
            We reviewed launch timing.
            """.trimIndent(),
            text,
        )
    }

    @Test
    fun formatShareText_keepsEmptyNotesSection() {
        val text = formatShareText(notes = "", transcript = "Plain transcript.")

        assertEquals(
            """
            Notes:

            Transcript:
            Plain transcript.
            """.trimIndent(),
            text,
        )
    }
}
