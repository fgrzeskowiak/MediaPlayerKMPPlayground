package com.filippo.mediaplayerplayground

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filippo.mediaplayerplayground.media.MediaPlayer
import com.filippo.mediaplayerplayground.media.MediaPlayerActions
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.koin.android.annotation.KoinViewModel
import kotlin.time.Duration

@KoinViewModel
class MainViewModel(
    private val mediaPlayer: MediaPlayer,
) : ViewModel(), MediaPlayerActions by mediaPlayer {

    val state = combine(
        mediaPlayer.playback,
        mediaPlayer.currentTrack.filterNotNull(),
    ) { playback, currentTrack ->
        PlayerState(
            canPlay = !playback.isLoading,
            showPlay = !playback.isPlaying,
            hasPrevious = playback.hasPrevious,
            hasNext = playback.hasNext,
            currentProgress = playback.progress.position,
            currentTime = buildDisplayTime(playback.progress.currentTime),
            duration = buildDisplayTime(playback.duration),
            trackTitle = currentTrack.metadata.title,
            trackArtist = currentTrack.metadata.artist,
        )
    }
        .onStart { mediaPlayer.init(prepareMediaItem1(), prepareMediaItem2()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, PlayerState())

    private fun buildDisplayTime(time: Duration) = buildString {
        time.toComponents { minutes, seconds, _ ->
            append(minutes.toString().padStart(length = 2, padChar = '0'))
            append(":")
            append(seconds.toString().padStart(length = 2, padChar = '0'))
        }
    }

    private fun prepareMediaItem1() = MediaPlayer.Track(
        id = "123asdfdsf",
        uri = "https://citizen-dj.labs.loc.gov/audio/samplepacks/loc-fma/Castle-in-the-cloud_fma-174212_001_00-00-00.mp3",
        metadata = MediaPlayer.Track.Metadata(
            artist = "Ciizen DJ",
            title = "Castle in the cloud",
            albumTitle = "",
            artworkUri = ""
        )
    )

    private fun prepareMediaItem2() = MediaPlayer.Track(
        id = "123asdfdsssf",
        uri = "https://citizen-dj.labs.loc.gov/audio/samplepacks/loc-fma/Childhood-scene_fma-153138_001_00-01-18.mp3",
        metadata = MediaPlayer.Track.Metadata(
            artist = "Ciizen DJ",
            title = "Childhood scene",
            albumTitle = "",
            artworkUri = ""
        )
    )

    override fun onCleared() {
        mediaPlayer.release()
    }
}

@Immutable
data class PlayerState(
    val canPlay: Boolean = false,
    val showPlay: Boolean = false,
    val hasPrevious: Boolean = true,
    val hasNext: Boolean = false,
    val currentTime: String = "",
    val duration: String = "",
    val trackTitle: String = "",
    val trackArtist: String = "",
    val currentProgress: Float = 0f,
)