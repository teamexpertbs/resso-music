package com.example.ui.player

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VideoBackground
import com.example.ui.theme.RessoCardBg
import com.example.ui.theme.RessoGreen
import com.example.ui.theme.RessoPrimary
import com.example.ui.theme.RessoSecondary
import com.example.ui.theme.RessoTertiary
import com.example.ui.theme.RessoTextSecondary
import com.example.ui.viewmodel.MusicViewModel
import com.example.util.LyricsParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibePlayerScreen(
    viewModel: MusicViewModel,
    onOpenVibeCreator: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val activeLyricIndex by viewModel.activeLyricIndex.collectAsState()
    val currentPos by viewModel.currentPositionMs.collectAsState()
    val duration by viewModel.durationMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentVibeUri by viewModel.currentVibeUri.collectAsState()
    val currentVibeFilter by viewModel.currentVibeFilter.collectAsState()
    val isFlashSync by viewModel.isFlashSyncEnabled.collectAsState()
    val isVolumeBooster by viewModel.isVolumeBoosterEnabled.collectAsState()
    val comments by viewModel.commentsForCurrentSong.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Double tap heart animation
    var showBigHeart by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }

    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "My Track"
            viewModel.addLocalSong(uri, fileName, "Local Artist")
            Toast.makeText(context, "Playing: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission launcher for back flashlight / beat sync
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.toggleFlashSync()
            Toast.makeText(
                context,
                "⚡ Beat Flash: ON (Song beat par flashlight chalegi)",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Flashlight (Torch) ke liye Camera permission zaroori hai",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Auto-scroll lyrics smoothly to active index
    LaunchedEffect(activeLyricIndex) {
        if (lyrics.isNotEmpty() && activeLyricIndex in lyrics.indices) {
            try {
                listState.animateScrollToItem(
                    index = activeLyricIndex,
                    scrollOffset = -180
                )
            } catch (_: Exception) {
            }
        }
    }

    // Main full screen container with swipe up/down gesture support
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -45) {
                        viewModel.playNextSong()
                    } else if (dragAmount > 45) {
                        viewModel.playPreviousSong()
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        viewModel.toggleLikeCurrentSong()
                        scope.launch {
                            showBigHeart = true
                            heartScale.snapTo(0.2f)
                            heartScale.animateTo(
                                targetValue = 1.3f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                            heartScale.animateTo(0f)
                            showBigHeart = false
                        }
                    }
                )
            }
            .testTag("vibe_player_screen")
    ) {
        // 1. Dynamic Video / Ambient Vibe Background
        VideoBackground(
            videoUri = currentVibeUri,
            filterType = currentVibeFilter,
            isPlaying = isPlaying
        )

        // 2. Synced Scrolling Lyrics Layer
        if (lyrics.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 76.dp), // Leave space for right sidebar
                contentPadding = PaddingValues(top = 220.dp, bottom = 260.dp, start = 20.dp, end = 12.dp)
            ) {
                itemsIndexed(lyrics) { index, lyric ->
                    val isActive = index == activeLyricIndex
                    val isPast = index < activeLyricIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.seekTo(lyric.timeMs)
                            }
                            .testTag("lyric_line_$index")
                    ) {
                        Text(
                            text = lyric.text,
                            fontSize = if (isActive) 26.sp else if (isPast) 19.sp else 21.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = when {
                                isActive -> Color.White
                                isPast -> Color.White.copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.35f)
                            },
                            lineHeight = if (isActive) 34.sp else 28.sp
                        )
                    }
                }
            }
        } else {
            // No lyrics available state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enjoy the Vibe ♪",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 3. Top Header: Song Info / Mood Tag
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIBE STREAM",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = RessoSecondary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "${currentSong?.mood ?: "Chill"} • ${currentSong?.genre ?: "Pop"}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Top Quick Actions: Search Songs & Upload Device Song
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onNavigateToSearch() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("top_search_songs_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = RessoSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Search", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { audioPickerLauncher.launch("audio/*") }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("import_audio_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Upload MP3",
                            tint = RessoPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Play MP3", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Right Side Action Bar (Resso signature vertical action strip)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Heart / Like Button
            PlayerActionButton(
                icon = if (currentSong?.isLiked == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (currentSong?.isLiked == true) "Liked" else "Like",
                tint = if (currentSong?.isLiked == true) RessoPrimary else Color.White,
                tag = "player_like_button",
                onClick = { viewModel.toggleLikeCurrentSong() }
            )

            // Comments Button with Badge
            Box(contentAlignment = Alignment.TopEnd) {
                PlayerActionButton(
                    icon = Icons.Default.ChatBubble,
                    label = "${comments.size}",
                    tint = Color.White,
                    tag = "player_comments_button",
                    onClick = { viewModel.setCommentsSheetOpen(true) }
                )
            }

            // Vibe Creator Button
            PlayerActionButton(
                icon = Icons.Default.AutoAwesome,
                label = "Vibe",
                tint = RessoSecondary,
                tag = "player_vibe_creator_button",
                onClick = onOpenVibeCreator
            )

            // Lyric Poster Button
            PlayerActionButton(
                icon = Icons.Default.FormatQuote,
                label = "Poster",
                tint = RessoTertiary,
                tag = "player_lyric_poster_button",
                onClick = {
                    viewModel.openLyricPosterDialog(null)
                }
            )

            // Flash Sync Toggle Button (Back Torch Flashes to Song Beats)
            PlayerActionButton(
                icon = Icons.Default.FlashOn,
                label = if (isFlashSync) "Flash On" else "Flash",
                tint = if (isFlashSync) RessoGreen else Color.White.copy(alpha = 0.6f),
                tag = "player_flash_sync_button",
                onClick = {
                    val hasCam = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasCam) {
                        viewModel.toggleFlashSync()
                        Toast.makeText(
                            context,
                            if (!isFlashSync) "⚡ Beat Flash: ON (Song beat par flashlight chalegi)" else "⚡ Beat Flash: OFF (Torch band)",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )

            // Volume Booster Toggle Button
            PlayerActionButton(
                icon = Icons.Default.VolumeUp,
                label = if (isVolumeBooster) "+150%" else "Boost",
                tint = if (isVolumeBooster) RessoPrimary else Color.White.copy(alpha = 0.6f),
                tag = "player_volume_booster_button",
                onClick = {
                    viewModel.toggleVolumeBooster()
                    Toast.makeText(
                        context,
                        if (!isVolumeBooster) "Volume Boost: +150% Active" else "Volume Boost: Normal",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        // 5. Double Tap Animated Heart Overlay
        if (showBigHeart) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = RessoPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(110.dp)
                    .scale(heartScale.value)
            )
        }

        // 6. Bottom Playback Controls Strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Track Info & Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "Select a Track",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = currentSong?.artist ?: "Resso Artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = RessoTextSecondary,
                        maxLines = 1
                    )
                }

                // Swipe hints
                Text(
                    text = "Swipe ↑↓ for songs",
                    fontSize = 10.sp,
                    color = RessoTextSecondary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Seekable Progress Bar
            var isUserSeeking by remember { mutableStateOf(false) }
            var seekValue by remember { mutableFloatStateOf(0f) }

            val progress = if (isUserSeeking) seekValue else {
                if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
            }

            Slider(
                value = progress,
                onValueChange = {
                    isUserSeeking = true
                    seekValue = it
                },
                onValueChangeFinished = {
                    val targetMs = (seekValue * duration).toLong()
                    viewModel.seekTo(targetMs)
                    isUserSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = RessoPrimary,
                    activeTrackColor = RessoPrimary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("playback_progress_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = LyricsParser.formatTime(currentPos),
                    color = RessoTextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = LyricsParser.formatTime(duration),
                    color = RessoTextSecondary,
                    fontSize = 11.sp
                )
            }

            // Buttons: Shuffle, Prev, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("toggle_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) RessoSecondary else Color.White.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = { viewModel.playPreviousSong() },
                    modifier = Modifier.testTag("play_previous_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Song",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause FAB
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(RessoPrimary)
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.playNextSong() },
                    modifier = Modifier.testTag("play_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Song",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleRepeat() },
                    modifier = Modifier.testTag("toggle_repeat_button")
                ) {
                    Icon(
                        imageVector = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) RessoSecondary else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}
