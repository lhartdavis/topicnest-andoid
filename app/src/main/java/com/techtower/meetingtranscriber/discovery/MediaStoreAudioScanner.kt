package com.techtower.meetingtranscriber.discovery

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.techtower.meetingtranscriber.data.entities.AudioSource
import com.techtower.meetingtranscriber.util.isSupportedAudioName

class MediaStoreAudioScanner(private val context: Context) {
    fun scan(): List<DiscoveredAudioFile> {
        val resolver = context.contentResolver
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.MIME_TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()

        return resolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val relativePathColumn =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    -1
                }
            @Suppress("DEPRECATION")
            val dataColumn =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                } else {
                    -1
                }

            buildList {
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(nameColumn) ?: continue
                    val relativePath = cursor.nullableString(relativePathColumn)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (!isInTargetRecorderPath(relativePath)) continue
                    } else {
                        val path = cursor.nullableString(dataColumn).orEmpty()
                        if (!path.contains("/Music/Record/SoundRecord/")) continue
                    }
                    if (!isSupportedAudioName(displayName)) continue

                    val mediaId = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, mediaId)
                    add(
                        DiscoveredAudioFile(
                            id = "mediastore:$mediaId",
                            uri = uri,
                            displayName = displayName,
                            sizeBytes = cursor.getLong(sizeColumn).coerceAtLeast(0),
                            durationMillis = cursor.nullableLong(durationColumn),
                            modifiedEpochMillis = cursor.nullableLong(modifiedColumn)?.times(1_000L),
                            mimeType = cursor.nullableString(mimeColumn),
                            relativePath = relativePath,
                            source = AudioSource.MEDIASTORE,
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    private fun android.database.Cursor.nullableLong(column: Int): Long? =
        if (column < 0 || isNull(column)) null else getLong(column)

    private fun android.database.Cursor.nullableString(column: Int): String? =
        if (column < 0 || isNull(column)) null else getString(column)
}
