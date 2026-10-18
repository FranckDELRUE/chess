package com.chesscompanion.core.ui.board

import androidx.compose.ui.graphics.Color

private const val LIGHT_SQUARE = 0xFFF0D9B5
private const val DARK_SQUARE = 0xFFB58863

data class BoardColors(
    val light: Color = Color(LIGHT_SQUARE),
    val dark: Color = Color(DARK_SQUARE),
)
