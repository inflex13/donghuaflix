package com.donghuaflix.ui.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.donghuaflix.ui.components.ShowCard
import com.donghuaflix.ui.theme.*

@Composable
fun WatchlistScreen(
    onShowClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

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
            modifier = Modifier.padding(bottom = 20.dp),
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
                text = "My List",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }

        if (uiState.shows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Text(
                        "Your watchlist is empty",
                        color = TextMuted,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        "Add shows from the detail page",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(uiState.shows, key = { it.id }) { show ->
                    ShowCard(show = show, onClick = { onShowClick(show.id) })
                }
            }
        }
    }
}
