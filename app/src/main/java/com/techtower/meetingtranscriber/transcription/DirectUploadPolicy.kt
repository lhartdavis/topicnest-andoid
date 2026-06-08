package com.techtower.meetingtranscriber.transcription

import kotlin.math.ceil

fun estimatedBase64UploadBytes(rawBytes: Long): Long {
    if (rawBytes <= 0L) return 0L
    return ceil(rawBytes.toDouble() / 3.0).toLong() * 4L
}

fun requiresDirectUploadWarning(rawBytes: Long, maxPayloadBytes: Long): Boolean =
    estimatedBase64UploadBytes(rawBytes) > maxPayloadBytes
