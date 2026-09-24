package com.example.ui.lyrics

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.RessoCardBg
import com.example.ui.theme.RessoPrimary
import com.example.ui.theme.RessoSecondary
import com.example.ui.theme.RessoSurface
import com.example.ui.theme.RessoTextSecondary
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun LyricPosterDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val selectedLyric by viewModel.selectedPosterLyric.collectAsState()
    val context = LocalContext.current

    var selectedTheme by remember { mutableStateOf("Neon Cyber") }

    val gradientBrush = when (selectedTheme) {
        "Sunset Dream" -> Brush.verticalGradient(listOf(Color(0xFFFF5E3A), Color(0xFFFF2A68), Color(0xFFFF7A00)))
        "Midnight Blue" -> Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
        "Aesthetic Pastel" -> Brush.verticalGradient(listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)))
        else -> Brush.verticalGradient(listOf(Color(0xFF14002C), Color(0xFF4A00E0), Color(0xFF8E2DE2)))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = RessoSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("lyric_poster_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lyric Poster",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_lyric_poster_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = RessoTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // The Poster Preview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .testTag("poster_preview_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradientBrush)
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.TopStart)
                        )

                        // The quote / lyric text
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = selectedLyric?.text ?: "Every starfall echoes back to you",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 30.sp
                            )
                        }

                        // Bottom track badge
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentSong?.title ?: "Song"} • ${currentSong?.artist ?: "Artist"}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "resso",
                                color = RessoPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Style Themes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Neon Cyber", "Sunset Dream", "Midnight Blue", "Aesthetic Pastel").forEach { themeName ->
                        val isSelected = selectedTheme == themeName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) RessoPrimary else RessoCardBg)
                                .clickable { selectedTheme = themeName }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = themeName.split(" ")[0],
                                color = if (isSelected) Color.White else RessoTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Share / Save Button
                Button(
                    onClick = {
                        Toast.makeText(context, "Lyric Poster shared successfully!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_poster_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Lyric Poster",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
