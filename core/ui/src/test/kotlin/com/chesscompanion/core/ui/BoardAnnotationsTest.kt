package com.chesscompanion.core.ui

import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.Arrow
import com.chesscompanion.core.ui.board.BoardAnnotations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoardAnnotationsTest {
    private val e2 = Square.parse("e2")!!
    private val e4 = Square.parse("e4")!!

    @Test
    fun `a square is marked, recoloured, then erased`() {
        val green = BoardAnnotations.EMPTY.toggleSquare(e4, AnnotationColor.GREEN)
        assertEquals(mapOf(e4 to AnnotationColor.GREEN), green.squares)
        val red = green.toggleSquare(e4, AnnotationColor.RED)
        assertEquals(mapOf(e4 to AnnotationColor.RED), red.squares)
        assertTrue(red.toggleSquare(e4, AnnotationColor.RED).isEmpty)
    }

    @Test
    fun `arrows follow the same rules, direction matters`() {
        val one = BoardAnnotations.EMPTY.toggleArrow(e2, e4, AnnotationColor.BLUE)
        val two = one.toggleArrow(e4, e2, AnnotationColor.BLUE)
        assertEquals(mapOf(Arrow(e2, e4) to AnnotationColor.BLUE, Arrow(e4, e2) to AnnotationColor.BLUE), two.arrows)
        assertEquals(mapOf(Arrow(e4, e2) to AnnotationColor.BLUE), two.toggleArrow(e2, e4, AnnotationColor.BLUE).arrows)
        assertEquals(AnnotationColor.RED, one.toggleArrow(e2, e4, AnnotationColor.RED).arrows[Arrow(e2, e4)])
    }

    @Test
    fun `an arrow to its own square marks the square, clear removes everything`() {
        val marked = BoardAnnotations.EMPTY.toggleArrow(e4, e4, AnnotationColor.GREEN)
        assertEquals(mapOf(e4 to AnnotationColor.GREEN), marked.squares)
        assertTrue(marked.arrows.isEmpty())
        assertTrue(marked.toggleArrow(e2, e4, AnnotationColor.RED).clear().isEmpty)
    }
}
