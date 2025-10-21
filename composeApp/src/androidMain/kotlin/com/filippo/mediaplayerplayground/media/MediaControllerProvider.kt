package com.filippo.mediaplayerplayground.media

import androidx.media3.session.MediaController
import com.google.common.util.concurrent.MoreExecutors
import jakarta.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MediaControllerProvider @Inject constructor(
    mediaControllerBuilder: MediaController.Builder,
) {
    private val future = mediaControllerBuilder.buildAsync()

    suspend fun provide(): MediaController = suspendCancellableCoroutine { continuation ->
        future.addListener(
            { continuation.resume(future.get()) },
            MoreExecutors.directExecutor()
        )
        continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
    }
}