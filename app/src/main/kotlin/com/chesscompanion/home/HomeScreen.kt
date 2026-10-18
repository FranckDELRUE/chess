package com.chesscompanion.home

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesscompanion.R

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeScreen() {
    val activity = LocalContext.current as Activity
    val widthClass = calculateWindowSizeClass(activity).widthSizeClass
    val landscapeLayout = widthClass != WindowWidthSizeClass.Compact

    Surface(modifier = Modifier.fillMaxSize()) {
        if (landscapeLayout) {
            Row(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BoardPlaceholder(Modifier.weight(1f).fillMaxSize())
                SidePanelPlaceholder(Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BoardPlaceholder(Modifier.fillMaxWidth())
                SidePanelPlaceholder(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BoardPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.aspectRatio(1f),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.home_board_placeholder))
            }
        }
    }
}

@Composable
private fun SidePanelPlaceholder(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.home_side_panel_placeholder))
        }
    }
}
