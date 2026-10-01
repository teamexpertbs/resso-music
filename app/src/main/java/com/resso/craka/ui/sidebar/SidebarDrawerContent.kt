package com.resso.craka.ui.sidebar

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.resso.craka.ui.components.AlbumArtwork
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoGreen
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoSecondary
import com.resso.craka.ui.theme.RessoSurface
import com.resso.craka.ui.theme.RessoTertiary
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel

@Composable
fun SidebarDrawerContent(
    viewModel: MusicViewModel,
    currentTab: String,
    onNavigateToTab: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isFlashSync by viewModel.isFlashSyncEnabled.collectAsState()
    val isVolumeBooster by viewModel.isVolumeBoosterEnabled.collectAsState()
    val is8DEnabled by viewModel.is8DAudioEnabled.collectAsState()
    val isCrossfadeEnabled by viewModel.isCrossfadeEnabled.collectAsState()
    val sleepMinutes by viewModel.sleepMinutesLeft.collectAsState()
    var showSleepDialog by remember { mutableStateOf(false) }

    // Camera permission for Flash Torch
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.toggleFlashSync()
            Toast.makeText(
                context,
                "⚡ Beat Flash: ON (Flashlight pulses to the beat)",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Flashlight requires Camera permission",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(RessoSurface)
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("app_sidebar_drawer")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(RessoPrimary, RessoSecondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Resso Music",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ByteDance Edition • VIP Active",
                        fontSize = 11.sp,
                        color = RessoSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onCloseDrawer,
                modifier = Modifier.testTag("close_sidebar_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Sidebar",
                    tint = RessoTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // VIP Membership Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(RessoPrimary.copy(alpha = 0.6f), RessoSecondary.copy(alpha = 0.6f))),
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF2E0854), Color(0xFF130924))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RessoPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = RessoPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "✨ Resso VIP Premium",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Lossless 320kbps • Ad-Free • Unlimited Skips",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Currently Playing Mini Banner
        if (currentSong != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RessoCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigateToTab("foryou")
                        onCloseDrawer()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    ) {
                        AlbumArtwork(
                            songId = currentSong?.id,
                            albumArtUrl = currentSong?.albumArtUrl,
                            contentDescription = currentSong?.title,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isPlaying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = RessoSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong?.title ?: "",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = currentSong?.artist ?: "",
                            color = RessoTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPlaying) RessoSecondary.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "Playing" else "Paused",
                            color = if (isPlaying) RessoSecondary else RessoTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: BEAT FLASH SYNC (Signature Resso Feature)
        Text(
            text = "⚡ BEAT FLASH SYNC",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RessoGreen,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RessoCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isFlashSync) RessoGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = if (isFlashSync) RessoGreen else RessoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Beat Flash Torch",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isFlashSync) "Pulsing with song beats" else "Torch off",
                            color = if (isFlashSync) RessoGreen else RessoTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = isFlashSync,
                    onCheckedChange = {
                        val hasCam = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            viewModel.toggleFlashSync()
                            Toast.makeText(
                                context,
                                if (!isFlashSync) "⚡ Beat Flash: ON" else "⚡ Beat Flash: OFF",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RessoGreen,
                        uncheckedThumbColor = RessoTextSecondary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("sidebar_flash_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: VOLUME BOOSTER (+150%)
        Text(
            text = "🔊 SOUND ENHANCEMENT",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RessoPrimary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RessoCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isVolumeBooster) RessoPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isVolumeBooster) RessoPrimary else RessoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Volume Booster (+150%)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isVolumeBooster) "Supercharged audio gain active" else "Normal gain",
                            color = if (isVolumeBooster) RessoPrimary else RessoTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = isVolumeBooster,
                    onCheckedChange = {
                        viewModel.toggleVolumeBooster()
                        Toast.makeText(
                            context,
                            if (!isVolumeBooster) "Volume Booster: +150% Active" else "Volume Booster: Normal",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RessoPrimary,
                        uncheckedThumbColor = RessoTextSecondary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("sidebar_volume_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2B: 8D AUDIO & SPATIAL SOUND
        Text(
            text = "🎧 8D SPATIAL AUDIO",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RessoSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RessoCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (is8DEnabled) RessoSecondary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = if (is8DEnabled) RessoSecondary else RessoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "8D Audio & Spatial Sound",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (is8DEnabled) "3D binaural surround sound (Headphones)" else "Standard stereo",
                            color = if (is8DEnabled) RessoSecondary else RessoTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = is8DEnabled,
                    onCheckedChange = {
                        viewModel.toggle8DAudio()
                        Toast.makeText(
                            context,
                            if (!is8DEnabled) "🎧 8D Spatial Sound: ON" else "8D Audio: OFF",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RessoSecondary,
                        uncheckedThumbColor = RessoTextSecondary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("sidebar_8d_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2C: SEAMLESS CROSSFADE (DJ TRANSITION)
        Text(
            text = "🎛️ SEAMLESS CROSSFADE",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFB300),
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RessoCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isCrossfadeEnabled) Color(0xFFFFB300).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isCrossfadeEnabled) Color(0xFFFFB300) else RessoTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "DJ Crossfade (4s)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (isCrossfadeEnabled) "Smooth gapless mix • Zero silence" else "Normal track switch",
                            color = if (isCrossfadeEnabled) Color(0xFFFFB300) else RessoTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = isCrossfadeEnabled,
                    onCheckedChange = {
                        viewModel.toggleCrossfade()
                        Toast.makeText(
                            context,
                            if (!isCrossfadeEnabled) "🎛️ DJ Crossfade: ON" else "Crossfade: OFF",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFFFB300),
                        uncheckedThumbColor = RessoTextSecondary,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.testTag("sidebar_crossfade_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2D: SLEEP TIMER (WITH FADE-OUT)
        Text(
            text = "🌙 SLEEP TIMER",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF64B5F6),
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RessoCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSleepDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (sleepMinutes > 0) Color(0xFF64B5F6).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = if (sleepMinutes > 0) Color(0xFF64B5F6) else RessoTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sleep Timer with Fade-Out",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (sleepMinutes > 0) "Active: $sleepMinutes mins left (Fades gently to stop)" else "Smooth 30s volume fade-out when falling asleep",
                        color = if (sleepMinutes > 0) Color(0xFF64B5F6) else RessoTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 3: LYRIC POSTER CREATOR
        Text(
            text = "📜 LYRIC POSTER",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RessoTertiary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RessoCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.openLyricPosterDialog(null)
                    onCloseDrawer()
                }
                .testTag("sidebar_lyric_poster_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RessoTertiary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = RessoTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Create Lyric Poster",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Generate aesthetic lyric cards & share",
                        color = RessoTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SECTION 4: STREAMING QUALITY & AUDIO ENGINE
        Text(
            text = "📶 STREAMING QUALITY",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RessoSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RessoCardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RessoSecondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = RessoSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lossless HD Audio Engine",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Auto 320 kbps High Fidelity • Zero Latency",
                        color = RessoSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Quick Links
        Text(
            text = "NAVIGATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = RessoTextSecondary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        SidebarNavItem(
            icon = Icons.Default.MusicVideo,
            title = "For You (Vertical Player)",
            isSelected = currentTab == "foryou",
            onClick = {
                onNavigateToTab("foryou")
                onCloseDrawer()
            }
        )

        SidebarNavItem(
            icon = Icons.Default.Explore,
            title = "Search & Discover",
            isSelected = currentTab == "explore",
            onClick = {
                onNavigateToTab("explore")
                onCloseDrawer()
            }
        )

        SidebarNavItem(
            icon = Icons.Default.LibraryMusic,
            title = "My Library & Liked",
            isSelected = currentTab == "library",
            onClick = {
                onNavigateToTab("library")
                onCloseDrawer()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Package Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RessoPrimary.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RessoPrimary.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = RessoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resso ByteDance Edition",
                        color = RessoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Package: com.resso.craka • v1.0",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Lag-Free • Real-Time Lyrics • Background Playback",
                    color = RessoTextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Sleep Timer Selector Dialog with 30s Fade-Out
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            containerColor = RessoSurface,
            title = {
                Text(
                    text = "🌙 Sleep Timer with Fade-Out",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Music will gently fade down over the last 30 seconds so you won't be jolted awake.",
                        color = RessoTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    listOf(15, 30, 45, 60).forEach { mins ->
                        Button(
                            onClick = {
                                viewModel.startSleepTimer(mins)
                                showSleepDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (sleepMinutes == mins) RessoPrimary else RessoCardBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "$mins Minutes",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (sleepMinutes > 0) {
                        Button(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Turn Off Sleep Timer", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Close", color = RessoTextSecondary)
                }
            }
        )
    }
}

@Composable
fun SidebarNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) RessoPrimary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) RessoPrimary else RessoTextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = if (isSelected) Color.White else RessoTextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}
