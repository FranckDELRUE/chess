package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Piece
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.engine.EngineStrength
import com.chesscompanion.core.engine.SearchLimit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** S2-4: the game against Stockfish seen through the game ViewModel, with a fake engine. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayOpponentTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val settings = OpponentSettings(PlayerColor.WHITE, EngineStrength.Elo(1500), 1_000)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun sq(name: String) = Square.parse(name)!!

    private fun vsEngine(
        opponent: FakeOpponent,
        saved: SavedStateHandle = SavedStateHandle(mapOf("vs" to "engine")),
    ) = PlayViewModel(saved, FakeDocuments(), FakeAnalyzer(), opponent)

    private fun PlayViewModel.start(chosen: OpponentSettings = settings) =
        onAction(PlayAction.StartOpponentGame(chosen))

    private fun PlayViewModel.tap(vararg squares: String) = squares.forEach { onAction(PlayAction.SquareTap(sq(it))) }

    private val PlayViewModel.ui get() = state.value

    private fun PlayViewModel.pieceAt(name: String): Piece? = ui.board[sq(name)]

    @Test
    fun `the engine route opens the setup with the default settings`() {
        val vm = vsEngine(FakeOpponent())
        assertEquals(OpponentSettings(), vm.ui.opponent!!.setup)
        assertEquals(OpponentSettings.DEFAULT_ELO, (OpponentSettings().strength as EngineStrength.Elo).elo)
        assertNull(newTwoPlayerViewModel().ui.opponent)
    }

    private fun newTwoPlayerViewModel() =
        PlayViewModel(SavedStateHandle(), FakeDocuments(), FakeAnalyzer(), FakeOpponent())

    @Test
    fun `dismissing the setup keeps the default game`() {
        val vm = vsEngine(FakeOpponent())
        vm.onAction(PlayAction.DismissOpponentSetup)
        assertNull(vm.ui.opponent!!.setup)
        assertEquals(Side.WHITE, vm.ui.opponent!!.humanSide)
    }

    @Test
    fun `stockfish answers the player's move with the history and the chosen strength`() {
        val engine = FakeOpponent("e7e5")
        val vm = vsEngine(engine)
        vm.start()
        vm.tap("e2", "e4")
        val request = engine.requests.single()
        assertEquals(Fen.START, request.fen)
        assertEquals(listOf("e2e4"), request.moves)
        assertEquals(EngineStrength.Elo(1500), request.strength)
        assertEquals(SearchLimit.MoveTime(1_000), request.limit)
        assertEquals(Piece(PieceType.PAWN, Side.BLACK), vm.pieceAt("e5"))
        assertEquals(Side.WHITE, vm.ui.sideToMove)
        assertFalse(vm.ui.opponent!!.thinking)
    }

    @Test
    fun `stockfish opens when the player has the black pieces`() {
        val engine = FakeOpponent("e2e4")
        val vm = vsEngine(engine)
        vm.start(settings.copy(color = PlayerColor.BLACK))
        assertEquals(Side.BLACK, vm.ui.opponent!!.humanSide)
        assertTrue(
            engine.requests
                .single()
                .moves
                .isEmpty(),
        )
        assertEquals(Piece(PieceType.PAWN, Side.WHITE), vm.pieceAt("e4"))
        assertEquals(Side.BLACK, vm.ui.sideToMove)
    }

    @Test
    fun `a game from the black side is shown from the black side`() {
        val vm = vsEngine(FakeOpponent("e2e4"))
        vm.start(settings.copy(color = PlayerColor.BLACK))
        assertTrue(vm.ui.flipped)
        vm.start(settings.copy(color = PlayerColor.WHITE))
        assertFalse(vm.ui.flipped)
    }

    @Test
    fun `the player cannot move on the turn of stockfish`() {
        val engine = FakeOpponent("e7e5")
        engine.gate = CompletableDeferred()
        val vm = vsEngine(engine)
        vm.start()
        vm.tap("e2", "e4")
        assertTrue(vm.ui.opponent!!.thinking)
        vm.tap("d7", "d5")
        assertEquals(Piece(PieceType.PAWN, Side.BLACK), vm.pieceAt("d7"))
        assertNull(vm.pieceAt("d5"))
        engine.gate!!.complete(Unit)
        assertFalse(vm.ui.opponent!!.thinking)
        assertEquals(Piece(PieceType.PAWN, Side.BLACK), vm.pieceAt("e5"))
    }

    @Test
    fun `an illegal move of stockfish is refused and not retried`() {
        val engine = FakeOpponent("e7e4")
        val vm = vsEngine(engine)
        vm.start()
        vm.tap("e2", "e4")
        assertEquals(PlayMessage.EngineFailed, vm.ui.message)
        assertEquals(Side.BLACK, vm.ui.sideToMove)
        vm.onAction(PlayAction.MessageShown)
        assertEquals(1, engine.requests.size)
    }

    @Test
    fun `a failing engine is reported`() {
        val engine = FakeOpponent()
        engine.failWith = IllegalStateException("no engine")
        val vm = vsEngine(engine)
        vm.start()
        vm.tap("e2", "e4")
        assertEquals(PlayMessage.EngineFailed, vm.ui.message)
        assertFalse(vm.ui.opponent!!.thinking)
    }

    @Test
    fun `taking back returns to the player's turn and keeps the moves as a variation`() {
        val engine = FakeOpponent("e7e5", "b8c6", "g8f6")
        val vm = vsEngine(engine)
        vm.start()
        vm.tap("e2", "e4")
        vm.tap("g1", "f3")
        assertEquals(Piece(PieceType.KNIGHT, Side.BLACK), vm.pieceAt("c6"))
        vm.onAction(PlayAction.TakeBack)
        // Back to the position after 1.e4 e5: White to play, Stockfish has not answered again.
        assertEquals(Piece(PieceType.PAWN, Side.BLACK), vm.pieceAt("e5"))
        assertNull(vm.pieceAt("c6"))
        assertEquals(Side.WHITE, vm.ui.sideToMove)
        assertEquals(2, engine.requests.size)
        vm.tap("d2", "d4")
        assertEquals(3, engine.requests.size)
        assertEquals(Piece(PieceType.KNIGHT, Side.BLACK), vm.pieceAt("f6"))
        // The moves taken back are still in the game, as a variation.
        assertTrue(vm.exportPgn().contains("Nf3"))
        assertTrue(vm.exportPgn().contains("d4"))
    }

    @Test
    fun `taking back from the player's turn goes one full move back`() {
        val vm = vsEngine(FakeOpponent("e7e5"))
        vm.start()
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.TakeBack)
        assertEquals(Fen.START, vm.ui.fen)
        assertEquals(Side.WHITE, vm.ui.sideToMove)
        assertNull(vm.pieceAt("e4"))
    }

    @Test
    fun `taking back while stockfish thinks cancels the search`() {
        val engine = FakeOpponent("e7e5")
        engine.gate = CompletableDeferred()
        val vm = vsEngine(engine)
        vm.start()
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.TakeBack)
        assertEquals(1, engine.cancelled)
        assertFalse(vm.ui.opponent!!.thinking)
        assertNull(vm.pieceAt("e4"))
    }

    @Test
    fun `browsing an old position does not make stockfish play`() {
        val engine = FakeOpponent("e7e5", "b8c6")
        val vm = vsEngine(engine)
        vm.start()
        vm.tap("e2", "e4")
        vm.tap("g1", "f3")
        assertEquals(2, engine.requests.size)
        vm.onAction(PlayAction.Back)
        vm.onAction(PlayAction.Back)
        vm.onAction(PlayAction.Forward)
        assertEquals(2, engine.requests.size)
    }

    @Test
    fun `resigning ends the game for the player`() {
        val vm = vsEngine(FakeOpponent("e7e5"))
        vm.start()
        vm.tap("e2", "e4")
        assertFalse(vm.ui.opponent!!.over)
        vm.onAction(PlayAction.Resign)
        assertTrue(vm.ui.opponent!!.resigned)
        assertTrue(vm.ui.opponent!!.over)
        vm.tap("g1", "f3")
        assertNull(vm.pieceAt("f3"))
        assertTrue(vm.exportPgn().contains("[Result \"0-1\"]"))
    }

    @Test
    fun `the exported game names the players and the strength`() {
        val vm = vsEngine(FakeOpponent())
        vm.start()
        val pgn = vm.exportPgn()
        assertTrue(pgn.contains("[White \"Player\"]"))
        assertTrue(pgn.contains("[Black \"Stockfish\"]"))
        assertTrue(pgn.contains("Stockfish (Elo 1500, 1 s a move)"))
    }

    @Test
    fun `the analysis is not available during the game and comes back at the end`() {
        val analyzer = FakeAnalyzer()
        val vm = PlayViewModel(SavedStateHandle(mapOf("vs" to "engine")), FakeDocuments(), analyzer, FakeOpponent())
        vm.start()
        vm.onAction(PlayAction.ToggleAnalysis)
        assertFalse(vm.analysisState.value.enabled)
        vm.onAction(PlayAction.Resign)
        vm.onAction(PlayAction.ToggleAnalysis)
        assertTrue(vm.analysisState.value.enabled)
    }

    @Test
    fun `the game survives process death and stockfish resumes its move`() {
        val saved = SavedStateHandle(mapOf("vs" to "engine"))
        val first = FakeOpponent("e7e5")
        first.gate = CompletableDeferred()
        val vm = vsEngine(first, saved)
        vm.start(settings.copy(strength = EngineStrength.Skill(4)))
        vm.tap("e2", "e4")
        assertTrue(vm.ui.opponent!!.thinking)

        val second = FakeOpponent("c7c5")
        val restored = vsEngine(second, saved)
        assertEquals(
            EngineStrength.Skill(4),
            restored.ui.opponent!!
                .settings.strength,
        )
        assertNull(restored.ui.opponent!!.setup)
        assertEquals(EngineStrength.Skill(4), second.requests.single().strength)
        assertEquals(Piece(PieceType.PAWN, Side.BLACK), restored.pieceAt("c5"))
    }

    @Test
    fun `a new game asks for the settings again`() {
        val vm = vsEngine(FakeOpponent("e7e5"))
        vm.start()
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.NewGame)
        assertNotNull(vm.ui.opponent!!.setup)
        assertNotNull(vm.pieceAt("e4"))
        vm.start(settings.copy(color = PlayerColor.WHITE))
        assertNull(vm.pieceAt("e4"))
    }
}
