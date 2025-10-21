package com.filippo.mediaplayerplayground.media

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MediaPlayerActions {
    fun playPause()
    fun goToPrevious()
    fun goToNext()
}

interface MediaPlayer: MediaPlayerActions {
    val playback: StateFlow<Playback>
    val currentTrack: StateFlow<Track?>

    fun init(vararg tracks: Track)
    fun release()

    data class Playback(
        val isLoading: Boolean = false,
        val isPlaying: Boolean = false,
        val duration: Duration = 0.seconds,
        val progress: Progress = Progress(),
        val hasNext: Boolean = false,
        val hasPrevious: Boolean = false,
    ) {
        data class Progress(
            val position: Float = 0f,
            val currentTime: Duration = 0.seconds,
        )
    }

    data class Track(
        val id: String,
        val uri: String,
        val metadata: Metadata,
    ) {
        data class Metadata(
            val title: String,
            val artist: String,
            val albumTitle: String,
            val artworkUri: String,
        )
    }
}