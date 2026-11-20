package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.GameRules
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.Game
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.data.AnalysisJob
import com.chesscompanion.core.data.AnalysisJobState
import com.chesscompanion.core.data.AnalysisStore
import com.chesscompanion.core.data.StoredAnalysis
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.SearchLimit
import com.chesscompanion.core.engine.SearchRequest
import com.chesscompanion.core.engine.forWhite
import kotlinx.coroutines.flow.Flow

/** Runs Stockfish for the background analysis (`EnginePriority.BACKGROUND` in production); a fake in tests. */
fun interface BackgroundEngine {
    fun search(request: SearchRequest): Flow<EngineEvent>
}

/**
 * Analyses every position of a game's main line (S2-5), from the start position, at [DEPTH] with one
 * line, and keeps the results in the [AnalysisStore]. Positions where the game is over, and positions
 * already analysed at least as deep, are skipped. The progress is stored after each position, so a
 * cancelled or killed analysis resumes where it stopped. The best move and the line are checked by the
 * rules before they are stored (rule 2).
 */
class GameAnalyzer(
    private val engine: BackgroundEngine,
    private val store: AnalysisStore,
) {
    /** Analyses the game of job [jobId]; [onProgress] after each position. Returns the state reached. */
    suspend fun run(
        jobId: String,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): AnalysisJobState {
        val job = store.job(jobId)
        if (job == null || job.state == AnalysisJobState.DONE || job.state == AnalysisJobState.CANCELLED) {
            return job?.state ?: AnalysisJobState.FAILED
        }
        val game = gameOf(job) ?: return AnalysisJobState.FAILED
        val positions = listOf(game.startPosition) + game.mainline.map { it.position }
        store.updateJob(jobId, AnalysisJobState.RUNNING, job.done)
        for (index in job.done until positions.size) {
            analyse(game, positions, index)
            store.updateJob(jobId, AnalysisJobState.RUNNING, index + 1)
            onProgress(index + 1, positions.size)
        }
        store.updateJob(jobId, AnalysisJobState.DONE, positions.size)
        return AnalysisJobState.DONE
    }

    /** The game of [job]; null (and the job failed) if the rules refuse its PGN (rule 2). */
    private suspend fun gameOf(job: AnalysisJob): Game? {
        val game = runCatching { PgnReader.read(job.pgn) }.getOrNull()
        if (game == null) store.updateJob(job.id, AnalysisJobState.FAILED, job.done)
        return game
    }

    private suspend fun analyse(
        game: Game,
        positions: List<Position>,
        index: Int,
    ) {
        val position = positions[index]
        val fen = position.toFen()
        val history = positions.take(index).map { it.zobristKey }
        if (GameRules.status(position, history).isOver) return
        val known = store.position(position.zobristKey, fen)
        if (known != null && known.depth >= DEPTH) return
        val request =
            SearchRequest(
                fen = game.startPosition.toFen(),
                limit = SearchLimit.Depth(DEPTH),
                moves = game.mainline.take(index).map { it.move.uci },
            )
        var deepest: EngineInfo? = null
        var best: String? = null
        engine.search(request).collect { event ->
            when (event) {
                is EngineEvent.Info ->
                    if (event.info.multiPv == 1 && event.info.depth >= (deepest?.depth ?: 0)) deepest = event.info
                is EngineEvent.BestMove -> best = event.move
                EngineEvent.Restarted -> Unit
            }
        }
        stored(position, deepest, best)?.let { store.savePosition(position.zobristKey, it) }
    }

    /** What may be stored: a legal best move and the legal start of its line; null otherwise. */
    private fun stored(
        position: Position,
        info: EngineInfo?,
        best: String?,
    ): StoredAnalysis? {
        val move = best?.let(Move::parseUci)?.takeIf { position.isLegal(it) }
        if (info == null || move == null) return null
        val line = legalLine(position, info.pv).takeIf { it.firstOrNull() == move.uci } ?: listOf(move.uci)
        val score = info.score.forWhite(position.sideToMove == Side.WHITE)
        return StoredAnalysis(
            fen = position.toFen(),
            depth = info.depth,
            scoreCp = (score as? Score.Centipawns)?.value,
            mateIn = (score as? Score.Mate)?.moves,
            bestMove = move.uci,
            pv = line,
        )
    }

    companion object {
        const val DEPTH = 18

        /** The moves of [pv] that the rules accept one after the other, from [from]. */
        fun legalLine(
            from: Position,
            pv: List<String>,
        ): List<String> {
            var position = from
            val legal = mutableListOf<String>()
            for (uci in pv) {
                val move = Move.parseUci(uci)?.takeIf { position.isLegal(it) } ?: break
                legal += move.uci
                position = position.play(move)
            }
            return legal
        }

        /** Number of positions analysed for [game]: the start position and one per move of the main line. */
        fun positionCount(game: Game): Int = game.mainline.size + 1
    }
}
