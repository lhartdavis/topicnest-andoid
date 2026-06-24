package com.techtower.meetingtranscriber.discovery

data class DiscoveryListSections<T>(
    val priorityFiles: List<T>,
    val browseFiles: List<T>,
    val transcribedFiles: List<T>,
)

fun <T> buildDiscoveryListSections(
    files: List<T>,
    searchQuery: String,
    transcribedUris: Set<String>,
    displayName: (T) -> String,
    audioUri: (T) -> String,
    isPriority: (T) -> Boolean,
): DiscoveryListSections<T> {
    val query = searchQuery.trim().lowercase()
    val matchingFiles = if (query.isBlank()) {
        files
    } else {
        files.filter { displayName(it).lowercase().contains(query) }
    }
    val transcribedFiles = matchingFiles.filter { audioUri(it) in transcribedUris }
    val browseFiles = matchingFiles.filterNot { audioUri(it) in transcribedUris }

    return DiscoveryListSections(
        priorityFiles = browseFiles.filter(isPriority),
        browseFiles = browseFiles,
        transcribedFiles = transcribedFiles,
    )
}
