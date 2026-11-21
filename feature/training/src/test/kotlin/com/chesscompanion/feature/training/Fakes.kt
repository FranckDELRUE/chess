package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.data.RepertoireStore
import com.chesscompanion.core.data.RepertoireSummary
import com.chesscompanion.core.data.ReviewCardStore
import com.chesscompanion.core.data.StoredRepertoire
import com.chesscompanion.core.engine.ChessEngine
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.EngineScheduler
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.SearchRequest
import com.chesscompanion.core.srs.FsrsCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.time.Instant

/** A repertoire merged from one PGN game per argument (each given headers so that games stay separate). */
fun repertoireOf(
    side: Side,
    vararg games: String,
): Repertoire {
    val pgn = games.joinToString("\n\n") { "[Event \"test\"]\n\n$it" }
    return PgnReader.readAll(pgn).games.fold(Repertoire.empty(side)) { r, g -> r.merge(g).repertoire }
}

/** One repertoire in memory, under [id]. */
class FakeRepertoireStore(
    private val id: String,
    private val name: String,
    private var repertoire: Repertoire,
) : RepertoireStore {
    /** Repertoires written by [save], oldest first. */
    val saved = mutableListOf<Repertoire>()

    override fun repertoires(): Flow<List<RepertoireSummary>> = emptyFlow()

    override suspend fun create(
        name: String,
        side: Side,
        start: Position,
    ): String = error("unused")

    override suspend fun load(id: String): StoredRepertoire? =
        if (id == this.id) StoredRepertoire(id, name, repertoire) else null

    override suspend fun save(
        id: String,
        repertoire: Repertoire,
    ) {
        this.repertoire = repertoire
        saved += repertoire
    }

    override suspend fun rename(
        id: String,
        name: String,
    ) = error("unused")

    override suspend fun delete(id: String) = error("unused")
}

class FakeReviewCardStore : ReviewCardStore {
    val stored = mutableMapOf<Pair<String, PositionId>, FsrsCard>()

    override suspend fun get(
        repertoireId: String,
        position: PositionId,
    ): FsrsCard? = stored[repertoireId to position]

    override suspend fun cards(repertoireId: String): Map<PositionId, FsrsCard> =
        stored.filterKeys { it.first == repertoireId }.mapKeys { it.key.second }

    override suspend fun put(
        repertoireId: String,
        position: PositionId,
        card: FsrsCard,
    ) {
        stored[repertoireId to position] = card
    }

    override suspend fun dueCount(
        repertoireId: String,
        now: Instant,
    ): Int = cards(repertoireId).values.count { it.isDue(now) }

    override suspend fun retainOnly(
        repertoireId: String,
        keep: Set<PositionId>,
    ): Int = 0

    override suspend fun deleteAll(repertoireId: String) = Unit
}

/** Engine that reports a score (default [defaultCp]) for any position and remembers the positions asked. */
class FakeEngine(
    var defaultCp: Int = 20,
    var fail: Boolean = false,
) : ChessEngine {
    val searched = mutableListOf<String>()

    override suspend fun start(options: Map<String, String>) = Unit

    override suspend fun setOption(
        name: String,
        value: String,
    ) = Unit

    override suspend fun newGame() = Unit

    override fun analyze(request: SearchRequest): Flow<EngineEvent> =
        flow {
            searched += request.fen
            check(!fail) { "engine cannot start" }
            emit(EngineEvent.Info(EngineInfo(depth = 10, score = Score.Centipawns(defaultCp))))
            emit(EngineEvent.BestMove("e2e4"))
        }

    override suspend fun close() = Unit
}

fun scheduler(engine: ChessEngine = FakeEngine()) = EngineScheduler { engine }
