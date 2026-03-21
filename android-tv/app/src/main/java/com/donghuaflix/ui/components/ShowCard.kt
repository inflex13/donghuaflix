package com.donghuaflix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.donghuaflix.domain.model.Show
import com.donghuaflix.ui.theme.*

@Composable
fun ShowCard(
    show: Show,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Float? = null,
    episodeLabel: String? = null,
    isInWatchlist: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(if (isFocused) 158.dp else 150.dp)
            .height(if (isFocused) 237.dp else 225.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) AccentFuchsia else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .background(SurfaceCard)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() },
    ) {
        // Poster
        AsyncImage(
            model = show.posterUrl,
            contentDescription = show.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Bottom gradient scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    )
                ),
        )

        // Episode label badge (for continue watching)
        if (episodeLabel != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentPurple, AccentFuchsia)),
                        RoundedCornerShape(4.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = episodeLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        // Rating badge
        if (show.rating != null && show.rating > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "★ ${String.format("%.1f", show.rating)}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentGold,
                )
            }
        }

        // Watchlist indicator
        if (isInWatchlist) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = if (show.rating != null) 28.dp else 6.dp, start = 6.dp)
                    .background(AccentPurple.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "♥",
                    fontSize = 10.sp,
                    color = Color.White,
                )
            }
        }

        // Bottom info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(10.dp),
        ) {
            Text(
                text = show.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (show.year != null) {
                    Text("${show.year}", fontSize = 10.sp, color = TextMuted)
                }
                if (show.totalEpisodes != null) {
                    Text(
                        "EP${show.totalEpisodes}",
                        fontSize = 10.sp,
                        color = TextAccent,
                    )
                }
            }
        }

        // Progress bar
        if (showProgress != null && showProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
            ) {
                // Track
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.15f)),
                )
                // Fill
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(showProgress)
                        .background(
                            Brush.horizontalGradient(listOf(AccentPurple, AccentFuchsia))
                        ),
                )
            }
        }
    }
}

@Composable
fun Text(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    color: Color = TextPrimary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        lineHeight = lineHeight,
        modifier = modifier,
    )
}
