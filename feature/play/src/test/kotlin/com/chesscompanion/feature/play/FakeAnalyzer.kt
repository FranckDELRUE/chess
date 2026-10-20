package com.chesscompanion.feature.play

import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.SearchRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Stands in for the engine: each search emits one line (the variation [pv], 25 cp, depth 5), then
 * waits until it is cancelled. Records the requests and how many searches were cancelled.
 */
class FakeAnalyzer(
    private val failWith: Exception? = null,
    private val pv: List<String> = listOf("g1f3"),
) : PositionAnalyzer {
    val requests = mutableListOf<SearchRequest>()
    var cancelled = 0

    /** The main dispatcher of the test: no extra thread, so the ViewModel tests stay deterministic. */
    override val workDispatcher: CoroutineDispatcher get() = Dispatchers.Main

    override fun analyze(request: SearchRequest): Flow<EngineEvent> =
        flow {
            requests += request
            failWith?.let { throw it }
            emit(EngineEvent.Info(EngineInfo(depth = 5, multiPv = 1, score = Score.Centipawns(25), pv = pv)))
            try {
                awaitCancellation()
            } finally {
                cancelled++
            }
        }
}
