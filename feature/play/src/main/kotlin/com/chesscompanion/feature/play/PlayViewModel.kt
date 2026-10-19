package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.GameSession
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.pgn.PgnWriter
import com.chesscompanion.core.ui.board.BoardHighlights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PlayUiState(
    val board: Board,
    val sideToMove: Side,
    val status: GameStatus,
    val highlights: BoardHighlights,
    val flipped: Boolean,
    val showLegalMoves: Boolean,
    /** Side whose pawn waits for a promotion piece, null when no choice is pending. */
    val promotionSide: Side?,
)

/**
 * Two players on one screen (S1-6). Taps, drags and drops arrive from the board; this ViewModel
 * decides what they mean, using the rules of `:core:chess`. The game survives process death
 * as PGN in the [SavedStateHandle].
 */
@HiltViewModel
class PlayViewModel
    @Inject
    constructor(
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private var input = MoveInput(restoreSession())
        private var flipped: Boolean = savedState[KEY_FLIPPED] ?: false
        private var showLegalMoves: Boolean = savedState[KEY_SHOW_LEGAL_MOVES] ?: true

        private val _state = MutableStateFlow(buildState())
        val state: StateFlow<PlayUiState> = _state.asStateFlow()

        fun onSquareTap(square: Square) = update(input.tap(square))

        fun onDragStart(square: Square) = update(input.dragStart(square))

        fun onMove(
            from: Square,
            to: Square,
        ) = update(input.drop(from, to))

        fun onPromotion(type: PieceType) = update(input.promote(type))

        fun onPromotionCancel() = update(input.cancelPromotion())

        fun newGame() = update(MoveInput(GameSession.new()))

        fun flip() {
            flipped = !flipped
            savedState[KEY_FLIPPED] = flipped
            update(input)
        }

        fun setShowLegalMoves(show: Boolean) {
            showLegalMoves = show
            savedState[KEY_SHOW_LEGAL_MOVES] = show
            update(input)
        }

        /** Applies the new input state, saves the game when it changed and publishes the UI state. */
        private fun update(next: MoveInput) {
            if (next.session != input.session) savedState[KEY_PGN] = PgnWriter.write(next.session.game)
            input = next
            _state.value = buildState()
        }

        private fun buildState(): PlayUiState {
            val session = input.session
            val position = session.position
            val selected = input.selected
            return PlayUiState(
                board = position.board,
                sideToMove = position.sideToMove,
                status = session.status(),
                highlights =
                    BoardHighlights(
                        selected = selected,
                        targets = selected?.takeIf { showLegalMoves }?.let { input.targetsOf(it) }.orEmpty(),
                        lastMove = session.lastMove,
                        check = if (position.isCheck) position.board.kingSquare(position.sideToMove) else null,
                    ),
                flipped = flipped,
                showLegalMoves = showLegalMoves,
                promotionSide = if (input.pendingPromotion != null) position.sideToMove else null,
            )
        }

        /** The saved game, played to the end of its main line; a new game if there is none. */
        private fun restoreSession(): GameSession {
            val pgn = savedState.get<String>(KEY_PGN) ?: return GameSession.new()
            val game = runCatching { PgnReader.read(pgn) }.getOrNull() ?: return GameSession.new()
            return GameSession(game, game.mainline.map { 0 })
        }

        private companion object {
            const val KEY_PGN = "play.pgn"
            const val KEY_FLIPPED = "play.flipped"
            const val KEY_SHOW_LEGAL_MOVES = "play.showLegalMoves"
        }
    }
