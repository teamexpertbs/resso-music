package com.example.ui.explore

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SongEntity
import com.example.ui.theme.RessoBackground
import com.example.ui.theme.RessoCardBg
import com.example.ui.theme.RessoPrimary
import com.example.ui.theme.RessoSecondary
import com.example.ui.theme.RessoSurface
import com.example.ui.theme.RessoTextSecondary
import com.example.ui.viewmodel.MusicViewModel
import com.example.util.LyricsParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: MusicViewModel,
    onSongSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    var searchKeyword by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<String?>(null) }

    val filteredSongs = allSongs.filter { song ->
        val matchesKeyword = if (searchKeyword.isBlank()) true else {
            song.title.contains(searchKeyword, ignoreCase = true) ||
            song.artist.contains(searchKeyword, ignoreCase = true) ||
            song.genre.contains(searchKeyword, ignoreCase = true)
        }
        val matchesMood = selectedMood == null || song.mood.equals(selectedMood, ignoreCase = true)
        matchesKeyword && matchesMood
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RessoBackground)
            .padding(16.dp)
            .testTag("explore_screen")
    ) {
        Text(
            text = "Discover Vibes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Trending hits, moods & immersive audio",
            style = MaterialTheme.typography.bodySmall,
            color = RessoTextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = { searchKeyword = it },
            placeholder = { Text("Search songs, artists, genres...", color = RessoTextSecondary) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = RessoPrimary)
            },
            trailingIcon = {
                if (searchKeyword.isNotEmpty()) {
                    IconButton(onClick = { searchKeyword = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = RessoTextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RessoSurface,
                unfocusedContainerColor = RessoSurface,
                focusedBorderColor = RessoPrimary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("explore_search_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Mood Filter Chips
        val moods = listOf("All", "Chill", "Party", "Focus", "Romance", "Workout")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            moods.forEach { mood ->
                val isSelected = (mood == "All" && selectedMood == null) || (selectedMood == mood)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) RessoPrimary else RessoCardBg)
                        .clickable {
                            selectedMood = if (mood == "All") null else mood
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("mood_chip_$mood")
                ) {
                    Text(
                        text = mood,
                        color = if (isSelected) Color.White else RessoTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Songs List
        Text(
            text = "Trending Vibes (${filteredSongs.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                SongListItem(
                    song = song,
                    isPlayingThis = currentSong?.id == song.id,
                    onPlay = {
                        viewModel.selectSong(song, index, autoPlay = true)
                        onSongSelected()
                    },
                    onToggleLike = {
                        viewModel.toggleLikeSong(song)
                    }
                )
            }
        }
    }
}

@Composable
fun SongListItem(
    song: SongEntity,
    isPlayingThis: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPlayingThis) RessoCardBg else RessoSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .border(
                width = 1.dp,
                color = if (isPlayingThis) RessoPrimary.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("song_item_${song.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RessoCardBg)
            ) {
                AsyncImage(
                    model = song.albumArtUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isPlayingThis) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = RessoSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlayingThis) RessoPrimary else Color.White,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        color = RessoTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = " • ${song.genre}",
                        color = RessoSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = LyricsParser.formatTime(song.durationMs),
                color = RessoTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            IconButton(onClick = onToggleLike) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like Song",
                    tint = if (song.isLiked) RessoPrimary else RessoTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
