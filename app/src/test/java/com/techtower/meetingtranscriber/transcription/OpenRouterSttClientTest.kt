package com.techtower.meetingtranscriber.transcription

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OpenRouterSttClientTest {
    @Test
    fun buildRequestJson_usesDocumentedSttRequestShape() {
        val json = OpenRouterSttClient().buildRequestJson(
            model = DEFAULT_TRANSCRIPTION_MODEL,
            audioBytes = byteArrayOf(1, 2, 3),
            format = "aac",
        )

        assertEquals(DEFAULT_TRANSCRIPTION_MODEL, json.stringValue("model"))
        assertEquals("en", json.stringValue("language"))
        assertEquals("0", json["temperature"].toString())
        val inputAudio = json["input_audio"] as JsonObject
        assertEquals("AQID", inputAudio.stringValue("data"))
        assertEquals("aac", inputAudio.stringValue("format"))
        assertFalse(json.containsKey("response_format"))
        assertFalse(json.containsKey("timestamp_granularities"))
    }

    private fun JsonObject.stringValue(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull
}
