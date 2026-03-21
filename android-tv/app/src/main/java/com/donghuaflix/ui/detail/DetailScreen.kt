package com.donghuaflix.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.donghuaflix.domain.model.WebsiteInfo
import com.donghuaflix.ui.theme.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    showId: Int,
    onPlayEpisode: (Int, String?) -> Unit,
    onBack: () -> Unit,
    resumeEpisode: Int? = null,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val show = uiState.show

    // Refresh watch history every time the screen becomes visible (e.g., returning from player)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWatchHistory()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Use passed resumeEpisode if available, otherwise fall back to DB lookup
    val effectiveResumeEp = resumeEpisode ?: uiState.lastWatched?.episodeNumber

    if (uiState.isLoading || show == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Obsidian),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Text(
                if (uiState.isLoading) "Loading..." else "Show not found",
                color = TextSecondary,
                fontSize = 16.sp,
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian),
    ) {
        // Background poster (blurred/dimmed)
        AsyncImage(
            model = show.posterUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentScale = ContentScale.Crop,
        )
        // Gradient overlay on background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Obsidian.copy(alpha = 0.4f),
                            Obsidian,
                        ),
                    )
                ),
        )

        // Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // Left: Poster
            AsyncImage(
                model = show.posterUrl,
                contentDescription = show.title,
                modifier = Modifier
                    .width(180.dp)
                    .height(270.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp),
                    ),
                contentScale = ContentScale.Crop,
            )

            // Right: Scrollable info + episodes
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                // Title
                item {
                    androidx.compose.material3.Text(
                        text = show.title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 30.sp,
                    )
                }

                // Chinese title
                if (show.titleChinese != null) {
                    item {
                        androidx.compose.material3.Text(
                            text = show.titleChinese,
                            fontSize = 14.sp,
                            color = TextMuted,
                        )
                    }
                }

                // Metadata row
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (show.rating != null) {
                            androidx.compose.material3.Text(
                                "★ ${String.format("%.1f", show.rating)}",
                                color = AccentGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (show.year != null) {
                            androidx.compose.material3.Text(
                                "${show.year}",
                                color = TextSecondary,
                                fontSize = 14.sp,
                            )
                        }
                        if (show.totalEpisodes != null) {
                            androidx.compose.material3.Text(
                                "${show.totalEpisodes} Episodes",
                                color = TextAccent,
                                fontSize = 14.sp,
                            )
                        }
                        if (show.status != null) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (show.status == "completed") Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        else AccentFuchsia.copy(alpha = 0.15f),
                                        RoundedCornerShape(4.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                androidx.compose.material3.Text(
                                    show.status.uppercase(),
                                    color = if (show.status == "completed") Color(0xFF4CAF50) else AccentFuchsia,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                // Genre chips
                if (show.genres.isNotEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            show.genres.take(5).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color.White.copy(alpha = 0.06f),
                                            RoundedCornerShape(6.dp),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    androidx.compose.material3.Text(
                                        genre,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }
                    }
                }

                // Website selector
                if (show.websites.size > 1) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            androidx.compose.material3.Text(
                                "Available on",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                show.websites.forEach { website ->
                                    WebsiteChip(
                                        website = website,
                                        isSelected = website.name == uiState.selectedWebsite?.name,
                                        onClick = { viewModel.selectWebsite(website) },
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        // Play button — gradient with fuchsia border on focus
                        var playFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (playFocused) 3.dp else 0.dp,
                                    color = if (playFocused) AccentFuchsia else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .background(
                                    Brush.horizontalGradient(listOf(AccentPurple, AccentFuchsia))
                                )
                                .onFocusChanged { playFocused = it.isFocused }
                                .clickable {
                                    val epNum = effectiveResumeEp
                                        ?: uiState.episodes.firstOrNull()?.episodeNumber
                                        ?: 1
                                    onPlayEpisode(epNum, uiState.selectedWebsite?.name)
                                }
                                .padding(horizontal = 28.dp, vertical = 12.dp),
                        ) {
                            androidx.compose.material3.Text(
                                text = if (effectiveResumeEp != null) "▶  Resume EP $effectiveResumeEp"
                                else "▶  Play",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }

                        // My List button — outlined with fuchsia border on focus
                        var listFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (listFocused) 3.dp else 1.dp,
                                    color = if (listFocused) AccentFuchsia else Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .background(
                                    if (listFocused) AccentFuchsia.copy(alpha = 0.1f) else Color.Transparent
                                )
                                .onFocusChanged { listFocused = it.isFocused }
                                .clickable { viewModel.toggleWatchlist() }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            androidx.compose.material3.Text(
                                text = if (uiState.isInWatchlist) "✓  In My List" else "+  My List",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (listFocused) AccentFuchsia else TextSecondary,
                            )
                        }
                    }
                }

                // Description
                if (show.description != null) {
                    item {
                        androidx.compose.material3.Text(
                            text = show.description,
                            fontSize = 13.sp,
                            color = TextMuted,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                        )
                    }
                }

                // Episodes header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(
                                    Brush.verticalGradient(listOf(AccentPurple, AccentFuchsia)),
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                        androidx.compose.material3.Text(
                            text = "Episodes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        uiState.selectedWebsite?.let {
                            androidx.compose.material3.Text(
                                text = "· ${it.displayName}",
                                fontSize = 14.sp,
                                color = TextMuted,
                            )
                        }
                    }
                }

                // Episode grid (inline in LazyColumn)
                item {
                    // Use a fixed-height box for the grid inside LazyColumn
                    val rowCount = (uiState.episodes.size + 5) / 6  // 6 columns
                    val gridHeight = (rowCount * 48).coerceAtMost(400).dp

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 60.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(gridHeight),
                    ) {
                        items(uiState.episodes) { episode ->
                            val isCurrentEp = effectiveResumeEp == episode.episodeNumber
                            val watchInfo = uiState.watchedEpisodes[episode.episodeNumber]
                            val isWatched = watchInfo?.completed == true
                            val isInProgress = watchInfo != null && !watchInfo.completed
                            var epFocused by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (epFocused) 3.dp else 0.dp,
                                        color = if (epFocused) AccentFuchsia else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .background(
                                        when {
                                            epFocused -> AccentFuchsia.copy(alpha = 0.2f)
                                            isCurrentEp -> AccentPurple.copy(alpha = 0.3f)
                                            isWatched -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            else -> SurfaceCard
                                        }
                                    )
                                    .onFocusChanged { epFocused = it.isFocused }
                                    .clickable { onPlayEpisode(episode.episodeNumber, uiState.selectedWebsite?.name) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    // Episode number + watched indicator
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        if (isWatched) {
                                            androidx.compose.material3.Text(
                                                text = "✓",
                                                fontSize = 10.sp,
                                                color = Color(0xFF4CAF50),
                                            )
                                        }
                                        androidx.compose.material3.Text(
                                            text = "${episode.episodeNumber}",
                                            fontSize = 13.sp,
                                            color = when {
                                                epFocused -> AccentFuchsia
                                                isCurrentEp -> AccentPurple
                                                isWatched -> Color(0xFF4CAF50)
                                                else -> TextSecondary
                                            },
                                            fontWeight = if (isCurrentEp || epFocused) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }

                                    // Progress bar for in-progress episodes
                                    if (isInProgress && watchInfo != null) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(1.dp)),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(watchInfo.progressFraction)
                                                    .background(AccentFuchsia, RoundedCornerShape(1.dp)),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebsiteChip(
    website: WebsiteInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> Brush.horizontalGradient(listOf(AccentPurple, AccentFuchsia))
                    focused -> Brush.horizontalGradient(listOf(AccentPurple.copy(alpha = 0.3f), AccentFuchsia.copy(alpha = 0.3f)))
                    else -> Brush.horizontalGradient(listOf(SurfaceCard, SurfaceCard))
                },
            )
            .then(
                if (focused && !isSelected) Modifier.border(1.dp, AccentFuchsia, RoundedCornerShape(10.dp))
                else Modifier
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Column {
            androidx.compose.material3.Text(
                text = website.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else TextPrimary,
            )
            website.episodeCount?.let {
                androidx.compose.material3.Text(
                    text = "$it episodes",
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.7f) else TextMuted,
                )
            }
        }
    }
}
