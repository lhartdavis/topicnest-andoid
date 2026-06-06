package com.techtower.meetingtranscriber.util

private val supportedExtensions = setOf(
    "m4a",
    "mp3",
    "wav",
    "flac",
    "ogg",
    "webm",
    "aac",
    "mp4",
    "mpeg",
    "mpga",
)

fun isSupportedAudioName(name: String): Boolean = audioFormatFromName(name) != null

fun audioFormatFromName(name: String): String? =
    name.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .takeIf { it in supportedExtensions }

fun audioFormatFromMimeType(mimeType: String?): String? =
    when (mimeType?.lowercase()) {
        "audio/mp4", "audio/x-m4a" -> "m4a"
        "audio/mpeg" -> "mp3"
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/flac" -> "flac"
        "audio/ogg" -> "ogg"
        "audio/webm" -> "webm"
        "audio/aac" -> "aac"
        else -> null
    }

fun audioFormatFromNameOrMimeType(name: String, mimeType: String?): String? =
    audioFormatFromName(name) ?: audioFormatFromMimeType(mimeType)
