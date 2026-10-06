package com.resso.craka.ui.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.data.model.SongEntity
import com.resso.craka.ui.components.AlbumArtwork
import com.resso.craka.ui.components.FirestoreSearchBar
import com.resso.craka.ui.components.SearchFilterType
import com.resso.craka.ui.theme.RessoBackground
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoSecondary
import com.resso.craka.ui.theme.RessoSurface
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel
import com.resso.craka.util.LyricsParser
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: MusicViewModel,
    onSongSelected: () -> Unit,
    onOpenSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val firestoreSearchResults by viewModel.firestoreSearchResults.collectAsState()
    val isFirestoreSearching by viewModel.isFirestoreSearching.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val pendingSearch by viewModel.pendingSearch.collectAsState()
    val homeRows by viewModel.homeRows.collectAsState()
    val recentSongs by viewModel.recentSongs.collectAsState()
    val filterType by viewModel.searchFilterType.collectAsState()

    var searchKeyword by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Dismiss keyboard when user scrolls the content
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    // Debounced automatic search while typing
    LaunchedEffect(pendingSearch) {
        val query = pendingSearch
        if (!query.isNullOrBlank()) {
            searchKeyword = query
            viewModel.consumePendingSearch()
        }
    }

    LaunchedEffect(searchKeyword) {
        if (searchKeyword.isNotBlank()) {
            delay(280) // Smooth typing debounce
            viewModel.searchMusic(searchKeyword)
        } else {
            viewModel.searchMusic("") // Instant clear
        }
    }

    // Determine songs to show - merges online catalog & local results and applies explicit filter chip if selected
    val displaySongs = remember(searchKeyword, searchResults, firestoreSearchResults, trendingSongs, allSongs, selectedMood, filterType) {
        val baseList = if (searchKeyword.isNotBlank()) {
            val merged = mutableListOf<SongEntity>()
            val seen = mutableSetOf<String>()
            for (song in searchResults) {
                if (seen.add(song.id)) merged.add(song)
            }
            for (song in firestoreSearchResults) {
                if (seen.add(song.id)) merged.add(song)
            }

            // Apply Material3 Title / Artist Filter ONLY if user explicitly changed filter chips
            when (filterType) {
                SearchFilterType.ALL -> merged
                SearchFilterType.TITLE -> {
                    val clean = searchKeyword.trim().lowercase()
                    merged.filter { it.title.lowercase().contains(clean) }
                }
                SearchFilterType.ARTIST -> {
                    val clean = searchKeyword.trim().lowercase()
                    merged.filter { it.artist.lowercase().contains(clean) }
                }
            }
        } else if (trendingSongs.isNotEmpty()) {
            trendingSongs
        } else {
            allSongs
        }

        if (selectedMood == null) {
            baseList
        } else {
            baseList.filter { it.mood.equals(selectedMood, ignoreCase = true) }
        }
    }

    val quickSearchTags = listOf(
        "🔥 Trending Bollywood",
        "🎤 Arijit Singh",
        "🏎️ Sidhu Moose Wala",
        "⚡ Diljit Dosanjh",
        "💖 Romantic Hits",
        "🥁 Punjabi Beats",
        "☕ Lo-Fi Hindi",
        "✨ International Pop",
        "🏋️ Workout EDM"
    )

    val moods = listOf("All", "Chill", "Party", "Focus", "Romance", "Workout")

    BackHandler(enabled = searchKeyword.isNotBlank()) {
        searchKeyword = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RessoBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .imePadding()
            .testTag("explore_screen")
    ) {
        // Pinned Header with Sidebar opener
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Explore & Search",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Discover songs, artists & trending hits",
                    style = MaterialTheme.typography.bodySmall,
                    color = RessoTextSecondary
                )
            }

            IconButton(
                onClick = onOpenSidebar,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .testTag("explore_open_sidebar_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Sidebar Tools",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Material 3 Firestore Search Bar with Title/Artist Filter Chips
        FirestoreSearchBar(
            query = searchKeyword,
            onQueryChange = { searchKeyword = it },
            filterType = filterType,
            onFilterChange = { viewModel.setSearchFilterType(it) },
            isSearching = isSearching || isFirestoreSearching,
            onSearchTriggered = {
                focusManager.clearFocus()
                if (it.isNotBlank()) {
                    viewModel.searchMusic(it)
                }
            },
            onClearQuery = {
                searchKeyword = ""
                focusManager.clearFocus()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        if (searchKeyword.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(RessoPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSearching || isFirestoreSearching) {
                            "Searching online catalog..."
                        } else {
                            "Search Results"
                        },
                        fontSize = 11.sp,
                        color = RessoTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Single Unified Scrollable LazyColumn - everything scrolls smoothly together!
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // Search History Chips (only when searchKeyword is blank)
            if (searchHistory.isNotEmpty() && searchKeyword.isBlank()) {
                item(key = "search_history_row") {
                    Column {
                        Text(
                            text = "Recent Searches",
                            color = RessoTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchHistory.forEach { query ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(RessoCardBg)
                                        .clickable {
                                            searchKeyword = query
                                            focusManager.clearFocus()
                                            viewModel.searchMusic(query)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(text = query, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Trending Artist & Genre Suggestion Chips
            item(key = "quick_tags_row") {
                Column {
                    Text(
                        text = "Popular Categories",
                        color = RessoTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSearchTags.forEach { tag ->
                            val cleanQuery = tag.substringAfter(" ").trim()
                            val isSelected = searchKeyword.equals(cleanQuery, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) RessoPrimary else RessoCardBg)
                                    .clickable {
                                        searchKeyword = cleanQuery
                                        focusManager.clearFocus()
                                        viewModel.searchMusic(cleanQuery)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color.White else RessoTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Mood Filter Chips
            item(key = "mood_chips_row") {
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
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) RessoSecondary.copy(alpha = 0.25f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) RessoSecondary else RessoCardBg,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    selectedMood = if (mood == "All") null else mood
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("mood_chip_$mood")
                        ) {
                            Text(
                                text = mood,
                                color = if (isSelected) RessoSecondary else RessoTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // When search query is blank: show Recently Played (हाल ही में बजाए गए)
            if (searchKeyword.isBlank() && recentSongs.isNotEmpty()) {
                item(key = "recently_played_shelf") {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "Recently Played • हाल ही में बजाए गए",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(recentSongs.size) { index ->
                                val song = recentSongs[index]
                                Column(
                                    modifier = Modifier
                                        .width(112.dp)
                                        .clickable {
                                            viewModel.playSongFromAnywhere(song, autoPlay = true, queue = recentSongs)
                                            onSongSelected()
                                        }
                                ) {
                                    AlbumArtwork(
                                        songId = song.id,
                                        albumArtUrl = song.albumArtUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier
                                            .size(112.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    Text(
                                        text = song.artist,
                                        color = RessoTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // When search query is blank: show Home Rows (Carousels) smoothly in the scrollable view
            if (searchKeyword.isBlank() && homeRows.isNotEmpty()) {
                items(homeRows, key = { it.first }) { (title, songs) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(songs.size) { index ->
                                val song = songs[index]
                                Column(
                                    modifier = Modifier
                                        .width(112.dp)
                                        .clickable {
                                            viewModel.playSongFromAnywhere(song, autoPlay = true, queue = songs)
                                            onSongSelected()
                                        }
                                ) {
                                    AlbumArtwork(
                                        songId = song.id,
                                        albumArtUrl = song.albumArtUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier
                                            .size(112.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    Text(
                                        text = song.artist,
                                        color = RessoTextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Results Section Title
            item(key = "results_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titleText = if (searchKeyword.isNotBlank()) {
                        "Results for \"$searchKeyword\" (${displaySongs.size})"
                    } else {
                        "Trending Songs (${displaySongs.size})"
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (isSearching) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = RessoPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Searching...", color = RessoPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Loading / Empty / Songs List
            if (isSearching && displaySongs.isEmpty()) {
                item(key = "searching_loader") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RessoPrimary)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Searching online & local tracks...",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (searchError != null && displaySongs.isEmpty()) {
                item(key = "search_error_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = RessoPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = searchError ?: "Unable to complete search",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.searchMusic(searchKeyword) },
                                colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Retry Search", fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else if (displaySongs.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = RessoTextSecondary,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchKeyword.isNotBlank()) "No songs found for \"$searchKeyword\"" else "No songs in this mood",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try searching for popular artists like Arijit Singh, Sidhu Moose Wala, or Diljit Dosanjh",
                                color = RessoTextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchKeyword = "Arijit Singh"
                                    focusManager.clearFocus()
                                    viewModel.searchMusic("Arijit Singh")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search Arijit Singh", fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(displaySongs, key = { _, song -> song.id }) { _, song ->
                    val isPlayingThis = currentSong?.id == song.id
                    val isFirestore = firestoreSearchResults.any { it.id == song.id }
                    SongListItem(
                        song = song,
                        isPlayingThis = isPlayingThis,
                        onPlay = {
                            viewModel.playSongFromAnywhere(song, autoPlay = true, queue = displaySongs)
                            onSongSelected()
                        },
                        onToggleLike = {
                            viewModel.toggleLikeSong(song)
                        },
                        isOnline = song.id.startsWith("online_") || song.id.startsWith("saavn_"),
                        isFirestoreResult = isFirestore
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SongListItem(
    song: SongEntity,
    isPlayingThis: Boolean,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    isOnline: Boolean = false,
    isFirestoreResult: Boolean = false
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
            // Album Art with Play / Equalizer overlay
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RessoCardBg)
            ) {
                AlbumArtwork(
                    songId = song.id,
                    albumArtUrl = song.albumArtUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize()
                )
                if (isPlayingThis) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Playing",
                            tint = RessoSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Artist, and Badges
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    color = if (isPlayingThis) RessoPrimary else Color.White,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        color = RessoTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " • ${song.genre}",
                        color = RessoSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                if (isFirestoreResult) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Cloud",
                            color = Color(0xFF00E676),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (song.id.startsWith("yt_")) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFCC0000).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFFFF3333),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Online • Full Song",
                            color = Color(0xFFFF4D4D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isOnline || song.id.startsWith("online_")) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = RessoPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Online Stream",
                            color = RessoPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = LyricsParser.formatTime(song.durationMs),
                color = RessoTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(end = 4.dp)
            )



            // Like / Heart button
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
