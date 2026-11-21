package com.chesscompanion.core.data.explorer

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import com.chesscompanion.core.chess.repertoire.signature
import kotlinx.coroutines.delay

/**
 * A reply the opponent plays often that the repertoire does not answer.
 *
 * @property position where the opponent is to move; [path] leads to it from the repertoire root.
 * @property move the frequent reply, checked legal by the rules engine.
 * @property games games of the explorer in which [move] was played from [position].
 * @property percent share of the games that reached [position].
 */
data class RepertoireGap(
    val position: PositionId,
    val path: List<RepertoireEdge>,
    val move: Move,
    val games: Long,
    val percent: Int,
)

/**
 * What a scan found. [scanned] positions were looked up out of [available] opponent positions of the repertoire;
 * [problem] is why the scan stopped early (no token, offline, rate limit...), null if it went through.
 * [signature] are the starting moves of the opening (S8b-5) and [ignored] the opponent positions left out of the scan
 * because a reply there would leave it.
 */
data class GapReport(
    val gaps: List<RepertoireGap>,
    val scanned: Int,
    val available: Int,
    val problem: ExplorerProblem?,
    val signature: List<RepertoireEdge> = emptyList(),
    val ignored: Int = 0,
) {
    /** More positions exist than were scanned (the limit was reached). */
    val truncated: Boolean get() = problem == null && scanned < available
}

/** Thresholds and limits of a scan. */
data class GapSettings(
    val level: ExplorerLevel = ExplorerLevel.CLUB,
    /** A reply counts as frequent from this share of the games of its position... */
    val minPercent: Int = DEFAULT_MIN_PERCENT,
    /** ...and from this number of games, so that rare positions do not produce noise. */
    val minGames: Long = DEFAULT_MIN_GAMES,
    /** Positions looked up in one scan, nearest to the root first (the network is not hammered). */
    val maxPositions: Int = DEFAULT_MAX_POSITIONS,
    /** Pause after each answer that came from the network, in milliseconds (none for one from the cache). */
    val pauseMs: Long = DEFAULT_PAUSE_MS,
    /** Plies (both sides) added when a gap is completed; a line always ends on the player's move. */
    val completionPlies: Int = DEFAULT_COMPLETION_PLIES,
    /** A move is followed when it was played in at least this many games. */
    val completionMinGames: Long = DEFAULT_COMPLETION_MIN_GAMES,
    /**
     * Also scan the positions where a reply would leave the opening (1...c5 in a Scotch repertoire). Off by default:
     * those replies belong to another opening (S8b-5).
     */
    val includeOffOpening: Boolean = false,
) {
    companion object {
        const val DEFAULT_MIN_PERCENT = 5
        const val DEFAULT_MIN_GAMES = 100L
        const val DEFAULT_MAX_POSITIONS = 40
        const val DEFAULT_PAUSE_MS = 300L
        const val DEFAULT_COMPLETION_PLIES = 12
        const val DEFAULT_COMPLETION_MIN_GAMES = 50L
    }
}

/**
 * Finds the holes of a repertoire (S8-6): for each position where the opponent is to move, the replies that are
 * frequent in the Lichess database and have no answer in the repertoire. The explorer is the only source of
 * frequencies and each reply is validated by the rules engine (rule 2). Needs a token or a cached position;
 * without them the scan stops at the first problem and says why (the rest of the app never depends on it).
 */
class GapFinder(
    private val explorer: ExplorerLookup,
) {
    /** Scans [repertoire]; [onProgress] gets (positions scanned, positions to scan). Cancellable. */
    suspend fun find(
        repertoire: Repertoire,
        settings: GapSettings = GapSettings(),
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): GapReport {
        val signature = repertoire.signature()
        val all = opponentPositions(repertoire)
        val targets = if (settings.includeOffOpening) all else all.filter { (id, _) -> signature.covers(id) }
        val toScan = targets.take(settings.maxPositions)
        val gaps = mutableListOf<RepertoireGap>()
        var scanned = 0
        var problem: ExplorerProblem? = null
        for ((id, path) in toScan) {
            val node = checkNotNull(repertoire.node(id))
            when (val outcome = lookup(node.position.toFen(), settings)) {
                is ExplorerOutcome.Failed -> {
                    problem = outcome.problem
                    break
                }
                is ExplorerOutcome.Loaded -> gaps += gapsOf(repertoire, id, path, outcome.stats, settings)
            }
            scanned++
            onProgress(scanned, toScan.size)
        }
        return GapReport(
            gaps = gaps.sortedByDescending { it.games },
            scanned = scanned,
            available = targets.size,
            problem = problem,
            signature = signature.edges,
            ignored = all.size - targets.size,
        )
    }

    private suspend fun lookup(
        fen: String,
        settings: GapSettings,
    ): ExplorerOutcome {
        // An answer from the cache costs no network; after one that came from the network, pause before the next.
        val outcome = explorer.lookup(fen, settings.level)
        if (outcome is ExplorerOutcome.Loaded && !outcome.fromCache && settings.pauseMs > 0) delay(settings.pauseMs)
        return outcome
    }

    private fun gapsOf(
        repertoire: Repertoire,
        id: PositionId,
        path: List<RepertoireEdge>,
        stats: ExplorerStats,
        settings: GapSettings,
    ): List<RepertoireGap> {
        val position = checkNotNull(repertoire.node(id)).position
        val answered = repertoire.movesFrom(id).mapTo(mutableSetOf()) { it.move.uci }
        val total = stats.games
        if (total < settings.minGames) return emptyList()
        return stats.moves.mapNotNull { move ->
            val parsed = Move.parseUci(move.uci)?.takeIf { position.isLegal(it) } ?: return@mapNotNull null
            val percent = (move.games * PERCENT / total).toInt()
            if (move.uci in answered || percent < settings.minPercent || move.games < settings.minGames) {
                null
            } else {
                RepertoireGap(id, path, parsed, move.games, percent)
            }
        }
    }

    /** Positions where the opponent is to move, nearest to the root first, each with its shortest path. */
    private fun opponentPositions(repertoire: Repertoire): List<Pair<PositionId, List<RepertoireEdge>>> {
        val paths = mutableMapOf<PositionId, List<RepertoireEdge>>(repertoire.root to emptyList())
        val queue = ArrayDeque<PositionId>().apply { add(repertoire.root) }
        val found = mutableListOf<Pair<PositionId, List<RepertoireEdge>>>()
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            val node = repertoire.node(id) ?: continue
            val path = paths.getValue(id)
            if (node.position.sideToMove != repertoire.side) found += id to path
            for (edge in repertoire.movesFrom(id)) {
                if (edge.to !in paths) {
                    paths[edge.to] = path + edge
                    queue.add(edge.to)
                }
            }
        }
        return found
    }

    private companion object {
        const val PERCENT = 100L
    }
}
