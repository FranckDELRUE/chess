package com.chesscompanion.core.chess.repertoire

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.IllegalMoveException
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.PgnReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RepertoireTest {
    private fun empty() = Repertoire.empty(Side.WHITE)

    private fun merge(
        repertoire: Repertoire,
        pgn: String,
    ): MergeResult = repertoire.merge(PgnReader.read(pgn))

    private fun Repertoire.play(
        from: PositionId,
        uci: String,
    ): AddResult = add(from, Move.parseUci(uci)!!)

    @Test
    fun `an empty repertoire holds only its start position`() {
        val repertoire = empty()
        assertEquals(1, repertoire.nodeCount)
        assertEquals(0, repertoire.edgeCount)
        assertEquals(Position.START, repertoire.node(repertoire.root)!!.position)
        assertTrue(repertoire.movesFrom(repertoire.root).isEmpty())
    }

    @Test
    fun `playing a move adds an edge and a node, replaying it adds nothing`() {
        val first = empty().play(empty().root, "e2e4")
        assertTrue(first.created)
        assertEquals(2, first.repertoire.nodeCount)
        assertEquals(1, first.repertoire.edgeCount)
        val again = first.repertoire.play(first.repertoire.root, "e2e4")
        assertFalse(again.created)
        assertEquals(first.target, again.target)
        assertEquals(1, again.repertoire.edgeCount)
    }

    @Test
    fun `an illegal move or an unknown position is refused`() {
        val repertoire = empty()
        assertThrows<IllegalMoveException> { repertoire.play(repertoire.root, "e2e5") }
        assertThrows<IllegalArgumentException> { repertoire.play(PositionId(1L, "x"), "e2e4") }
    }

    @Test
    fun `the original repertoire is not modified by an addition`() {
        val repertoire = empty()
        repertoire.play(repertoire.root, "e2e4")
        assertEquals(1, repertoire.nodeCount)
    }

    @Test
    fun `a move records its SAN`() {
        val repertoire = empty()
        val result = repertoire.play(repertoire.root, "g1f3")
        assertEquals(
            "Nf3",
            result.repertoire
                .movesFrom(repertoire.root)
                .single()
                .san,
        )
    }

    @Test
    fun `variations are separate moves from the same position, in order`() {
        val merged = merge(empty(), "1. e4 (1. d4 d5) (1. c4) e5 *").repertoire
        assertEquals(listOf("e4", "d4", "c4"), merged.movesFrom(merged.root).map { it.san })
        assertEquals(1, merged.movesFrom(merged.movesFrom(merged.root)[1].to).size)
    }

    @Test
    fun `two move orders that transpose reach the same node`() {
        val result = merge(empty(), "1. d4 Nf6 2. c4 e6 *")
        val other = merge(result.repertoire, "1. c4 e6 2. d4 Nf6 *")
        val repertoire = other.repertoire
        // Both games end in the same position: 4 shared plies plus 3 new nodes on the second path.
        assertEquals(1 + 4 + 3, repertoire.nodeCount)
        assertEquals(8, repertoire.edgeCount)
        val reachedTwice =
            repertoire.allNodes.filter { node ->
                repertoire.allNodes.sumOf { n -> repertoire.movesFrom(n.id).count { it.to == node.id } } == 2
            }
        assertEquals(1, reachedTwice.size)
        assertEquals(4, other.addedMoves)
        assertEquals(3, other.addedPositions)
    }

    @Test
    fun `merging the same PGN twice changes nothing`() {
        val pgn = "1. e4 e5 (1... c5 2. Nf3 d6) 2. Nf3 Nc6 *"
        val once = merge(empty(), pgn)
        val twice = merge(once.repertoire, pgn)
        assertEquals(0, twice.addedMoves)
        assertEquals(0, twice.addedPositions)
        assertEquals(once.addedMoves, twice.existingMoves)
        assertEquals(once.repertoire.nodeCount, twice.repertoire.nodeCount)
        assertEquals(once.repertoire.edgeCount, twice.repertoire.edgeCount)
    }

    @Test
    fun `a game that follows existing moves only adds the new ones`() {
        val base = merge(empty(), "1. e4 e5 2. Nf3 *").repertoire
        val result = merge(base, "1. e4 e5 2. Nf3 Nc6 3. Bb5 *")
        assertEquals(2, result.addedMoves)
        assertEquals(3, result.existingMoves)
        assertEquals(2, result.addedPositions)
    }

    @Test
    fun `a position that repeats gives an edge back to an earlier node`() {
        val result = merge(empty(), "1. Nf3 Nf6 2. Ng1 Ng8 *")
        assertEquals(4, result.addedMoves)
        assertEquals(4, result.repertoire.nodeCount)
        val lastMove =
            result.repertoire.allNodes
                .flatMap { result.repertoire.movesFrom(it.id) }
                .last()
        assertEquals("Ng8", lastMove.san)
        assertEquals(result.repertoire.root, lastMove.to)
    }

    @Test
    fun `an en passant square nobody can use does not split a node`() {
        val withSquare = Fen.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
        val without = Fen.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")
        assertEquals(PositionId.of(without), PositionId.of(withSquare))
    }

    @Test
    fun `positions that differ only by move counters share an id`() {
        val a = Fen.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")
        val b = Fen.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 7 12")
        assertEquals(PositionId.of(a), PositionId.of(b))
    }

    @Test
    fun `different castling rights make different ids`() {
        val a = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val b = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1")
        assertNotEquals(PositionId.of(a), PositionId.of(b))
    }

    @Test
    fun `a Zobrist key collision is told apart by the FEN`() {
        val constantKey: (Position) -> PositionId = { PositionId(42L, PositionId.of(it).fen) }
        val repertoire = Repertoire.empty(Side.WHITE, Position.START, constantKey)
        val result = repertoire.merge(PgnReader.read("1. e4 e5 *"))
        assertEquals(3, result.repertoire.nodeCount)
        assertEquals(2, result.repertoire.edgeCount)
    }

    @Test
    fun `a game from another start position is refused`() {
        val game = PgnReader.read("[FEN \"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1\"]\n[SetUp \"1\"]\n\n1. O-O *")
        assertThrows<StartMismatchException> { empty().merge(game) }
    }

    @Test
    fun `a repertoire can start from another position`() {
        val start = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        val game = PgnReader.read("[FEN \"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1\"]\n[SetUp \"1\"]\n\n1. O-O *")
        val result = Repertoire.empty(Side.BLACK, start).merge(game)
        assertEquals(1, result.addedMoves)
        assertEquals(Side.BLACK, result.repertoire.side)
    }
}
