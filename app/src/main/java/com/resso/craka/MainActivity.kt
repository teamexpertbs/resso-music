package com.resso.craka

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.resso.craka.ui.comments.CommentsBottomSheet
import com.resso.craka.ui.components.AlbumArtwork
import com.resso.craka.ui.explore.ExploreScreen
import com.resso.craka.ui.library.LibraryScreen
import com.resso.craka.ui.lyrics.LyricPosterDialog
import com.resso.craka.ui.player.VibePlayerScreen
import com.resso.craka.ui.sidebar.SidebarDrawerContent
import com.resso.craka.ui.theme.MyApplicationTheme
import com.resso.craka.ui.theme.RessoBackground
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoSecondary
import com.resso.craka.ui.theme.RessoSurface
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.vibe.VibeCreatorScreen
import com.resso.craka.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()

    override fun onPause() {
        super.onPause()
        musicViewModel.streamPlayerManager.stayAwake()
    }

    override fun onStop() {
        super.onStop()
        musicViewModel.streamPlayerManager.stayAwake()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current

                // Runtime permissions (Camera for flashlight torch sync, Notifications for background play)
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        permissions.add(Manifest.permission.CAMERA)
                    }
                    if (permissions.isNotEmpty()) {
                        permissionsLauncher.launch(permissions.toTypedArray())
                    }
                }

                var currentTab by remember { mutableStateOf("foryou") } // "foryou", "explore", "library", "vibe_creator"
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()

                val isCommentsOpen by musicViewModel.isCommentsSheetOpen.collectAsState()
                val isPosterOpen by musicViewModel.isPosterDialogOpen.collectAsState()
                val currentSong by musicViewModel.currentSong.collectAsState()
                val isPlaying by musicViewModel.isPlaying.collectAsState()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentTab != "vibe_creator",
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = RessoBackground,
                            drawerContentColor = Color.White,
                             modifier = Modifier
                                 .fillMaxWidth(0.88f)
                                 .widthIn(max = 320.dp)
                        ) {
                            SidebarDrawerContent(
                                viewModel = musicViewModel,
                                currentTab = currentTab,
                                onNavigateToTab = { tab ->
                                    currentTab = tab
                                    coroutineScope.launch { drawerState.close() }
                                },
                                onOpenVibeCreator = {
                                    currentTab = "vibe_creator"
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
                        bottomBar = {
                            if (currentTab != "vibe_creator") {
                                Column {
                                    // Mini player bar when navigating outside the main For You player
                                    if (currentTab != "foryou" && currentSong != null) {
                                        MiniPlayerBar(
                                            songId = currentSong?.id,
                                            songTitle = currentSong?.title ?: "",
                                            artist = currentSong?.artist ?: "",
                                            albumArtUrl = currentSong?.albumArtUrl ?: "",
                                            isPlaying = isPlaying,
                                            onTogglePlay = { musicViewModel.togglePlayPause() },
                                            onOpenPlayer = { currentTab = "foryou" }
                                        )
                                    }

                                    NavigationBar(
                                        containerColor = RessoBackground.copy(alpha = 0.95f),
                                        contentColor = Color.White,
                                        modifier = Modifier.navigationBarsPadding().testTag("main_bottom_nav_bar")
                                    ) {
                                        NavigationBarItem(
                                            selected = currentTab == "foryou",
                                            onClick = { currentTab = "foryou" },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.MusicVideo,
                                                    contentDescription = "For You",
                                                    tint = if (currentTab == "foryou") RessoPrimary else RessoTextSecondary
                                                )
                                            },
                                            label = {
                                                Text(
                                                    "For You",
                                                    color = if (currentTab == "foryou") RessoPrimary else RessoTextSecondary,
                                                    fontWeight = if (currentTab == "foryou") FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier.testTag("nav_item_for_you")
                                        )

                                        NavigationBarItem(
                                            selected = currentTab == "explore",
                                            onClick = { currentTab = "explore" },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = "Search",
                                                    tint = if (currentTab == "explore") RessoPrimary else RessoTextSecondary
                                                )
                                            },
                                            label = {
                                                Text(
                                                    "Search",
                                                    color = if (currentTab == "explore") RessoPrimary else RessoTextSecondary,
                                                    fontWeight = if (currentTab == "explore") FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier.testTag("nav_item_search")
                                        )

                                        NavigationBarItem(
                                            selected = currentTab == "library",
                                            onClick = { currentTab = "library" },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.LibraryMusic,
                                                    contentDescription = "Library",
                                                    tint = if (currentTab == "library") RessoPrimary else RessoTextSecondary
                                                )
                                            },
                                            label = {
                                                Text(
                                                    "Library",
                                                    color = if (currentTab == "library") RessoPrimary else RessoTextSecondary,
                                                    fontWeight = if (currentTab == "library") FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier.testTag("nav_item_library")
                                        )
                                    }
                                }
                            }
                        },
                        containerColor = RessoBackground,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (currentTab == "vibe_creator") 0.dp else innerPadding.calculateBottomPadding())
                        ) {
                            val streamVisible by musicViewModel.streamVisible.collectAsState()
                            if (streamVisible) {
                                AndroidView(
                                    factory = { _ ->
                                        val wv = musicViewModel.streamPlayerManager.getWebView()
                                        (wv.parent as? ViewGroup)?.removeView(wv)
                                        wv
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            when (currentTab) {
                                "foryou" -> {
                                    VibePlayerScreen(
                                        viewModel = musicViewModel,
                                        onOpenVibeCreator = { currentTab = "vibe_creator" },
                                        onNavigateToSearch = { currentTab = "explore" },
                                        onOpenSidebar = { coroutineScope.launch { drawerState.open() } }
                                    )
                                }
                                "explore" -> {
                                    ExploreScreen(
                                        viewModel = musicViewModel,
                                        onSongSelected = { currentTab = "foryou" },
                                        onOpenSidebar = { coroutineScope.launch { drawerState.open() } }
                                    )
                                }
                                "library" -> {
                                    LibraryScreen(
                                        viewModel = musicViewModel,
                                        onSongSelected = { currentTab = "foryou" },
                                        onOpenVibeCreator = { currentTab = "vibe_creator" },
                                        onOpenSidebar = { coroutineScope.launch { drawerState.open() } }
                                    )
                                }
                                "vibe_creator" -> {
                                    VibeCreatorScreen(
                                        viewModel = musicViewModel,
                                        onNavigateBack = { currentTab = "foryou" }
                                    )
                                }
                            }

                            // Comments Bottom Sheet
                            if (isCommentsOpen) {
                                CommentsBottomSheet(
                                    viewModel = musicViewModel,
                                    onDismiss = { musicViewModel.setCommentsSheetOpen(false) }
                                )
                            }

                            // Lyric Poster Dialog
                            if (isPosterOpen) {
                                LyricPosterDialog(
                                    viewModel = musicViewModel,
                                    onDismiss = { musicViewModel.closeLyricPosterDialog() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    songId: String?,
    songTitle: String,
    artist: String,
    albumArtUrl: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = RessoCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenPlayer)
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RessoSurface)
            ) {
                AlbumArtwork(
                    songId = songId,
                    albumArtUrl = albumArtUrl,
                    contentDescription = songTitle,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = songTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = artist,
                    color = RessoTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(36.dp)
                    .background(RessoPrimary, CircleShape)
                    .testTag("mini_player_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
