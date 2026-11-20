package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import com.chesscompanion.core.chess.toSan
import com.chesscompanion.core.engine.Score
import java.util.Locale

private const val CENTIPAWNS = 100.0

/** `1. d4 d5 2. Cf3`, in French letters, for [edges] played one after the other from the repertoire root. */
internal fun lineText(
    repertoire: Repertoire,
    edges: List<RepertoireEdge>,
): String {
    val start = checkNotNull(repertoire.node(repertoire.root)).position
    var number = start.fullmoveNumber
    var white = start.sideToMove == Side.WHITE
    val tokens = mutableListOf<String>()
    edges.forEachIndexed { index, edge ->
        val before = checkNotNull(repertoire.node(edge.from)).position
        if (white) {
            tokens += "$number."
        } else if (index == 0) {
            tokens += "$number..."
        }
        tokens += before.toSan(edge.move, PieceLetters.FRENCH)
        if (!white) number++
        white = !white
    }
    return tokens.joinToString(" ")
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
