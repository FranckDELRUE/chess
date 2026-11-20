package com.chesscompanion.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.srs.Fsrs
import com.chesscompanion.core.srs.FsrsCard
import com.chesscompanion.core.srs.Rating
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
import java.time.Duration
import java.time.Instant

/** S8-2: review cards on Room, in memory. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReviewCardStoreTest {
    private lateinit var database: ChessDatabase
    private lateinit var repertoires: RoomRepertoireStore
    private lateinit var cards: RoomReviewCardStore
    private val fsrs = Fsrs()
    private val t0 = Instant.parse("2026-02-01T10:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChessDatabase::class.java).allowMainThreadQueries().build()
        repertoires = RoomRepertoireStore(database)
        cards = RoomReviewCardStore(database)
    }

    @After
    fun tearDown() = database.close()

    private fun graph(pgn: String): Repertoire =
        PgnReader.readAll(pgn).games.fold(Repertoire.empty(Side.WHITE)) { r, g -> r.merge(g).repertoire }

    @Test
    fun aPositionWithNoCardHasNeverBeenReviewed() =
        runTest {
            val id = repertoires.create("Anglaise", Side.WHITE)
            assertNull(cards.get(id, PositionId(1L, "fen")))
            assertTrue(cards.cards(id).isEmpty())
        }

    @Test
    fun aCardComesBackIdentical() =
        runTest {
            val id = repertoires.create("Anglaise", Side.WHITE)
            val position = PositionId(42L, "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq -")
            val card = fsrs.review(FsrsCard(), Rating.Good, t0)
            cards.put(id, position, card)
            assertEquals(card, cards.get(id, position))
            assertEquals(mapOf(position to card), cards.cards(id))
        }

    @Test
    fun putReplacesTheCardOfThePosition() =
        runTest {
            val id = repertoires.create("Anglaise", Side.WHITE)
            val position = PositionId(7L, "fen")
            val first = fsrs.review(FsrsCard(), Rating.Good, t0)
            val second = fsrs.review(first, Rating.Again, first.due!!)
            cards.put(id, position, first)
            cards.put(id, position, second)
            assertEquals(second, cards.get(id, position))
            assertEquals(1, cards.cards(id).size)
        }

    @Test
    fun cardsOfTwoRepertoiresStayApart() =
        runTest {
            val a = repertoires.create("A", Side.WHITE)
            val b = repertoires.create("B", Side.WHITE)
            val position = PositionId(7L, "fen")
            cards.put(a, position, fsrs.review(FsrsCard(), Rating.Good, t0))
            assertNull(cards.get(b, position))
        }

    @Test
    fun dueCountCountsReviewedPositionsOnly() =
        runTest {
            val id = repertoires.create("Anglaise", Side.WHITE)
            val failed = fsrs.review(FsrsCard(), Rating.Again, t0)
            val passed = fsrs.review(FsrsCard(), Rating.Easy, t0)
            cards.put(id, PositionId(1L, "a"), failed)
            cards.put(id, PositionId(2L, "b"), passed)
            assertEquals(0, cards.dueCount(id, t0))
            assertEquals(1, cards.dueCount(id, t0 + Duration.ofDays(1)))
            assertEquals(2, cards.dueCount(id, t0 + Duration.ofDays(30)))
        }

    @Test
    fun savingTheRepertoireKeepsItsCards() =
        runTest {
            val id = repertoires.create("Anglaise", Side.WHITE)
            val graph = graph("1. d4 Nf6 2. c4 e6 *")
            repertoires.save(id, graph)
            val position = graph.allNodes.last().id
            val card = fsrs.review(FsrsCard(), Rating.Good, t0)
            cards.put(id, position, card)

            repertoires.save(id, graph)
            repertoires.save(id, Repertoire.empty(Side.WHITE))

            assertEquals(card, cards.get(id, position))
        }

    @Test
    fun deletingTheRepertoireDeletesItsCards() =
        runTest {
            val id = repertoires.create("Anglaise", Side.WHITE)
            val other = repertoires.create("Autre", Side.WHITE)
            val position = PositionId(7L, "fen")
            val card = fsrs.review(FsrsCard(), Rating.Good, t0)
            cards.put(id, position, card)
            cards.put(other, position, card)

            repertoires.delete(id)

            assertTrue(cards.cards(id).isEmpty())
            assertEquals(card, cards.get(other, position))
        }

    @Test
    fun retainOnlyDeletesTheCardsOfRemovedPositions() =
        runTest {
            val id = repertoires.create("Anglaise", Side.WHITE)
            val keep = PositionId(1L, "keep")
            val drop = PositionId(2L, "drop")
            val card = fsrs.review(FsrsCard(), Rating.Good, t0)
            cards.put(id, keep, card)
            cards.put(id, drop, card)

            assertEquals(1, cards.retainOnly(id, setOf(keep)))

            assertEquals(setOf(keep), cards.cards(id).keys)
        }
}
