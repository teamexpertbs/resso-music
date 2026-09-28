package com.resso.craka.ui.player

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.content.Intent
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.text.style.TextOverflow
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
            .pointerInput(currentSong?.id) {
                var dragTotal = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onVerticalDrag = { _, dragAmount -> dragTotal += dragAmount },
                    onDragEnd = {
                        when {
                            dragTotal < -90f -> viewModel.playNextSong()
                            dragTotal > 90f -> viewModel.playPreviousSong()
                        }
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f }
                )
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
        if (isVideoMode && currentSong?.id?.startsWith("yt_") == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
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
                    .height(280.dp)
                    .padding(end = 72.dp)
                    .testTag("lyrics_compact_container")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
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
                                        fontSize = if (isActive) 26.sp else 16.sp,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = when {
                                            isActive -> Color.White
                                            isPast -> Color.White.copy(alpha = 0.42f)
                                            else -> Color.White.copy(alpha = 0.28f)
                                        },
                                        lineHeight = if (isActive) 32.sp else 22.sp
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
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Column(modifier = Modifier.weight(1f)) {
                     Text(
                         text = "resso",
                         fontSize = 20.sp,
                         fontWeight = FontWeight.ExtraBold,
                         color = Color.White,
                         letterSpacing = (-0.4).sp,
                         maxLines = 1
                     )
                    Text(
                         text = "${currentSong?.mood ?: "Chill"} · ${currentSong?.genre ?: "Pop"}",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Compact controls keep the header usable on narrow Android screens.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSong?.id?.startsWith("yt_") == true) {
                    PlayerTopActionButton(
                        icon = Icons.Default.MusicVideo,
                        contentDescription = if (isVideoMode) "Hide music video" else "Show music video",
                        selected = isVideoMode,
                        tag = "toggle_mv_mode_button",
                        onClick = { viewModel.toggleVideoMode() }
                    )
                }

                PlayerTopActionButton(
                    icon = Icons.Default.FormatQuote,
                    contentDescription = if (isLyricsVisible) "Hide lyrics" else "Show lyrics",
                    selected = isLyricsVisible,
                    tag = "toggle_lyrics_chip",
                    onClick = { viewModel.toggleLyrics() }
                )
                PlayerTopActionButton(
                    icon = Icons.Default.Search,
                    contentDescription = "Search songs",
                    selected = false,
                    tint = RessoSecondary,
                    tag = "top_search_songs_chip",
                    onClick = onNavigateToSearch
                )
            }
        }

        // 4. Right Side Action Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 196.dp),
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

            PlayerActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                tint = Color.White,
                tag = "player_share_button",
                onClick = {
                    val title = currentSong?.title ?: "a song"
                    val artist = currentSong?.artist ?: "Resso"
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Listening to $title by $artist on Resso")
                    }
                    context.startActivity(Intent.createChooser(share, "Share"))
                }
            )

            PlayerActionButton(
                icon = Icons.Default.AutoAwesome,
                label = "Vibe",
                tint = Color.White,
                tag = "player_lyric_poster_button",
                onClick = onOpenVibeCreator
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
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentSong?.artist ?: "Resso Artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = RessoTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                    .height(32.dp)
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
                        .size(48.dp)
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

@Composable
private fun PlayerTopActionButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (selected) RessoPrimary else Color.Black.copy(alpha = 0.42f))
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}
