package com.donghuaflix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.donghuaflix.domain.model.Show
import com.donghuaflix.ui.components.ShowCard
import com.donghuaflix.ui.theme.*

@Composable
fun HomeScreen(
    onShowClick: (Int, Int?) -> Unit,
    onBrowseClick: (String?) -> Unit,
    onSearchClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onUpdateClick: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian),
    ) {
        if (uiState.isLoading) {
            // Splash screen
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.donghuaflix.R.drawable.splash_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.Text(
                            text = "D+F",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentFuchsia,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            text = "DONGHUA FLIX",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 4.sp,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        androidx.compose.material3.Text(
                            text = "Loading your library...",
                            fontSize = 13.sp,
                            color = TextMuted,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                // -- Top Navigation Bar --
                item {
                    TopNavBar(
                        onBrowseClick = { onBrowseClick(null) },
                        onSearchClick = onSearchClick,
                        onWatchlistClick = onWatchlistClick,
                        onUpdateClick = { viewModel.checkForUpdate() },
                    )
                }

                // -- Hero Banner (first show) --
                val heroShow = uiState.sections.flatMap { it.shows }.firstOrNull()
                if (heroShow != null) {
                    item {
                        HeroBanner(show = heroShow, onClick = { onShowClick(heroShow.id, null) })
                    }
                }

                // -- Continue Watching Row --
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Continue Watching", accentColor = AccentFuchsia)
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(uiState.continueWatching) { (show, progress) ->
                                val fraction = if (progress.durationSeconds != null && progress.durationSeconds > 0)
                                    progress.progressSeconds.toFloat() / progress.durationSeconds else null
                                ShowCard(
                                    show = show,
                                    onClick = { onShowClick(show.id, progress.episodeNumber) },
                                    showProgress = fraction,
                                    episodeLabel = "EP ${progress.episodeNumber}",
                                )
                            }
                        }
                    }
                }

                // -- Content Sections --
                items(uiState.sections) { section ->
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(title = section.title)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(section.shows) { show ->
                            ShowCard(
                                show = show,
                                onClick = { onShowClick(show.id, null) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopNavBar(
    onBrowseClick: () -> Unit,
    onSearchClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onUpdateClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo
        androidx.compose.material3.Text(
            text = "DF",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = AccentPurple,
        )
        androidx.compose.material3.Text(
            text = " DonghuaFlix",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Nav items
        NavPill("Browse", onClick = onBrowseClick)
        Spacer(modifier = Modifier.width(8.dp))
        NavPill("Search", onClick = onSearchClick)
        Spacer(modifier = Modifier.width(8.dp))
        NavPill("My List", onClick = onWatchlistClick)
        Spacer(modifier = Modifier.width(8.dp))
        NavPill("Update", onClick = onUpdateClick)
    }
}

@Composable
private fun NavPill(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) AccentFuchsia else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .background(if (focused) AccentFuchsia.copy(alpha = 0.15f) else SurfaceCard)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        androidx.compose.material3.Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) AccentFuchsia else TextSecondary,
        )
    }
}

@Composable
private fun HeroBanner(show: Show, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        // Background poster
        AsyncImage(
            model = show.posterUrl,
            contentDescription = show.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Gradient overlays
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Obsidian.copy(alpha = 0.9f), Color.Transparent),
                        startX = 0f,
                        endX = 600f,
                    )
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Obsidian.copy(alpha = 0.8f)),
                        startY = 200f,
                    )
                ),
        )

        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .widthIn(max = 400.dp),
        ) {
            // Genres
            if (show.genres.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    show.genres.take(3).forEach { genre ->
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            androidx.compose.material3.Text(
                                text = genre,
                                fontSize = 10.sp,
                                color = TextAccent,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            androidx.compose.material3.Text(
                text = show.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 30.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (show.rating != null) {
                    androidx.compose.material3.Text(
                        text = "★ ${String.format("%.1f", show.rating)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGold,
                    )
                }
                if (show.year != null) {
                    androidx.compose.material3.Text("${show.year}", fontSize = 13.sp, color = TextSecondary)
                }
                if (show.totalEpisodes != null) {
                    androidx.compose.material3.Text(
                        "EP${show.totalEpisodes}",
                        fontSize = 13.sp,
                        color = AccentFuchsia,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Play button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(listOf(AccentPurple, AccentFuchsia))
                    )
                    .clickable { onClick() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                androidx.compose.material3.Text(
                    text = "▶  Watch Now",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    accentColor: Color = AccentPurple,
) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .background(
                    Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.3f))),
                    RoundedCornerShape(2.dp),
                ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        androidx.compose.material3.Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            letterSpacing = 0.3.sp,
        )
    }
}
