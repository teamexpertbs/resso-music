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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.player.AppEqualizer
import com.resso.craka.ui.components.AlbumArtwork
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
    onOpenArtist: (String) -> Unit = {},
    onOpenSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val lyricsStatus by viewModel.lyricsStatus.collectAsState()
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
        val showMusicVideo = isVideoMode && currentSong?.id?.startsWith("yt_") == true
        if (showMusicVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.28f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
            )
        } else if (!currentVibeUri.isNullOrBlank()) {
            VideoBackground(
                videoUri = currentVibeUri,
                filterType = currentVibeFilter,
                isPlaying = isPlaying
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenSidebar,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("open_sidebar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open menu",
                        tint = Color.White
                    )
                }
                Text(
                    text = "resso",
                    modifier = Modifier.weight(1f),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.8).sp,
                    textAlign = TextAlign.Center
                )
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
                    icon = Icons.Default.Search,
                    contentDescription = "Search songs",
                    selected = false,
                    tint = Color.White,
                    tag = "top_search_songs_chip",
                    onClick = onNavigateToSearch
                )
            }

            if (!showMusicVideo && !isLyricsVisible) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AlbumArtwork(
                        songId = currentSong?.id,
                        albumArtUrl = currentSong?.albumArtUrl,
                        contentDescription = currentSong?.title ?: "Song cover",
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("player_album_art")
                    )
                }
                Text(
                    text = currentSong?.title ?: "Pick a song",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = currentSong?.artist ?: "",
                    color = RessoTextSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable {
                            val artist = currentSong?.artist?.substringBefore(",")?.trim().orEmpty()
                            if (artist.isNotBlank()) onOpenArtist(artist)
                        }
                )
                PlayerToolRow(viewModel)
                val lyricLine = lyrics.getOrNull(activeLyricIndex)?.text
                    ?: lyricsStatus.ifBlank { "Lyrics" }
                Text(
                    text = lyricLine,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, start = 12.dp, end = 12.dp)
                        .clickable { viewModel.toggleLyrics() }
                        .testTag("toggle_lyrics_chip")
                )
            } else if (isLyricsVisible && !showMusicVideo) {
                Text(
                    text = currentSong?.title ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp, end = 56.dp)
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 56.dp)
                        .testTag("lyrics_compact_container")
                ) {
                    if (lyrics.isEmpty()) {
                        item {
                            Text(
                                text = lyricsStatus.ifBlank { "Lyrics not available for this song" },
                                color = RessoTextSecondary,
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                    }
                    itemsIndexed(lyrics) { index, lyric ->
                        val isActive = index == activeLyricIndex
                        Text(
                            text = lyric.text,
                            fontSize = if (isActive) 22.sp else 16.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.38f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { viewModel.seekToLyric(lyric.timeMs) }
                                .testTag("lyric_line_$index")
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
                if (showMusicVideo) {
                    Text(
                        text = currentSong?.title ?: "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentSong?.artist ?: "",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }

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
                    viewModel.seekTo((seekValue * duration).toLong())
                    isUserSeeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = RessoPrimary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("playback_progress_slider")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = LyricsParser.formatTime(currentPos), color = RessoTextSecondary, fontSize = 11.sp)
                Text(text = LyricsParser.formatTime(duration), color = RessoTextSecondary, fontSize = 11.sp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }, modifier = Modifier.testTag("toggle_shuffle_button")) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) RessoPrimary else Color.White.copy(alpha = 0.7f)
                    )
                }
                IconButton(onClick = { viewModel.playPreviousSong() }, modifier = Modifier.testTag("play_previous_button")) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { viewModel.playNextSong() }, modifier = Modifier.testTag("play_next_button")) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { viewModel.toggleRepeat() }, modifier = Modifier.testTag("toggle_repeat_button")) {
                    Icon(
                        imageVector = if (repeatMode == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) RessoPrimary else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp, bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    val song = currentSong
                    val link = if (song?.id?.startsWith("yt_") == true) {
                        "https://www.youtube.com/watch?v=${song.id.removePrefix("yt_")}"
                    } else {
                        "Listening to ${song?.title ?: "a song"} by ${song?.artist ?: "Resso"}"
                    }
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, link)
                    }
                    context.startActivity(Intent.createChooser(share, "Share"))
                }
            )

            PlayerActionButton(
                icon = Icons.Default.FormatQuote,
                label = "Lyrics",
                tint = if (isLyricsVisible) RessoPrimary else Color.White,
                tag = "player_lyric_poster_button",
                onClick = { viewModel.toggleLyrics() }
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

@Composable
private fun PlayerToolRow(viewModel: MusicViewModel) {
    val queue by viewModel.queue.collectAsState()
    val sleep by viewModel.sleepMinutesLeft.collectAsState()
    val offset by viewModel.lyricOffsetMs.collectAsState()
    var sheet by remember { mutableStateOf<String?>(null) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ToolChip("Queue") { sheet = "queue" }
        ToolChip(if (sleep > 0) "Sleep ${sleep}m" else "Sleep") { sheet = "sleep" }
        ToolChip("EQ") { sheet = "eq" }
        ToolChip("Save") { viewModel.saveCurrentOffline() }
        ToolChip("Sync") { sheet = "offset" }
    }
    when (sheet) {
        "queue" -> AlertDialog(
            onDismissRequest = { sheet = null },
            containerColor = Color(0xFF161616),
            title = { Text("Up next", color = Color.White) },
            text = {
                if (queue.isEmpty()) {
                    Text("Queue is empty", color = RessoTextSecondary)
                } else {
                    Column {
                        queue.forEachIndexed { index, song ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(song.title, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.moveInQueue(index, -1) }) { Text("Up") }
                                TextButton(onClick = { viewModel.moveInQueue(index, 1) }) { Text("Down") }
                                TextButton(onClick = { viewModel.removeFromQueue(song.id) }) { Text("Remove") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sheet = null }) { Text("Close") } }
        )
        "sleep" -> AlertDialog(
            onDismissRequest = { sheet = null },
            containerColor = Color(0xFF161616),
            title = { Text("Sleep timer", color = Color.White) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { minutes ->
                        TextButton(onClick = {
                            viewModel.startSleepTimer(minutes)
                            sheet = null
                        }) { Text("${minutes}m") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelSleepTimer()
                    sheet = null
                }) { Text("Cancel timer") }
            }
        )
        "eq" -> AlertDialog(
            onDismissRequest = { sheet = null },
            containerColor = Color(0xFF161616),
            title = { Text("Equalizer", color = Color.White) },
            text = {
                Column {
                    AppEqualizer.presets.forEach { preset ->
                        Text(
                            text = preset,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setEqualizerPreset(preset)
                                    sheet = null
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { sheet = null }) { Text("Close") } }
        )
        "offset" -> AlertDialog(
            onDismissRequest = { sheet = null },
            containerColor = Color(0xFF161616),
            title = { Text("Lyrics sync ${offset / 1000f}s", color = Color.White) },
            text = {
                Row {
                    TextButton(onClick = { viewModel.nudgeLyricOffset(-500) }) { Text("-0.5s") }
                    TextButton(onClick = { viewModel.nudgeLyricOffset(500) }) { Text("+0.5s") }
                }
            },
            confirmButton = { TextButton(onClick = { sheet = null }) { Text("Done") } }
        )
    }
}

@Composable
private fun ToolChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
