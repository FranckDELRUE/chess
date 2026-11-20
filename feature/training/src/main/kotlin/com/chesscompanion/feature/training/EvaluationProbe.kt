package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EnginePriority
import com.chesscompanion.core.engine.EngineScheduler
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.SearchLimit
import com.chesscompanion.core.engine.SearchRequest
import java.io.IOException

private const val EVALUATION_MS = 1_000L
private const val DRAW_TEXT = "0,00"

/** Asks the shared Stockfish (`LIVE` priority) how a move changes the evaluation. */
internal class EvaluationProbe(
    private val engine: EngineScheduler,
) {
    /** The evaluation after [move] from [position], as White sees it; null when no engine answers. */
    suspend fun after(
        position: Position,
        move: Move,
    ): String? {
        val after = position.play(move)
        if (after.legalMoves.isEmpty()) return if (after.isCheck) matedText(position.sideToMove) else DRAW_TEXT
        var score: Score? = null
        try {
            val request = SearchRequest(after.toFen(), SearchLimit.MoveTime(EVALUATION_MS))
            engine.search(EnginePriority.LIVE, request).collect { event ->
                if (event is EngineEvent.Info && event.info.multiPv == 1) score = event.info.score
            }
        } catch (_: IOException) {
            // No engine, no evaluation: the rest of the feedback is unaffected.
        } catch (_: IllegalStateException) {
            // Same, when the engine cannot start.
        }
        return score?.let { formatEvaluation(it, after.sideToMove) }
    }
}
