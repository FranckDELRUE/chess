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
    /** The move was added to the repertoire (by a tap on the row). */
    val added: Boolean = false,
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

    /** Adds the missing move of the row [index] to the repertoire. */
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
         * Adds the opponent's missing move to the repertoire (checked legal by the rules engine) and saves it. Only
         * the opponent's move is added: the player's answer to it is still to be chosen in the editor.
         */
        private fun add(index: Int) {
            val graph = repertoire ?: return
            val gap = found.getOrNull(index) ?: return
            if (_state.value.gaps
                    .getOrNull(index)
                    ?.added != false
            ) {
                return
            }
            val updated = graph.add(gap.position, gap.move).repertoire
            repertoire = updated
            _state.update { state ->
                state.copy(gaps = state.gaps.mapIndexed { i, row -> if (i == index) row.copy(added = true) else row })
            }
            viewModelScope.launch { repertoires.save(id, updated) }
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
