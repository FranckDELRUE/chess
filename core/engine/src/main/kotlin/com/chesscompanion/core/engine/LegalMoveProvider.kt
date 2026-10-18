package com.chesscompanion.core.engine

/**
 * Coups légaux d'une position, en notation UCI (roque `e1g1`, promotion `e7e8q`).
 *
 * Spike S0-5 : implémenté par Stockfish (`go perft 1`) faute de générateur de coups dans
 * `:core:chess` ; remplacé par le générateur de `:core:chess` en S1.
 */
fun interface LegalMoveProvider {
    suspend fun legalMoves(fen: String): Set<String>
}
