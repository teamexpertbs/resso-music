package com.resso.craka.ui.storyboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToSearch: () -> Unit,
    onNavigateToCharts: () -> Unit,
    onOpenSongPlayer: (SongEntity) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSubTab by remember { mutableStateOf("For You") }
    var searchFilterTab by remember { mutableStateOf("All") } // "All", "Songs", "Artists"

    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchExpanded) {
        isSearchExpanded = false
        searchQuery = ""
        viewModel.searchMusic("")
        focusManager.clearFocus()
    }
    val subTabs = listOf("For You", "Trending", "Moods", "Chart")
    val quickSuggestions = listOf("Arijit Singh", "AP Dhillon", "Sidhu Moose Wala", "Anuv Jain", "Trending", "Lo-Fi", "Party", "Chill")

    // Extract distinct artists
    val distinctArtists = remember(allSongs, trendingSongs) {
        (allSongs + trendingSongs).map { it.artist }.distinct()
    }

    // Matching artists based on search query
    val matchingArtists = remember(searchQuery, distinctArtists) {
        val q = searchQuery.trim()
        if (q.isBlank()) emptyList()
        else distinctArtists.filter { it.contains(q, ignoreCase = true) }
    }

    // Matching songs based on real-time local search and network results
    val matchingSongs = remember(searchQuery, allSongs, trendingSongs, searchResults) {
        val q = searchQuery.trim()
        if (q.isBlank()) emptyList()
        else {
            val localMatches = (allSongs + trendingSongs).filter {
                it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true)
            }
            (localMatches + searchResults).distinctBy { it.id }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RessoBackground)
            .statusBarsPadding()
    ) {
        // 1. Top Header: Resso brand logo header, search toggle, and notifications
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RessoBrandHeader(
                iconSize = 32.dp,
                fontSize = 24.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) {
                            searchQuery = ""
                            viewModel.searchMusic("")
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier.testTag("home_search_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchExpanded) "Close Search" else "Open Search",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onNavigateToCharts,
                    modifier = Modifier.testTag("home_cast_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }
        }

        // 2. Interactive Search Bar at Top of Home Screen (Opens only when user clicks Search icon)
        if (isSearchExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        width = 1.dp,
                        color = if (searchQuery.isNotEmpty()) RessoPrimary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("home_search_bar"),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) RessoPrimary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search songs, artists, moods...",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 14.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { newQuery ->
                                searchQuery = newQuery
                                viewModel.searchMusic(newQuery)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(RessoPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    viewModel.searchMusic(searchQuery)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_search_input")
                        )
                    }

                    if (isSearching) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = RessoPrimary
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Search",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    searchQuery = ""
                                    viewModel.searchMusic("")
                                    focusManager.clearFocus()
                                }
                                .testTag("home_search_clear_btn")
                        )
                    }
                }
            }
        }

        // If search bar is open and query is entered, display live search results view!
        if (isSearchExpanded && searchQuery.isNotEmpty()) {
            // Search Filter Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("All", "Songs (${matchingSongs.size})", "Artists (${matchingArtists.size})")) { tabName ->
                    val tabKey = if (tabName.startsWith("Songs")) "Songs" else if (tabName.startsWith("Artists")) "Artists" else "All"
                    val isSelected = searchFilterTab == tabKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) RessoPrimary else Color.White.copy(alpha = 0.08f))
                            .clickable { searchFilterTab = tabKey }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tabName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Matching Artists
                if ((searchFilterTab == "All" || searchFilterTab == "Artists") && matchingArtists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Artists",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(matchingArtists) { artistName ->
                                Column(
                                    modifier = Modifier
                                        .clickable { onOpenArtist(artistName) }
                                        .testTag("home_search_result_artist_$artistName"),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFFFF1B6B), Color(0xFF45CAFF))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = artistName,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = artistName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Artist",
                                        fontSize = 11.sp,
                                        color = RessoTextSecondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Matching Songs
                if ((searchFilterTab == "All" || searchFilterTab == "Songs") && matchingSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }

                    itemsIndexed(matchingSongs) { index, song ->
                        val isCurrent = currentSong?.id == song.id
                        val isPlayingCurrent = isCurrent && isPlaying

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isCurrent) RessoPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    viewModel.selectSong(song, index, autoPlay = true, queue = matchingSongs)
                                    onOpenSongPlayer(song)
                                }
                                .padding(vertical = 6.dp, horizontal = 6.dp)
                                .testTag("home_search_result_song_${song.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RessoCardBg)
                            ) {
                                AlbumArtwork(
                                    songId = song.id,
                                    albumArtUrl = song.albumArtUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) RessoPrimary else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 13.sp,
                                    color = RessoTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Interactive Play/Pause integration with playback state
                            IconButton(
                                onClick = {
                                    if (isCurrent) {
                                        viewModel.togglePlayPause()
                                    } else {
                                        viewModel.selectSong(song, index, autoPlay = true, queue = matchingSongs)
                                    }
                                },
                                modifier = Modifier.testTag("home_search_play_btn_${song.id}")
                            ) {
                                Icon(
                                    imageVector = if (isPlayingCurrent) Icons.Default.Pause else if (isCurrent) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingCurrent) "Pause" else "Play",
                                    tint = if (isCurrent) RessoPrimary else Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Empty state if nothing found
                if (matchingSongs.isEmpty() && matchingArtists.isEmpty() && !isSearching) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = RessoTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results found for \"$searchQuery\"",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try searching for Bollywood, Punjabi, Arijit Singh, or explore other genres",
                                color = RessoTextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(RessoPrimary)
                                    .clickable {
                                        searchQuery = ""
                                        viewModel.searchMusic("")
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "Clear Search",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Normal Home Screen Feed when search bar is not active
            // Quick suggestions chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickSuggestions) { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                isSearchExpanded = true
                                searchQuery = suggestion
                                viewModel.searchMusic(suggestion)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Sub Tabs: For You, Trending, Moods, Chart
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subTabs) { tab ->
                    val isSelected = activeSubTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) RessoPrimary else Color.White.copy(alpha = 0.08f)
                            )
                            .clickable {
                                activeSubTab = tab
                                if (tab == "Chart") onNavigateToCharts()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("home_tab_$tab")
                    ) {
                        Text(
                            text = tab,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // 3. Hero Trending Card (Screen 10 Storyboard)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(160.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFFF1B6B),
                                        Color(0xFF8A00FF)
                                    )
                                )
                            )
                            .clickable {
                                val pick = trendingSongs.firstOrNull() ?: allSongs.firstOrNull()
                                pick?.let {
                                    viewModel.selectSong(it, 0)
                                    onOpenSongPlayer(it)
                                }
                            }
                            .testTag("home_trending_hero")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "TRENDING",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Trending\nThis Week",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 28.sp
                                )
                            }

                            // Right side glossy play circle
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // 4. "Recommended For You" Section (Screen 10 Storyboard)
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recommended For You",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "See All",
                            fontSize = 13.sp,
                            color = RessoPrimary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onOpenPlaylist("Bollywood Hits") }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val playlists = listOf(
                        Triple("Hindi Hits", "Top Bollywood hits", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80"),
                        Triple("Chill Vibes", "Relax & unwind", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"),
                        Triple("Love Songs", "Romantic melodies", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"),
                        Triple("Punjabi 101", "High energy beats", "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&auto=format&fit=crop&q=80")
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(playlists) { (title, sub, art) ->
                            Column(
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { onOpenPlaylist(title) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(135.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(RessoCardBg)
                                ) {
                                    AlbumArtwork(
                                        songId = title,
                                        albumArtUrl = art,
                                        contentDescription = title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = sub,
                                    fontSize = 12.sp,
                                    color = RessoTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 5. "Recently Played" Section (Screen 10 Storyboard)
                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Played",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val songsToShow = if (allSongs.isNotEmpty()) allSongs.take(8) else trendingSongs.take(8)
                itemsIndexed(songsToShow) { index, song ->
                    val isCurrentlyPlaying = currentSong?.id == song.id && isPlaying

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrentlyPlaying) RessoPrimary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                viewModel.selectSong(song, index)
                                onOpenSongPlayer(song)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RessoCardBg)
                        ) {
                            AlbumArtwork(
                                songId = song.id,
                                albumArtUrl = song.albumArtUrl,
                                contentDescription = song.title,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrentlyPlaying) RessoPrimary else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                fontSize = 13.sp,
                                color = RessoTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                if (currentSong?.id == song.id) {
                                    viewModel.togglePlayPause()
                                } else {
                                    viewModel.selectSong(song, index)
                                    onOpenSongPlayer(song)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isCurrentlyPlaying) "Pause" else "Play",
                                tint = if (isCurrentlyPlaying) RessoPrimary else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
