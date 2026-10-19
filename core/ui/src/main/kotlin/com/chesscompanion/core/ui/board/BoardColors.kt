package com.chesscompanion.core.ui.board

import androidx.compose.ui.graphics.Color

private const val LIGHT_SQUARE = 0xFFF0D9B5
private const val DARK_SQUARE = 0xFFB58863
private const val SELECTED = 0x8064A0E0
private const val LAST_MOVE = 0x66CDD26A
private const val TARGET = 0x59000000
private const val CHECK = 0xCCE53935
private const val ANNOTATION_GREEN = 0xCC15781B
private const val ANNOTATION_RED = 0xCC882020
private const val ANNOTATION_BLUE = 0xCC003088

/** Board colours; highlights are translucent so they read on both square colours and themes. */
data class BoardColors(
    val light: Color = Color(LIGHT_SQUARE),
    val dark: Color = Color(DARK_SQUARE),
    val selected: Color = Color(SELECTED),
    val lastMove: Color = Color(LAST_MOVE),
    val target: Color = Color(TARGET),
    val check: Color = Color(CHECK),
    val annotationGreen: Color = Color(ANNOTATION_GREEN),
    val annotationRed: Color = Color(ANNOTATION_RED),
    val annotationBlue: Color = Color(ANNOTATION_BLUE),
) {
    fun of(annotation: AnnotationColor): Color =
        when (annotation) {
            AnnotationColor.GREEN -> annotationGreen
            AnnotationColor.RED -> annotationRed
            AnnotationColor.BLUE -> annotationBlue
        }
}
