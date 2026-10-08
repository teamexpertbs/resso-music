package com.resso.craka.ui.compose

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.resso.craka.data.model.Song
import com.resso.craka.ui.components.AlbumArtwork

@Composable
fun HomeScreen(
    trendingSongs: List<Song>,
    likedSongs: List<Song>,
    historySongs: List<Song>,
    searchResults: List<Song>,
    isSearching: Boolean,
    currentPlayingSongId: String?,
    isPlaying: Boolean,
    onSongSelected: (Song, List<Song>) -> Unit,
    onSearchQuerySubmit: (String) -> Unit,
    onToggleLike: (Song) -> Unit,
    onOpenSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchModeActive by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    val tabs = listOf(
        "🔥 Trending" to Icons.Filled.Whatshot,
        "❤️ Liked" to Icons.Filled.Favorite,
        "🕒 History" to Icons.Filled.History
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Header Branding with Tools/Sidebar Drawer Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF2D3A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RESSO MUSIC",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "VIBE • LYRICS • 0-DATA FREE",
                            color = Color(0xFF00E676),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSidebar,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1E1E))
                        .testTag("home_open_sidebar_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Tools, EQ & Settings",
                        tint = Color(0xFFFF2D3A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. Search Bar (NEVER auto-focused on launch)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        if (query.isBlank()) {
                            isSearchModeActive = false
                        }
                    },
                    placeholder = {
                        Text(
                            text = "Search songs, artists, hits...",
                            color = Color(0xFF777777),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFAAAAAA)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                isSearchModeActive = false
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (searchQuery.isNotBlank()) {
                            isSearchModeActive = true
                            focusManager.clearFocus()
                            onSearchQuerySubmit(searchQuery.trim())
                        }
                    }),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF161616),
                        unfocusedContainerColor = Color(0xFF121212),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFFF2D3A),
                        focusedIndicatorColor = Color(0xFFFF2D3A),
                        unfocusedIndicatorColor = Color(0xFF262626)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar")
                )
            }

            // 3. Category Tabs (Active when not in active search mode)
            if (!isSearchModeActive) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF000000),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFFF2D3A),
                            height = 3.dp
                        )
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    tabs.forEachIndexed { index, (label, icon) ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) Color.White else Color(0xFF888888)
                                )
                            }
                        )
                    }
                }
            } else {
                // Header showing Search Results indicator with back/clear button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Results for \"$searchQuery\"",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Close Search",
                        color = Color(0xFFFF2D3A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                searchQuery = ""
                                isSearchModeActive = false
                                focusManager.clearFocus()
                            }
                            .padding(4.dp)
                    )
                }
            }

            // 4. Songs List Content
            val songsToDisplay = if (isSearchModeActive) {
                searchResults
            } else {
                when (selectedTab) {
                    0 -> trendingSongs
                    1 -> likedSongs
                    2 -> historySongs
                    else -> trendingSongs
                }
            }

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF2D3A))
                }
            } else if (songsToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMessage = if (isSearchModeActive) {
                        "No results found for \"$searchQuery\".\nTry searching for another song or artist."
                    } else {
                        when (selectedTab) {
                            1 -> "No liked songs yet.\nTap the heart icon on any song to save favorites!"
                            2 -> "No playback history yet.\nPlay songs to build your history."
                            else -> "Loading trending music..."
                        }
                    }
                    Text(
                        text = emptyMessage,
                        color = Color(0xFF777777),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 150.dp, top = 8.dp)
                ) {
                    itemsIndexed(songsToDisplay) { index, song ->
                        val isCurrentPlaying = song.id == currentPlayingSongId
                        val isSongLiked = likedSongs.any { it.id == song.id }

                        SongListItem(
                            index = index + 1,
                            song = song,
                            isCurrentPlaying = isCurrentPlaying,
                            isPlaying = isPlaying,
                            isLiked = isSongLiked,
                            onClick = { onSongSelected(song, songsToDisplay) },
                            onToggleLike = { onToggleLike(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongListItem(
    index: Int,
    song: Song,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isCurrentPlaying) Color(0xFF141414) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large, crisp Thumbnail
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtwork(
                songId = song.id,
                albumArtUrl = song.getEffectiveThumbnail(),
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize()
            )

            if (isCurrentPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = "Playing",
                        tint = Color(0xFFFF2D3A),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrentPlaying) Color(0xFFFF2D3A) else Color.White,
                fontSize = 15.sp,
                fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!song.telegramUrl.isNullOrBlank()) {
                    Text(
                        text = "⚡ TELEGRAM CDN",
                        color = Color(0xFF00E676),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(Color(0xFF003815), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = song.getDisplayArtist(),
                    color = Color(0xFFB0B0B0),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duration
        if (song.duration.isNotBlank()) {
            Text(
                text = song.duration,
                color = Color(0xFF666666),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        // Like Button
        IconButton(
            onClick = onToggleLike,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Toggle Like",
                tint = if (isLiked) Color(0xFFFF2D3A) else Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
