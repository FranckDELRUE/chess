package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.data.explorer.ExplorerLookup
import com.chesscompanion.core.data.explorer.ExplorerOutcome
import com.chesscompanion.core.data.explorer.TokenStore
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EnginePriority
import com.chesscompanion.core.engine.EngineScheduler
import com.chesscompanion.core.engine.SearchLimit
import com.chesscompanion.core.engine.SearchRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * The explorer of the editor (ADR 0013): for the position shown, the statistics of the Lichess database
 * (cache first, then the network) and an evaluation of the shared Stockfish at `LIVE` priority. A new
 * position cancels the requests still running. Neither the network nor the engine is ever required:
 * a failure only changes what the panel says.
 */
internal class ExplorerController(
    private val lookup: ExplorerLookup,
    private val tokens: TokenStore,
    private val engine: EngineScheduler,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ExplorerUiState(hasToken = tokens.get() != null))
    val state: StateFlow<ExplorerUiState> = _state.asStateFlow()

    private var position: Position? = null
    private var known: Set<String> = emptySet()
    private var outcome: ExplorerOutcome? = null
    private var statsJob: Job? = null
    private var evalJob: Job? = null

    /** Shows [next]; [knownMoves] are the moves (UCI) the repertoire already has from it. */
    fun show(
        next: Position,
        knownMoves: Set<String>,
    ) {
        val samePosition = position?.toFen() == next.toFen()
        if (samePosition && known == knownMoves) return
        position = next
        known = knownMoves
        if (samePosition) {
            outcome?.let { result -> _state.update { it.copy(content = result.toContent(next, knownMoves)) } }
        } else {
            loadStats()
            loadEvaluation()
        }
    }

    fun onAction(action: ExplorerAction) {
        when (action) {
            is ExplorerAction.SetLevel -> {
                _state.update { it.copy(level = action.level) }
                loadStats()
            }
            is ExplorerAction.SaveToken -> setToken(action.text)
            ExplorerAction.ClearToken -> setToken(null)
        }
    }

    private fun setToken(text: String?) {
        tokens.set(text)
        _state.update { it.copy(hasToken = tokens.get() != null) }
        loadStats()
    }

    private fun loadStats() {
        val shown = position ?: return
        statsJob?.cancel()
        outcome = null
        _state.update { it.copy(content = ExplorerContent.Loading) }
        val level = _state.value.level
        statsJob =
            scope.launch {
                val result = lookup.lookup(shown.toFen(), level)
                outcome = result
                _state.update { it.copy(content = result.toContent(shown, known)) }
            }
    }

    private fun loadEvaluation() {
        val shown = position ?: return
        evalJob?.cancel()
        _state.update { it.copy(evaluation = null) }
        if (shown.legalMoves.isEmpty()) return
        evalJob =
            scope.launch {
                try {
                    val request = SearchRequest(shown.toFen(), SearchLimit.MoveTime(EVALUATION_MS))
                    engine.search(EnginePriority.LIVE, request).collect { event ->
                        if (event is EngineEvent.Info && event.info.multiPv == 1) {
                            val text = formatEvaluation(event.info.score, shown.sideToMove)
                            _state.update { it.copy(evaluation = text) }
                        }
                    }
                } catch (_: IOException) {
                    // No engine, no evaluation: the rest of the panel is unaffected.
                } catch (_: IllegalStateException) {
                    // Same, when the engine cannot start.
                }
            }
    }

    private companion object {
        const val EVALUATION_MS = 2_000L
    }
}
