package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.data.explorer.ExplorerLevel
import com.chesscompanion.core.data.explorer.ExplorerLookup
import com.chesscompanion.core.data.explorer.ExplorerOutcome
import com.chesscompanion.core.data.explorer.ExplorerProblem
import com.chesscompanion.core.data.explorer.TokenStore
import com.chesscompanion.core.engine.ChessEngine
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.EngineScheduler
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.SearchRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** [ExplorerLookup] that answers [outcome] and remembers what was asked. */
class FakeExplorer(
    var outcome: ExplorerOutcome = ExplorerOutcome.Failed(ExplorerProblem.NO_TOKEN),
) : ExplorerLookup {
    val asked = mutableListOf<Pair<String, ExplorerLevel>>()

    override suspend fun lookup(
        fen: String,
        level: ExplorerLevel,
    ): ExplorerOutcome {
        asked += fen to level
        return outcome
    }
}

class FakeTokens(
    var value: String? = null,
) : TokenStore {
    override fun get() = value

    override fun set(token: String?) {
        value = token?.takeIf { it.isNotBlank() }
    }
}

/** Engine that reports [infos] then a best move, and remembers the positions it was asked about. */
class FakeEngine(
    var infos: List<EngineInfo> = emptyList(),
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
            infos.forEach { emit(EngineEvent.Info(it)) }
            emit(EngineEvent.BestMove("e2e4"))
        }

    override suspend fun close() = Unit
}

fun scheduler(engine: ChessEngine = FakeEngine()) = EngineScheduler { engine }

fun centipawns(value: Int) = EngineInfo(depth = 12, score = Score.Centipawns(value))
