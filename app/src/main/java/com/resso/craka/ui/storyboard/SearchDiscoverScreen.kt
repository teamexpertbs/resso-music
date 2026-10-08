package com.resso.craka.ui.storyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.data.model.SongEntity
import com.resso.craka.ui.components.AlbumArtwork
import com.resso.craka.ui.theme.RessoBackground
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchDiscoverScreen(
    viewModel: MusicViewModel,
    onOpenSongPlayer: (SongEntity) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()

    var activeTab by remember { mutableStateOf("Search") } // "Search" or "Discover"

    val recentSearches = listOf("Arijit Singh", "Kesariya", "AP Dhillon", "Ed Sheeran")
    val trendingKeywords = listOf("Jawan", "Arijit Singh", "AP Dhillon", "Love Songs", "Workout Music")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RessoBackground)
            .statusBarsPadding()
    ) {
        // Top Switch Tabs: Search (Screen 11) vs Discover (Screen 12)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search",
                fontSize = 24.sp,
                fontWeight = if (activeTab == "Search") FontWeight.Bold else FontWeight.Medium,
                color = if (activeTab == "Search") Color.White else RessoTextSecondary,
                modifier = Modifier
                    .clickable { activeTab = "Search" }
                    .padding(end = 16.dp)
            )
            Text(
                text = "Discover",
                fontSize = 24.sp,
                fontWeight = if (activeTab == "Discover") FontWeight.Bold else FontWeight.Medium,
                color = if (activeTab == "Discover") Color.White else RessoTextSecondary,
                modifier = Modifier.clickable { activeTab = "Discover" }
            )
        }

        // Search Input Bar (Screen 11)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.isNotBlank()) {
                    activeTab = "Search"
                    viewModel.searchMusic(it)
                }
            },
            placeholder = { Text("Songs, artists, or podcasts…", color = RessoTextSecondary, fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = RessoTextSecondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; viewModel.searchMusic("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = RessoCardBg,
                unfocusedContainerColor = RessoCardBg,
                focusedBorderColor = RessoPrimary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .testTag("search_input_field")
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                // Live Search Results
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Results for \"$searchQuery\"",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (isSearching) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = RessoPrimary)
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Text(
                            text = "No songs found. Try a different artist or title.",
                            color = RessoTextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                        )
                    }
                } else {
                    itemsIndexed(searchResults) { index, song ->
                        SongResultRow(
                            song = song,
                            onClick = {
                                viewModel.selectSong(song, index)
                                onOpenSongPlayer(song)
                            }
                        )
                    }
                }
            } else if (activeTab == "Search") {
                // SCREEN 11: Recent Searches & Trending Searches
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            color = RessoPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recentSearches) { query ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(RessoCardBg)
                                    .clickable {
                                        searchQuery = query
                                        viewModel.searchMusic(query)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = RessoTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = query, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Trending Searches",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                itemsIndexed(trendingKeywords) { index, keyword ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchQuery = keyword
                                viewModel.searchMusic(keyword)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (index < 3) RessoPrimary else RessoTextSecondary,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = keyword,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = RessoTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // SCREEN 12: Discover Mode
                // 1. New Releases (Jawan, Maan Meri Jaan)
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New Releases",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            color = RessoPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val releases = listOf(
                        Triple("Jawan", "Album · 2023", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"),
                        Triple("Maan Meri Jaan", "Single · 2023", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"),
                        Triple("GLORY", "Honey Singh · 2024", "https://c.saavncdn.com/173/GLORY-Hindi-2024-20250117161048-500x500.jpg")
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(releases) { (title, subtitle, art) ->
                            Column(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { onOpenPlaylist(title) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(RessoCardBg)
                                ) {
                                    AlbumArtwork(songId = title, albumArtUrl = art, contentDescription = title, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                Text(text = subtitle, fontSize = 12.sp, color = RessoTextSecondary, maxLines = 1)
                            }
                        }
                    }
                }

                // 2. Top Playlists (Bollywood Hits, Punjabi 101, Chill Mix)
                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Top Playlists",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val topPlaylists = listOf("Bollywood Hits", "Punjabi 101", "Chill Mix", "Devotional Hits")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topPlaylists) { name ->
                            Box(
                                modifier = Modifier
                                    .size(120.dp, 80.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(listOf(Color(0xFF8A00FF), Color(0xFFFF2A6D)))
                                    )
                                    .clickable { onOpenPlaylist(name) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 3. Mood & Genre Pills
                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = "Mood & Genre",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val moods = listOf(
                        "Romantic" to Color(0xFFFF2A6D),
                        "Sad" to Color(0xFF2979FF),
                        "Workout" to Color(0xFFFF6D00),
                        "Party" to Color(0xFFAA00FF),
                        "Focus" to Color(0xFF00BFA5)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        moods.forEach { (mood, color) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(color.copy(alpha = 0.25f))
                                    .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                    .clickable { onOpenPlaylist(mood) }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(text = mood, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongResultRow(
    song: SongEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(RessoCardBg)
        ) {
            AlbumArtwork(songId = song.id, albumArtUrl = song.albumArtUrl, contentDescription = song.title, modifier = Modifier.fillMaxSize())
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = song.artist, fontSize = 13.sp, color = RessoTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
        }
    }
}
