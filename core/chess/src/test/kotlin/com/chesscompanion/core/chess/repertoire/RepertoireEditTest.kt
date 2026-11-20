package com.chesscompanion.core.chess.repertoire

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.pgn.PgnWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepertoireEditTest {
    private fun sq(name: String) = Square.parse(name)!!

    private fun mv(uci: String) = Move.parseUci(uci)!!

    private fun merge(
        repertoire: Repertoire,
        pgn: String,
    ) = repertoire.merge(PgnReader.read(pgn))

    private fun Repertoire.signature() =
        Triple(
            allNodes.map { it.id }.toSet(),
            allMoves.map { listOf(it.from, it.to, it.move, it.san) }.toSet(),
            allNodes.associate { it.id to notes(it.id) }.filterValues { !it.isEmpty },
        )

    // ---- PgnMarks ----

    @Test
    fun `marks are read from a comment and removed from its text`() {
        val parsed = PgnMarks.parse("Idée centrale. [%cal Ge2e4,Rd7d5] [%csl Gd4,Bf6] Fin.")
        assertEquals("Idée centrale. Fin.", parsed.text)
        assertEquals(
            listOf(
                RepertoireArrow(sq("e2"), sq("e4"), MarkColor.GREEN),
                RepertoireArrow(sq("d7"), sq("d5"), MarkColor.RED),
            ),
            parsed.arrows,
        )
        assertEquals(
            listOf(RepertoireSquareMark(sq("d4"), MarkColor.GREEN), RepertoireSquareMark(sq("f6"), MarkColor.BLUE)),
            parsed.squares,
        )
    }

    @Test
    fun `yellow and malformed items are dropped`() {
        val parsed = PgnMarks.parse("[%cal Ye2e4,Ge2,Gz9e4,Rg1f3] [%csl Yd4,G,Rh9]")
        assertEquals(listOf(RepertoireArrow(sq("g1"), sq("f3"), MarkColor.RED)), parsed.arrows)
        assertTrue(parsed.squares.isEmpty())
        assertEquals("", parsed.text)
    }

    @Test
    fun `a comment without marks is left alone`() {
        val parsed = PgnMarks.parse("Rien à dessiner.\nDeuxième ligne.")
        assertEquals("Rien à dessiner.\nDeuxième ligne.", parsed.text)
        assertTrue(parsed.arrows.isEmpty() && parsed.squares.isEmpty())
    }

    @Test
    fun `notes are written as text then squares then arrows, and read back`() {
        val notes =
            NodeNotes(
                comment = "Plan : f4.",
                arrows = listOf(RepertoireArrow(sq("f2"), sq("f4"), MarkColor.BLUE)),
                squares = listOf(RepertoireSquareMark(sq("e5"), MarkColor.RED)),
            )
        val text = PgnMarks.format(notes)
        assertEquals("Plan : f4. [%csl Re5] [%cal Bf2f4]", text)
        val back = PgnMarks.parse(text!!)
        assertEquals(NodeNotes(back.text, back.arrows, back.squares), notes)
        assertNull(PgnMarks.format(NodeNotes()))
    }

    // ---- merge with notes ----

    @Test
    fun `a merged game writes its comments and marks on the reached position`() {
        val result = merge(Repertoire.empty(Side.WHITE), "1. e4 {Le coup du roi [%cal Ge2e4]} e5 *")
        val repertoire = result.repertoire
        val afterE4 = repertoire.movesFrom(repertoire.root).single().to
        assertEquals("Le coup du roi", repertoire.notes(afterE4).comment)
        assertEquals(1, repertoire.notes(afterE4).arrows.size)
        assertEquals(1, result.notedPositions)
    }

    @Test
    fun `merging the same commented game twice repeats nothing`() {
        val pgn = "1. e4 {Roi [%csl Ge4]} e5 {Symétrie} *"
        val once = merge(Repertoire.empty(Side.WHITE), pgn).repertoire
        val twice = merge(once, pgn)
        assertEquals(0, twice.notedPositions)
        assertEquals(once.signature(), twice.repertoire.signature())
    }

    @Test
    fun `a different comment on a known position is added on a new line`() {
        val once = merge(Repertoire.empty(Side.WHITE), "1. e4 {Premier} *").repertoire
        val twice = merge(once, "1. e4 {Second} *").repertoire
        val node = twice.movesFrom(twice.root).single().to
        assertEquals("Premier\nSecond", twice.notes(node).comment)
    }

    // ---- removeMove ----

    @Test
    fun `removing a move removes what only it led to, with its notes`() {
        val full = merge(Repertoire.empty(Side.WHITE), "1. e4 {A} e5 2. Nf3 {B} Nc6 (2... d6) *").repertoire
        val afterE4 = full.movesFrom(full.root).single()
        val afterE5 = full.movesFrom(afterE4.to).single()
        val cut = full.removeMove(afterE5.to, full.movesFrom(afterE5.to).single().move)
        // Nf3 is removed from the position after 1... e5: the whole subtree of Nf3 goes.
        assertEquals(full.nodeCount - 3, cut.nodeCount)
        assertEquals(full.edgeCount - 3, cut.edgeCount)
        assertEquals("A", cut.notes(afterE4.to).comment)
    }

    @Test
    fun `a position still reached by another move stays`() {
        val graph =
            merge(Repertoire.empty(Side.WHITE), "1. d4 Nf6 2. c4 e6 *")
                .repertoire
                .let { merge(it, "1. c4 e6 2. d4 Nf6 *").repertoire }
        val c4 = graph.movesFrom(graph.root).first { it.san == "c4" }
        val cut = graph.removeMove(graph.root, c4.move)
        // The branch 1. c4 e6 2. d4 Nf6 goes (3 nodes that only it reached); the shared end stays.
        assertEquals(graph.nodeCount - 3, cut.nodeCount)
        assertEquals(graph.edgeCount - 4, cut.edgeCount)
        assertTrue(cut.allMoves.any { it.san == "e6" })
    }

    @Test
    fun `removing an unknown move changes nothing`() {
        val graph = merge(Repertoire.empty(Side.WHITE), "1. e4 *").repertoire
        assertEquals(graph.signature(), graph.removeMove(graph.root, mv("d2d4")).signature())
    }

    @Test
    fun `removing the last move leaves the root with its own notes`() {
        val graph = merge(Repertoire.empty(Side.WHITE), "1. e4 *").repertoire
        val noted = graph.withNotes(graph.root, NodeNotes(comment = "Départ"))
        val cut = noted.removeMove(noted.root, mv("e2e4"))
        assertEquals(1, cut.nodeCount)
        assertEquals("Départ", cut.notes(cut.root).comment)
    }

    // ---- export ----

    @Test
    fun `edit, export to PGN and read back gives the same repertoire with its annotations`() {
        var repertoire = Repertoire.empty(Side.WHITE)
        val e4 = repertoire.add(repertoire.root, mv("e2e4"))
        repertoire = e4.repertoire
        val d4 = repertoire.add(repertoire.root, mv("d2d4"))
        repertoire = d4.repertoire.add(d4.target, mv("d7d5")).repertoire
        repertoire =
            repertoire.withNotes(
                e4.target,
                NodeNotes(
                    comment = "Ouverture de la partie.",
                    arrows = listOf(RepertoireArrow(sq("g1"), sq("f3"), MarkColor.GREEN)),
                    squares = listOf(RepertoireSquareMark(sq("e5"), MarkColor.RED)),
                ),
            )

        val pgn = PgnWriter.write(repertoire.toGame("Mon répertoire"))
        val game = PgnReader.read(pgn)
        assertEquals("Mon répertoire", game.tag("Event"))
        val back = Repertoire.empty(Side.WHITE).merge(game).repertoire

        assertEquals(repertoire.signature(), back.signature())
    }

    @Test
    fun `transpositions survive the export and the graph is the same`() {
        val graph =
            merge(Repertoire.empty(Side.WHITE), "1. d4 Nf6 2. c4 e6 {Fin de la ligne} *")
                .repertoire
                .let { merge(it, "1. c4 e6 2. d4 Nf6 *").repertoire }
        val back = Repertoire.empty(Side.WHITE).merge(PgnReader.read(PgnWriter.write(graph.toGame()))).repertoire
        assertEquals(graph.signature(), back.signature())
    }

    @Test
    fun `a repetition cycle is exported once and does not loop`() {
        val graph = merge(Repertoire.empty(Side.WHITE), "1. Nf3 Nf6 2. Ng1 Ng8 *").repertoire
        val back = Repertoire.empty(Side.WHITE).merge(PgnReader.read(PgnWriter.write(graph.toGame()))).repertoire
        assertEquals(graph.signature(), back.signature())
    }

    @Test
    fun `a repertoire from another start position exports its FEN`() {
        val fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"
        val start = Fen.parse(fen)
        val graph = Repertoire.empty(Side.BLACK, start).let { it.add(it.root, mv("e1g1")).repertoire }
        val game = graph.toGame()
        assertEquals(fen, game.tag("FEN"))
        val back = Repertoire.empty(Side.BLACK, start).merge(PgnReader.read(PgnWriter.write(game))).repertoire
        assertEquals(graph.signature(), back.signature())
    }

    @Test
    fun `an empty repertoire exports a game without moves`() {
        val game = Repertoire.empty(Side.WHITE).toGame()
        assertTrue(game.moves.isEmpty())
    }
}
