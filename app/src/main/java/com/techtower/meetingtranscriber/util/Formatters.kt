package com.techtower.meetingtranscriber.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val BYTES_PER_MB = 1024.0 * 1024.0

fun formatFileSize(sizeBytes: Long): String = String.format(Locale.US, "%.1f MB", sizeBytes / BYTES_PER_MB)

fun formatDuration(durationMillis: Long?): String {
    if (durationMillis == null || durationMillis <= 0) return "Unknown duration"
    val totalSeconds = durationMillis / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatClockTime(positionMillis: Long): String {
    val safePosition = positionMillis.coerceAtLeast(0)
    val totalSeconds = safePosition / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

fun formatModifiedTime(epochMillis: Long?): String =
    epochMillis?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) }
        ?: "Unknown modified time"

fun shortPreview(text: String?, maxChars: Int = 120): String {
    val clean = text?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
    return if (clean.length <= maxChars) clean else clean.take(maxChars).trimEnd() + "..."
}
