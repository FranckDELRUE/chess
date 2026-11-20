package com.chesscompanion.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S2-5: the Room store, in memory. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomAnalysisStoreTest {
    private lateinit var database: ChessDatabase
    private lateinit var store: RoomAnalysisStore
    private var now = 1_000L

    private val start = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private val key = 0x1234_5678L

    private fun analysis(
        depth: Int,
        cp: Int = 30,
        fen: String = start,
    ) = StoredAnalysis(fen, depth, scoreCp = cp, mateIn = null, bestMove = "e2e4", pv = listOf("e2e4", "e7e5"))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ChessDatabase::class.java).allowMainThreadQueries().build()
        store = RoomAnalysisStore(database) { now++ }
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun aStoredPositionIsReadBack() =
        runTest {
            store.savePosition(key, analysis(18))
            assertEquals(analysis(18).copy(fen = positionFen(start)), store.position(key, start))
            assertNull(store.position(key + 1, start))
        }

    @Test
    fun theMoveCountersDoNotMatter() =
        runTest {
            store.savePosition(key, analysis(18))
            val sameLater = start.replace(" 0 1", " 4 7")
            assertEquals(18, store.position(key, sameLater)!!.depth)
        }

    @Test
    fun theDeepestAnalysisWins() =
        runTest {
            store.savePosition(key, analysis(20, cp = 25))
            store.savePosition(key, analysis(16, cp = -90))
            assertEquals(20, store.position(key, start)!!.depth)
            assertEquals(25, store.position(key, start)!!.scoreCp)
            store.savePosition(key, analysis(22, cp = 31))
            assertEquals(31, store.position(key, start)!!.scoreCp)
        }

    @Test
    fun aKeyCollisionNeverReturnsAnotherPosition() =
        runTest {
            val other = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"
            store.savePosition(key, analysis(20))
            assertNull(store.position(key, other))
            // Another position with the same key replaces the stored one, whatever its depth.
            store.savePosition(key, analysis(10, fen = other))
            assertEquals(10, store.position(key, other)!!.depth)
            assertNull(store.position(key, start))
        }

    @Test
    fun aMateIsKeptAsAMate() =
        runTest {
            val mate = StoredAnalysis(start, 18, scoreCp = null, mateIn = -3, bestMove = "e2e4", pv = listOf("e2e4"))
            store.savePosition(key, mate)
            assertEquals(-3, store.position(key, start)!!.mateIn)
            assertNull(store.position(key, start)!!.scoreCp)
        }

    @Test
    fun jobsAreCreatedUpdatedAndObserved() =
        runTest {
            assertNull(store.latestJob().first())
            val first = store.newJob("1. e4 *", total = 2)
            val second = store.newJob("1. d4 *", total = 2)
            assertEquals(second, store.latestJob().first()!!.id)
            store.updateJob(first, AnalysisJobState.RUNNING, done = 1)
            val job = store.job(first)!!
            assertEquals(AnalysisJobState.RUNNING, job.state)
            assertEquals(1, job.done)
            assertEquals("1. e4 *", job.pgn)
            assertEquals(AnalysisJobState.PENDING, store.job(second)!!.state)
            assertNull(store.job("missing"))
        }

    @Test
    fun cancellingTheQueueCancelsOnlyUnfinishedJobs() =
        runTest {
            val done = store.newJob("1. e4 *", total = 2)
            store.updateJob(done, AnalysisJobState.DONE, done = 2)
            val running = store.newJob("1. d4 *", total = 2)
            store.updateJob(running, AnalysisJobState.RUNNING, done = 1)
            val waiting = store.newJob("1. c4 *", total = 2)
            store.cancelUnfinished()
            assertEquals(AnalysisJobState.DONE, store.job(done)!!.state)
            assertEquals(AnalysisJobState.CANCELLED, store.job(running)!!.state)
            assertEquals(1, store.job(running)!!.done)
            assertEquals(AnalysisJobState.CANCELLED, store.job(waiting)!!.state)
        }
}
