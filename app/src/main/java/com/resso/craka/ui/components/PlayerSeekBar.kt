package com.resso.craka.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.util.LyricsParser

/**
 * Material 3 Seek Bar (Slider) component for audio playback.
 * Reflects current song progress, displays live scrubbed timestamps,
 * and allows users to manually skip to different parts of the audio track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDuration = durationMs.coerceAtLeast(1000L)
    var isUserSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }

    val currentProgress = if (isUserSeeking) {
        seekProgress
    } else {
        (currentPositionMs.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    }

    val displayedPositionMs = if (isUserSeeking) {
        (seekProgress * totalDuration).toLong().coerceIn(0L, totalDuration)
    } else {
        currentPositionMs.coerceIn(0L, totalDuration)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val thumbSize by animateDpAsState(
        targetValue = if (isDragged || isPressed || isUserSeeking) 16.dp else 12.dp,
        label = "thumb_size"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("player_seek_bar_container")
    ) {
        // Material 3 Slider with live seeking
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = currentProgress,
                onValueChange = { newProgress ->
                    isUserSeeking = true
                    seekProgress = newProgress
                },
                onValueChangeFinished = {
                    val targetMs = (seekProgress * totalDuration).toLong().coerceIn(0L, totalDuration)
                    onSeekTo(targetMs)
                    isUserSeeking = false
                },
                interactionSource = interactionSource,
                thumb = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = if (isUserSeeking) 6.dp else 2.dp,
                        modifier = Modifier
                            .size(thumbSize)
                            .testTag("seek_bar_thumb")
                    ) {}
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        colors = SliderDefaults.colors(
                            activeTrackColor = RessoPrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier.height(4.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_progress_slider")
            )
        }

        // Live Timestamps (Current / Scrubbed vs Total)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LyricsParser.formatTime(displayedPositionMs),
                color = if (isUserSeeking) RessoPrimary else RessoTextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isUserSeeking) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.testTag("seek_current_time")
            )

            Text(
                text = LyricsParser.formatTime(totalDuration),
                color = RessoTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.testTag("seek_total_duration")
            )
        }
    }
}
