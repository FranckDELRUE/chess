package com.chesscompanion.enginedebug

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesscompanion.core.chess.FenParser
import com.chesscompanion.core.chess.InvalidFenException
import com.chesscompanion.core.engine.ChessEngine
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.Score
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DebugStatus { IDLE, STARTING, ANALYZING, BATCH, THERMAL, ERROR }

data class EngineDebugState(
    val fen: String = FenParser.START_POSITION,
    val depth: Int = 16,
    val threads: Int = EngineProvider.DEFAULT_THREADS,
    val status: DebugStatus = DebugStatus.IDLE,
    val info: EngineInfo? = null,
    val bestMove: String? = null,
    val elapsedMs: Long = 0,
    val log: List<String> = emptyList(),
    val error: String? = null,
) {
    val busy: Boolean get() = status != DebugStatus.IDLE && status != DebugStatus.ERROR
}

@HiltViewModel
class EngineDebugViewModel
    @Inject
    constructor(
        private val provider: EngineProvider,
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _state = MutableStateFlow(EngineDebugState())
        val state: StateFlow<EngineDebugState> = _state.asStateFlow()
        private var job: Job? = null

        fun setFen(fen: String) = _state.update { it.copy(fen = fen) }

        fun setDepth(depth: Int) = _state.update { it.copy(depth = depth) }

        fun setThreads(threads: Int) {
            _state.update { it.copy(threads = threads) }
            viewModelScope.launch { runCatching { provider.get().setOption("Threads", threads.toString()) } }
        }

        fun analyze() {
            val fen = _state.value.fen.trim()
            try {
                FenParser.parseBoard(fen)
            } catch (e: InvalidFenException) {
                _state.update { it.copy(status = DebugStatus.ERROR, error = e.message) }
                return
            }
            launchJob(DebugStatus.ANALYZING) { engine ->
                val result = runSearch(engine, fen, _state.value.depth)
                _state.update { it.copy(info = result.info, bestMove = result.bestMove, elapsedMs = result.timeMs) }
            }
        }

        /** Cancels the running job; the engine receives `stop` through flow cancellation. */
        fun stop() {
            job?.cancel()
        }

        /** 10 positions x depths 16/18/20, one log line per cell (also in Logcat, tag SF_BENCH). */
        fun runBatch() =
            launchJob(DebugStatus.BATCH) { engine ->
                log("threads=${_state.value.threads} temp=${DeviceProbe.batteryTempC(context)}C")
                BenchmarkPositions.FENS.forEachIndexed { index, fen ->
                    BenchmarkPositions.DEPTHS.forEach { depth ->
                        engine.newGame()
                        val r = runSearch(engine, fen, depth)
                        log("pos=${index + 1} depth=$depth ${r.summary()} pss=${DeviceProbe.pssMb()}MB")
                    }
                }
                log("done temp=${DeviceProbe.batteryTempC(context)}C")
            }

        /** Back-to-back depth-20 searches for 5 minutes: heat and sustained speed. */
        fun runThermal() =
            launchJob(DebugStatus.THERMAL) { engine ->
                val start = System.nanoTime()
                log("thermal start threads=${_state.value.threads} temp=${DeviceProbe.batteryTempC(context)}C")
                var round = 0
                while (System.nanoTime() - start < THERMAL_DURATION_NS) {
                    val fen = BenchmarkPositions.FENS[round % BenchmarkPositions.FENS.size]
                    engine.newGame()
                    val r = runSearch(engine, fen, THERMAL_DEPTH)
                    round++
                    val seconds = (System.nanoTime() - start) / NS_PER_S
                    log("round=$round t=${seconds}s ${r.summary()} temp=${DeviceProbe.batteryTempC(context)}C")
                }
                log(
                    "thermal done rounds=$round temp=${DeviceProbe.batteryTempC(
                        context,
                    )}C pss=${DeviceProbe.pssMb()}MB",
                )
            }

        private fun launchJob(
            status: DebugStatus,
            block: suspend (ChessEngine) -> Unit,
        ) {
            if (_state.value.busy) return
            _state.update {
                it.copy(
                    status = DebugStatus.STARTING,
                    error = null,
                    log =
                        if (status ==
                            DebugStatus.ANALYZING
                        ) {
                            it.log
                        } else {
                            emptyList()
                        },
                )
            }
            job =
                viewModelScope.launch {
                    try {
                        val engine = provider.get()
                        _state.update { it.copy(status = status) }
                        block(engine)
                        _state.update { it.copy(status = DebugStatus.IDLE) }
                    } catch (e: CancellationException) {
                        _state.update { it.copy(status = DebugStatus.IDLE) }
                        throw e
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        _state.update { it.copy(status = DebugStatus.ERROR, error = e.message ?: e.toString()) }
                    }
                }
        }

        private class SearchResult(
            val info: EngineInfo?,
            val bestMove: String?,
            val timeMs: Long,
        ) {
            fun summary(): String {
                val score =
                    when (val s = info?.score) {
                        is Score.Centipawns -> "cp ${s.value}"
                        is Score.Mate -> "mate ${s.moves}"
                        null -> "-"
                    }
                return "time=${timeMs}ms nodes=${info?.nodes} nps=${info?.nodesPerSecond} score=$score best=$bestMove"
            }
        }

        private suspend fun runSearch(
            engine: ChessEngine,
            fen: String,
            depth: Int,
        ): SearchResult {
            val start = System.nanoTime()
            var last: EngineInfo? = null
            var best: String? = null
            engine.analyze(fen, depth).collect { event ->
                when (event) {
                    is EngineEvent.Info -> {
                        last = event.info
                        _state.update {
                            it.copy(
                                info = event.info,
                                elapsedMs = (System.nanoTime() - start) / NS_PER_MS,
                            )
                        }
                    }
                    is EngineEvent.BestMove -> best = event.move
                }
            }
            return SearchResult(last, best, (System.nanoTime() - start) / NS_PER_MS)
        }

        private fun log(line: String) {
            Log.i(TAG, line)
            _state.update { it.copy(log = it.log + line) }
        }

        private companion object {
            const val TAG = "SF_BENCH"
            const val THERMAL_DEPTH = 20
            const val NS_PER_MS = 1_000_000L
            const val NS_PER_S = 1_000_000_000L
            const val THERMAL_DURATION_NS = 5 * 60 * NS_PER_S
        }
    }
