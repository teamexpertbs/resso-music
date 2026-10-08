package com.resso.craka.ui.storyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.ui.theme.RessoBackground
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoTextSecondary
import com.resso.craka.ui.viewmodel.MusicViewModel

@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEqEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf("Custom") }
    val presets = listOf("Custom", "Pop", "Rock", "Jazz", "Hip Hop")

    // 5 Frequency bands: 60Hz, 230Hz, 910Hz, 3.4kHz, 14kHz (values -1f to 1f)
    val bandLevels = remember { mutableStateListOf(0.2f, 0.5f, 0.1f, 0.4f, 0.3f) }
    val bandLabels = listOf("60", "230", "910", "3.4K", "14K")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RessoBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Header: Back arrow, "Equalizer", Switch (Screen 25)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Equalizer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Switch(
                checked = isEqEnabled,
                onCheckedChange = { isEqEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RessoPrimary,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color.DarkGray
                )
            )
        }

        // Preset Chips: Custom, Pop, Rock, Jazz, Hip Hop
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(presets) { preset ->
                val isSelected = selectedPreset == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) RessoPrimary else RessoCardBg)
                        .clickable {
                            selectedPreset = preset
                            when (preset) {
                                "Pop" -> { bandLevels[0]=0.1f; bandLevels[1]=0.3f; bandLevels[2]=0.5f; bandLevels[3]=0.4f; bandLevels[4]=0.2f }
                                "Rock" -> { bandLevels[0]=0.6f; bandLevels[1]=0.3f; bandLevels[2]=0.1f; bandLevels[3]=0.4f; bandLevels[4]=0.7f }
                                "Jazz" -> { bandLevels[0]=0.3f; bandLevels[1]=0.2f; bandLevels[2]=0.1f; bandLevels[3]=0.3f; bandLevels[4]=0.4f }
                                "Hip Hop" -> { bandLevels[0]=0.7f; bandLevels[1]=0.6f; bandLevels[2]=0.2f; bandLevels[3]=0.3f; bandLevels[4]=0.5f }
                                else -> {}
                            }
                            viewModel.setEqualizerPreset(preset)
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Interactive Neon Spline Waveform / Curve Area (Screen 25 Storyboard)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(RessoCardBg)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Grid guidelines
                val gridLines = 5
                for (i in 0..gridLines) {
                    val y = h * (i.toFloat() / gridLines)
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Calculate spline path from 5 band points
                val stepX = w / (bandLevels.size - 1)
                val points = bandLevels.mapIndexed { index, level ->
                    val x = index * stepX
                    // level ranges from -1 to 1; 0 is middle
                    val y = (h / 2f) - (level * (h / 2.2f))
                    Offset(x, y.coerceIn(10f, h - 10f))
                }

                // Fill under curve with gradient
                val fillPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                    lineTo(w, h)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF2A6D).copy(alpha = 0.35f),
                            Color(0xFFFF2A6D).copy(alpha = 0.02f)
                        )
                    )
                )

                // Neon stroke line
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFF0844), Color(0xFFFF2A6D), Color(0xFFFF6584))
                    ),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Control node dots
                points.forEach { pt ->
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = RessoPrimary, radius = 3.dp.toPx(), center = pt)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Frequency Labels (60, 230, 910, 3.4K, 14K)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            bandLabels.forEach { label ->
                Text(text = label, color = RessoTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Reset Button (Screen 25)
        Button(
            onClick = {
                bandLevels[0] = 0.2f
                bandLevels[1] = 0.2f
                bandLevels[2] = 0.2f
                bandLevels[3] = 0.2f
                bandLevels[4] = 0.2f
                selectedPreset = "Custom"
                viewModel.setEqualizerPreset("Off")
            },
            colors = ButtonDefaults.buttonColors(containerColor = RessoCardBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .height(48.dp)
        ) {
            Text(text = "Reset", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
