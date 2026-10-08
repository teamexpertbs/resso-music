package com.resso.craka.ui.storyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.R

/**
 * Official Resso Icon.
 * Uses official vector graphic geometry with vibrant crimson-magenta gradient.
 */
@Composable
fun RessoLogoIcon(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.ic_resso_icon),
        contentDescription = "Resso Logo",
        modifier = modifier.size(size)
    )
}

/**
 * Official Resso Brand Header with 3D Folded Play-R Icon and bold white 'resso' Wordmark.
 */
@Composable
fun RessoBrandHeader(
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    fontSize: TextUnit = 24.sp,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        RessoLogoIcon(size = iconSize)
        if (showText) {
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "resso",
                fontSize = fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-0.8).sp
            )
        }
    }
}

/**
 * Flowing Glowing Silk Sound Waves from Splash Screen Bottom.
 * Reproduces the luminous magenta, hot pink, and deep purple acoustic ribbon waves.
 */
@Composable
fun RessoSoundWaves(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Ambient bottom purple/magenta glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x55C2185B),
                    Color(0x227B1FA2),
                    Color.Transparent
                ),
                center = Offset(w * 0.35f, h * 0.88f),
                radius = w * 0.75f
            )
        )

        // 2. Deep purple-magenta backdrop wave
        val deepWave = Path().apply {
            moveTo(0f, h * 0.68f)
            cubicTo(
                w * 0.25f, h * 0.75f,
                w * 0.55f, h * 0.85f,
                w, h * 0.72f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = deepWave,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x558E24AA),
                    Color(0x884A148C),
                    Color(0xDD1B0927)
                ),
                startY = h * 0.65f,
                endY = h
            )
        )

        // 3. Mid flowing magenta ribbon wave
        val midWave = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(
                w * 0.20f, h * 0.65f,
                w * 0.45f, h * 0.86f,
                w * 0.75f, h * 0.80f
            )
            cubicTo(
                w * 0.88f, h * 0.76f,
                w * 0.95f, h * 0.74f,
                w, h * 0.75f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = midWave,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x77FF1E68),
                    Color(0x66C2185B),
                    Color(0x446A1B9A)
                ),
                start = Offset(0f, h * 0.70f),
                end = Offset(w, h * 0.85f)
            )
        )

        // 4. Vibrant Hot Pink Foreground Silk Wave (Main visible wave from reference image)
        val foregroundWave = Path().apply {
            moveTo(0f, h * 0.74f)
            cubicTo(
                w * 0.15f, h * 0.82f,
                w * 0.30f, h * 0.88f,
                w * 0.48f, h * 0.82f
            )
            cubicTo(
                w * 0.65f, h * 0.74f,
                w * 0.80f, h * 0.86f,
                w, h * 0.72f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = foregroundWave,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xAAFF2A6D),
                    Color(0x88D81B60),
                    Color(0x444A148C)
                ),
                startY = h * 0.70f,
                endY = h
            )
        )

        // 5. Glowing silk crest lines (fine light ribbons seen in reference image)
        val crest1 = Path().apply {
            moveTo(0f, h * 0.72f)
            cubicTo(
                w * 0.20f, h * 0.68f,
                w * 0.40f, h * 0.82f,
                w * 0.70f, h * 0.76f
            )
            cubicTo(
                w * 0.85f, h * 0.72f,
                w * 0.95f, h * 0.76f,
                w, h * 0.78f
            )
        }
        drawPath(
            path = crest1,
            color = Color(0x66FF6584),
            style = Stroke(width = 2.5f)
        )

        val crest2 = Path().apply {
            moveTo(0f, h * 0.76f)
            cubicTo(
                w * 0.18f, h * 0.83f,
                w * 0.35f, h * 0.86f,
                w * 0.52f, h * 0.80f
            )
            cubicTo(
                w * 0.68f, h * 0.74f,
                w * 0.82f, h * 0.84f,
                w, h * 0.74f
            )
        }
        drawPath(
            path = crest2,
            color = Color(0x99FF4081),
            style = Stroke(width = 3.5f)
        )
    }
}
