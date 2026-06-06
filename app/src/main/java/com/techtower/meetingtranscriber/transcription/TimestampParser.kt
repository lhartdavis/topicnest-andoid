package com.techtower.meetingtranscriber.transcription

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class TimestampParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(rawJson: String): ParsedTranscription {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val segments = root["segments"].asArray().mapNotNull { parseSegment(it) }
        val topLevelWords = root["words"].asArray().mapNotNull { parseWord(it) }
        val nestedWords = root["segments"].asArray().flatMap { segment ->
            segment.asObject()?.get("words").asArray().mapNotNull { parseWord(it) }
        }
        val words = if (topLevelWords.isNotEmpty()) topLevelWords else nestedWords
        val plainText = root["text"].stringOrNull()
            ?: segments.joinToString(separator = " ") { it.text }.trim()

        return ParsedTranscription(
            plainText = plainText,
            segments = segments,
            words = words,
            usage = parseUsage(root["usage"].asObject()),
        )
    }

    private fun parseSegment(element: JsonElement): ParsedTimestampSegment? {
        val obj = element.asObject() ?: return null
        val startMillis = obj["start"].secondsToMillisOrNull() ?: return null
        val endMillis = obj["end"].secondsToMillisOrNull() ?: return null
        val text = obj["text"].stringOrNull()?.trim().orEmpty()
        if (text.isBlank()) return null
        return ParsedTimestampSegment(startMillis = startMillis, endMillis = endMillis, text = text)
    }

    private fun parseWord(element: JsonElement): ParsedTimestampWord? {
        val obj = element.asObject() ?: return null
        val startMillis = obj["start"].secondsToMillisOrNull() ?: return null
        val endMillis = obj["end"].secondsToMillisOrNull() ?: return null
        val word = obj["word"].stringOrNull()?.trim().orEmpty()
        if (word.isBlank()) return null
        return ParsedTimestampWord(startMillis = startMillis, endMillis = endMillis, word = word)
    }

    private fun parseUsage(obj: JsonObject?): ParsedUsage? {
        if (obj == null) return null
        return ParsedUsage(
            seconds = obj["seconds"].doubleOrNull(),
            cost = obj["cost"].doubleOrNull(),
            totalTokens = obj["total_tokens"].longOrNull(),
            inputTokens = obj["input_tokens"].longOrNull(),
            outputTokens = obj["output_tokens"].longOrNull(),
        )
    }

    private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

    private fun JsonElement?.asArray(): JsonArray = (this as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.content

    private fun JsonElement?.doubleOrNull(): Double? =
        (this as? JsonPrimitive)?.content?.toDoubleOrNull()

    private fun JsonElement?.longOrNull(): Long? =
        (this as? JsonPrimitive)?.content?.toLongOrNull()

    private fun JsonElement?.secondsToMillisOrNull(): Long? =
        doubleOrNull()?.let { (it * 1_000).toLong() }
}
