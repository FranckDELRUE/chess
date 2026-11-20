package com.chesscompanion.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.repertoire.MarkColor
import com.chesscompanion.core.chess.repertoire.NodeNotes
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.RepertoireArrow
import com.chesscompanion.core.chess.repertoire.RepertoireSquareMark
import com.chesscompanion.core.chess.repertoire.Source
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S7-2: the repertoire store on Room, in memory. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomRepertoireStoreTest {
    private lateinit var database: ChessDatabase
    private lateinit var store: RoomRepertoireStore
    private var now = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChessDatabase::class.java).allowMainThreadQueries().build()
        store = RoomRepertoireStore(database) { now++ }
    }

    @After
    fun tearDown() = database.close()

    private fun sample(): Repertoire {
        // 1. d4 Nf6 2. c4 e6 and the transposed order 1. c4 e6 2. d4 Nf6, plus a variation.
        val pgn = "1. d4 (1. e4 c5) Nf6 2. c4 e6 *\n\n1. c4 e6 2. d4 Nf6 *"
        return PgnReader.readAll(pgn).games.fold(Repertoire.empty(Side.WHITE)) { r, g -> r.merge(g).repertoire }
    }

    private fun Repertoire.signature() =
        Triple(
            allNodes.map { it.id to it.position.toFen() }.toSet(),
            allMoves.map { listOf(it.from, it.to, it.move, it.san) },
            allNodes.associate { it.id to notes(it.id) }.filterValues { !it.isEmpty },
        )

    @Test
    fun aNewRepertoireIsEmptyAndListed() =
        runTest {
            val id = store.create("Italienne", Side.WHITE)
            val loaded = store.load(id)!!
            assertEquals("Italienne", loaded.name)
            assertEquals(Side.WHITE, loaded.repertoire.side)
            assertEquals(1, loaded.repertoire.nodeCount)
            assertEquals(0, loaded.repertoire.edgeCount)
            assertEquals(listOf(RepertoireSummary(id, "Italienne", Side.WHITE, 0, 1_000L)), store.repertoires().first())
        }

    @Test
    fun aGraphWithTranspositionsComesBackIdentical() =
        runTest {
            val id = store.create("Anglaise", Side.WHITE)
            val graph = sample()
            store.save(id, graph)
            val back = store.load(id)!!.repertoire
            assertEquals(graph.signature(), back.signature())
            assertEquals(graph.nodeCount, back.nodeCount)
            assertEquals(graph.edgeCount, back.edgeCount)
            assertEquals(
                graph.edgeCount,
                store
                    .repertoires()
                    .first()
                    .single()
                    .moveCount,
            )
        }

    @Test
    fun theOrderOfMovesIsKept() =
        runTest {
            val id = store.create("Ordre", Side.WHITE)
            val graph =
                Repertoire
                    .empty(
                        Side.WHITE,
                    ).merge(PgnReader.read("1. e4 (1. d4) (1. c4) (1. Nf3) *"))
                    .repertoire
            store.save(id, graph)
            val back = store.load(id)!!.repertoire
            assertEquals(listOf("e4", "d4", "c4", "Nf3"), back.movesFrom(back.root).map { it.san })
        }

    @Test
    fun notesAreKept() =
        runTest {
            val id = store.create("Notes", Side.BLACK)
            val start = Repertoire.empty(Side.BLACK)
            val afterE4 = start.add(start.root, Move.parseUci("e2e4")!!)
            val notes =
                NodeNotes(
                    comment = "Réponse solide.\nÀ revoir.",
                    arrows = listOf(RepertoireArrow(Square.parse("c7")!!, Square.parse("c5")!!, MarkColor.GREEN)),
                    squares = listOf(RepertoireSquareMark(Square.parse("d5")!!, MarkColor.RED)),
                    source = Source("Les Échecs", chapter = "3", page = "42"),
                )
            val graph = afterE4.repertoire.withNotes(afterE4.target, notes)
            store.save(id, graph)
            val back = store.load(id)!!.repertoire
            assertEquals(notes, back.notes(afterE4.target))
            assertTrue(back.notes(back.root).isEmpty)
        }

    @Test
    fun aSourceWithOnlyABookIsKept() =
        runTest {
            val id = store.create("Livre", Side.WHITE)
            val start = Repertoire.empty(Side.WHITE)
            val graph = start.withNotes(start.root, NodeNotes(source = Source("Mon livre")))
            store.save(id, graph)
            assertEquals(
                Source("Mon livre", null, null),
                store
                    .load(id)!!
                    .repertoire
                    .notes(start.root)
                    .source,
            )
        }

    @Test
    fun savingAgainReplacesTheContent() =
        runTest {
            val id = store.create("R", Side.WHITE)
            store.save(id, sample())
            val small = Repertoire.empty(Side.WHITE).merge(PgnReader.read("1. e4 *")).repertoire
            store.save(id, small)
            assertEquals(small.signature(), store.load(id)!!.repertoire.signature())
            assertEquals(
                1,
                store
                    .repertoires()
                    .first()
                    .single()
                    .moveCount,
            )
        }

    @Test
    fun aRepertoireStartingFromAnotherPositionComesBack() =
        runTest {
            val start = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
            val id = store.create("Roques", Side.WHITE, start)
            val loaded = store.load(id)!!.repertoire
            assertEquals(PositionId.of(start), loaded.root)
            assertEquals(start, loaded.node(loaded.root)!!.position)
        }

    @Test
    fun aRepertoireFromAnotherStartIsRefused() =
        runTest {
            val id = store.create("R", Side.WHITE)
            val other = Repertoire.empty(Side.WHITE, Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"))
            assertRefused { store.save(id, other) }
            assertRefused { store.save("none", sample()) }
            assertRefused { store.save(id, Repertoire.empty(Side.BLACK)) }
        }

    private suspend fun assertRefused(block: suspend () -> Unit) {
        val error = runCatching { block() }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun renameAndDelete() =
        runTest {
            val id = store.create("Ancien", Side.WHITE)
            val other = store.create("Autre", Side.BLACK)
            store.save(id, sample())
            store.rename(id, "Nouveau")
            assertEquals("Nouveau", store.load(id)!!.name)
            assertEquals(listOf("Nouveau", "Autre"), store.repertoires().first().map { it.name })
            store.delete(id)
            assertNull(store.load(id))
            assertEquals(listOf(other), store.repertoires().first().map { it.id })
            assertEquals(
                0,
                database.repertoireContentDao().nodes(id).size + database.repertoireContentDao().edges(id).size,
            )
        }

    @Test
    fun anUnknownRepertoireIsNull() = runTest { assertNull(store.load("none")) }
}
