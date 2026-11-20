package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.data.AnalysisJob
import com.chesscompanion.core.data.StoredAnalysis
import kotlinx.coroutines.flow.MutableStateFlow

/** The background analysis queue without WorkManager: records what the screen asks, serves stored lines. */
class FakeQueue : GameAnalysisQueue {
    override val latest = MutableStateFlow<AnalysisJob?>(null)
    val enqueued = mutableListOf<String>()
    var cancelled = 0
    val stored = mutableMapOf<Long, StoredAnalysis>()

    override suspend fun enqueue(pgn: String) {
        enqueued += pgn
    }

    override suspend fun cancelAll() {
        cancelled++
    }

    override suspend fun stored(position: Position): StoredAnalysis? = stored[position.zobristKey]
}
