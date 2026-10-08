package com.resso.craka.ui.storyboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resso.craka.ui.theme.RessoBackground
import com.resso.craka.ui.theme.RessoCardBg
import com.resso.craka.ui.theme.RessoPrimary
import com.resso.craka.ui.theme.RessoTextSecondary

@Composable
fun OnboardingFlow(
    onComplete: () -> Unit
) {
    // Steps: 1: Splash, 2: Onb1, 3: Onb2, 4: Onb3, 5: Onb4, 8: Permissions, 9: Interests
    var step by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RessoBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding_step"
        ) { currentStep ->
            when (currentStep) {
                1 -> SplashScreenView(onNext = { step = 2 })
                2 -> Onboarding1View(onNext = { step = 3 }, onBack = { step = 1 })
                3 -> Onboarding2View(onNext = { step = 4 }, onBack = { step = 2 })
                4 -> Onboarding3View(onNext = { step = 5 }, onBack = { step = 3 })
                5 -> Onboarding4View(onNext = { step = 8 }, onBack = { step = 4 })
                8 -> PermissionsView(onNext = { step = 9 }, onBack = { step = 5 })
                9 -> InterestsView(onComplete = onComplete, onBack = { step = 8 })
                else -> onComplete()
            }
        }
    }
}

// 1. Splash Screen (Matches Image 2 exactly with 3D Folded Play-R Logo and Silk Sound Waves)
@Composable
fun SplashScreenView(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07050A),
                        Color(0xFF0F0816),
                        Color(0xFF160620)
                    )
                )
            )
            .clickable(onClick = onNext)
            .testTag("screen_splash")
    ) {
        // Luminous magenta silk sound waves from Image 2
        RessoSoundWaves(
            modifier = Modifier.fillMaxSize()
        )

        // Center Content from Image 2
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 3D Folded Play-R Ribbon Logo
            RessoLogoIcon(size = 135.dp)

            Spacer(modifier = Modifier.height(24.dp))

            // 'resso' bold wordmark
            Text(
                text = "resso",
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-1.0).sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Music for the moments\nthat matter",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )

            // Spacing to keep content slightly elevated above bottom waves
            Spacer(modifier = Modifier.height(60.dp))
        }

        Text(
            text = "Tap anywhere to begin",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}

// 2. Onboarding 1
@Composable
fun Onboarding1View(onNext: () -> Unit, onBack: () -> Unit) {
    OnboardingTemplate(
        icon = Icons.Default.MusicNote,
        title = "Music\nfor every you",
        subtitle = "Discover, listen, and share your world with Resso.",
        buttonText = "Get Started",
        stepIndicator = 1,
        totalSteps = 4,
        onNext = onNext,
        onBack = onBack
    )
}

// 3. Onboarding 2
@Composable
fun Onboarding2View(onNext: () -> Unit, onBack: () -> Unit) {
    OnboardingTemplate(
        icon = Icons.Default.Headphones,
        title = "Discover\nNew Music",
        subtitle = "Personalized recommendations just for you.",
        buttonText = "Next",
        stepIndicator = 2,
        totalSteps = 4,
        onNext = onNext,
        onBack = onBack
    )
}

// 4. Onboarding 3
@Composable
fun Onboarding3View(onNext: () -> Unit, onBack: () -> Unit) {
    OnboardingTemplate(
        icon = Icons.Default.MusicNote,
        title = "Lyrics",
        subtitle = "Feel the music deeper with real-time lyrics.",
        buttonText = "Next",
        stepIndicator = 3,
        totalSteps = 4,
        onNext = onNext,
        onBack = onBack
    )
}

// 5. Onboarding 4
@Composable
fun Onboarding4View(onNext: () -> Unit, onBack: () -> Unit) {
    OnboardingTemplate(
        icon = Icons.Default.Share,
        title = "Share",
        subtitle = "Music connects people.",
        buttonText = "Next",
        stepIndicator = 4,
        totalSteps = 4,
        onNext = onNext,
        onBack = onBack
    )
}

@Composable
fun OnboardingTemplate(
    icon: ImageVector,
    title: String,
    subtitle: String,
    buttonText: String,
    stepIndicator: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            TextButton(onClick = onNext) {
                Text("Skip", color = RessoTextSecondary, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.weight(0.6f))

        // Vibrant Glowing Visual Card
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF2A6D).copy(alpha = 0.35f),
                            Color(0xFF13151F)
                        )
                    )
                )
                .border(1.dp, Color(0xFFFF2A6D).copy(alpha = 0.4f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF2A6D),
                modifier = Modifier.size(90.dp)
            )
        }

        Spacer(modifier = Modifier.weight(0.4f))

        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = subtitle,
            fontSize = 15.sp,
            color = RessoTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Progress Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSteps) {
                Box(
                    modifier = Modifier
                        .size(if (i == stepIndicator) 22.dp else 7.dp, 7.dp)
                        .clip(CircleShape)
                        .background(if (i == stepIndicator) RessoPrimary else Color.White.copy(alpha = 0.2f))
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// 8. Permissions Screen
@Composable
fun PermissionsView(onNext: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(RessoPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = RessoPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Let's personalize\nyour experience",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Allow access to provide better\nmusic recommendations.",
            fontSize = 14.sp,
            color = RessoTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        PermissionItem(
            icon = Icons.Default.Folder,
            title = "Access storage",
            subtitle = "For downloading songs & local playback"
        )
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "For updates, background controls and new music"
        )
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem(
            icon = Icons.Default.LocationOn,
            title = "Location (optional)",
            subtitle = "For local trending charts & recommendations"
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Allow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onNext) {
            Text("Maybe later", color = RessoTextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
fun PermissionItem(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RessoCardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text = subtitle, color = RessoTextSecondary, fontSize = 12.sp)
        }
    }
}

// 9. Interests Screen
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestsView(onComplete: () -> Unit, onBack: () -> Unit) {
    val genres = remember {
        listOf(
            "Bollywood", "Punjabi", "Pop", "Hip Hop",
            "Love", "Chill", "Rock", "Indie",
            "EDM", "Devotional", "Acoustic", "Party"
        )
    }
    val selectedGenres = remember { mutableStateListOf("Bollywood", "Love", "Punjabi") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Select your\nmusic interests",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose 3 or more to get better recommendations.",
            fontSize = 14.sp,
            color = RessoTextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            genres.forEach { genre ->
                val isSelected = selectedGenres.contains(genre)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isSelected) RessoPrimary else RessoCardBg
                        )
                        .border(
                            1.dp,
                            if (isSelected) RessoPrimary else Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            if (isSelected) selectedGenres.remove(genre)
                            else selectedGenres.add(genre)
                        }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = genre,
                            color = Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = RessoPrimary),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}
