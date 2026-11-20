package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.toSan
import com.chesscompanion.core.data.explorer.ExplorerOutcome
import com.chesscompanion.core.data.explorer.ExplorerStats
import com.chesscompanion.core.engine.Score
import java.util.Locale
import kotlin.math.roundToInt

private const val PERCENT = 100.0
private const val CENTIPAWNS = 100.0

/**
 * The moves of [this] as rows for [position]. A move that is not legal there is left out: what comes
 * from the network never reaches the screen unchecked (rule 2), and the SAN of Lichess is not used.
 */
internal fun ExplorerStats.toRows(
    position: Position,
    known: Set<String>,
): List<ExplorerRow> =
    moves.mapNotNull { move ->
        val parsed = Move.parseUci(move.uci)?.takeIf { position.isLegal(it) } ?: return@mapNotNull null
        val games = move.games.coerceAtLeast(1)
        val white = (move.white * PERCENT / games).roundToInt()
        val draws = (move.draws * PERCENT / games).roundToInt()
        ExplorerRow(
            uci = move.uci,
            label = position.toSan(parsed, PieceLetters.FRENCH),
            games = move.games,
            whitePercent = white,
            drawPercent = draws,
            blackPercent = if (move.games == 0L) 0 else (100 - white - draws).coerceAtLeast(0),
            known = move.uci in known,
        )
    }

internal fun ExplorerOutcome.toContent(
    position: Position,
    known: Set<String>,
): ExplorerContent =
    when (this) {
        is ExplorerOutcome.Loaded -> ExplorerContent.Loaded(stats.toRows(position, known), stats.games, fromCache)
        is ExplorerOutcome.Failed -> ExplorerContent.Problem(problem)
    }

/** [score] (from the side to move) as White sees it: `+0,35`, `-1,20`, `#3`, `-#2`. */
internal fun formatEvaluation(
    score: Score,
    sideToMove: Side,
): String {
    val sign = if (sideToMove == Side.WHITE) 1 else -1
    return when (score) {
        is Score.Centipawns -> String.format(Locale.FRANCE, "%+.2f", sign * score.value / CENTIPAWNS)
        is Score.Mate -> (sign * score.moves).let { if (it < 0) "-#${-it}" else "#$it" }
    }
}
