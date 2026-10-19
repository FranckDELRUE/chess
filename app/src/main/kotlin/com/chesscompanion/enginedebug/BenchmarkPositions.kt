package com.chesscompanion.enginedebug

/**
 * Twenty middlegames of the Heidoji corpus for the Stockfish benchmark (S2-2), twenty mixed
 * positions for S0-5 (Maia).
 */
object BenchmarkPositions {
    /** Same list as `testdata/positions/heidoji-middlegames.txt` (S2-2, Heidoji corpus, balanced). */
    val FENS =
        listOf(
            "r4nnr/p1k1b2p/1p1p1p2/B1p1p1p1/2P1P1B1/3P3P/PPN2PP1/R4RK1 w - - 0 17",
            "r1b1qr2/1pp1npbk/p2p2pp/3Pp3/4P2N/3BB1Q1/PPP2PPP/4RRK1 b - - 0 16",
            "1n1q1rk1/3bnppp/4p3/1p1pP3/r1pP4/P1P2N2/1Q2BPPP/R1B1K2R w KQ - 1 16",
            "2r2rk1/1p2qpp1/p2pbn1p/4p3/3pP2B/3P2PP/PPP2PB1/R2QR1K1 w - - 2 16",
            "r1b1qrk1/ppp3b1/2n1p1np/1N1pPp2/7Q/3B1NB1/PPP2PPP/R4RK1 w - - 4 16",
            "5rk1/p1p1q1pp/2Qpprn1/6N1/4PPnP/6P1/PPPN4/R3KR2 b Q - 4 18",
            "r1b2rk1/1p3pp1/p1p1p2p/2P1n3/8/1BPRB2P/P1P2PP1/5RK1 w - - 0 17",
            "2kr3r/1ppq2p1/1p1p1n2/1P2p2p/P2pP3/B2P4/2P2PPP/R2Q1RK1 w - h6 0 16",
            "r1b1r1k1/ppp2pp1/7p/3q4/3P4/1P3N2/1PPQ2PP/2KR3R w - - 0 16",
            "r4rk1/pp2qp2/3p3p/nPp1pp2/P1P1P1b1/3P1NP1/4BP1P/R2Q1RK1 b - - 1 16",
            "2k2b1r/pppr1p1p/5p2/1B2p3/1B4P1/2N2P2/PPb4P/R3K2R b KQ - 7 16",
            "1r3rk1/2p2ppp/p1Bnp3/3p1b2/3P1P2/2P1P1Pq/PP5P/R1BQ1RK1 w - - 0 17",
            "r3k2r/1p2b1p1/2nP3p/p2B3n/5B2/7P/PPP2P2/2KRR3 b kq - 0 18",
            "2kr3r/ppp1q2p/2nn4/3p1pp1/P2P4/2P2Pp1/1PQNP1BP/R3R1K1 w - - 0 16",
            "r4rk1/ppp2p1p/3p1qpQ/2bNp3/2B1P1P1/P2P1n2/1PP2PK1/R4R2 b - - 4 16",
            "r2qk2r/5pb1/p1p1p1pp/2PpPb2/2p2B2/5N2/PPP2PPP/R2Q1RK1 w kq - 0 16",
            "2r1kb1r/ppB1pppp/2n1q3/8/8/2N5/PP2NK1P/R2Q1R2 w k - 3 16",
            "rnb1k1nr/1pp4p/p3pp2/3P2p1/8/2B1PNP1/1PP4P/2KR1B1R b kq - 0 16",
            "r3r1k1/pb3pp1/2pp1q1p/2p5/2P1P3/1P1B4/P4PPP/1R1Q1RK1 w - - 1 16",
            "3r1rk1/p1p2ppp/BpQ1pn2/3pq3/1P4b1/2P1P3/P2N1PPP/R4RK1 w - - 6 17",
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
