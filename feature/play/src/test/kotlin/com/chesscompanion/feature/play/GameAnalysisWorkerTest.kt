package com.chesscompanion.feature.play

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.data.AnalysisJobState
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.Score
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S2-5: the worker around [GameAnalyzer], run by WorkManager's test builder. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameAnalysisWorkerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = FakeStore()

    /** Plays the first legal move of each position at depth 18. */
    private val engine =
        BackgroundEngine { request ->
            val position = request.moves.fold(Fen.parse(request.fen)) { p, uci -> p.play(Move.parseUci(uci)!!) }
            val move =
                position.legalMoves
                    .map { it.uci }
                    .sorted()
                    .first()
            flowOf(
                EngineEvent.Info(EngineInfo(depth = 18, score = Score.Centipawns(10), pv = listOf(move))),
                EngineEvent.BestMove(move),
            )
        }

    private fun worker(jobId: String?): GameAnalysisWorker =
        TestListenableWorkerBuilder<GameAnalysisWorker>(context)
            .setInputData(if (jobId == null) workDataOf() else workDataOf(GameAnalysisWorker.KEY_JOB to jobId))
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker =
                        GameAnalysisWorker(appContext, workerParameters, GameAnalyzer(engine, store), store)
                },
            ).build()

    @Test
    fun aGameIsAnalysedToTheEnd() =
        runBlocking {
            val id = store.newJob("1. e4 e5 *", total = 3)
            val result = worker(id).doWork()
            assertEquals(ListenableWorker.Result.success(), result)
            assertEquals(AnalysisJobState.DONE, store.job(id)!!.state)
            assertEquals(3, store.job(id)!!.done)
            assertEquals(3, store.positions.size)
        }

    @Test
    fun aGameTheRulesRefuseFails() =
        runBlocking {
            val id = store.newJob("1. e5 *", total = 2)
            assertTrue(worker(id).doWork() is ListenableWorker.Result.Failure)
            assertEquals(AnalysisJobState.FAILED, store.job(id)!!.state)
        }

    @Test
    fun aMissingJobFails() =
        runBlocking {
            assertTrue(worker(null).doWork() is ListenableWorker.Result.Failure)
            assertTrue(worker("missing").doWork() is ListenableWorker.Result.Failure)
        }
}
