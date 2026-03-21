package com.donghuaflix.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.donghuaflix.ui.components.ShowCard
import com.donghuaflix.ui.theme.*

@Composable
fun SearchScreen(
    onShowClick: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
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
            modifier = Modifier.padding(bottom = 16.dp),
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
                text = "Search",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }

        // Search input
        BasicTextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChange(it) },
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
            cursorBrush = SolidColor(AccentFuchsia),
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, RoundedCornerShape(10.dp))
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (uiState.query.isEmpty()) {
                        androidx.compose.material3.Text(
                            "Search donghua...",
                            color = TextMuted,
                            fontSize = 16.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Results
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("Searching...", color = TextSecondary)
            }
        } else if (uiState.results.isEmpty() && uiState.query.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text("No results found", color = TextMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(uiState.results, key = { it.id }) { show ->
                    ShowCard(show = show, onClick = { onShowClick(show.id) })
                }
            }
        }
    }
}
