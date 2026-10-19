package com.chesscompanion.core.engine

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.engine.maia.MaiaReference
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RulesLegalMoveProviderTest {
    @Test
    fun `legal moves match lc0 on the 20 maia reference positions`() =
        runTest {
            // lc0 gives a prior to every legal move: the reference lists them all.
            val reference = MaiaReference.rows.filter { it.net == "1100" }.groupBy({ it.fen }, { it.move })
            assertEquals(20, reference.size)
            reference.forEach { (fen, moves) ->
                assertEquals(moves.toSet(), RulesLegalMoveProvider.legalMoves(fen), fen)
            }
        }

    @Test
    fun `cost of legal moves on the pc`() {
        val fens = MaiaReference.rows.map { it.fen }.distinct()
        repeat(WARM_UP) { fens.forEach { Fen.parse(it).legalMoves } }
        val start = System.nanoTime()
        repeat(ROUNDS) { fens.forEach { Fen.parse(it).legalMoves } }
        val micros = (System.nanoTime() - start) / NANOS_PER_MICRO / (ROUNDS * fens.size)
        println("Rules legal moves (FEN parse included): $micros µs per position on the PC")
    }

    private companion object {
        const val WARM_UP = 200
        const val ROUNDS = 1_000
        const val NANOS_PER_MICRO = 1_000.0
    }
}
