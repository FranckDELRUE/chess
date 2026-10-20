package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.SearchLimit
import com.chesscompanion.core.engine.SearchRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Runs a search for the live analysis (`EnginePriority.LIVE` in production); a fake in tests. */
fun interface PositionAnalyzer {
    fun analyze(request: SearchRequest): Flow<EngineEvent>

    /**
     * Where the events are folded into the displayed lines (SAN needs move generation, which must not
     * run on the main thread while the engine streams results: measured on the tablet in S2-3).
     */
    val workDispatcher: CoroutineDispatcher get() = Dispatchers.Default
}

/**
 * Live analysis of the position shown (S2-3). While enabled, every new position cancels the search
 * in progress and starts another after [debounceMs] (fast navigation does not reach the engine).
 * Three lines, depth up to [MAX_DEPTH]; a position where the game is over is not searched.
 */
class AnalysisController(
    private val analyzer: PositionAnalyzer,
    private val scope: CoroutineScope,
    private val debounceMs: Long = DEBOUNCE_MS,
    private val work: CoroutineDispatcher = analyzer.workDispatcher,
) {
    private val _state = MutableStateFlow(AnalysisState())
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    private var enabled = false
    private var position: Position? = null
    private var gameOver = false
    private var job: Job? = null

    fun setEnabled(value: Boolean) {
        if (value == enabled) return
        enabled = value
        restart()
    }

    /**
     * Handles an analysis action on the position [shown] by the game; returns the move to play when a
     * line was tapped and it is still legal there (the line belongs to the position it was computed for).
     */
    fun accept(
        action: PlayAction.Analysis,
        shown: Position,
    ): Move? =
        when (action) {
            PlayAction.ToggleAnalysis -> {
                setEnabled(!enabled)
                null
            }
            is PlayAction.PlayAnalysisMove ->
                action.move.takeIf { _state.value.fen == shown.toFen() && shown.isLegal(it) }
        }

    /** The position now shown; searching again only if it is a different one. */
    fun show(
        next: Position,
        over: Boolean,
    ) {
        val same = position?.toFen() == next.toFen() && gameOver == over
        position = next
        gameOver = over
        if (!same) restart()
    }

    private fun restart() {
        job?.cancel()
        val shown = position
        if (!enabled || shown == null) {
            _state.value = AnalysisState(enabled = enabled)
            return
        }
        _state.value = AnalysisState(enabled = true, fen = shown.toFen(), gameOver = gameOver)
        if (gameOver) return
        job =
            scope.launch {
                delay(debounceMs)
                try {
                    // Folded on [work]; the screen gets the latest state at most every [UI_PERIOD_MS]
                    // (the engine sends hundreds of lines in the first fraction of a second).
                    analyzer
                        .analyze(request(shown))
                        .scan(_state.value) { state, event -> AnalysisReducer.reduce(state, event, shown) }
                        .flowOn(work)
                        .conflate()
                        .collect {
                            _state.value = it
                            delay(UI_PERIOD_MS)
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    _state.update { it.copy(error = e.message ?: e.toString(), finished = true) }
                }
            }
    }

    private fun request(position: Position) =
        SearchRequest(position.toFen(), SearchLimit.Depth(MAX_DEPTH), multiPv = LINES)

    companion object {
        const val LINES = 3
        const val MAX_DEPTH = 24
        const val DEBOUNCE_MS = 150L
        const val UI_PERIOD_MS = 250L
    }
}
