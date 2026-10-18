package com.chesscompanion.core.ui

import com.chesscompanion.core.ui.board.isLightSquare
import com.chesscompanion.core.ui.board.squareAt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
}
