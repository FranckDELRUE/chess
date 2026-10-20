package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.SearchRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Stands in for Stockfish as the player's opponent. Replies with the scripted [moves] in turn, then
 * with the first legal move; records the requests and the cancelled searches. While [gate] is set
 * the search waits for it, which keeps Stockfish "thinking".
 */
class FakeOpponent(
    vararg moves: String,
) : OpponentEngine {
    private val script = ArrayDeque(moves.toList())
    val requests = mutableListOf<SearchRequest>()
    var cancelled = 0
    var gate: CompletableDeferred<Unit>? = null
    var failWith: Exception? = null

    override fun search(request: SearchRequest): Flow<EngineEvent> =
        flow {
            requests += request
            failWith?.let { throw it }
            try {
                gate?.await()
                emit(EngineEvent.BestMove(script.removeFirstOrNull() ?: firstLegal(request)))
            } catch (e: CancellationException) {
                cancelled++
                throw e
            }
        }

    private fun firstLegal(request: SearchRequest): String {
        val position =
            request.moves.fold(Fen.parse(request.fen)) { p, uci -> p.play(checkNotNull(Move.parseUci(uci))) }
        return position.legalMoves
            .map { it.uci }
            .sorted()
            .first()
    }
}
