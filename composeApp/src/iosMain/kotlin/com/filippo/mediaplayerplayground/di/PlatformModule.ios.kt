package com.filippo.mediaplayerplayground.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import platform.AVFAudio.AVAudioSession
import platform.AVFoundation.AVPlayer

@Module
@Configuration
@ComponentScan("com.filippo.mediaplayerplayground")
actual class PlatformModule {

    @Factory
    fun player() = AVPlayer()

    @Factory
    fun audioSession() = AVAudioSession.sharedInstance()
}