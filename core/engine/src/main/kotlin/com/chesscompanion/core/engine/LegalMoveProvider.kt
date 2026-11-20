package com.chesscompanion.core.engine

import com.chesscompanion.core.chess.Fen

/** Coups légaux d'une position, en notation UCI (roque `e1g1`, promotion `e7e8q`). */
fun interface LegalMoveProvider {
    suspend fun legalMoves(fen: String): Set<String>
}

/**
 * Coups légaux donnés par le moteur de règles de `:core:chess` (S1-5, remplace `go perft 1` de
 * Stockfish). Une FEN invalide lève `InvalidFenException` (règle 2).
 */
object RulesLegalMoveProvider : LegalMoveProvider {
    override suspend fun legalMoves(fen: String): Set<String> =
        Fen.parse(fen).legalMoves.mapTo(LinkedHashSet()) { it.uci }
}
