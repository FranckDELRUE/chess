package com.chesscompanion.enginedebug

import com.chesscompanion.core.chess.FenParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BenchmarkPositionsTest {
    @Test
    fun `ten distinct positions that parse`() {
        assertEquals(10, BenchmarkPositions.FENS.toSet().size)
        BenchmarkPositions.FENS.forEach { FenParser.parseBoard(it) }
    }
}
