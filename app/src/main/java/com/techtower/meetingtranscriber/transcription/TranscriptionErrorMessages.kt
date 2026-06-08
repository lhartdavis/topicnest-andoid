package com.techtower.meetingtranscriber.transcription

fun transcriptionErrorMessage(
    rawMessage: String?,
    apiKeyIsValid: Boolean = false,
    uploadFormat: String? = null,
): String {
    val message = rawMessage?.takeIf { it.isNotBlank() } ?: return "Transcription failed."
    val lowercase = message.lowercase()
    return when {
        "could not convert audio to mp3" in lowercase ->
            "The app could not convert this recording to MP3 on this device. Retry once; if it still fails, try a different source recording format."
        "could not split audio into mp3 chunks" in lowercase ->
            "The app could not split this recording into MP3 chunks. Retry once; if it still fails, use a shorter recording."
        "converted mp3 is too large" in lowercase || "mp3 chunk" in lowercase && "too large" in lowercase ->
            "The converted MP3 is still too large for the current upload path. Try a shorter recording until provider-side or streaming upload support is added."
        "failed to allocate" in lowercase || "outofmemory" in lowercase || "out of memory" in lowercase ->
            "Audio became too large for the phone to prepare for upload. Retry with automatic MP3 conversion, or use a shorter recording."
        "wav fallback" in lowercase && ("too large" in lowercase || "exceed" in lowercase) ->
            "This recording is too long for the automatic WAV retry. The app should try MP3 conversion first; retry the job, or use a shorter recording if it still fails."
        "broken pipe" in lowercase ->
            "Upload connection broke while sending audio. Retry on a stable connection, restart the queue if jobs are stuck, or let the app convert/chunk the file automatically."
        "http 400" in lowercase ->
            if (apiKeyIsValid) {
                val formatHint = if (uploadFormat == "aac") {
                    " The app could not convert this .aac recording to MP3, or the provider still rejected the converted audio."
                } else if (uploadFormat == "m4a") {
                    " The app sent m4a audio and should retry as MP3 for AAC-like provider failures."
                } else if (uploadFormat == "mp3") {
                    " The app sent MP3 audio and the provider still rejected it; try a shorter recording or a different model route."
                } else if (uploadFormat == "wav") {
                    " The app retried with WAV and the provider still rejected the request. Try a shorter recording or a different model route."
                } else {
                    " The audio format or provider route may not support direct transcription."
                }
                "Your OpenRouter key is valid, but OpenRouter/provider rejected this audio request (HTTP 400).$formatHint"
            } else {
                "OpenRouter/provider rejected the transcription request (HTTP 400). Test the API key; if it is valid, this audio format or provider route may not support direct transcription."
            }
        "payload" in lowercase || "too large" in lowercase ->
            "Audio is too large for direct upload. Try a smaller file or wait for chunked upload support."
        else -> message
    }
}
