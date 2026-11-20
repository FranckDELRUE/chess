package com.chesscompanion.feature.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.toSan
import com.chesscompanion.core.data.RepertoireStore
import com.chesscompanion.core.data.ReviewCardStore
import com.chesscompanion.core.engine.EngineScheduler
import com.chesscompanion.core.srs.DrillOutcome
import com.chesscompanion.core.srs.DrillResult
import com.chesscompanion.core.srs.DrillSession
import com.chesscompanion.core.srs.LearnResult
import com.chesscompanion.core.srs.ReviewItem
import com.chesscompanion.core.srs.ReviewQueueBuilder
import com.chesscompanion.core.ui.board.BoardAnnotations
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

/** Navigation argument: the id of the repertoire to drill. */
const val DRILL_REPERTOIRE_ID_ARG = "id"

/**
 * The drill of one repertoire (S8-4): asks the daily queue position by position (S8-3), stores each graded
 * answer as a review card, and after a miss tells the facts: the expected move, the move played, the line and
 * Stockfish's evaluation of both (shared engine at `LIVE` priority; no engine, no evaluation, nothing else changes).
 * One entry point, [onAction].
 */
@HiltViewModel
class DrillViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val repertoires: RepertoireStore,
        private val cards: ReviewCardStore,
        private val engine: EngineScheduler,
        private val clock: Clock,
    ) : ViewModel() {
        private val id: String = savedState.get<String>(DRILL_REPERTOIRE_ID_ARG).orEmpty()
        private val _state = MutableStateFlow(DrillUiState())
        val state: StateFlow<DrillUiState> = _state.asStateFlow()

        private var repertoire: Repertoire? = null
        private var session: DrillSession? = null
        private var input: DrillInput? = null
        private var hintUsed = false
        private var summary = DrillSummary()
        private var leftDue = 0
        private var leftNew = 0
        private var evalJob: Job? = null
        private val probe = EvaluationProbe(engine)

        init {
            viewModelScope.launch { start() }
        }

        private suspend fun start() {
            val stored = repertoires.load(id)
            if (stored == null) {
                _state.value = DrillUiState(status = DrillStatus.NOT_FOUND)
                return
            }
            repertoire = stored.repertoire
            val now = Instant.now(clock)
            val queue =
                ReviewQueueBuilder.build(stored.repertoire, cards.cards(id), now, clock.zone)
            leftDue = queue.totalDue - queue.dueCount
            leftNew = queue.totalNew - queue.newCount
            session = DrillSession(queue.items)
            _state.value = DrillUiState(name = stored.name)
            if (queue.items.isEmpty()) finish(nothingToReview = true) else ask()
        }

        fun onAction(action: DrillAction) {
            when (action) {
                DrillAction.Continue -> next()
                DrillAction.ShowHint -> showHint()
                else -> if (_state.value.status.takesMoves()) gesture(action)
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
            when {
                played == null -> showInput()
                _state.value.status == DrillStatus.LEARNING -> learn(played)
                else -> answer(played)
            }
        }

        private fun showHint() {
            val shown = session?.takeIf { _state.value.status == DrillStatus.ASKING } ?: return
            val edge = shown.hint ?: return
            val position = shown.current?.node?.position ?: return
            hintUsed = true
            _state.update { it.copy(hint = position.toSan(edge.move, PieceLetters.FRENCH)) }
        }

        private fun ask() {
            val shown = session?.current ?: return finish()
            val position = shown.node.position
            input = DrillInput(position)
            hintUsed = false
            val repertoire = checkNotNull(repertoire)
            val learning = session?.isLearning == true
            val notes = repertoire.notes(shown.id)
            _state.update {
                it.copy(
                    status = if (learning) DrillStatus.LEARNING else DrillStatus.ASKING,
                    learnMoves =
                        if (learning) {
                            shown.expected.map { e ->
                                position.toSan(e.move, PieceLetters.FRENCH)
                            }
                        } else {
                            emptyList()
                        },
                    learnComment = if (learning) notes.comment else "",
                    learnSource = if (learning) notes.source?.toText() else null,
                    learnMiss = false,
                    board = position.board,
                    flipped = repertoire.side == Side.BLACK,
                    highlights = BoardHighlights(check = position.checkSquare()),
                    annotations = if (learning) expectedArrows(shown) else BoardAnnotations.EMPTY,
                    promotionSide = null,
                    lineText = lineText(repertoire, shown.path),
                    toMove = position.sideToMove,
                    isNewPosition = shown.isNew,
                    isRetry = session?.isRetry == true,
                    remaining = session?.remaining ?: 0,
                    hint = null,
                    feedback = null,
                )
            }
        }

        private fun showInput() {
            val current = input ?: return
            val position = current.position
            val selected = current.selected
            _state.update {
                it.copy(
                    highlights =
                        BoardHighlights(
                            selected = selected,
                            targets = selected?.let(current::targetsOf).orEmpty(),
                            check = position.checkSquare(),
                        ),
                    promotionSide = if (current.pendingPromotion != null) position.sideToMove else null,
                )
            }
        }

        /** The player plays the repertoire move shown for a new position: the card is created, due tomorrow. */
        private fun learn(played: Move) {
            val shown = session ?: return
            val item = shown.current ?: return
            when (val result = shown.learn(played, Instant.now(clock))) {
                is LearnResult.NotTheMove -> {
                    _state.update { it.copy(learnMiss = true) }
                    showInput()
                }
                is LearnResult.Learned -> {
                    viewModelScope.launch { cards.put(id, item.id, result.card) }
                    summary = summary.learnedOne()
                    _state.update {
                        it.copy(
                            status = DrillStatus.FEEDBACK,
                            highlights = BoardHighlights(lastMove = played),
                            promotionSide = null,
                            feedback = learnedFeedback(item, result, checkNotNull(repertoire)),
                        )
                    }
                }
            }
        }

        private fun answer(played: Move) {
            val shown = session ?: return
            val item = shown.current ?: return
            val result = shown.answer(played, Instant.now(clock), hintUsed)
            result.updatedCard?.let { card -> viewModelScope.launch { cards.put(id, item.id, card) } }
            summary = summary.plus(result.rating)
            val feedback = feedbackOf(item, result, checkNotNull(repertoire))
            _state.update {
                it.copy(
                    status = DrillStatus.FEEDBACK,
                    highlights = BoardHighlights(lastMove = played),
                    annotations = annotationsOf(item, result),
                    promotionSide = null,
                    feedback = feedback,
                )
            }
            if (!result.isCorrect) evaluate(item, played, result)
        }

        /** Evaluates the position after the move played, then after the expected one (1 s each). */
        private fun evaluate(
            item: ReviewItem,
            played: Move,
            result: DrillResult,
        ) {
            evalJob?.cancel()
            val position = item.node.position
            val expected = (result.outcome as DrillOutcome.Deviation).expected.first().move
            evalJob =
                viewModelScope.launch {
                    val afterPlayed = probe.after(position, played)
                    _state.updateFeedback { it.copy(playedEvaluation = afterPlayed) }
                    val afterExpected = probe.after(position, expected)
                    _state.updateFeedback { it.copy(expectedEvaluation = afterExpected, evaluationDone = true) }
                }
        }

        private fun next() {
            if (_state.value.status != DrillStatus.FEEDBACK) return
            evalJob?.cancel()
            if (session?.isFinished == true) finish() else ask()
        }

        private fun finish(nothingToReview: Boolean = false) {
            _state.update {
                it.copy(
                    status = DrillStatus.FINISHED,
                    summary = summary,
                    leftDue = leftDue,
                    leftNew = leftNew,
                    nothingToReview = nothingToReview,
                    remaining = 0,
                    feedback = null,
                    annotations = BoardAnnotations.EMPTY,
                    highlights = BoardHighlights(),
                )
            }
        }
    }
