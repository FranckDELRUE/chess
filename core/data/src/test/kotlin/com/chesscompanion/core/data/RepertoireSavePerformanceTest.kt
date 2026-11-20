package com.chesscompanion.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.srs.Fsrs
import com.chesscompanion.core.srs.FsrsCard
import com.chesscompanion.core.srs.Rating
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * S8-2: cost of `save` and `load` on a large repertoire, and proof that cards do not slow them down.
 * Timings are printed (JVM and Robolectric, so not the tablet's: the order of magnitude and the ratios count).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepertoireSavePerformanceTest {
    private lateinit var database: ChessDatabase
    private lateinit var repertoires: RoomRepertoireStore
    private lateinit var cards: RoomReviewCardStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChessDatabase::class.java).allowMainThreadQueries().build()
        repertoires = RoomRepertoireStore(database)
        cards = RoomReviewCardStore(database)
    }

    @After
    fun tearDown() = database.close()

    /** A repertoire of about [moves] moves, grown by seeded random legal moves from existing positions. */
    private fun bigRepertoire(moves: Int): Repertoire {
        val random = Random(SEED)
        var graph = Repertoire.empty(Side.WHITE)
        while (graph.edgeCount < moves) {
            val nodes = graph.allNodes.toList()
            val node = nodes[random.nextInt(nodes.size)]
            val legal = node.position.legalMoves.toList()
            if (legal.isEmpty()) continue
            graph = graph.add(node.id, legal[random.nextInt(legal.size)]).repertoire
        }
        return graph
    }

    @Test
    fun saveAndLoadOfALargeRepertoireWithAndWithoutCards() =
        runTest {
            val graph = bigRepertoire(MOVES)
            val id = repertoires.create("Gros", Side.WHITE)

            val saveMs = measureTimeMillis { repertoires.save(id, graph) }
            val loadMs =
                measureTimeMillis { assertEquals(graph.edgeCount, repertoires.load(id)!!.repertoire.edgeCount) }

            val fsrs = Fsrs()
            val card = fsrs.review(FsrsCard(), Rating.Good, Instant.parse("2026-02-01T10:00:00Z"))
            val cardMs =
                measureTimeMillis {
                    graph.allNodes.forEach { cards.put(id, it.id, card) }
                }
            val saveWithCardsMs = measureTimeMillis { repertoires.save(id, graph) }
            val readMs = measureTimeMillis { assertEquals(graph.nodeCount, cards.cards(id).size) }

            println(
                "S8-2 perf: ${graph.edgeCount} moves / ${graph.nodeCount} positions — save=$saveMs ms, " +
                    "load=$loadMs ms, ${graph.nodeCount} card puts=$cardMs ms, save with cards=$saveWithCardsMs ms, " +
                    "read all cards=$readMs ms",
            )
            assertTrue("save must not erase cards", cards.cards(id).size == graph.nodeCount)
        }

    private companion object {
        const val SEED = 8
        const val MOVES = 3_000
    }
}
