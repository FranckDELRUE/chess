package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/**
 * Perft reference counts: the six positions of chessprogramming.org ("Perft Results") and Martin
 * Sedlak's edge cases (en passant, castling, promotion, stalemate).
 */
class PerftTest {
    @ParameterizedTest(name = "{0} depth {2}")
    @MethodSource("fast")
    fun `perft matches reference counts`(
        name: String,
        fen: String,
        depth: Int,
        nodes: Long,
    ) {
        assertEquals(nodes, Perft.count(Fen.parse(fen), depth), name)
    }

    @Tag("slow")
    @ParameterizedTest(name = "{0} depth {2}")
    @MethodSource("slow")
    fun `deep perft matches reference counts`(
        name: String,
        fen: String,
        depth: Int,
        nodes: Long,
    ) {
        val start = System.nanoTime()
        assertEquals(nodes, Perft.count(Fen.parse(fen), depth), name)
        val seconds = (System.nanoTime() - start) / NANOS_PER_SECOND
        println("perft $name depth $depth: $nodes nodes in %.1f s (%.0f nodes/s)".format(seconds, nodes / seconds))
    }

    @Test
    fun `divide sums to perft`() {
        val position = Fen.parse(KIWIPETE)
        val divide = Perft.divide(position, 2)
        assertEquals(48, divide.size)
        assertEquals(2039L, divide.values.sum())
    }

    companion object {
        private const val NANOS_PER_SECOND = 1e9
        private const val KIWIPETE = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"
        private const val POSITION_3 = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1"
        private const val POSITION_4 = "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1"
        private const val POSITION_4_MIRRORED = "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1"
        private const val POSITION_5 = "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8"
        private const val POSITION_6 = "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10"

        private fun case(
            name: String,
            fen: String,
            depth: Int,
            nodes: Long,
        ) = Arguments.of(name, fen, depth, nodes)

        @JvmStatic
        fun fast() =
            listOf(
                case("start", Fen.START, 1, 20),
                case("start", Fen.START, 2, 400),
                case("start", Fen.START, 3, 8_902),
                case("start", Fen.START, 4, 197_281),
                case("start", Fen.START, 5, 4_865_609),
                case("kiwipete", KIWIPETE, 1, 48),
                case("kiwipete", KIWIPETE, 2, 2_039),
                case("kiwipete", KIWIPETE, 3, 97_862),
                case("position 3", POSITION_3, 1, 14),
                case("position 3", POSITION_3, 3, 2_812),
                case("position 3", POSITION_3, 5, 674_624),
                case("position 4", POSITION_4, 1, 6),
                case("position 4", POSITION_4, 3, 9_467),
                case("position 4", POSITION_4, 4, 422_333),
                case("position 4 mirrored", POSITION_4_MIRRORED, 4, 422_333),
                case("position 5", POSITION_5, 1, 44),
                case("position 5", POSITION_5, 3, 62_379),
                case("position 6", POSITION_6, 1, 46),
                case("position 6", POSITION_6, 3, 89_890),
                // Martin Sedlak's edge cases, at their reference depth (about 13 M nodes in all).
                case("illegal en passant 1", "3k4/3p4/8/K1P4r/8/8/8/8 b - - 0 1", 6, 1_134_888),
                case("illegal en passant 2", "8/8/4k3/8/2p5/8/B2P2K1/8 w - - 0 1", 6, 1_015_133),
                case("en passant gives check", "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1", 6, 1_440_467),
                case("short castling gives check", "5k2/8/8/8/8/8/8/4K2R w K - 0 1", 6, 661_072),
                case("long castling gives check", "3k4/8/8/8/8/8/8/R3K3 w Q - 0 1", 6, 803_711),
                case("castling rights", "r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1", 4, 1_274_206),
                case("castling prevented", "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1", 4, 1_720_476),
                case("promote out of check", "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1", 6, 3_821_001),
                case("discovered check", "8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1", 5, 1_004_658),
                case("promote to give check", "4k3/1P6/8/8/8/8/K7/8 w - - 0 1", 6, 217_342),
                case("underpromote to check", "8/P1k5/K7/8/8/8/8/8 w - - 0 1", 6, 92_683),
                case("self stalemate", "K1k5/8/P7/8/8/8/8/8 w - - 0 1", 6, 2_217),
                case("stalemate and checkmate 1", "8/k1P5/8/1K6/8/8/8/8 w - - 0 1", 7, 567_584),
                case("stalemate and checkmate 2", "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1", 4, 23_527),
            )

        @JvmStatic
        fun slow() =
            listOf(
                case("start", Fen.START, 6, 119_060_324),
                case("kiwipete", KIWIPETE, 4, 4_085_603),
                case("kiwipete", KIWIPETE, 5, 193_690_690),
                case("position 3", POSITION_3, 6, 11_030_083),
                case("position 3", POSITION_3, 7, 178_633_661),
                case("position 4", POSITION_4, 5, 15_833_292),
                case("position 5", POSITION_5, 4, 2_103_487),
                case("position 5", POSITION_5, 5, 89_941_194),
                case("position 6", POSITION_6, 4, 3_894_594),
                case("position 6", POSITION_6, 5, 164_075_551),
            )
    }
}
