package com.chesscompanion.feature.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.toSan
import com.chesscompanion.core.data.RepertoireStore
import com.chesscompanion.core.data.explorer.ExplorerProblem
import com.chesscompanion.core.data.explorer.GapFinder
import com.chesscompanion.core.data.explorer.GapReport
import com.chesscompanion.core.data.explorer.GapSettings
import com.chesscompanion.core.data.explorer.LineCompleter
import com.chesscompanion.core.data.explorer.RepertoireGap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Navigation argument: the id of the repertoire to scan (same name as the drill's). */
const val GAPS_REPERTOIRE_ID_ARG = "id"

enum class GapsStatus { LOADING, NOT_FOUND, IDLE, SCANNING, DONE }

/** A frequent reply without an answer: the line to reach the position, the missing move, how common it is. */
data class GapRow(
    val line: String,
    val moveSan: String,
    val games: Long,
    val percent: Int,
    /** The line is being completed (the explorer is asked move after move). */
    val adding: Boolean = false,
    /** The line was added to the repertoire (by a tap on the row): [addedMoves] moves, both sides. */
    val added: Boolean = false,
    val addedMoves: Int = 0,
    /** Why the continuation stopped early (no token, offline...); the gap move itself was added anyway. */
    val addProblem: ExplorerProblem? = null,
)

data class GapsUiState(
    val status: GapsStatus = GapsStatus.LOADING,
    val name: String = "",
    val done: Int = 0,
    val total: Int = 0,
    val gaps: List<GapRow> = emptyList(),
    val scanned: Int = 0,
    val available: Int = 0,
    /** More positions exist than the scan covers. */
    val truncated: Boolean = false,
    /** Why the scan stopped early (no token, offline...), if it did. */
    val problem: ExplorerProblem? = null,
)

sealed interface GapsAction {
    data object Scan : GapsAction

    data object Cancel : GapsAction

    /** Adds the missing move of the row [index] to the repertoire and completes the line after it. */
    data class Add(
        val index: Int,
    ) : GapsAction
}

/**
 * The holes of a repertoire (S8-6): looks up, in the Lichess explorer (cache first), the positions where the opponent
 * replies and lists the frequent replies the repertoire does not answer. Started by the player, cancellable; the
 * rest of the app never depends on it.
 */
@HiltViewModel
class GapsViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val repertoires: RepertoireStore,
        private val finder: GapFinder,
        private val completer: LineCompleter,
    ) : ViewModel() {
        private val id: String = savedState.get<String>(GAPS_REPERTOIRE_ID_ARG).orEmpty()
        private val _state = MutableStateFlow(GapsUiState())
        val state: StateFlow<GapsUiState> = _state.asStateFlow()

        private var repertoire: Repertoire? = null
        private var job: Job? = null
        private var found: List<RepertoireGap> = emptyList()

        /** Thresholds of the scan; tests remove the pause between network requests. */
        internal var settings = GapSettings()

        init {
            viewModelScope.launch {
                val stored = repertoires.load(id)
                if (stored == null) {
                    _state.value = GapsUiState(status = GapsStatus.NOT_FOUND)
                } else {
                    repertoire = stored.repertoire
                    _state.value = GapsUiState(status = GapsStatus.IDLE, name = stored.name)
                }
            }
        }

        fun onAction(action: GapsAction) {
            when (action) {
                GapsAction.Scan -> scan()
                GapsAction.Cancel -> cancel()
                is GapsAction.Add -> add(action.index)
            }
        }

        private fun scan() {
            val graph = repertoire ?: return
            if (_state.value.status == GapsStatus.SCANNING) return
            _state.update {
                it.copy(
                    status = GapsStatus.SCANNING,
                    done = 0,
                    total = 0,
                    gaps = emptyList(),
                    problem = null,
                )
            }
            job =
                viewModelScope.launch {
                    val report =
                        finder.find(graph, settings) { done, total ->
                            _state.update { it.copy(done = done, total = total) }
                        }
                    found = report.gaps
                    _state.update { it.copy(status = GapsStatus.DONE).withReport(report, graph) }
                }
        }

        /**
         * Adds the opponent's missing move to the repertoire, then the most played moves of both sides after it (the
         * player's answer and the rest of the line, from the Lichess explorer), every move checked by the rules engine,
         * and saves. Without the explorer only the gap move is added and the row says why.
         */
        private fun add(index: Int) {
            if (!canAdd(index)) return
            val gap = found.getOrNull(index) ?: return
            val graph = repertoire ?: return
            updateRow(index) { it.copy(adding = true) }
            viewModelScope.launch {
                val completed = completer.complete(graph, gap.position, gap.move, settings)
                repertoire = completed.repertoire
                repertoires.save(id, completed.repertoire)
                updateRow(index) {
                    it.copy(
                        adding = false,
                        added = true,
                        addedMoves = completed.addedMoves,
                        addProblem = completed.problem,
                    )
                }
            }
        }

        /** The row exists, is not added yet and no other line is being completed (saves go one at a time). */
        private fun canAdd(index: Int): Boolean {
            val rows = _state.value.gaps
            return rows.getOrNull(index)?.added == false && rows.none { it.adding }
        }

        private fun updateRow(
            index: Int,
            change: (GapRow) -> GapRow,
        ) = _state.update { state ->
            state.copy(gaps = state.gaps.mapIndexed { i, row -> if (i == index) change(row) else row })
        }

        private fun cancel() {
            job?.cancel()
            _state.update { it.copy(status = GapsStatus.IDLE) }
        }
    }

private fun GapsUiState.withReport(
    report: GapReport,
    repertoire: Repertoire,
): GapsUiState =
    copy(
        gaps =
            report.gaps.map { gap ->
                val position = checkNotNull(repertoire.node(gap.position)).position
                GapRow(
                    line = lineText(repertoire, gap.path),
                    moveSan = position.toSan(gap.move, PieceLetters.FRENCH),
                    games = gap.games,
                    percent = gap.percent,
                )
            },
        scanned = report.scanned,
        available = report.available,
        truncated = report.truncated,
        problem = report.problem,
    )
