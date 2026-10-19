package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.Game
import com.chesscompanion.core.chess.pgn.GameSession
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.pgn.PgnWriter
import com.chesscompanion.core.ui.board.BoardHighlights
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val moveList: List<MoveListItem> = emptyList(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    /** FEN of the position shown. */
    val fen: String = "",
    /** Games to choose from after importing a PGN that holds several; null otherwise. */
    val gameChoice: List<GameSummary>? = null,
    val message: PlayMessage? = null,
)

/**
 * The game screen (S1-6, S1-7): two players on one board, navigation in the move tree, PGN and
 * FEN import and export. Every request arrives through [onAction]; the rules of `:core:chess`
 * decide. The game survives process death as PGN in the [SavedStateHandle].
 */
@HiltViewModel
class PlayViewModel
    @Inject
    constructor(
        private val savedState: SavedStateHandle,
        private val documents: GameDocuments,
    ) : ViewModel() {
        private var input = MoveInput(restoreSession())
        private var flipped: Boolean = savedState[KEY_FLIPPED] ?: false
        private var showLegalMoves: Boolean = savedState[KEY_SHOW_LEGAL_MOVES] ?: true
        private var choice: List<Game>? = null
        private var message: PlayMessage? = null

        private val _state = MutableStateFlow(buildState())
        val state: StateFlow<PlayUiState> = _state.asStateFlow()

        fun onAction(action: PlayAction) {
            when (action) {
                is PlayAction.Board -> onBoard(action)
                is PlayAction.Navigation -> onNavigation(action)
                is PlayAction.Document -> onDocument(action)
            }
        }

        /** The game as PGN, with its result when the main line is finished. */
        fun exportPgn(): String = PgnWriter.write(input.session.toGame())

        private fun onBoard(action: PlayAction.Board) {
            when (action) {
                is PlayAction.SquareTap -> update(input.tap(action.square))
                is PlayAction.DragStart -> update(input.dragStart(action.square))
                is PlayAction.Drop -> update(input.drop(action.from, action.to))
                is PlayAction.Promote -> update(input.promote(action.type))
                PlayAction.CancelPromotion -> update(input.cancelPromotion())
                PlayAction.NewGame -> update(MoveInput(GameSession.new()))
                PlayAction.Flip -> {
                    flipped = !flipped
                    savedState[KEY_FLIPPED] = flipped
                    update(input)
                }
                is PlayAction.ShowLegalMoves -> {
                    showLegalMoves = action.show
                    savedState[KEY_SHOW_LEGAL_MOVES] = action.show
                    update(input)
                }
            }
        }

        private fun onNavigation(action: PlayAction.Navigation) {
            val session = input.session
            val target =
                when (action) {
                    PlayAction.ToStart -> session.toStart()
                    PlayAction.Back -> session.back()
                    PlayAction.Forward -> session.forward()
                    PlayAction.ToEnd -> session.toEnd()
                    is PlayAction.GoTo -> runCatching { session.goTo(action.path) }.getOrDefault(session)
                }
            update(MoveInput(target))
        }

        private fun onDocument(action: PlayAction.Document) {
            when (action) {
                is PlayAction.ImportText -> importText(action.text)
                is PlayAction.OpenFile ->
                    viewModelScope.launch {
                        runCatching { documents.read(action.uri) }
                            .onSuccess { importText(it) }
                            .onFailure { notify(PlayMessage.FileError(it.message.orEmpty())) }
                    }
                is PlayAction.SaveFile ->
                    viewModelScope.launch {
                        val pgn = exportPgn()
                        runCatching { documents.write(action.uri, pgn) }
                            .onSuccess { notify(PlayMessage.Saved) }
                            .onFailure { notify(PlayMessage.FileError(it.message.orEmpty())) }
                    }
                is PlayAction.PickGame -> {
                    val game = choice?.getOrNull(action.index)
                    choice = null
                    update(game?.let { MoveInput(GameSession(it)) } ?: input)
                }
                PlayAction.DismissGameChoice -> {
                    choice = null
                    update(input)
                }
                PlayAction.MessageShown -> notify(null)
            }
        }

        /** A FEN starts a new game from that position; otherwise the text is read as PGN (rule 2). */
        private fun importText(text: String) {
            val position = runCatching { Fen.parse(text) }.getOrNull()
            if (position != null) {
                message = PlayMessage.PositionLoaded
                update(MoveInput(GameSession.new(position)))
                return
            }
            val import = PgnReader.readAll(text)
            val games = import.games
            message =
                if (games.isEmpty()) {
                    PlayMessage.ImportFailed(import.errors.firstOrNull())
                } else {
                    PlayMessage.Imported(games.size, import.errors.size)
                }
            when (games.size) {
                0 -> update(input)
                1 -> update(MoveInput(GameSession(games.single())))
                else -> {
                    choice = games
                    update(input)
                }
            }
        }

        private fun notify(next: PlayMessage?) {
            message = next
            update(input)
        }

        /** Applies the new input state, saves the game when it changed and publishes the UI state. */
        private fun update(next: MoveInput) {
            if (next.session.game !== input.session.game) savedState[KEY_PGN] = PgnWriter.write(next.session.game)
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
                moveList = MoveListBuilder.build(session),
                canGoBack = session.canGoBack,
                canGoForward = session.canGoForward,
                fen = position.toFen(),
                gameChoice =
                    choice?.map { game ->
                        GameSummary(
                            white = game.tag("White").orEmpty(),
                            black = game.tag("Black").orEmpty(),
                            date = game.tag("Date").orEmpty(),
                            result = game.result.token,
                        )
                    },
                message = message,
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
