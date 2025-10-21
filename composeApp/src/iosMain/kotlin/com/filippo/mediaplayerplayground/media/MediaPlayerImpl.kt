package com.filippo.mediaplayerplayground.media

import com.filippo.mediaplayerplayground.di.MainDispatcher
import com.filippo.mediaplayerplayground.media.MediaPlayer.Playback
import com.filippo.mediaplayerplayground.media.MediaPlayer.Track
import jakarta.inject.Singleton
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerStatusReadyToPlay
import platform.AVFoundation.AVPlayerStatusUnknown
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.addBoundaryTimeObserverForTimes
import platform.AVFoundation.asset
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVFoundation.valueWithCMTime
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSURL.Companion.URLWithString
import platform.Foundation.NSValue
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.foundation.NSKeyValueObservingProtocol
import kotlin.time.Duration.Companion.seconds

@Singleton
@OptIn(ExperimentalForeignApi::class)
class MediaPlayerImpl(
    private val player: AVPlayer,
    private val audioSession: AVAudioSession,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : MediaPlayer {
    private var currentIndex = -1
    private var tracks = emptyList<Track>()

    override val playback = MutableStateFlow(Playback())
    override val currentTrack = MutableStateFlow<Track?>(null)

    private var progressJob: Job? = null
    private val scope = CoroutineScope(mainDispatcher)

    private var trackEndObserver: Any? = null

    override fun init(vararg tracks: Track) {
        player.addObserver(
            observer = playbackStateObserver,
            forKeyPath = "timeControlStatus",
            options = NSKeyValueObservingOptionNew,
            context = null
        )
        audioSession.runCatching {
            setCategory(
                category = AVAudioSessionCategoryPlayback,
                error = null,
            )
        }.onFailure { println("AudioSession setCategory error $it") }

        this.tracks = tracks.toList()
        loadTrack(0)
    }

    override fun release() {
        stop()
        player.removeObserver(playbackStateObserver, "timeControlStatus")
        progressJob?.cancel()
        progressJob = null
        scope.coroutineContext.cancelChildren()
    }

    override fun playPause() {
        if (player.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun goToPrevious() {
        if (currentIndex - 1 < 0) {
            seekToInitialTime()
        } else {
            loadTrack(currentIndex - 1)
            player.play()
        }
    }

    override fun goToNext() {
        if (currentIndex + 1 > tracks.lastIndex) {
            stop()
        } else {
            loadTrack(currentIndex + 1)
            play()
        }
    }

    private fun loadTrack(trackIndex: Int) {
        currentIndex = trackIndex
        removeCurrentItemObservers()
        val track = tracks[trackIndex]
        currentTrack.value = track
        val contentUrl = URLWithString(track.uri) ?: return
        with(AVPlayerItem(contentUrl)) {
            player.replaceCurrentItemWithPlayerItem(this)
            addObserver(
                observer = itemStatusObserver,
                forKeyPath = "status",
                options = NSKeyValueObservingOptionNew,
                context = null
            )
            asset.loadValuesAsynchronouslyForKeys(listOf("duration")) {
                registerTrackEndObserver(asset.duration)
                updateProgress()
            }
        }
    }

    private fun setSessionState(isActive: Boolean) {
        audioSession.runCatching {
            setActive(
                active = isActive,
                withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                error = null
            )
        }.onFailure {
            println("AudioSession setActive error $it")
        }
    }

    private fun registerTrackEndObserver(duration: CValue<CMTime>) {
        val endTime = CMTimeMakeWithSeconds(
            seconds = CMTimeGetSeconds(duration),
            preferredTimescale = 1
        )
        trackEndObserver = player.addBoundaryTimeObserverForTimes(
            times = listOf(NSValue.valueWithCMTime(endTime)),
            queue = dispatch_get_main_queue(),
        ) {
            goToNext()
        }
    }

    private fun play() {
        setSessionState(isActive = true)
        player.play()
    }

    private fun pause() {
        player.pause()
        setSessionState(isActive = false)
    }

    private fun stop() {
        player.pause()
        seekToInitialTime()
        setSessionState(isActive = false)
    }

    private fun seekToInitialTime() {
        player.seekToTime(
            CMTimeMakeWithSeconds(
                seconds = 0.0,
                preferredTimescale = 1,
            )
        )
    }

    private fun updateProgress() {
        val currentItem = player.currentItem ?: return
        if (currentItem.status != AVPlayerStatusReadyToPlay) return
        val duration = CMTimeGetSeconds(currentItem.duration)
        val currentTime = CMTimeGetSeconds(player.currentTime())
        playback.update {
            it.copy(
                progress = Playback.Progress(
                    position = (currentTime / duration).toFloat(),
                    currentTime = currentTime.seconds,
                ),
                duration = duration.seconds,
            )
        }
    }

    private fun removeCurrentItemObservers() {
        trackEndObserver?.let {
            player.removeTimeObserver(it)
        }
        trackEndObserver = null
        player.currentItem?.removeObserver(itemStatusObserver, "status")
    }

    private fun observeProgress() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updateProgress()
                delay(500)
            }
        }
    }

    private val itemStatusObserver: NSObject = object : NSObject(), NSKeyValueObservingProtocol {
        override fun observeValueForKeyPath(
            keyPath: String?,
            ofObject: Any?,
            change: Map<Any?, *>?,
            context: COpaquePointer?,
        ) {
            playback.update { playback ->
                playback.copy(
                    isLoading = player.currentItem?.status == AVPlayerStatusUnknown,
                )
            }
        }
    }

    private val playbackStateObserver = object : NSObject(), NSKeyValueObservingProtocol {
        override fun observeValueForKeyPath(
            keyPath: String?,
            ofObject: Any?,
            change: Map<Any?, *>?,
            context: COpaquePointer?,
        ) {
            playback.update {
                it.copy(
                    isPlaying = player.isPlaying,
                    hasPrevious = true,
                    hasNext = currentIndex + 1 <= tracks.lastIndex
                )
            }

            if (player.isPlaying) {
                observeProgress()
            } else {
                progressJob?.cancel()
            }
        }
    }

    private val AVPlayer.isPlaying
        get() = timeControlStatus == AVPlayerTimeControlStatusPlaying
}