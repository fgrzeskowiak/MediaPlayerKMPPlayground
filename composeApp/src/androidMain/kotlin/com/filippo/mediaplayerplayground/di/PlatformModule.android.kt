package com.filippo.mediaplayerplayground.di

import android.content.ComponentName
import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.filippo.mediaplayerplayground.media.MediaControllerProvider
import com.filippo.mediaplayerplayground.media.MediaPlayerImpl
import com.filippo.mediaplayerplayground.media.PlaybackService
import jakarta.inject.Singleton
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@Configuration
@ComponentScan("com.filippo.mediaplayerplayground")
actual class PlatformModule {

    @Factory
    fun exoplayer(context: Context) = ExoPlayer.Builder(context)
        .setHandleAudioBecomingNoisy(true)
        .build()

    @Singleton
    fun sessionToken(context: Context) = SessionToken(
        context,
        ComponentName(
            context,
            PlaybackService::class.java
        )
    )

    @Singleton
    fun mediaControllerBuilder(context: Context, sessionToken: SessionToken) =
        MediaController.Builder(context, sessionToken)

    @Factory
    fun mediaSession(context: Context, player: ExoPlayer) =
        MediaSession.Builder(context, player).build()
}