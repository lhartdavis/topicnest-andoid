package com.techtower.meetingtranscriber.discovery

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.techtower.meetingtranscriber.data.entities.AudioSource
import com.techtower.meetingtranscriber.util.isSupportedAudioName

class SafAudioScanner(private val context: Context) {
    fun scan(treeUri: Uri): List<DiscoveredAudioFile> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return scanDirectory(root)
    }

    private fun scanDirectory(directory: DocumentFile): List<DiscoveredAudioFile> =
        buildList {
            directory.listFiles().forEach { child ->
                when {
                    child.isDirectory -> addAll(scanDirectory(child))
                    child.isFile && isSupportedAudioName(child.name.orEmpty()) -> add(child.toAudioFile())
                }
            }
        }

    private fun DocumentFile.toAudioFile(): DiscoveredAudioFile =
        DiscoveredAudioFile(
            id = "saf:${uri}",
            uri = uri,
            displayName = name ?: "Audio file",
            sizeBytes = length().coerceAtLeast(0),
            durationMillis = readDuration(uri),
            modifiedEpochMillis = lastModified().takeIf { it > 0 },
            mimeType = type,
            relativePath = null,
            source = AudioSource.SAF,
        )

    private fun readDuration(uri: Uri): Long? =
        try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        } catch (_: RuntimeException) {
            null
        }
}
