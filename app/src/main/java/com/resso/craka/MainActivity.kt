package com.resso.craka

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.resso.craka.ui.explore.ExploreScreen
import com.resso.craka.ui.library.LibraryScreen
import com.resso.craka.ui.sidebar.SidebarDrawerContent
import kotlinx.coroutines.launch
import com.resso.craka.data.model.Song
import com.resso.craka.data.model.toSong
import com.resso.craka.player.YouTubeMusicPlayerManager
import com.resso.craka.ui.compose.BottomMiniPlayer
import com.resso.craka.ui.compose.HomeScreen
import com.resso.craka.ui.player.VibePlayerScreen
import com.resso.craka.ui.theme.MyApplicationTheme
import com.resso.craka.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        // Load trending songs for Home view; NO auto-search, NO keyboard
        viewModel.restoreHomeFeed()

        setContent {
            MyApplicationTheme {
                MainMusicApp(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        YouTubeMusicPlayerManager.release()
    }
}

@Composable
fun MainMusicApp(viewModel: MusicViewModel) {
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // StateFlow observations
    val songsListFromApi by viewModel.songsListFlow.collectAsState()
    val allSongsFromDb by viewModel.allSongs.collectAsState()
    val likedSongsFromFirestore by viewModel.firestoreLikedSongs.collectAsState()
    val historySongsFromFirestore by viewModel.firestoreHistorySongs.collectAsState()

    val searchResultsEntities by viewModel.searchResults.collectAsState()
    val searchResults = remember(searchResultsEntities) {
        searchResultsEntities.map { it.toSong() }
    }

    val isSearching by viewModel.isSearching.collectAsState()
    val currentSong by viewModel.currentPlayingSongModel.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val queue by viewModel.currentQueueSongModels.collectAsState()

    // Determine trending songs: prefer Vercel API results or fallback to DB/repo
    val trendingSongs = remember(songsListFromApi, allSongsFromDb) {
        if (songsListFromApi.isNotEmpty()) {
            songsListFromApi
        } else if (allSongsFromDb.isNotEmpty()) {
            allSongsFromDb.map { it.toSong() }
        } else {
            emptyList()
        }
    }

    val currentIndex = remember(currentSong, queue) {
        val currId = currentSong?.id
        if (currId != null) queue.indexOfFirst { it.id == currId }.coerceAtLeast(0) else 0
    }

    val isCurrentSongLiked = remember(currentSong, likedSongsFromFirestore) {
        val currId = currentSong?.id
        currId != null && likedSongsFromFirestore.any { it.id == currId }
    }

    val progressFraction = remember(currentPositionMs, durationMs) {
        if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    var currentBottomTab by remember { mutableStateOf("home") } // "home", "explore", "library"
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen || (!isPlayerExpanded && currentBottomTab != "home")) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (!isPlayerExpanded && currentBottomTab != "home") {
            currentBottomTab = "home"
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF141414),
                modifier = Modifier.width(320.dp)
            ) {
                SidebarDrawerContent(
                    viewModel = viewModel,
                    currentTab = currentBottomTab,
                    onNavigateToTab = { tab ->
                        currentBottomTab = tab
                        coroutineScope.launch { drawerState.close() }
                    },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Color(0xFF000000),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Background YouTube Player View (Rendered with alpha 1.0 behind HomeScreen for active media playback)
                AndroidView(
                    factory = { context ->
                        val frameLayout = FrameLayout(context).apply {
                            layoutParams = ViewGroup.LayoutParams(240, 160)
                        }
                        val ytView = YouTubePlayerView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        }
                        frameLayout.addView(ytView)
                        YouTubeMusicPlayerManager.attachPlayerView(ytView)
                        frameLayout
                    },
                    modifier = Modifier
                        .size(240.dp, 160.dp)
                        .align(Alignment.BottomEnd)
                )

                // 2. Active Screen based on currentBottomTab
                when (currentBottomTab) {
                    "home" -> HomeScreen(
                        trendingSongs = trendingSongs,
                        likedSongs = likedSongsFromFirestore,
                        historySongs = historySongsFromFirestore,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        currentPlayingSongId = currentSong?.id,
                        isPlaying = isPlaying,
                        onSongSelected = { song, songList ->
                            viewModel.playSongFromCompose(song, songList)
                            isPlayerExpanded = true
                        },
                        onSearchQuerySubmit = { query ->
                            viewModel.searchSongs(query)
                        },
                        onToggleLike = { song ->
                            viewModel.toggleLikeSong(song)
                        },
                        onOpenSidebar = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    "explore" -> ExploreScreen(
                        viewModel = viewModel,
                        onSongSelected = { isPlayerExpanded = true },
                        onOpenSidebar = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    "library" -> LibraryScreen(
                        viewModel = viewModel,
                        onSongSelected = { isPlayerExpanded = true },
                        onOpenSidebar = {
                            coroutineScope.launch { drawerState.open() }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Persistent Bottom Mini-Player & Resso Bottom Navigation Bar (Shown when full player is collapsed)
                if (!isPlayerExpanded) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        if (currentSong != null) {
                            BottomMiniPlayer(
                                song = currentSong,
                                isPlaying = isPlaying,
                                progressFraction = progressFraction,
                                onPlayPauseClick = { viewModel.togglePlayPause() },
                                onNextClick = { viewModel.playNextSong() },
                                onPreviousClick = { viewModel.playPreviousSong() },
                                onExpandClick = { isPlayerExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        NavigationBar(
                            containerColor = Color(0xFF0C0C0C),
                            contentColor = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            NavigationBarItem(
                                selected = currentBottomTab == "home",
                                onClick = { currentBottomTab = "home" },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Whatshot,
                                        contentDescription = "For You",
                                        tint = if (currentBottomTab == "home") Color(0xFFFF2D3A) else Color(0xFF888888)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "For You",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentBottomTab == "home") FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentBottomTab == "home") Color(0xFFFF2D3A) else Color(0xFF888888)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFFF2D3A),
                                    selectedTextColor = Color(0xFFFF2D3A),
                                    unselectedIconColor = Color(0xFF888888),
                                    unselectedTextColor = Color(0xFF888888),
                                    indicatorColor = Color.Transparent
                                )
                            )

                            NavigationBarItem(
                                selected = currentBottomTab == "explore",
                                onClick = { currentBottomTab = "explore" },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Explore",
                                        tint = if (currentBottomTab == "explore") Color(0xFFFF2D3A) else Color(0xFF888888)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Explore",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentBottomTab == "explore") FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentBottomTab == "explore") Color(0xFFFF2D3A) else Color(0xFF888888)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFFF2D3A),
                                    selectedTextColor = Color(0xFFFF2D3A),
                                    unselectedIconColor = Color(0xFF888888),
                                    unselectedTextColor = Color(0xFF888888),
                                    indicatorColor = Color.Transparent
                                )
                            )

                            NavigationBarItem(
                                selected = currentBottomTab == "library",
                                onClick = { currentBottomTab = "library" },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.LibraryMusic,
                                        contentDescription = "Me",
                                        tint = if (currentBottomTab == "library") Color(0xFFFF2D3A) else Color(0xFF888888)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Me",
                                        fontSize = 11.sp,
                                        fontWeight = if (currentBottomTab == "library") FontWeight.Bold else FontWeight.Normal,
                                        color = if (currentBottomTab == "library") Color(0xFFFF2D3A) else Color(0xFF888888)
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFFF2D3A),
                                    selectedTextColor = Color(0xFFFF2D3A),
                                    unselectedIconColor = Color(0xFF888888),
                                    unselectedTextColor = Color(0xFF888888),
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                // 4. Full Screen Resso Vibe Player (Expands with vertical swipe, lyrics, vinyl, and controls)
                AnimatedVisibility(
                    visible = isPlayerExpanded,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    VibePlayerScreen(
                        viewModel = viewModel,
                        onCollapse = { isPlayerExpanded = false },
                        onNavigateToSearch = {
                            isPlayerExpanded = false
                            currentBottomTab = "explore"
                        },
                        onOpenSidebar = {
                            isPlayerExpanded = false
                            coroutineScope.launch { drawerState.open() }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
