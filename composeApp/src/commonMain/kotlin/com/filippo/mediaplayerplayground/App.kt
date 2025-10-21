package com.filippo.mediaplayerplayground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filippo.mediaplayerplayground.di.KoinApp
import org.koin.compose.KoinMultiplatformApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration
import org.koin.ksp.generated.koinConfiguration

@Composable
@OptIn(KoinExperimentalAPI::class)
fun App() {
    MaterialTheme {
        KoinMultiplatformApplication(koinConfiguration(KoinApp.koinConfiguration())) {
            val viewModel = koinViewModel<MainViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlayerScreen(
                state = state,
                onPlayPauseClick = viewModel::playPause,
                onPreviousClick = viewModel::goToPrevious,
                onNextClick = viewModel::goToNext,
            )
        }
    }
}

@Composable
private fun PlayerScreen(
    state: PlayerState,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    val currentProgress by rememberUpdatedState(state.currentProgress)
    Surface(
        color = Color.DarkGray,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(state.trackTitle, color = Color.White)
            Text(state.trackArtist, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PreviousButton(
                    isEnabled = state.hasPrevious,
                    onClick = onPreviousClick
                )
                PlayPauseButton(
                    isEnabled = state.canPlay,
                    showPlay = state.showPlay,
                    onClick = onPlayPauseClick
                )
                NextButton(
                    isEnabled = state.hasNext,
                    onClick = onNextClick,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(state.currentTime, color = Color.White)
                Text(state.duration, color = Color.White)
            }
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { currentProgress },
            )
        }
    }
}

@Composable
private fun PreviousButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        enabled = isEnabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White,
        ),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.SkipPrevious,
            contentDescription = "Previous"
        )
    }
}

@Composable
private fun PlayPauseButton(
    isEnabled: Boolean,
    showPlay: Boolean,
    onClick: () -> Unit,
) {

    IconButton(
        enabled = isEnabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White,
        ),
        onClick = onClick,
    ) {
        Icon(
            imageVector = if (showPlay) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = if (showPlay) "Play" else "Pause"
        )
    }
}

@Composable
private fun NextButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        enabled = isEnabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White,
        ),
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next"
        )
    }
}