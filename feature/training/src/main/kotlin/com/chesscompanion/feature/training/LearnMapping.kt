package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import com.chesscompanion.core.data.explorer.ExplorerStats
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.Arrow
import com.chesscompanion.core.ui.board.BoardAnnotations

private const val PERCENT = 100L

/** Green arrows for the repertoire moves of the lesson position. */
internal fun expectedArrows(expected: List<RepertoireEdge>): BoardAnnotations =
    BoardAnnotations(arrows = expected.associate { Arrow(it.move.from, it.move.to) to AnnotationColor.GREEN })

/** The facts with the database figures of each move of [expected] (same order); 0 games if never played. */
internal fun List<MoveFact>.withStats(
    stats: ExplorerStats,
    expected: List<RepertoireEdge>,
): List<MoveFact> =
    mapIndexed { i, fact ->
        val played = stats.moves.firstOrNull { it.uci == expected[i].move.uci }
        when {
            stats.games == 0L -> fact
            played == null || played.games == 0L -> fact.copy(games = 0, percent = 0)
            else ->
                fact.copy(
                    games = played.games,
                    percent = (played.games * PERCENT / stats.games).toInt(),
                    whitePercent = (played.white * PERCENT / played.games).toInt(),
                    drawPercent = (played.draws * PERCENT / played.games).toInt(),
                    blackPercent = (played.black * PERCENT / played.games).toInt(),
                )
        }
    }

/** The facts with Stockfish's evaluation of the move number [index]. */
internal fun List<MoveFact>.withEvaluation(
    index: Int,
    evaluation: String?,
): List<MoveFact> = mapIndexed { i, fact -> if (i == index) fact.copy(evaluation = evaluation) else fact }
