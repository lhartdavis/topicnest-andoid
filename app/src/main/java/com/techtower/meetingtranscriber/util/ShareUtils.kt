package com.techtower.meetingtranscriber.util

import android.content.Intent

fun formatShareText(notes: String?, transcript: String?): String {
    val normalizedNotes = notes?.trim().orEmpty()
    val normalizedTranscript = transcript?.trim().orEmpty()
    return buildString {
        appendLine("Notes:")
        if (normalizedNotes.isNotEmpty()) {
            appendLine(normalizedNotes)
        }
        appendLine()
        appendLine("Transcript:")
        append(normalizedTranscript)
    }
}

fun buildShareIntent(notes: String?, transcript: String?): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, formatShareText(notes, transcript))
    }
