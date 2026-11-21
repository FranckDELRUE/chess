package com.chesscompanion.core.data.explorer

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import kotlinx.coroutines.delay

/**
 * A repertoire completed after a gap.
 *
 * @property addedMoves moves added to [repertoire]: the gap move and the continuation after it.
 * @property problem why the continuation stopped early (no token, offline...), null if it ended by itself.
 */
data class CompletedLine(
    val repertoire: Repertoire,
    val addedMoves: Int,
    val problem: ExplorerProblem?,
)

/**
 * Completes a line after a gap (S8b-1): adds the opponent's missing move, then follows the most played move of the
 * Lichess explorer for both sides, so that the repertoire gets an answer to it and the line goes on as far as the
 * theory does. Every move is validated by the rules engine (rule 2). The gap move itself is always added; the
 * continuation stops at a position the explorer lacks or knows too little (few games), at a position the repertoire
 * already has, when a limit is reached, or at the first explorer problem (then it says why).
 */
class LineCompleter(
    private val explorer: ExplorerLookup,
) {
    suspend fun complete(
        repertoire: Repertoire,
        from: PositionId,
        gapMove: Move,
        settings: GapSettings = GapSettings(),
    ): CompletedLine {
        var current = repertoire.add(from, gapMove)
        var added = 1
        var problem: ExplorerProblem? = null
        while (current.created && !reachedLimit(current.repertoire, current.target, added, settings)) {
            val next = nextMove(current.repertoire, current.target, settings)
            problem = next.problem
            val move = next.move ?: break
            current = current.repertoire.add(current.target, move)
            added++
        }
        return CompletedLine(current.repertoire, added, problem)
    }

    /** The move to follow from [at]; a null move ends the line, with a problem when the explorer failed. */
    private suspend fun nextMove(
        repertoire: Repertoire,
        at: PositionId,
        settings: GapSettings,
    ): Next {
        val position = checkNotNull(repertoire.node(at)).position
        return when (val outcome = explorer.lookup(position.toFen(), settings.level)) {
            is ExplorerOutcome.Failed -> Next(null, outcome.problem)
            is ExplorerOutcome.Loaded -> {
                if (!outcome.fromCache && settings.pauseMs > 0) delay(settings.pauseMs)
                val best =
                    outcome.stats.moves
                        .filter { it.games >= settings.completionMinGames }
                        .sortedByDescending { it.games }
                        .firstNotNullOfOrNull { move -> Move.parseUci(move.uci)?.takeIf { position.isLegal(it) } }
                Next(best, null)
            }
        }
    }

    private class Next(
        val move: Move?,
        val problem: ExplorerProblem?,
    )

    /** The depth limit only applies once the player has answered: a line never stops on the opponent's move. */
    private fun reachedLimit(
        repertoire: Repertoire,
        at: PositionId,
        added: Int,
        settings: GapSettings,
    ): Boolean {
        val position = checkNotNull(repertoire.node(at)).position
        return position.sideToMove != repertoire.side && added >= settings.completionPlies
    }
}
