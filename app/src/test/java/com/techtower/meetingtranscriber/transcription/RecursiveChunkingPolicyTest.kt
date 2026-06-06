package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecursiveChunkingPolicyTest {
    @Test
    fun plan_usesDirectUploadForSmallFiles() {
        val plan = RecursiveChunkingPolicy(directUploadLimitBytes = 100).plan(
            sizeBytes = 99,
            durationMillis = 60_000,
        )

        assertEquals(ChunkingStrategy.DIRECT_UPLOAD, plan.strategy)
        assertTrue(plan.chunks.isEmpty())
    }

    @Test
    fun plan_warnsWhenLargeFileHasNoDuration() {
        val plan = RecursiveChunkingPolicy(directUploadLimitBytes = 100).plan(
            sizeBytes = 101,
            durationMillis = null,
        )

        assertEquals(ChunkingStrategy.DIRECT_UPLOAD_WITH_WARNING, plan.strategy)
        assertTrue(plan.message.contains("no usable duration"))
    }

    @Test
    fun plan_recursivelySplitsUntilChunksFitSizeAndDurationPolicy() {
        val plan = RecursiveChunkingPolicy(
            directUploadLimitBytes = 100,
            targetChunkDurationMillis = 60_000,
            overlapMillis = 0,
        ).plan(
            sizeBytes = 400,
            durationMillis = 240_000,
        )

        assertEquals(ChunkingStrategy.RECURSIVE_CHUNKS, plan.strategy)
        assertEquals(4, plan.chunks.size)
        assertEquals(0L, plan.chunks.first().startMillis)
        assertEquals(60_000L, plan.chunks.first().endMillis)
        assertEquals(240_000L, plan.chunks.last().endMillis)
        assertTrue(plan.chunks.all { it.estimatedUploadBytes <= 100 })
    }

    @Test
    fun plan_addsOverlapToInteriorUploadWindows() {
        val plan = RecursiveChunkingPolicy(
            directUploadLimitBytes = 100,
            targetChunkDurationMillis = 60_000,
            overlapMillis = 1_000,
        ).plan(
            sizeBytes = 120,
            durationMillis = 120_000,
        )

        assertEquals(ChunkingStrategy.RECURSIVE_CHUNKS, plan.strategy)
        assertEquals(0L, plan.chunks.first().uploadStartMillis)
        assertEquals(61_000L, plan.chunks.first().uploadEndMillis)
        assertEquals(59_000L, plan.chunks.last().uploadStartMillis)
        assertEquals(120_000L, plan.chunks.last().uploadEndMillis)
    }
}
