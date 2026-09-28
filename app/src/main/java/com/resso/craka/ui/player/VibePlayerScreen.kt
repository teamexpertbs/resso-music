package com.resso.craka.ui.player

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.ui.components.VideoBackground
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoGreen
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoSecondary
import com.resso.craka.ui.theme.RessoTertiary
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel
import com.resso.craka.util.LyricsParser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibePlayerScreen(
    viewModel: MusicViewModel,
    onOpenVibeCreator: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onOpenSidebar: () -> Unit = {},
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
    val networkMessage by viewModel.networkStatusMessage.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(networkMessage) {
        networkMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearNetworkStatusMessage()
        }
    }

    // Double tap heart animation
    var showBigHeart by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }
    val isVideoMode by viewModel.isVideoMode.collectAsState()
    val isLyricsVisible by viewModel.isLyricsVisible.collectAsState()
    val isFlashSyncEnabled by viewModel.isFlashSyncEnabled.collectAsState()

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

    // Auto-scroll lyrics smoothly to active index when lyrics are open
    LaunchedEffect(activeLyricIndex, isLyricsVisible) {
        if (isLyricsVisible && lyrics.isNotEmpty() && activeLyricIndex in lyrics.indices) {
            try {
                listState.animateScrollToItem(
                    index = (activeLyricIndex - 1).coerceAtLeast(0)
                )
            } catch (_: Exception) {
            }
        }
    }

    // Main full screen container with swipe up/down gesture support
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isVideoMode && currentSong?.id?.startsWith("yt_") == true) Color.Transparent else Color.Black)
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
        // 1. Dynamic Video / Ambient Vibe Background or Official MV Scrim
        if (currentSong?.id?.startsWith("yt_") == true) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = {
                        val wv = viewModel.streamPlayerManager.getWebView()
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv
                    },
                    modifier = if (isVideoMode) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.size(1.dp).background(Color.Transparent)
                    }
                )
                if (isVideoMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )
                } else {
                    VideoBackground(
                        videoUri = currentVibeUri,
                        filterType = currentVibeFilter,
                        isPlaying = isPlaying
                    )
                }
            }
        } else {
            VideoBackground(
                videoUri = currentVibeUri,
                filterType = currentVibeFilter,
                isPlaying = isPlaying
            )
        }

        // 2. Synced Scrolling Lyrics Layer (Compact, Floating & Toggleable - NOT Full Screen)
        AnimatedVisibility(
            visible = isLyricsVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xE6140D1F))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("lyrics_compact_container")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = RessoSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYNCED LYRICS",
                                color = RessoSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setLyricsVisible(false) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("close_lyrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Lyrics",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (lyrics.isNotEmpty()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(lyrics) { index, lyric ->
                                val isActive = index == activeLyricIndex
                                val isPast = index < activeLyricIndex

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.seekTo(lyric.timeMs) }
                                        .testTag("lyric_line_$index")
                                ) {
                                    Text(
                                        text = lyric.text,
                                        fontSize = if (isActive) 17.sp else 14.sp,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = when {
                                            isActive -> Color.White
                                            isPast -> Color.White.copy(alpha = 0.5f)
                                            else -> Color.White.copy(alpha = 0.35f)
                                        },
                                        lineHeight = if (isActive) 24.sp else 20.sp
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No synced lyrics available for this track",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Top Header: Sidebar Menu, Song Info / Mood Tag & Quick Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenSidebar,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("open_sidebar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Sidebar Menu",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                     Text(
                         text = "resso",
                         fontSize = 20.sp,
                         fontWeight = FontWeight.ExtraBold,
                         color = Color.White,
                         letterSpacing = 1.2.sp
                     )
                    Text(
                         text = "${currentSong?.mood ?: "Chill"} · ${currentSong?.genre ?: "Pop"}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Top Quick Actions: Watch MV, Search Songs & Upload Device Song
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSong?.id?.startsWith("yt_") == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isVideoMode) Color(0xFFE91E63) else Color.Black.copy(alpha = 0.5f))
                            .clickable { viewModel.toggleVideoMode() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("toggle_mv_mode_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicVideo,
                                contentDescription = "Toggle Video",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                             text = if (isVideoMode) "MV" else "Video",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Lyrics toggle chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isLyricsVisible) RessoPrimary else Color.Black.copy(alpha = 0.5f))
                        .clickable { viewModel.toggleLyrics() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("toggle_lyrics_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Toggle Lyrics",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                             text = "Lyrics",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
            }
        }

        // 4. Right Side Action Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // Lyrics Toggle Button
            PlayerActionButton(
                icon = Icons.Default.FormatQuote,
                label = if (isLyricsVisible) "Lyrics ON" else "Lyrics",
                tint = if (isLyricsVisible) RessoPrimary else Color.White,
                tag = "player_lyrics_toggle_button",
                onClick = { viewModel.toggleLyrics() }
            )

            // Lyric Poster Button
            PlayerActionButton(
                icon = Icons.Default.AutoAwesome,
                label = "Poster",
                tint = RessoTertiary,
                tag = "player_lyric_poster_button",
                onClick = {
                    viewModel.openLyricPosterDialog(null)
                }
            )

            // Flash Torch Beat Sync Button (Real-time physical back flashlight)
            PlayerActionButton(
                icon = Icons.Default.FlashOn,
                label = if (isFlashSyncEnabled) "Torch ON" else "Torch",
                tint = if (isFlashSyncEnabled) Color(0xFFFFD700) else Color.White,
                tag = "player_torch_sync_button",
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.toggleFlashSync()
                        val state = if (!isFlashSyncEnabled) "ON 🔦 (Song beat par back torch chalegi)" else "OFF"
                        Toast.makeText(context, "Back Torch Flash: $state", Toast.LENGTH_SHORT).show()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )

            // Sidebar Tools Button (Quick drawer opener)
            PlayerActionButton(
                icon = Icons.Default.Tune,
                label = "Tools",
                tint = RessoSecondary,
                tag = "player_tools_sidebar_button",
                onClick = onOpenSidebar
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 10.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentSong?.artist ?: "Resso Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = RessoTextSecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (currentSong?.id?.startsWith("yt_") == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• HD Stream",
                                color = Color(0xFFFF4D4D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Swipe hints
                Text(
                    text = "Swipe to switch",
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
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
