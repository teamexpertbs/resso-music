package com.resso.craka.ui.lyrics

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.core.content.FileProvider
import com.resso.craka.data.model.SongEntity
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoSurface
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun LyricPosterDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val selectedLyric by viewModel.selectedPosterLyric.collectAsState()
    val lyricText = selectedLyric?.text ?: ""
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
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = RessoTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Poster Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(gradientBrush)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Quote Icon
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )

                        // Lyric Text
                        val quoteDisplay = if (lyricText.isNotBlank()) "\"$lyricText\"" else "\"Music is life itself.\""
                        Text(
                            text = quoteDisplay,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Song Info & Branding
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentSong?.title ?: "Unknown Track",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = currentSong?.artist ?: "Unknown Artist",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🎧 Resso Music Vibe",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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

                // Real Share / Save Button
                Button(
                    onClick = {
                        shareLyricPoster(
                            context = context,
                            song = currentSong,
                            lyric = lyricText,
                            themeName = selectedTheme
                        )
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

/**
 * Generates an HD 1080x1350 visual poster image and shares via Android Intent sheet
 */
private fun shareLyricPoster(
    context: Context,
    song: SongEntity?,
    lyric: String,
    themeName: String
) {
    try {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Theme Background Gradient
        val colors = when (themeName) {
            "Sunset Dream" -> intArrayOf(0xFFFF5E3A.toInt(), 0xFFFF2A68.toInt(), 0xFFFF7A00.toInt())
            "Midnight Blue" -> intArrayOf(0xFF0F2027.toInt(), 0xFF203A43.toInt(), 0xFF2C5364.toInt())
            "Aesthetic Pastel" -> intArrayOf(0xFFA18CD1.toInt(), 0xFFFBC2EB.toInt())
            else -> intArrayOf(0xFF14002C.toInt(), 0xFF4A00E0.toInt(), 0xFF8E2DE2.toInt())
        }
        val shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colors, null, Shader.TileMode.CLAMP)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { setShader(shader) }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw Decorative Card Frame
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            alpha = 25
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(60f, 60f, width - 60f, height - 60f), 48f, 48f, framePaint)

        // 3. Draw Quotation Marks
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            alpha = 90
            textSize = 140f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("“", (width / 2).toFloat(), 260f, quotePaint)

        // 4. Draw Lyric Text
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        }
        val displayLyric = if (lyric.isNotBlank()) lyric else "Music gives a soul to the universe and life to everything."
        val textWidth = (width - 240).coerceAtLeast(100)
        val staticLayout = StaticLayout.Builder.obtain(displayLyric, 0, displayLyric.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(16f, 1f)
            .build()

        canvas.save()
        val textY = (height / 2f) - (staticLayout.height / 2f) - 60f
        canvas.translate(120f, textY)
        staticLayout.draw(canvas)
        canvas.restore()

        // 5. Draw Song Title & Artist
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            alpha = 210
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            alpha = 150
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.15f
        }

        val trackTitle = song?.title ?: "Track"
        val trackArtist = song?.artist ?: "Artist"
        canvas.drawText(trackTitle, (width / 2).toFloat(), height - 250f, titlePaint)
        canvas.drawText(trackArtist, (width / 2).toFloat(), height - 195f, artistPaint)
        canvas.drawText("🎧 RESSO MUSIC", (width / 2).toFloat(), height - 120f, brandPaint)

        // 6. Save to cache and get FileProvider URI
        val cacheFolder = File(context.cacheDir, "shared_posters").apply { mkdirs() }
        val posterFile = File(cacheFolder, "lyric_poster.png")
        FileOutputStream(posterFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", posterFile)
        val shareCaption = "🎵 \"$displayLyric\"\n— $trackTitle by $trackArtist\n\nShared via Resso Music 🎧"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_TEXT, shareCaption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Lyric Poster")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        // Fallback to text intent if image generation fails
        val fallbackText = "🎵 \"$lyric\"\n— ${song?.title ?: "Song"} by ${song?.artist ?: ""}\n\nShared via Resso Music 🎧"
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, fallbackText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(textIntent, "Share Lyric Quote")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
