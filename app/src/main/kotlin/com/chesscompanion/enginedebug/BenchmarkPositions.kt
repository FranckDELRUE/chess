package com.chesscompanion.enginedebug

/** Ten middlegame positions for the S0-4 benchmark, twenty mixed positions for S0-5 (Maia). */
object BenchmarkPositions {
    val FENS =
        listOf(
            "r2q1rk1/pp2bppp/2n1pn2/3p4/3P4/2N1PN2/PP2BPPP/R2Q1RK1 w - - 0 10",
            "r1bq1rk1/pp2ppbp/2np1np1/8/3NP3/2N1BP2/PPPQ2PP/R3KB1R w KQ - 2 9",
            "r1bqkb1r/pp3ppp/2n1pn2/2pp4/2PP4/2N2N2/PP2PPPP/R1BQKB1R w KQkq - 0 6",
            "2rq1rk1/pp1bppbp/3p1np1/8/3NP3/2N1BP2/PPPQ2PP/2KR1B1R w - - 0 11",
            "r3k2r/pp1nbppp/2p1pn2/q7/2PP4/2N1PN2/PPQ2PPP/R1B1KB1R w KQkq - 3 9",
            "r1b2rk1/ppq1bppp/2n1pn2/2pp4/3P1B2/2PBPN2/PP1N1PPP/R2Q1RK1 w - - 0 10",
            "rnbq1rk1/pp2bppp/4pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 b - - 0 7",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            "2r2rk1/pp2qppp/2n1p3/3pP3/3P4/P1N1B3/1PQ2PPP/R4RK1 w - - 0 15",
            "r1bq1rk1/ppp2ppp/2n2n2/3pp3/1bP5/2NP1NP1/PP2PPBP/R1BQ1RK1 w - - 0 7",
        )

    val DEPTHS = listOf(16, 18, 20)

    /** Same list as `core/engine/src/test/resources/maia-positions.txt` (lc0 reference). */
    val MAIA_FENS =
        listOf(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2",
            "rnbqkb1r/ppp2ppp/4pn2/3p4/2PP4/2N5/PP2PPPP/R1BQKBNR w KQkq - 2 4",
            "r3k2r/pppq1ppp/2npbn2/2b1p3/2B1P3/2NPBN2/PPPQ1PPP/R3K2R w KQkq - 4 8",
            "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R b KQkq - 0 5",
            "r2q1rk1/pp2bppp/2n1pn2/3p4/3P4/2N1PN2/PP2BPPP/R2Q1RK1 w - - 0 10",
            "r1bq1rk1/pp2ppbp/2np1np1/8/3NP3/2N1BP2/PPPQ2PP/R3KB1R w KQ - 2 9",
            "r1b1kbnr/pppp1ppp/2n5/4p1q1/4P3/3P1N2/PPP2PPP/RNBQKB1R w KQkq - 1 4",
            "rnbq1rk1/pp2bppp/4pn2/2pp4/2PP4/2N2NP1/PP2PPBP/R1BQ1RK1 b - - 0 7",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3",
            "6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1",
            "8/8/4k3/8/4P3/4K3/8/8 w - - 0 1",
            "8/5pk1/6p1/7p/7P/6P1/r4PK1/1R6 b - - 0 40",
            "r6k/1P6/8/8/8/8/6PP/6K1 w - - 0 1",
            "4k3/8/8/8/8/8/1p4K1/R7 b - - 0 50",
            "2r2rk1/pp2qppp/2n1p3/3pP3/3P4/P1N1B3/1PQ2PPP/R4RK1 b - - 1 15",
            "8/8/3k4/8/2Q5/8/5K2/1q6 w - - 10 60",
        )
}
