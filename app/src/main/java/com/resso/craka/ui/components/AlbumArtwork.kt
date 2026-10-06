package com.resso.craka.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
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

    // Dynamic gradient placeholder based on songId hash
    val ambientGradient = remember(songId) {
        val hash = (songId ?: "resso").hashCode()
        val c1 = when (kotlin.math.abs(hash) % 4) {
            0 -> Color(0xFF2E1065) // Deep violet
            1 -> Color(0xFF831843) // Deep magenta
            2 -> Color(0xFF1E3A8A) // Deep blue
            else -> Color(0xFF134E4A) // Deep teal
        }
        val c2 = Color(0xFF0F0B15)
        listOf(c1, c2)
    }

    Box(
        modifier = modifier.background(Brush.radialGradient(ambientGradient)),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(250)
                    .allowHardware(false)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .setHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    if (index < candidates.lastIndex) {
                        index += 1
                    }
                }
            )
        }
    }
}
