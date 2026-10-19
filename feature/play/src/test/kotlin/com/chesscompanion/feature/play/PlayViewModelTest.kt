package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Piece
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.GameResult
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.Arrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayViewModelTest {
    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val documents = FakeDocuments()

    private fun newViewModel(saved: SavedStateHandle = SavedStateHandle()) = PlayViewModel(saved, documents)

    private fun sq(name: String) = Square.parse(name)!!

    private fun fromFen(fen: String) = newViewModel().also { it.onAction(PlayAction.ImportText(fen)) }

    private fun PlayViewModel.tap(vararg squares: String) = squares.forEach { onAction(PlayAction.SquareTap(sq(it))) }

    private val PlayViewModel.ui get() = state.value

    // --- Moves (S1-6) ---

    @Test
    fun `tapping an own piece selects it and shows its targets`() {
        val vm = newViewModel()
        vm.tap("e2")
        assertEquals(sq("e2"), vm.ui.highlights.selected)
        assertEquals(setOf(sq("e3"), sq("e4")), vm.ui.highlights.targets)
    }

    @Test
    fun `tapping a target plays the move`() {
        val vm = newViewModel()
        vm.tap("e2", "e4")
        assertEquals(Piece(PieceType.PAWN, Side.WHITE), vm.ui.board[sq("e4")])
        assertEquals(Side.BLACK, vm.ui.sideToMove)
        assertEquals(Move.parseUci("e2e4"), vm.ui.highlights.lastMove)
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `tapping the selected square or an opponent piece clears the selection`() {
        val vm = newViewModel()
        vm.tap("e2", "e2")
        assertNull(vm.ui.highlights.selected)
        vm.tap("e2", "e7")
        assertNull(vm.ui.highlights.selected)
        vm.tap("e7")
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `tapping another own piece switches the selection`() {
        val vm = newViewModel()
        vm.tap("e2", "g1")
        assertEquals(sq("g1"), vm.ui.highlights.selected)
        assertEquals(setOf(sq("f3"), sq("h3")), vm.ui.highlights.targets)
    }

    @Test
    fun `drag and drop plays legal moves and ignores illegal ones`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.DragStart(sq("e2")))
        assertEquals(sq("e2"), vm.ui.highlights.selected)
        vm.onAction(PlayAction.Drop(sq("e2"), sq("e5")))
        assertEquals(Position.START.board, vm.ui.board)
        assertNull(vm.ui.highlights.selected)
        vm.onAction(PlayAction.Drop(sq("g1"), sq("f3")))
        assertEquals(Piece(PieceType.KNIGHT, Side.WHITE), vm.ui.board[sq("f3")])
    }

    @Test
    fun `dragging an opponent piece does nothing`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.DragStart(sq("e7")))
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `legal moves can be hidden`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ShowLegalMoves(false))
        vm.tap("e2")
        assertEquals(sq("e2"), vm.ui.highlights.selected)
        assertEquals(emptySet<Square>(), vm.ui.highlights.targets)
        assertFalse(vm.ui.showLegalMoves)
    }

    @Test
    fun `promotion waits for the piece then plays it`() {
        val vm = fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
        vm.onAction(PlayAction.Drop(sq("e7"), sq("e8")))
        assertEquals(Side.WHITE, vm.ui.promotionSide)
        vm.tap("a2")
        assertEquals(Side.WHITE, vm.ui.promotionSide)
        vm.onAction(PlayAction.Promote(PieceType.KNIGHT))
        assertNull(vm.ui.promotionSide)
        assertEquals(Piece(PieceType.KNIGHT, Side.WHITE), vm.ui.board[sq("e8")])
    }

    @Test
    fun `cancelled promotion leaves the pawn`() {
        val vm = fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
        vm.tap("e7", "e8")
        vm.onAction(PlayAction.CancelPromotion)
        assertNull(vm.ui.promotionSide)
        assertEquals(Piece(PieceType.PAWN, Side.WHITE), vm.ui.board[sq("e7")])
    }

    @Test
    fun `check is highlighted and checkmate ends the game`() {
        val vm = newViewModel()
        vm.tap("f2", "f3", "e7", "e5", "g2", "g4", "d8", "h4")
        assertEquals(sq("e1"), vm.ui.highlights.check)
        assertEquals(GameStatus.Checkmate(Side.BLACK), vm.ui.status)
        vm.tap("e1")
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `new game, flip and restore after process death`() {
        val saved = SavedStateHandle()
        val vm = newViewModel(saved)
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.Flip)
        val restored = newViewModel(saved)
        assertEquals(vm.ui.board, restored.ui.board)
        assertTrue(restored.ui.flipped)
        assertEquals(Move.parseUci("e2e4"), restored.ui.highlights.lastMove)
        vm.onAction(PlayAction.NewGame)
        assertEquals(Position.START.board, vm.ui.board)
        assertEquals(GameStatus.Ongoing, vm.ui.status)
    }

    // --- Navigation (S1-7) ---

    @Test
    fun `navigation buttons and move list`() {
        val vm = newViewModel()
        vm.tap("e2", "e4", "e7", "e5", "g1", "f3")
        assertTrue(vm.ui.canGoBack)
        assertFalse(vm.ui.canGoForward)
        vm.onAction(PlayAction.ToStart)
        assertEquals(Position.START.board, vm.ui.board)
        assertFalse(vm.ui.canGoBack)
        vm.onAction(PlayAction.Forward)
        assertEquals(Move.parseUci("e2e4"), vm.ui.highlights.lastMove)
        vm.onAction(PlayAction.GoTo(listOf(0, 0)))
        assertEquals(Move.parseUci("e7e5"), vm.ui.highlights.lastMove)
        val current =
            vm.ui.moveList
                .filterIsInstance<MoveListItem.Move>()
                .single { it.current }
        assertEquals("e5", current.text)
        vm.onAction(PlayAction.ToEnd)
        assertEquals(Move.parseUci("g1f3"), vm.ui.highlights.lastMove)
        vm.onAction(PlayAction.Back)
        assertEquals(Move.parseUci("e7e5"), vm.ui.highlights.lastMove)
        vm.onAction(PlayAction.GoTo(listOf(5)))
        assertEquals(Move.parseUci("e7e5"), vm.ui.highlights.lastMove)
    }

    @Test
    fun `a move from a past position creates a variation`() {
        val vm = newViewModel()
        vm.tap("e2", "e4", "e7", "e5")
        vm.onAction(PlayAction.Back)
        vm.tap("c7", "c5")
        val texts = vm.ui.moveList.map { (it as? MoveListItem.Move)?.text ?: it::class.simpleName }
        assertEquals(listOf("Number", "e4", "e5", "VariationStart", "Number", "c5", "VariationEnd"), texts)
        assertTrue(
            vm.ui.moveList
                .filterIsInstance<MoveListItem.Move>()
                .single { it.text == "c5" }
                .current,
        )
    }

    // --- Import and export (S1-7) ---

    @Test
    fun `pasted pgn opens at the start and exports with its result`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ImportText("[White \"A\"]\n[Black \"B\"]\n[Result \"0-1\"]\n\n1. e4 e5 2. Nf3 0-1"))
        assertEquals(PlayMessage.Imported(1, 0), vm.ui.message)
        assertEquals(Position.START.board, vm.ui.board)
        assertTrue(vm.ui.canGoForward)
        val exported = PgnReader.read(vm.exportPgn())
        assertEquals(GameResult.BLACK_WINS, exported.result)
        assertEquals("A", exported.tag("White"))
        assertEquals(listOf("e4", "e5", "Nf3"), exported.mainline.map { it.san })
        vm.onAction(PlayAction.MessageShown)
        assertNull(vm.ui.message)
    }

    @Test
    fun `several games are offered, faulty ones counted`() {
        val vm = newViewModel()
        val text = "[White \"A\"]\n\n1. e4 *\n\n[White \"B\"]\n\n1. e4 e5 2. Ke3 *\n\n[White \"C\"]\n\n1. d4 *"
        vm.onAction(PlayAction.ImportText(text))
        assertEquals(PlayMessage.Imported(2, 1), vm.ui.message)
        assertEquals(listOf("A", "C"), vm.ui.gameChoice?.map { it.white })
        vm.onAction(PlayAction.PickGame(1))
        assertNull(vm.ui.gameChoice)
        vm.onAction(PlayAction.ToEnd)
        assertEquals(Move.parseUci("d2d4"), vm.ui.highlights.lastMove)
    }

    @Test
    fun `dismissing the game choice keeps the current game`() {
        val vm = newViewModel()
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.ImportText("1. d4 *\n\n[Event \"x\"]\n\n1. c4 *"))
        vm.onAction(PlayAction.DismissGameChoice)
        assertNull(vm.ui.gameChoice)
        assertEquals(Move.parseUci("e2e4"), vm.ui.highlights.lastMove)
    }

    @Test
    fun `pasted fen starts a game from that position`() {
        val fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"
        val vm = fromFen(fen)
        assertEquals(PlayMessage.PositionLoaded, vm.ui.message)
        assertEquals(fen, vm.ui.fen)
        assertEquals(Fen.parse(fen).board, vm.ui.board)
    }

    @Test
    fun `unreadable text keeps the game and explains`() {
        val vm = newViewModel()
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.ImportText("hello"))
        val failed = vm.ui.message as PlayMessage.ImportFailed
        assertEquals(1, failed.error?.gameNumber)
        assertEquals(Move.parseUci("e2e4"), vm.ui.highlights.lastMove)
        vm.onAction(PlayAction.ImportText("  "))
        assertEquals(PlayMessage.ImportFailed(null), vm.ui.message)
    }

    @Test
    fun `files are opened and saved through the documents`() {
        documents.files["content://game.pgn"] = "1. e4 e5 *"
        val vm = newViewModel()
        vm.onAction(PlayAction.OpenFile("content://game.pgn"))
        assertEquals(PlayMessage.Imported(1, 0), vm.ui.message)
        vm.onAction(PlayAction.ToEnd)
        vm.onAction(PlayAction.SaveFile("content://saved.pgn"))
        assertEquals(PlayMessage.Saved, vm.ui.message)
        assertEquals(vm.exportPgn(), documents.files["content://saved.pgn"])
        vm.onAction(PlayAction.OpenFile("content://missing.pgn"))
        assertTrue(vm.ui.message is PlayMessage.FileError)
        vm.onAction(PlayAction.SaveFile("readonly:x"))
        assertTrue(vm.ui.message is PlayMessage.FileError)
    }

    // --- Annotations (S1-8) ---

    @Test
    fun `in annotate mode a tap colours a square instead of selecting`() {
        val vm = newViewModel()
        vm.tap("e2")
        vm.onAction(PlayAction.PickAnnotationColor(AnnotationColor.RED))
        assertTrue(vm.ui.annotating)
        assertNull(vm.ui.highlights.selected)
        vm.tap("e4")
        assertEquals(mapOf(sq("e4") to AnnotationColor.RED), vm.ui.annotations.squares)
        assertEquals(Position.START.board, vm.ui.board)
        vm.tap("e4")
        assertTrue(vm.ui.annotations.isEmpty)
    }

    @Test
    fun `in annotate mode a drag draws an arrow and plays nothing`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnnotate)
        vm.onAction(PlayAction.DragStart(sq("e2")))
        vm.onAction(PlayAction.Drop(sq("e2"), sq("e4")))
        assertEquals(mapOf(Arrow(sq("e2"), sq("e4")) to AnnotationColor.GREEN), vm.ui.annotations.arrows)
        assertEquals(Position.START.board, vm.ui.board)
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `leaving annotate mode plays again and keeps the marks of the position`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnnotate)
        vm.tap("d4")
        vm.onAction(PlayAction.ToggleAnnotate)
        assertFalse(vm.ui.annotating)
        assertEquals(setOf(sq("d4")), vm.ui.annotations.squares.keys)
        vm.tap("e2")
        assertEquals(sq("e2"), vm.ui.highlights.selected)
    }

    @Test
    fun `marks are cleared when the position changes, or on request`() {
        val vm = newViewModel()
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.ToggleAnnotate)
        vm.tap("e5")
        vm.onAction(PlayAction.Back)
        assertTrue(vm.ui.annotations.isEmpty)
        vm.tap("e2")
        vm.onAction(PlayAction.ClearAnnotations)
        assertTrue(vm.ui.annotations.isEmpty)
        vm.onAction(PlayAction.ToggleAnnotate)
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.ToggleAnnotate)
        vm.tap("d4")
        vm.onAction(PlayAction.ToggleAnnotate)
        vm.tap("e7", "e5")
        assertTrue(vm.ui.annotations.isEmpty)
    }
}
