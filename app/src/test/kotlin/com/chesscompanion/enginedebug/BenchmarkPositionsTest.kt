package com.chesscompanion.enginedebug

import com.chesscompanion.core.chess.Fen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class BenchmarkPositionsTest {
    @Test
    fun `ten distinct valid positions`() {
        assertEquals(10, BenchmarkPositions.FENS.toSet().size)
        BenchmarkPositions.FENS.forEach { Fen.parse(it) }
        BenchmarkPositions.MAIA_FENS.forEach { Fen.parse(it) }
    }

    @Test
    fun `maia positions are the ones of the lc0 reference`() {
        val reference =
            File(
                "../core/engine/src/test/resources/maia-positions.txt",
            ).readLines().filter { it.isNotBlank() }
        assertEquals(reference, BenchmarkPositions.MAIA_FENS)
    }
}
