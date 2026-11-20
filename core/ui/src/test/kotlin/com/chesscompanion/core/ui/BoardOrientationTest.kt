package com.chesscompanion.core.ui

import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.isLightSquare
import com.chesscompanion.core.ui.board.squareAt
import com.chesscompanion.core.ui.board.squareAtOffset
import com.chesscompanion.core.ui.board.squareCenter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoardOrientationTest {
    @Test
    fun `white at bottom puts a1 bottom-left and h8 top-right`() {
        val bottomLeft = squareAt(row = 7, column = 0, flipped = false)
        assertEquals(0, bottomLeft.file)
        assertEquals(0, bottomLeft.rank)
        val topRight = squareAt(row = 0, column = 7, flipped = false)
        assertEquals(7, topRight.file)
        assertEquals(7, topRight.rank)
    }

    @Test
    fun `flipped puts h8 bottom-left and a1 top-right`() {
        val bottomLeft = squareAt(row = 7, column = 0, flipped = true)
        assertEquals(7, bottomLeft.file)
        assertEquals(7, bottomLeft.rank)
        val topRight = squareAt(row = 0, column = 7, flipped = true)
        assertEquals(0, topRight.file)
        assertEquals(0, topRight.rank)
    }

    @Test
    fun `a1 is dark and h1 is light`() {
        assertFalse(isLightSquare(squareAt(7, 0, flipped = false)))
        assertTrue(isLightSquare(squareAt(7, 7, flipped = false)))
    }

    @Test
    fun `touch point maps to the square under it, null outside the board`() {
        // 100 px cells: (50, 750) is the bottom-left cell, (750, 50) the top-right one.
        assertEquals(Square.A1, squareAtOffset(50f, 750f, 100f, flipped = false))
        assertEquals(Square.H8, squareAtOffset(750f, 50f, 100f, flipped = false))
        assertEquals(Square.H8, squareAtOffset(50f, 750f, 100f, flipped = true))
        assertEquals(Square.parse("e4"), squareAtOffset(450f, 450f, 100f, flipped = false))
        assertNull(squareAtOffset(-1f, 50f, 100f, flipped = false))
        assertNull(squareAtOffset(50f, 800f, 100f, flipped = false))
        assertNull(squareAtOffset(50f, 50f, 0f, flipped = false))
    }

    @Test
    fun `arrows start and end at square centres in both orientations`() {
        val e2 = Square.parse("e2")!!
        assertEquals(450f, squareCenter(e2, 100f, flipped = false).x)
        assertEquals(650f, squareCenter(e2, 100f, flipped = false).y)
        assertEquals(350f, squareCenter(e2, 100f, flipped = true).x)
        assertEquals(150f, squareCenter(e2, 100f, flipped = true).y)
    }
}
