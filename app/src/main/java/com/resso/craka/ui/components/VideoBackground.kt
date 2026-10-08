package com.resso.craka.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoBackground(
    videoUri: String?,
    filterType: String = "Neon",
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (!videoUri.isNullOrBlank()) {
            val videoPlayer = remember(videoUri) {
                val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
                    .setEnableDecoderFallback(true)
                    .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                ExoPlayer.Builder(context, renderersFactory).build().apply {
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = 0f // Muted background video for Vibe
                    try {
                        setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
                        prepare()
                        playWhenReady = true
                    } catch (e: Exception) {
                    }
                }
            }

            DisposableEffect(videoPlayer) {
                onDispose {
                    videoPlayer.release()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = videoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Generative Ambient Animated Visualizer background (Resso Aura)
            AmbientVibeCanvas(filterType = filterType, isPlaying = isPlaying)
        }

        // Color filter layer based on chosen vibe style
        val filterColor = when (filterType) {
            "Cyberpunk" -> Color(0xFF00E5FF).copy(alpha = 0.25f)
            "Dreamy" -> Color(0xFFE056FD).copy(alpha = 0.25f)
            "Retro VHS" -> Color(0xFFFF9F1A).copy(alpha = 0.20f)
            "B&W Noir" -> Color(0xFF333333).copy(alpha = 0.40f)
            else -> Color(0xFFFF2D3A).copy(alpha = 0.22f)
        }
        Box(modifier = Modifier.fillMaxSize().background(filterColor))

        // Dark gradient vignette overlay for optimal lyric contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.90f)
                        )
                    )
                )
        )
    }
}

@Composable
fun AmbientVibeCanvas(filterType: String, isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "vibe_anim")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isPlaying) 1.25f else 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 1800 else 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 12000 else 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val (primaryAura, secondaryAura, tertiaryAura) = when (filterType) {
        "Cyberpunk" -> Triple(Color(0xFF00F5D4), Color(0xFFFFE600), Color(0xFF7928CA))
        "Dreamy" -> Triple(Color(0xFFFF9A9E), Color(0xFFFECFEF), Color(0xFFA1C4FD))
        "Retro VHS" -> Triple(Color(0xFFFF5722), Color(0xFFFFC107), Color(0xFF3E2723))
        "B&W Noir" -> Triple(Color(0xFF757575), Color(0xFF424242), Color(0xFF212121))
        else -> Triple(Color(0xFFFF2D3A), Color(0xFFFF6B76), Color(0xFF3A0A10))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Orb 1 (top right)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryAura.copy(alpha = 0.7f), Color.Transparent),
                center = Offset(w * 0.75f, h * 0.25f),
                radius = (w * 0.6f) * pulse
            ),
            center = Offset(w * 0.75f, h * 0.25f),
            radius = (w * 0.6f) * pulse
        )

        // Orb 2 (center bottom)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryAura.copy(alpha = 0.6f), Color.Transparent),
                center = Offset(w * 0.3f, h * 0.65f),
                radius = (w * 0.7f) * pulse
            ),
            center = Offset(w * 0.3f, h * 0.65f),
            radius = (w * 0.7f) * pulse
        )

        // Orb 3 (mid screen accent)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryAura.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(w * 0.85f, h * 0.8f),
                radius = (w * 0.5f) * (2f - pulse)
            ),
            center = Offset(w * 0.85f, h * 0.8f),
            radius = (w * 0.5f) * (2f - pulse)
        )
    }
}
