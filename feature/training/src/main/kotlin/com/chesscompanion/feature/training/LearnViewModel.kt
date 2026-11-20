package com.chesscompanion.feature.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.toSan
import com.chesscompanion.core.data.RepertoireStore
import com.chesscompanion.core.data.ReviewCardStore
import com.chesscompanion.core.data.explorer.ExplorerLevel
import com.chesscompanion.core.data.explorer.ExplorerLookup
import com.chesscompanion.core.data.explorer.ExplorerOutcome
import com.chesscompanion.core.engine.EngineScheduler
import com.chesscompanion.core.srs.FsrsCard
import com.chesscompanion.core.srs.LessonPlanner
import com.chesscompanion.core.srs.LessonStep
import com.chesscompanion.core.srs.learnedCard
import com.chesscompanion.core.ui.board.BoardHighlights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Navigation argument: the id of the repertoire to learn (same name as the drill's). */
const val LEARN_REPERTOIRE_ID_ARG = "id"

/**
 * An opening lesson (S8b-2): walks through the player's positions of a repertoire, main line first. Each position
 * shows the repertoire move with the facts that explain it (what the author wrote, Stockfish's evaluation, the
 * Lichess statistics) and asks the player to play it; a right move learns the position (its review card is created,
 * due tomorrow). Nothing is graded and the player moves freely between positions. One entry point, [onAction].
 */
@HiltViewModel
class LearnViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val repertoires: RepertoireStore,
        private val cards: ReviewCardStore,
        engine: EngineScheduler,
        private val explorer: ExplorerLookup,
        private val clock: Clock,
    ) : ViewModel() {
        private val id: String = savedState.get<String>(LEARN_REPERTOIRE_ID_ARG).orEmpty()
        private val _state = MutableStateFlow(LearnUiState())
        val state: StateFlow<LearnUiState> = _state.asStateFlow()

        private var repertoire: Repertoire? = null
        private var name = ""
        private var steps: List<LessonStep> = emptyList()
        private var index = 0
        private var input: DrillInput? = null
        private var factsJob: Job? = null
        private val probe = EvaluationProbe(engine)

        init {
            viewModelScope.launch { start() }
        }

        private suspend fun start() {
            val stored = repertoires.load(id)
            if (stored == null) {
                _state.value = LearnUiState(status = LearnStatus.NOT_FOUND)
                return
            }
            repertoire = stored.repertoire
            name = stored.name
            val known: Map<PositionId, FsrsCard> = cards.cards(id)
            steps = LessonPlanner.plan(stored.repertoire, known)
            if (steps.isEmpty()) {
                _state.value = LearnUiState(status = LearnStatus.EMPTY, name = name)
            } else {
                index = steps.indexOfFirst { !it.learned }.coerceAtLeast(0)
                show()
            }
        }

        fun onAction(action: LearnAction) {
            when (action) {
                LearnAction.Next -> go(index + 1)
                LearnAction.Previous -> go(index - 1)
                is LearnAction.Gesture -> if (_state.value.status == LearnStatus.LESSON) gesture(action.action)
            }
        }

        private fun go(target: Int) {
            if (target !in steps.indices) return
            index = target
            show()
        }

        private fun show() {
            val graph = checkNotNull(repertoire)
            val step = steps[index]
            val position = step.node.position
            input = DrillInput(position)
            val notes = graph.notes(step.id)
            _state.value =
                LearnUiState(
                    status = LearnStatus.LESSON,
                    name = name,
                    index = index,
                    total = steps.size,
                    learned = steps.count { it.learned },
                    board = position.board,
                    flipped = graph.side == Side.BLACK,
                    highlights =
                        BoardHighlights(
                            lastMove = step.path.lastOrNull()?.move,
                            check = position.checkSquare(),
                        ),
                    annotations = expectedArrows(step.expected),
                    lineText = lineText(graph, step.path),
                    toMove = position.sideToMove,
                    facts = step.expected.map { MoveFact(position.toSan(it.move, PieceLetters.FRENCH)) },
                    comment = notes.comment,
                    source = notes.source?.toText(),
                    stepLearned = step.learned,
                )
            loadFacts(step)
        }

        /** Asks the explorer (cache first), then Stockfish for each move; the lesson works without either. */
        private fun loadFacts(step: LessonStep) {
            factsJob?.cancel()
            factsJob =
                viewModelScope.launch {
                    when (val outcome = explorer.lookup(step.node.position.toFen(), ExplorerLevel.CLUB)) {
                        is ExplorerOutcome.Loaded ->
                            _state.update {
                                it.copy(facts = it.facts.withStats(outcome.stats, step.expected), explorerLoaded = true)
                            }
                        is ExplorerOutcome.Failed -> _state.update { it.copy(explorerProblem = outcome.problem) }
                    }
                    step.expected.forEachIndexed { i, edge ->
                        val evaluation = probe.after(step.node.position, edge.move)
                        _state.update { it.copy(facts = it.facts.withEvaluation(i, evaluation)) }
                    }
                    _state.update { it.copy(evaluationsDone = true) }
                }
        }

        private fun gesture(action: DrillAction) {
            val current = input ?: return
            val step =
                when (action) {
                    is DrillAction.SquareTap -> current.tap(action.square)
                    is DrillAction.DragStart -> current.dragStart(action.square)
                    is DrillAction.Drop -> current.drop(action.from, action.to)
                    is DrillAction.Promote -> current.promote(action.type)
                    DrillAction.CancelPromotion -> DrillInputStep(current.cancelPromotion())
                    else -> return
                }
            input = step.input
            val played = step.played
            if (played == null) showInput() else play(played)
        }

        private fun showInput() {
            val current = input ?: return
            val selected = current.selected
            _state.update {
                it.copy(
                    highlights =
                        BoardHighlights(
                            selected = selected,
                            targets = selected?.let(current::targetsOf).orEmpty(),
                            lastMove = it.highlights.lastMove,
                            check = current.position.checkSquare(),
                        ),
                    promotionSide = if (current.pendingPromotion != null) current.position.sideToMove else null,
                )
            }
        }

        /** A right move learns the position; any other one changes nothing but a message. */
        private fun play(played: Move) {
            val step = steps[index]
            if (step.expected.none { it.move == played }) {
                _state.update {
                    it.copy(
                        miss = true,
                        promotionSide = null,
                        highlights = it.highlights.copy(selected = null, targets = emptySet()),
                    )
                }
                return
            }
            if (!step.learned) {
                viewModelScope.launch { cards.put(id, step.id, learnedCard(Instant.now(clock))) }
                steps = steps.mapIndexed { i, s -> if (i == index) s.copy(learned = true) else s }
            }
            _state.update {
                it.copy(
                    learned = steps.count { s -> s.learned },
                    stepLearned = true,
                    justLearned = true,
                    miss = false,
                    promotionSide = null,
                    highlights = BoardHighlights(lastMove = played),
                )
            }
        }
    }
