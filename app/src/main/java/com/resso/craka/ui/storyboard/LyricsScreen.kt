package com.resso.craka.ui.storyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.ui.theme.RessoBackground
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel

@Composable
fun LyricsScreen(
    viewModel: MusicViewModel,
    onBackToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val activeLyricIndex by viewModel.activeLyricIndex.collectAsState()
    val lyricsStatus by viewModel.lyricsStatus.collectAsState()

    val listState = rememberLazyListState()
    var selectedTab by remember { mutableStateOf("Lyrics") } // "Song" | "Lyrics"

    LaunchedEffect(activeLyricIndex) {
        if (lyrics.isNotEmpty() && activeLyricIndex in lyrics.indices) {
            try {
                listState.animateScrollToItem((activeLyricIndex - 2).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF280E1C),
                        Color(0xFF140B18),
                        RessoBackground
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lyrics_screen_storyboard")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // 1. Top bar: Back arrow, Center Toggle: Song | Lyrics (Screen 14)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToPlayer) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(4.dp)
                    ) {
                        Row {
                            Text(
                                text = "Song",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == "Song") FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == "Song") Color.White else RessoTextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedTab == "Song") RessoPrimary else Color.Transparent)
                                    .clickable {
                                        selectedTab = "Song"
                                        onBackToPlayer()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            Text(
                                text = "Lyrics",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == "Lyrics") FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == "Lyrics") Color.White else RessoTextSecondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selectedTab == "Lyrics") RessoPrimary else Color.Transparent)
                                    .clickable { selectedTab = "Lyrics" }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Current Track Header Subtitle
            Text(
                text = currentSong?.title ?: "",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = currentSong?.artist ?: "",
                fontSize = 13.sp,
                color = RessoTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Synced Lyrics Flow (Screen 14)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
            ) {
                if (lyrics.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
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
                        Text(
                            text = lyric.text,
                            fontSize = if (isActive) 24.sp else 17.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
                            lineHeight = if (isActive) 34.sp else 26.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable { viewModel.seekToLyric(lyric.timeMs) }
                                .testTag("lyric_line_$index")
                        )
                    }
                }
            }
        }
    }
}
