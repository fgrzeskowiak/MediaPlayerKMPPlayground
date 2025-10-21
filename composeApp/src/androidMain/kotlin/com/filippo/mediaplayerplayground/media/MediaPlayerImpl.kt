package com.filippo.mediaplayerplayground.media

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.listen
import com.filippo.mediaplayerplayground.di.MainDispatcher
import com.filippo.mediaplayerplayground.media.MediaPlayer.Playback
import com.filippo.mediaplayerplayground.media.MediaPlayer.Track
import com.filippo.mediaplayerplayground.media.mappers.toMediaItem
import com.filippo.mediaplayerplayground.media.mappers.toTrack
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import androidx.media3.common.util.Util as MediaUtils

@Singleton
class MediaPlayerImpl(
    private val mediaControllerProvider: MediaControllerProvider,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : MediaPlayer {
    private var player: Player? = null

    override val currentTrack = MutableStateFlow<Track?>(null)
    override val playback = MutableStateFlow(Playback())

    private val scope = CoroutineScope(mainDispatcher)
    private var progressJob: Job? = null

    override fun init(vararg tracks: Track) {
        scope.launch {
            player = mediaControllerProvider.provide()
            player?.setMediaItems(tracks.map { it.toMediaItem() })
            player?.listen { events ->
                if (Player.EVENT_AVAILABLE_COMMANDS_CHANGED in events) updateAvailableCommands()
                if (Player.EVENT_IS_PLAYING_CHANGED in events) updateIsPlaying()
                if (Player.EVENT_MEDIA_METADATA_CHANGED in events) updateCurrentTrack()
                if (Player.EVENT_TIMELINE_CHANGED in events) updateDuration()
                if (Player.EVENT_PLAYBACK_STATE_CHANGED in events) updateProgress()
            }
        }
    }

    override fun playPause() {
        player?.let(MediaUtils::handlePlayPauseButtonAction)
    }

    override fun goToPrevious() {
        player?.seekToPrevious()
    }

    override fun goToNext() {
        player?.seekToNext()
    }

    override fun release() {
        progressJob?.cancel()
        progressJob = null
        scope.coroutineContext.cancelChildren()
        player?.release()
        player = null
    }

    private fun Player.observeProgress() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updateProgress()
                delay(500)
            }
        }
    }

    private fun Player.updateProgress() {
        if (isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) {
            playback.update { playback ->
                playback.copy(
                    progress = Playback.Progress(
                        position = currentPosition / duration.toFloat(),
                        currentTime = currentPosition.milliseconds,
                    )
                )
            }
        }
    }

    private fun Player.updateDuration() {
        if (isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) {
            playback.update {
                it.copy(
                    duration = if (duration == C.TIME_UNSET) {
                        0.seconds
                    } else {
                        duration.milliseconds
                    }
                )
            }
        }
    }

    private fun Player.updateCurrentTrack() {
        if (
            isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM) &&
            isCommandAvailable(Player.COMMAND_GET_METADATA)
        ) {
            currentTrack.value = currentMediaItem?.toTrack()
        }
    }

    private fun Player.updateIsPlaying() {
        playback.update { it.copy(isPlaying = isPlaying) }
        if (isPlaying) {
            observeProgress()
        } else {
            progressJob?.cancel()
        }
    }

    private fun Player.updateAvailableCommands() {
        playback.update {
            it.copy(
                hasNext = isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT),
                hasPrevious = isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS)
            )
        }
    }
}