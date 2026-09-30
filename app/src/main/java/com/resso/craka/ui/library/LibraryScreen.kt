package com.resso.craka.ui.library

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.data.model.VibeEntity
import com.resso.craka.ui.explore.SongListItem
import com.resso.craka.ui.theme.RessoBackground
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoSecondary
import com.resso.craka.ui.theme.RessoSurface
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel
import com.resso.craka.util.LyricsParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onSongSelected: () -> Unit,
    onOpenVibeCreator: () -> Unit,
    onOpenSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val likedSongs by viewModel.likedSongs.collectAsState()
    val allVibes by viewModel.allVibes.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val openPlaylistSongs by viewModel.openPlaylistSongs.collectAsState()
    val customUploads = allSongs.filter { it.isCustomUpload }
    var openPlaylist by remember { mutableStateOf<com.resso.craka.data.model.PlaylistEntity?>(null) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isNewPlaylistDialogOpen by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }

    val recentSongs by viewModel.recentSongs.collectAsState()
    val tabTitles = listOf("Liked", "Recent", "Playlists", "Uploads", "Vibes")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RessoBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("library_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenSidebar,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .testTag("library_open_sidebar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Sidebar Tools",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Your saved music and playlists",
                        style = MaterialTheme.typography.bodySmall,
                        color = RessoTextSecondary
                    )
                }
            }

            IconButton(
                onClick = onOpenVibeCreator,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(RessoPrimary)
                    .testTag("library_create_vibe_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Create Vibe",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = RessoPrimary,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = RessoPrimary
                )
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) RessoPrimary else RessoTextSecondary,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                // Liked Songs
                if (likedSongs.isEmpty()) {
                    EmptyStateBox(
                        title = "No Liked Songs Yet",
                        subtitle = "Double tap or heart songs in the player to save them here!",
                        icon = Icons.Default.Favorite
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(likedSongs, key = { _, s -> s.id }) { index, song ->
                            SongListItem(
                                song = song,
                                isPlayingThis = false,
                                onPlay = {
                                    viewModel.selectSong(song, index, autoPlay = true, queue = likedSongs)
                                    onSongSelected()
                                },
                                onToggleLike = { viewModel.toggleLikeSong(song) }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (recentSongs.isEmpty()) {
                    EmptyStateBox(
                        title = "No recent songs",
                        subtitle = "Songs you play will show up here.",
                        icon = Icons.Default.MusicNote
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(recentSongs, key = { _, s -> s.id }) { index, song ->
                            SongListItem(
                                song = song,
                                isPlayingThis = false,
                                onPlay = {
                                    viewModel.selectSong(song, index, autoPlay = true, queue = recentSongs)
                                    onSongSelected()
                                },
                                onToggleLike = { viewModel.toggleLikeSong(song) }
                            )
                        }
                    }
                }
            }
            4 -> {
                // Created Vibes (from Room database)
                if (allVibes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = RessoSecondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Custom Vibes Created",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Trim a video from gallery to create your first Vibe background!",
                                color = RessoTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenVibeCreator,
                                colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Vibe Now")
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(allVibes, key = { it.id }) { vibe ->
                            CreatedVibeCard(
                                vibe = vibe,
                                onApply = {
                                    viewModel.applyVibeToCurrent(vibe)
                                    Toast.makeText(context, "Applied '${vibe.title}' to player!", Toast.LENGTH_SHORT).show()
                                    onSongSelected()
                                }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Playlists
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your Playlists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Button(
                            onClick = { isNewPlaylistDialogOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RessoCardBg),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = RessoSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Playlist", color = RessoSecondary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(allPlaylists, key = { it.id }) { playlist ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = RessoSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        openPlaylist = playlist
                                        viewModel.loadPlaylistSongs(playlist)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val coverId = playlist.songIdsCsv.split(",").firstOrNull { it.isNotBlank() }
                                    val cover = allSongs.firstOrNull { it.id == coverId }?.albumArtUrl
                                    if (cover.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(RessoPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistPlay,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                    } else {
                                        com.resso.craka.ui.components.AlbumArtwork(
                                            songId = coverId,
                                            albumArtUrl = cover,
                                            contentDescription = playlist.name,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        val count = playlist.songIdsCsv.split(",").count { it.isNotBlank() }
                                        Text(
                                            text = if (count == 0) "Empty playlist" else "$count songs",
                                            color = RessoTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val song = currentSong
                                            if (song == null) {
                                                Toast.makeText(context, "Play a song first, then add it", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.addSongToPlaylist(playlist.id, song)
                                                Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add current song", tint = RessoPrimary)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.playPlaylist(playlist)
                                            onSongSelected()
                                        }
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play playlist", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Uploads
                if (customUploads.isEmpty()) {
                    EmptyStateBox(
                        title = "No Uploaded Tracks",
                        subtitle = "Add your favorite songs and offline tracks anytime to your personal collection!",
                        icon = Icons.Default.Upload
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(customUploads, key = { _, s -> s.id }) { index, song ->
                            SongListItem(
                                song = song,
                                isPlayingThis = false,
                                onPlay = {
                                    viewModel.selectSong(song, index, autoPlay = true, queue = customUploads)
                                    onSongSelected()
                                },
                                onToggleLike = { viewModel.toggleLikeSong(song) }
                            )
                        }
                    }
                }
            }
        }
    }

    // New Playlist Dialog
    if (isNewPlaylistDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewPlaylistDialogOpen = false },
            containerColor = RessoSurface,
            title = { Text("Create Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistTitle,
                    onValueChange = { newPlaylistTitle = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RessoPrimary,
                        unfocusedBorderColor = RessoTextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistTitle.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistTitle)
                            Toast.makeText(context, "Playlist '$newPlaylistTitle' created", Toast.LENGTH_SHORT).show()
                            newPlaylistTitle = ""
                            isNewPlaylistDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewPlaylistDialogOpen = false }) {
                    Text("Cancel", color = RessoTextSecondary)
                }
            }
        )
    }

    val shownPlaylist = openPlaylist
    if (shownPlaylist != null) {
        AlertDialog(
            onDismissRequest = { openPlaylist = null },
            containerColor = RessoSurface,
            title = { Text(shownPlaylist.name, color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                if (openPlaylistSongs.isEmpty()) {
                    Text("This playlist is empty. Use + on the playlist to add the song that is playing.", color = RessoTextSecondary)
                } else {
                    Column {
                        openPlaylistSongs.forEachIndexed { index, song ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.selectSong(song, index, autoPlay = true, queue = openPlaylistSongs)
                                            openPlaylist = null
                                            onSongSelected()
                                        }
                                        .padding(vertical = 8.dp)
                                )
                                TextButton(onClick = { viewModel.moveInPlaylist(shownPlaylist.id, index, -1) }) { Text("Up") }
                                TextButton(onClick = { viewModel.moveInPlaylist(shownPlaylist.id, index, 1) }) { Text("Down") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.playPlaylist(shownPlaylist)
                        openPlaylist = null
                        onSongSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary)
                ) { Text("Play") }
            },
            dismissButton = {
                TextButton(onClick = { openPlaylist = null }) {
                    Text("Close", color = RessoTextSecondary)
                }
            }
        )
    }
}

@Composable
fun CreatedVibeCard(
    vibe: VibeEntity,
    onApply: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RessoSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RessoCardBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = RessoSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vibe.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    text = "Trim: ${LyricsParser.formatTime(vibe.startTrimMs)} - ${LyricsParser.formatTime(vibe.endTrimMs)} • ${vibe.filterType}",
                    color = RessoTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Created by ${vibe.creatorName}",
                    color = RessoSecondary,
                    fontSize = 10.sp
                )
            }

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Apply", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyStateBox(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RessoTextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = subtitle,
                color = RessoTextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
