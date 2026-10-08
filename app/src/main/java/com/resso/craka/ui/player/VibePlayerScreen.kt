package com.resso.craka.ui.player

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.resso.craka.ui.storyboard.RessoLogoIcon
import com.resso.craka.ui.comments.CommentsBottomSheet
import com.resso.craka.ui.lyrics.LyricPosterDialog
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.resso.craka.ui.components.PlayerSeekBar
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
    onNavigateToSearch: () -> Unit = {},
    onCollapse: () -> Unit = {},
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
    val isPosterDialogOpen by viewModel.isPosterDialogOpen.collectAsState()
    var showCommentsSheet by remember { mutableStateOf(false) }
    var activeDialogSheet by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) {
        if (isLyricsVisible) {
            viewModel.setLyricsVisible(false)
        } else {
            onCollapse()
        }
    }

    // Auto-scroll lyrics smoothly to active index when lyrics are open
    LaunchedEffect(activeLyricIndex, isLyricsVisible) {
        if (isLyricsVisible && lyrics.isNotEmpty() && activeLyricIndex in lyrics.indices) {
            try {
                listState.animateScrollToItem(
                    index = (activeLyricIndex - 2).coerceAtLeast(0)
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
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("collapse_vibe_player")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("top_header_resso_lyrics"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setLyricsVisible(false) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("header_tab_resso")
                    ) {
                        RessoLogoIcon(size = if (!isLyricsVisible) 20.dp else 16.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "resso",
                            fontSize = if (!isLyricsVisible) 22.sp else 18.sp,
                            fontWeight = if (!isLyricsVisible) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (!isLyricsVisible) Color.White else Color.White.copy(alpha = 0.45f),
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Text(
                        text = "/",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = "lyrics",
                        fontSize = if (isLyricsVisible) 22.sp else 18.sp,
                        fontWeight = if (isLyricsVisible) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isLyricsVisible) Color.White else Color.White.copy(alpha = 0.45f),
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.setLyricsVisible(true) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("header_tab_lyrics")
                    )
                }
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
                        .padding(vertical = 4.dp)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 20.dp, end = 58.dp, top = 4.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val artSize = minOf(maxWidth, maxHeight)
                        AlbumArtwork(
                            songId = currentSong?.id,
                            albumArtUrl = currentSong?.albumArtUrl,
                            contentDescription = currentSong?.title ?: "Song cover",
                            modifier = Modifier
                                .size(artSize)
                                .clip(RoundedCornerShape(20.dp))
                                .testTag("player_album_art")
                        )
                    }

                    // Floating Right-Side Action Bar (Iconic Resso UI)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isLiked = currentSong?.isLiked == true
                        PlayerActionButton(
                            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (isLiked) "Liked" else "Like",
                            tint = if (isLiked) RessoPrimary else Color.White,
                            tag = "side_like_button",
                            onClick = { viewModel.toggleLikeCurrentSong() }
                        )

                        PlayerActionButton(
                            icon = Icons.Default.ChatBubble,
                            label = if (comments.isNotEmpty()) "${comments.size}" else "Vibe",
                            tint = Color.White,
                            tag = "side_comments_button",
                            onClick = { showCommentsSheet = true }
                        )

                        PlayerActionButton(
                            icon = Icons.Default.FormatQuote,
                            label = "Quote",
                            tint = Color.White,
                            tag = "side_quote_button",
                            onClick = { viewModel.openLyricPosterDialog(null) }
                        )

                        PlayerActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            tint = Color.White,
                            tag = "side_share_button",
                            onClick = {
                                val s = currentSong
                                val shareText = "Listening to \"${s?.title ?: "Music"}\" by ${s?.artist ?: ""} on Resso 🎵\n${s?.audioUrl.orEmpty()}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share song"))
                            }
                        )

                        PlayerActionButton(
                            icon = Icons.Default.AutoAwesome,
                            label = "Vibe",
                            tint = if (!currentVibeUri.isNullOrBlank()) RessoPrimary else Color.White,
                            tag = "side_vibe_button",
                            onClick = { activeDialogSheet = "vibe" }
                        )
                    }
                }
                Text(
                    text = currentSong?.title ?: "Pick a song",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                        .padding(top = 4.dp, start = 20.dp, end = 20.dp)
                        .clickable {
                            val artist = currentSong?.artist?.substringBefore(",")?.trim().orEmpty()
                            if (artist.isNotBlank()) onOpenArtist(artist)
                        }
                )
            } else if (isLyricsVisible && !showMusicVideo) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong?.title ?: "",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong?.artist ?: "",
                            color = RessoTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openLyricPosterDialog(null) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("quote_lyrics_poster_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = "Create Lyric Poster",
                            tint = RessoPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.setLyricsVisible(false) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_lyrics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close lyrics",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("lyrics_fullscreen_container")
                ) {
                    if (lyrics.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = lyricsStatus.ifBlank { "Lyrics not available for this song" },
                                        color = RessoTextSecondary,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(lyrics) { index, lyric ->
                            val isActive = index == activeLyricIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.seekToLyric(lyric.timeMs) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                                    .testTag("lyric_line_$index"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lyric.text,
                                    fontSize = if (isActive) 23.sp else 16.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.38f),
                                    lineHeight = if (isActive) 32.sp else 24.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isActive) {
                                    IconButton(
                                        onClick = { viewModel.openLyricPosterDialog(lyric) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatQuote,
                                            contentDescription = "Quote lyric",
                                            tint = RessoPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isLiked = currentSong?.isLiked == true
                        PlayerActionButton(
                            icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            label = if (isLiked) "Liked" else "Like",
                            tint = if (isLiked) RessoPrimary else Color.White,
                            tag = "side_like_button",
                            onClick = { viewModel.toggleLikeCurrentSong() }
                        )

                        PlayerActionButton(
                            icon = Icons.Default.ChatBubble,
                            label = if (comments.isNotEmpty()) "${comments.size}" else "Vibe",
                            tint = Color.White,
                            tag = "side_comments_button",
                            onClick = { showCommentsSheet = true }
                        )

                        PlayerActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            tint = Color.White,
                            tag = "side_share_button",
                            onClick = {
                                val s = currentSong
                                val shareText = "Listening to \"${s?.title ?: "Music"}\" by ${s?.artist ?: ""} on Resso 🎵\n${s?.audioUrl.orEmpty()}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share song"))
                            }
                        )
                    }
                }
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

            Spacer(modifier = Modifier.height(10.dp))

            // Seek Bar (Slider) Component reflecting song progress & manual audio seeking
            PlayerSeekBar(
                currentPositionMs = currentPos,
                durationMs = duration,
                onSeekTo = { targetMs -> viewModel.seekTo(targetMs) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
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

            // Secondary tools row (Sleep, 8D Audio, Vibe, EQ, Save, Sync)
            PlayerToolRow(
                viewModel = viewModel,
                sheet = activeDialogSheet,
                onSetSheet = { activeDialogSheet = it }
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

        // 6. Resso Overlays: Comments Bottom Sheet & Lyric Poster Dialog
        if (showCommentsSheet) {
            CommentsBottomSheet(
                viewModel = viewModel,
                onDismiss = { showCommentsSheet = false }
            )
        }

        if (isPosterDialogOpen) {
            LyricPosterDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeLyricPosterDialog() }
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
private fun PlayerToolRow(
    viewModel: MusicViewModel,
    sheet: String?,
    onSetSheet: (String?) -> Unit
) {
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsState()
    val currentVibeUri by viewModel.currentVibeUri.collectAsState()
    val sleep by viewModel.sleepMinutesLeft.collectAsState()
    val offset by viewModel.lyricOffsetMs.collectAsState()
    val is8D by viewModel.is8DAudioEnabled.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ToolChip(if (sleep > 0) "🌙 ${sleep}m" else "🌙 Sleep") { onSetSheet("sleep") }
        ToolChip(if (is8D) "🎧 8D ON" else "🎧 8D", selected = is8D) { viewModel.toggle8DAudio() }
        ToolChip(if (!currentVibeUri.isNullOrBlank()) "✨ Vibe ON" else "✨ Vibe", selected = !currentVibeUri.isNullOrBlank()) { onSetSheet("vibe") }
        ToolChip("EQ") { onSetSheet("eq") }
        ToolChip("Save") { viewModel.saveCurrentOffline() }
        ToolChip("Sync") { onSetSheet("offset") }
    }
    when (sheet) {
        "sleep" -> AlertDialog(
            onDismissRequest = { onSetSheet(null) },
            containerColor = Color(0xFF161616),
            title = { Text("Sleep timer", color = Color.White) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60).forEach { minutes ->
                        TextButton(onClick = {
                            viewModel.startSleepTimer(minutes)
                            onSetSheet(null)
                        }) { Text("${minutes}m") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelSleepTimer()
                    onSetSheet(null)
                }) { Text("Cancel timer") }
            }
        )
        "eq" -> AlertDialog(
            onDismissRequest = { onSetSheet(null) },
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
                                    onSetSheet(null)
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { onSetSheet(null) }) { Text("Close") } }
        )
        "offset" -> AlertDialog(
            onDismissRequest = { onSetSheet(null) },
            containerColor = Color(0xFF161616),
            title = { Text("Lyrics sync ${offset / 1000f}s", color = Color.White) },
            text = {
                Row {
                    TextButton(onClick = { viewModel.nudgeLyricOffset(-500) }) { Text("-0.5s") }
                    TextButton(onClick = { viewModel.nudgeLyricOffset(500) }) { Text("+0.5s") }
                }
            },
            confirmButton = { TextButton(onClick = { onSetSheet(null) }) { Text("Done") } }
        )
        "vibe" -> {
            val videoPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null && currentSong != null) {
                    viewModel.saveCustomSongVibe(
                        songId = currentSong!!.id,
                        title = "${currentSong!!.title} Vibe",
                        videoUri = uri.toString(),
                        startTrimMs = 0L,
                        endTrimMs = 30000L,
                        filterType = "Normal"
                    )
                    onSetSheet(null)
                }
            }

            AlertDialog(
                onDismissRequest = { onSetSheet(null) },
                containerColor = Color(0xFF161616),
                title = { Text("Song Vibe & Background", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Add a dynamic video background for this song:",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        androidx.compose.material3.Button(
                            onClick = { videoPicker.launch("video/*") },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RessoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MusicVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Choose Video from Gallery", fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "Preset Motion Vibes:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        val presets = listOf(
                            "🌌 Cyber Neon Wave" to "https://assets.mixkit.co/videos/preview/mixkit-tunnel-of-futuristic-neon-lights-41584-large.mp4",
                            "🌅 Sunset Horizon" to "https://assets.mixkit.co/videos/preview/mixkit-clouds-and-blue-sky-2408-large.mp4",
                            "🌧️ Lofi Rain Drops" to "https://assets.mixkit.co/videos/preview/mixkit-rain-drops-falling-on-a-window-1520-large.mp4"
                        )

                        presets.forEach { (name, url) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        currentSong?.let { s ->
                                            viewModel.saveCustomSongVibe(s.id, name, url, 0L, 30000L, "Normal")
                                        }
                                        onSetSheet(null)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(name, color = Color.White, fontSize = 13.sp)
                            }
                        }

                        if (!currentVibeUri.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    currentSong?.let { s ->
                                        viewModel.saveCustomSongVibe(s.id, "", "", 0L, 0L, "")
                                    }
                                    onSetSheet(null)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Remove Current Vibe", color = Color(0xFFFF5252))
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { onSetSheet(null) }) { Text("Close") } }
            )
        }
    }
}

@Composable
private fun ToolChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (selected) Color.Black else Color.White,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) RessoPrimary else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
