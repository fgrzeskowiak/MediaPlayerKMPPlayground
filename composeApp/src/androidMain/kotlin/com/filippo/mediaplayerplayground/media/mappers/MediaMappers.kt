package com.filippo.mediaplayerplayground.media.mappers

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.filippo.mediaplayerplayground.media.MediaPlayer.Track

fun Track.toMediaItem() = MediaItem.Builder()
    .setMediaMetadata(metadata.toMediaMetadata())
    .setUri(uri)
    .build()

fun MediaItem.toTrack(): Track = Track(
    id = mediaId,
    uri = localConfiguration?.uri?.toString().orEmpty(),
    metadata = mediaMetadata.toTrackMetadata(),
)

private fun MediaMetadata.toTrackMetadata(): Track.Metadata = Track.Metadata(
    title = title?.toString().orEmpty(),
    artist = artist?.toString().orEmpty(),
    albumTitle = albumTitle?.toString().orEmpty(),
    artworkUri = artworkUri?.toString().orEmpty()
)

private fun Track.Metadata.toMediaMetadata(): MediaMetadata = MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(artist)
    .build()
