package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SideTest {
    @Test
    fun `there are exactly two sides`() {
        assertEquals(listOf(Side.WHITE, Side.BLACK), Side.entries)
    }
}
