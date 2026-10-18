package com.chesscompanion.core.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UciParserTest {
    @Test
    fun `parses a centipawn info line with pv`() {
        val info =
            UciParser.parseInfo(
                "info depth 16 seldepth 22 multipv 1 score cp 34 nodes 1234567 nps 1500000 " +
                    "hashfull 12 tbhits 0 time 823 pv e2e4 e7e5 g1f3",
            )!!
        assertEquals(16, info.depth)
        assertEquals(Score.Centipawns(34), info.score)
        assertEquals(1_234_567L, info.nodes)
        assertEquals(1_500_000L, info.nodesPerSecond)
        assertEquals(823L, info.timeMs)
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), info.pv)
    }

    @Test
    fun `parses mate scores and multipv`() {
        val info = UciParser.parseInfo("info depth 9 multipv 2 score mate -3 nodes 10 pv h7h8")!!
        assertEquals(Score.Mate(-3), info.score)
        assertEquals(2, info.multiPv)
    }

    @Test
    fun `ignores lines without depth and score`() {
        assertNull(UciParser.parseInfo("info string NNUE evaluation using nn-1a298aa575a0.nnue"))
        assertNull(UciParser.parseInfo("info depth 12 currmove e2e4 currmovenumber 1"))
        assertNull(UciParser.parseInfo("readyok"))
    }

    @Test
    fun `parses bestmove with and without ponder`() {
        assertEquals(EngineEvent.BestMove("e2e4", "e7e5"), UciParser.parseBestMove("bestmove e2e4 ponder e7e5"))
        assertEquals(EngineEvent.BestMove("a1a2", null), UciParser.parseBestMove("bestmove a1a2"))
        assertNull(UciParser.parseBestMove("info depth 1 score cp 0"))
    }
}
