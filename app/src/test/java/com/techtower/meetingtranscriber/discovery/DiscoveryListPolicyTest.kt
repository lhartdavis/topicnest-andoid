package com.techtower.meetingtranscriber.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryListPolicyTest {
    @Test
    fun buildDiscoveryListSections_filtersBothSectionsButKeepsUntranscribedFirst() {
        val sections = buildSections(
            files = listOf(
                TestFile(id = "1", uri = "content://new", title = "Planning meeting"),
                TestFile(id = "2", uri = "content://done", title = "Planning retro"),
                TestFile(id = "3", uri = "content://other", title = "Sales call"),
            ),
            searchQuery = "planning",
            transcribedUris = setOf("content://done"),
        )

        assertEquals(listOf("1"), sections.browseFiles.map { it.id })
        assertEquals(listOf("2"), sections.transcribedFiles.map { it.id })
    }

    @Test
    fun buildDiscoveryListSections_excludesTranscribedFilesFromPriorityRows() {
        val sections = buildSections(
            files = listOf(
                TestFile(id = "1", uri = "content://new", title = "New long meeting", priority = true),
                TestFile(id = "2", uri = "content://done", title = "Done long meeting", priority = true),
            ),
            searchQuery = "",
            transcribedUris = setOf("content://done"),
        )

        assertEquals(listOf("1"), sections.priorityFiles.map { it.id })
        assertEquals(listOf("1"), sections.browseFiles.map { it.id })
        assertEquals(listOf("2"), sections.transcribedFiles.map { it.id })
        assertTrue(sections.priorityFiles.none { it.uri == "content://done" })
    }

    private fun buildSections(
        files: List<TestFile>,
        searchQuery: String,
        transcribedUris: Set<String>,
    ): DiscoveryListSections<TestFile> =
        buildDiscoveryListSections(
            files = files,
            searchQuery = searchQuery,
            transcribedUris = transcribedUris,
            displayName = { it.title },
            audioUri = { it.uri },
            isPriority = { it.priority },
        )

    private data class TestFile(
        val id: String,
        val uri: String,
        val title: String,
        val priority: Boolean = false,
    )
}
