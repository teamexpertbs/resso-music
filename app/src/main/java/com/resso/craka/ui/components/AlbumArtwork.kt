package com.resso.craka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.resso.craka.util.ArtworkUrls

@Composable
fun AlbumArtwork(
    songId: String?,
    albumArtUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val candidates = remember(songId, albumArtUrl) {
        ArtworkUrls.candidates(songId, albumArtUrl)
    }
    var index by remember(candidates) { mutableIntStateOf(0) }
    val url = candidates.getOrNull(index)

    Box(
        modifier = modifier.background(Color(0xFF1C1C1E)),
        contentAlignment = Alignment.Center
    ) {
        if (url == null) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.7f)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(180)
                    .allowHardware(false)
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    if (index < candidates.lastIndex) index += 1
                }
            )
        }
    }
}
