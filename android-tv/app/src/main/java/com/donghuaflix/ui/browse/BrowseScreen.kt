package com.donghuaflix.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.donghuaflix.ui.components.ShowCard
import com.donghuaflix.ui.theme.*

@Composable
fun BrowseScreen(
    genre: String?,
    onShowClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(genre) {
        if (genre != null) viewModel.selectGenre(genre)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian)
            .padding(24.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(22.dp)
                    .background(
                        Brush.verticalGradient(listOf(AccentPurple, AccentFuchsia)),
                        RoundedCornerShape(2.dp),
                    ),
            )
            androidx.compose.material3.Text(
                text = "Browse",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Website filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            FilterChip(
                label = "All Sites",
                isSelected = uiState.selectedWebsite == null,
                onClick = { viewModel.selectWebsite(null) },
            )
            FilterChip(
                label = "DonghuaFun",
                isSelected = uiState.selectedWebsite == "donghuafun",
                onClick = { viewModel.selectWebsite("donghuafun") },
            )
            FilterChip(
                label = "AnimeKhor",
                isSelected = uiState.selectedWebsite == "animekhor",
                onClick = { viewModel.selectWebsite("animekhor") },
            )
        }

        // Genre filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 20.dp),
        ) {
            item {
                FilterChip(
                    label = "All",
                    isSelected = uiState.selectedGenre == null,
                    onClick = { viewModel.selectGenre(null) },
                )
            }
            items(uiState.genres) { g ->
                FilterChip(
                    label = g,
                    isSelected = uiState.selectedGenre == g,
                    onClick = { viewModel.selectGenre(g) },
                )
            }
        }

        // Shows grid
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("Loading...", color = TextSecondary)
            }
        } else if (uiState.shows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("No shows found", color = TextMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(uiState.shows) { show ->
                    ShowCard(show = show, onClick = { onShowClick(show.id) })
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) AccentFuchsia else Color.Transparent,
                shape = RoundedCornerShape(20.dp),
            )
            .background(
                when {
                    isSelected -> Brush.horizontalGradient(listOf(AccentPurple, AccentFuchsia))
                    focused -> Brush.horizontalGradient(listOf(AccentFuchsia.copy(alpha = 0.15f), AccentFuchsia.copy(alpha = 0.15f)))
                    else -> Brush.horizontalGradient(listOf(SurfaceCard, SurfaceCard))
                }
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        androidx.compose.material3.Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isSelected -> Color.White
                focused -> AccentFuchsia
                else -> TextSecondary
            },
        )
    }
}
