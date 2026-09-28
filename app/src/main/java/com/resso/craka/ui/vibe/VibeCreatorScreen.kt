package com.resso.craka.ui.vibe

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.resso.craka.ui.theme.RessoBackground
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoSecondary
import com.resso.craka.ui.theme.RessoSurface
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel
import com.resso.craka.util.LyricsParser
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VibeCreatorScreen(
    viewModel: MusicViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsState()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var vibeTitle by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Neon") }

    // Video duration & trimming range (in milliseconds)
    var videoDurationMs by remember { mutableFloatStateOf(30000f) }
    var startTrimMs by remember { mutableFloatStateOf(0f) }
    var endTrimMs by remember { mutableFloatStateOf(15000f) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    // Preset video loops for immediate testing
    val sampleVideoPresets = listOf(
        Pair("Cosmic Waves", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"),
        Pair("Cyber Street", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"),
        Pair("Tokyo Lights", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4")
    )

    // Media picker launcher using ActivityResultContracts.PickVisualMedia
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            if (vibeTitle.isBlank()) {
                vibeTitle = "${currentSong?.title ?: "Song"} Vibe"
            }
        }
    }

    // Secondary fallback video picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            if (vibeTitle.isBlank()) {
                vibeTitle = "${currentSong?.title ?: "Song"} Vibe"
            }
        }
    }

    // ExoPlayer for trimming preview
    val previewPlayer = remember(selectedVideoUri) {
        if (selectedVideoUri != null) {
            ExoPlayer.Builder(context).build().apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                setMediaItem(MediaItem.fromUri(selectedVideoUri!!))
                prepare()
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            val dur = duration.toFloat()
                            if (dur > 1000f) {
                                videoDurationMs = dur
                                endTrimMs = minOf(startTrimMs + 15000f, dur)
                            }
                        }
                    }
                })
            }
        } else null
    }

    DisposableEffect(previewPlayer) {
        onDispose {
            previewPlayer?.release()
        }
    }

    // Keep preview looping inside the trimmed section
    LaunchedEffect(previewPlayer, startTrimMs, endTrimMs, isPreviewPlaying) {
        if (previewPlayer != null && isPreviewPlaying) {
            previewPlayer.seekTo(startTrimMs.toLong())
            previewPlayer.play()
            while (isPreviewPlaying) {
                if (previewPlayer.currentPosition >= endTrimMs.toLong()) {
                    previewPlayer.seekTo(startTrimMs.toLong())
                }
                delay(100)
            }
        } else {
            previewPlayer?.pause()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Create Song Vibe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = currentSong?.title ?: "Select song first",
                            style = MaterialTheme.typography.bodySmall,
                            color = RessoPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("vibe_creator_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RessoBackground)
            )
        },
        containerColor = RessoBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video Preview Area
            Card(
                colors = CardDefaults.cardColors(containerColor = RessoSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .testTag("video_preview_card")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (previewPlayer != null && selectedVideoUri != null) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = previewPlayer
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

                        // Play/Pause Overlay Button
                        IconButton(
                            onClick = { isPreviewPlaying = !isPreviewPlaying },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(56.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .testTag("toggle_preview_playback_button")
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Check else Icons.Default.PlayArrow,
                                contentDescription = "Play Trim Preview",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Trim info chip at bottom of preview
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val durationSec = ((endTrimMs - startTrimMs) / 1000f).coerceAtLeast(0f)
                            Text(
                                text = "Trim: ${"%.1f".format(durationSec)}s",
                                color = RessoSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Empty state: Choose video placeholder
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Select Video",
                                tint = RessoPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Select a video from your gallery",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Trim up to 15 seconds to loop behind lyrics",
                                style = MaterialTheme.typography.bodySmall,
                                color = RessoTextSecondary
                            )
                        }
                    }
                }
            }

            // Video Selection Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        try {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        } catch (_: Exception) {
                            documentPickerLauncher.launch("video/*")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("select_video_button")
                ) {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Local Video")
                }

                Button(
                    onClick = {
                        // Use first preset if user has no video file
                        val preset = sampleVideoPresets.random()
                        selectedVideoUri = Uri.parse(preset.second)
                        vibeTitle = "${currentSong?.title ?: "Song"} ${preset.first}"
                        Toast.makeText(context, "Loaded preset: ${preset.first}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RessoCardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("use_preset_video_button")
                ) {
                    Text("Use Preset", color = RessoSecondary)
                }
            }

            // Trimming Section (Sliders)
            if (selectedVideoUri != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RessoSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = RessoSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Trim Section",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${LyricsParser.formatTime(startTrimMs.toLong())} - ${LyricsParser.formatTime(endTrimMs.toLong())}",
                                color = RessoSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Range slider for trimming video
                        RangeSlider(
                            value = startTrimMs..endTrimMs,
                            onValueChange = { range ->
                                startTrimMs = range.start
                                endTrimMs = maxOf(range.endInclusive, range.start + 1000f)
                                previewPlayer?.seekTo(startTrimMs.toLong())
                            },
                            valueRange = 0f..videoDurationMs.coerceAtLeast(1000f),
                            colors = SliderDefaults.colors(
                                thumbColor = RessoPrimary,
                                activeTrackColor = RessoPrimary,
                                inactiveTrackColor = RessoCardBg
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trim_range_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Start: 00:00", fontSize = 11.sp, color = RessoTextSecondary)
                            Text(
                                text = "Max loop: 15-30s",
                                fontSize = 11.sp,
                                color = RessoTextSecondary
                            )
                            Text(
                                text = "End: ${LyricsParser.formatTime(videoDurationMs.toLong())}",
                                fontSize = 11.sp,
                                color = RessoTextSecondary
                            )
                        }
                    }
                }
            }

            // Vibe Title Input
            OutlinedTextField(
                value = vibeTitle,
                onValueChange = { vibeTitle = it },
                label = { Text("Vibe Title (e.g. Neon City, Late Night)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RessoPrimary,
                    unfocusedBorderColor = RessoTextSecondary.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = RessoPrimary,
                    unfocusedLabelColor = RessoTextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vibe_title_input")
            )

            // Vibe Aesthetic Filters
            Column {
                Text(
                    text = "Aesthetic Atmosphere",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("Neon", "Cyberpunk", "Dreamy", "Retro VHS", "B&W Noir")
                    filters.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) RessoPrimary else RessoCardBg)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) RessoSecondary else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("filter_chip_$filter")
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Color.White else RessoTextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save & Apply Vibe Button
            Button(
                onClick = {
                    val song = currentSong
                    if (song == null) {
                        Toast.makeText(context, "No active song selected", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val videoPath = selectedVideoUri?.toString()
                    if (videoPath.isNullOrBlank()) {
                        Toast.makeText(context, "Please select a video first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Save to Room Database and update current song!
                    viewModel.saveCustomSongVibe(
                        songId = song.id,
                        title = vibeTitle.ifBlank { "${song.title} Vibe" },
                        videoUri = videoPath,
                        startTrimMs = startTrimMs.toLong(),
                        endTrimMs = endTrimMs.toLong(),
                        filterType = selectedFilter
                    )

                    Toast.makeText(context, "Vibe applied to ${song.title}!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                enabled = selectedVideoUri != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RessoPrimary,
                    disabledContainerColor = RessoCardBg
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_and_apply_vibe_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save & Apply Vibe to Song",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
