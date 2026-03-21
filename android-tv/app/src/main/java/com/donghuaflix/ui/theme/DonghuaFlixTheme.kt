package com.donghuaflix.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

// -- Palette: Deep cinematic blacks with electric purple/fuchsia accents --
val Obsidian = Color(0xFF07060B)
val DeepVoid = Color(0xFF0D0B14)
val SurfaceDark = Color(0xFF13111C)
val SurfaceCard = Color(0xFF1A1726)
val SurfaceCardHover = Color(0xFF231F33)

val AccentPurple = Color(0xFF8B5CF6)
val AccentFuchsia = Color(0xFFD946EF)
val AccentPink = Color(0xFFEC4899)
val AccentGold = Color(0xFFFBBF24)

val TextPrimary = Color(0xFFF1F0F5)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
val TextAccent = Color(0xFFC084FC)

val GradientStart = Color(0xFF7C3AED)
val GradientEnd = Color(0xFFDB2777)

val DarkBackground = Obsidian
val OnSurface = TextPrimary
val OnSurfaceVariant = TextSecondary
val PrimaryColor = AccentPurple
val SecondaryColor = AccentFuchsia
val CardBackground = SurfaceCard

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DonghuaFlixTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = AccentPurple,
        onPrimary = Color.White,
        secondary = AccentFuchsia,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        background = Obsidian,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
