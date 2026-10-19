package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Piece
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayViewModelTest {
    private fun sq(name: String) = Square.parse(name)!!

    private fun fromFen(fen: String) =
        PlayViewModel(
            SavedStateHandle(
                mapOf(
                    "play.pgn" to "[SetUp \"1\"]\n[FEN \"$fen\"]\n\n*",
                ),
            ),
        )

    private fun PlayViewModel.tap(vararg squares: String) = squares.forEach { onSquareTap(sq(it)) }

    private val PlayViewModel.ui get() = state.value

    @Test
    fun `tapping an own piece selects it and shows its targets`() {
        val vm = PlayViewModel(SavedStateHandle())
        vm.tap("e2")
        assertEquals(sq("e2"), vm.ui.highlights.selected)
        assertEquals(setOf(sq("e3"), sq("e4")), vm.ui.highlights.targets)
    }

    @Test
    fun `tapping a target plays the move`() {
        val vm = PlayViewModel(SavedStateHandle())
        vm.tap("e2", "e4")
        assertEquals(Piece(PieceType.PAWN, Side.WHITE), vm.ui.board[sq("e4")])
        assertEquals(Side.BLACK, vm.ui.sideToMove)
        assertEquals(Move.parseUci("e2e4"), vm.ui.highlights.lastMove)
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `tapping the selected square or an opponent piece clears the selection`() {
        val vm = PlayViewModel(SavedStateHandle())
        vm.tap("e2", "e2")
        assertNull(vm.ui.highlights.selected)
        vm.tap("e2", "e7")
        assertNull(vm.ui.highlights.selected)
        vm.tap("e7")
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `tapping another own piece switches the selection`() {
        val vm = PlayViewModel(SavedStateHandle())
        vm.tap("e2", "g1")
        assertEquals(sq("g1"), vm.ui.highlights.selected)
        assertEquals(setOf(sq("f3"), sq("h3")), vm.ui.highlights.targets)
    }

    @Test
    fun `drag and drop plays legal moves and ignores illegal ones`() {
        val vm = PlayViewModel(SavedStateHandle())
        vm.onDragStart(sq("e2"))
        assertEquals(sq("e2"), vm.ui.highlights.selected)
        vm.onMove(sq("e2"), sq("e5"))
        assertEquals(Position.START.board, vm.ui.board)
        assertNull(vm.ui.highlights.selected)
        vm.onMove(sq("g1"), sq("f3"))
        assertEquals(Piece(PieceType.KNIGHT, Side.WHITE), vm.ui.board[sq("f3")])
    }

    @Test
    fun `dragging an opponent piece does nothing`() {
        val vm = PlayViewModel(SavedStateHandle())
        vm.onDragStart(sq("e7"))
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `legal moves can be hidden`() {
        val vm = PlayViewModel(SavedStateHandle())
        vm.setShowLegalMoves(false)
        vm.tap("e2")
        assertEquals(sq("e2"), vm.ui.highlights.selected)
        assertEquals(emptySet<Square>(), vm.ui.highlights.targets)
        assertEquals(false, vm.ui.showLegalMoves)
    }

    @Test
    fun `promotion waits for the piece then plays it`() {
        val vm = fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
        vm.onMove(sq("e7"), sq("e8"))
        assertEquals(Side.WHITE, vm.ui.promotionSide)
        vm.tap("a2")
        assertEquals(Side.WHITE, vm.ui.promotionSide)
        vm.onPromotion(PieceType.KNIGHT)
        assertNull(vm.ui.promotionSide)
        assertEquals(Piece(PieceType.KNIGHT, Side.WHITE), vm.ui.board[sq("e8")])
    }

    @Test
    fun `cancelled promotion leaves the pawn`() {
        val vm = fromFen("k7/4P3/8/8/8/8/8/4K3 w - - 0 1")
        vm.tap("e7", "e8")
        vm.onPromotionCancel()
        assertNull(vm.ui.promotionSide)
        assertEquals(Piece(PieceType.PAWN, Side.WHITE), vm.ui.board[sq("e7")])
    }

    @Test
    fun `check is highlighted and checkmate ends the game`() {
        val vm = PlayViewModel(SavedStateHandle())
        vm.tap("f2", "f3", "e7", "e5", "g2", "g4", "d8", "h4")
        assertEquals(sq("e1"), vm.ui.highlights.check)
        assertEquals(GameStatus.Checkmate(Side.BLACK), vm.ui.status)
        vm.tap("e1")
        assertNull(vm.ui.highlights.selected)
    }

    @Test
    fun `new game, flip and restore after process death`() {
        val saved = SavedStateHandle()
        val vm = PlayViewModel(saved)
        vm.tap("e2", "e4")
        vm.flip()
        val restored = PlayViewModel(saved)
        assertEquals(vm.ui.board, restored.ui.board)
        assertTrue(restored.ui.flipped)
        assertEquals(Move.parseUci("e2e4"), restored.ui.highlights.lastMove)
        vm.newGame()
        assertEquals(Position.START.board, vm.ui.board)
        assertEquals(GameStatus.Ongoing, vm.ui.status)
    }
}
