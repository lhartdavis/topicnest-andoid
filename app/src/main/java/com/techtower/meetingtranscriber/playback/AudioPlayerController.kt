package com.techtower.meetingtranscriber.playback

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

class AudioPlayerController(
    context: Context,
    audioUri: Uri,
) {
    private val player: MediaPlayer? = MediaPlayer.create(context, audioUri)

    var isPlaying by mutableStateOf(false)
        private set
    var positionMillis by mutableLongStateOf(0L)
        private set
    var durationMillis by mutableLongStateOf(player?.duration?.toLong()?.coerceAtLeast(0L) ?: 0L)
        private set

    init {
        player?.setOnCompletionListener {
            isPlaying = false
            refreshPosition()
        }
    }

    fun togglePlayPause() {
        val mediaPlayer = player ?: return
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        } else {
            mediaPlayer.start()
            isPlaying = true
        }
        refreshPosition()
    }

    fun seekTo(positionMillis: Long) {
        player?.seekTo(positionMillis.toInt())
        refreshPosition()
    }

    fun refreshPosition() {
        val mediaPlayer = player ?: return
        positionMillis = mediaPlayer.currentPosition.toLong().coerceAtLeast(0L)
        durationMillis = mediaPlayer.duration.toLong().coerceAtLeast(0L)
        isPlaying = mediaPlayer.isPlaying
    }

    fun release() {
        player?.release()
        isPlaying = false
    }
}

@Composable
fun rememberAudioPlayerController(audioUri: String): AudioPlayerController {
    val context = LocalContext.current.applicationContext
    val controller = remember(audioUri) {
        AudioPlayerController(context, Uri.parse(audioUri))
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    LaunchedEffect(controller, controller.isPlaying) {
        while (true) {
            controller.refreshPosition()
            delay(if (controller.isPlaying) 250L else 1_000L)
        }
    }
    return controller
}
