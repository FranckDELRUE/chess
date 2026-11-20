package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.San
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.forWhite
import com.chesscompanion.core.engine.winProbability
import java.util.Locale

/** One principal variation of the engine, ready to display. */
data class AnalysisLine(
    /** 1 for the best line, 2 and 3 for the following ones (UCI `multipv`). */
    val rank: Int,
    val depth: Int,
    /** Seen from White, whoever is to move. */
    val score: Score,
    /** The variation in French SAN, cut at the first move the rules refuse (rule 2). */
    val moves: List<String>,
    /** First move, played when the line is tapped. */
    val firstMove: Move,
    /** Move number of the first move, and whether Black plays it (for "12...Cc6"). */
    val moveNumber: Int,
    val blackFirst: Boolean,
) {
    /** "12.Cf3 Cc6 13.d4", at most [maxPlies] half-moves. */
    fun text(maxPlies: Int = MAX_PLIES): String {
        var number = moveNumber
        var black = blackFirst
        val parts = mutableListOf<String>()
        moves.take(maxPlies).forEachIndexed { index, san ->
            when {
                !black -> parts += "$number.$san"
                index == 0 -> parts += "$number...$san"
                else -> parts += san
            }
            if (black) number++
            black = !black
        }
        return parts.joinToString(" ")
    }

    companion object {
        const val MAX_PLIES = 8
    }
}

/** What the analysis shows for [fen]: the best lines so far, the depth reached and the evaluation bar. */
data class AnalysisState(
    val enabled: Boolean = false,
    val fen: String = "",
    val lines: List<AnalysisLine> = emptyList(),
    val finished: Boolean = false,
    /** The game is over on this position: no search, the result is the evaluation. */
    val gameOver: Boolean = false,
    /** Why the engine could not start or stopped, null when all is well. */
    val error: String? = null,
) {
    val failed: Boolean get() = error != null

    val best: AnalysisLine? get() = lines.firstOrNull()
    val depth: Int get() = best?.depth ?: 0

    /** White's chances in [0, 1] from the best line, 0.5 before the first result. */
    val whiteShare: Float get() = best?.score?.winProbability()?.toFloat() ?: NEUTRAL

    companion object {
        const val NEUTRAL = 0.5f
    }
}

/** Folds the events of one search into an [AnalysisState]. Pure: tested without Android. */
object AnalysisReducer {
    /**
     * A line replaces the one of its rank only if it is at least as deep, so a late or restarted
     * (pre-empted, S2-1) search never makes the display go back to shallower results.
     */
    fun reduce(
        state: AnalysisState,
        event: EngineEvent,
        position: Position,
    ): AnalysisState =
        when (event) {
            is EngineEvent.Info -> withLine(state, event, position)
            is EngineEvent.BestMove -> state.copy(finished = true)
            EngineEvent.Restarted -> state.copy(finished = false)
        }

    private fun withLine(
        state: AnalysisState,
        event: EngineEvent.Info,
        position: Position,
    ): AnalysisState {
        val info = event.info
        val first =
            info.pv
                .firstOrNull()
                ?.let(Move::parseUci)
                ?.takeIf { position.isLegal(it) } ?: return state
        val current = state.lines.firstOrNull { it.rank == info.multiPv }
        if (current != null && current.depth > info.depth) return state
        val line =
            AnalysisLine(
                rank = info.multiPv,
                depth = info.depth,
                score = info.score.forWhite(position.sideToMove == Side.WHITE),
                moves = sanLine(position, info.pv.take(AnalysisLine.MAX_PLIES)),
                firstMove = first,
                moveNumber = position.fullmoveNumber,
                blackFirst = position.sideToMove == Side.BLACK,
            )
        return state.copy(lines = (state.lines.filter { it.rank != line.rank } + line).sortedBy { it.rank })
    }

    /** The variation in French SAN (only the plies shown are converted); stops at the first illegal move. */
    fun sanLine(
        from: Position,
        pv: List<String>,
    ): List<String> {
        var position = from
        val moves = mutableListOf<String>()
        for (uci in pv) {
            val move = Move.parseUci(uci)?.takeIf { position.isLegal(it) } ?: break
            moves += San.write(position, move, PieceLetters.FRENCH)
            position = position.play(move)
        }
        return moves
    }
}

/** "+0,4", "-1,2", "M3" (White mates in 3), "-M2"; always from White's point of view. */
fun formatScore(score: Score): String =
    when (score) {
        is Score.Centipawns -> String.format(Locale.FRANCE, "%+.1f", score.value / CENTIPAWNS_PER_PAWN)
        is Score.Mate -> if (score.moves < 0) "-M${-score.moves}" else "M${score.moves}"
    }

private const val CENTIPAWNS_PER_PAWN = 100.0
