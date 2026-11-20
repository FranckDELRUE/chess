package com.chesscompanion.core.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EngineSchedulerTest {
    private val fenA = "8/8/8/8/8/8/8/K6k w - - 0 1"
    private val fenB = "8/8/8/8/8/8/8/K5k1 w - - 0 1"
    private val fenC = "8/8/8/8/8/8/8/K4k2 w - - 0 1"

    private val transport = FakeUciTransport()
    private var starts = 0
    private val scheduler =
        EngineScheduler {
            starts++
            UciEngine(transport).also { it.start() }
        }

    /** Recherche longue (jusqu'à `stop`) collectée en tâche de fond. */
    private fun CoroutineScope.collectLong(
        priority: EnginePriority,
        fen: String,
        into: MutableList<EngineEvent>,
        strength: EngineStrength = EngineStrength.Full,
    ): Job =
        launch {
            scheduler
                .search(priority, SearchRequest(fen, SearchLimit.MoveTime(60_000), strength = strength))
                .collect { into += it }
        }

    private suspend fun short(
        priority: EnginePriority,
        fen: String,
        strength: EngineStrength = EngineStrength.Full,
    ) = scheduler.search(priority, SearchRequest(fen, SearchLimit.Depth(2), strength = strength)).toList()

    private fun positionsSent() =
        transport.sent
            .filter {
                it.startsWith("position")
            }.map { it.removePrefix("position fen ") }

    @Test
    fun `a single search passes the engine events through`() =
        runTest {
            val events = short(EnginePriority.LIVE, fenA)
            assertEquals(3, events.size)
            assertEquals(EngineEvent.BestMove("e2e4", "e7e5"), events.last())
        }

    @Test
    fun `the engine is started once, lazily`() =
        runTest {
            assertEquals(0, starts)
            short(EnginePriority.LIVE, fenA)
            short(EnginePriority.BACKGROUND, fenB)
            assertEquals(1, starts)
            assertEquals(1, transport.sent.count { it == "uci" })
        }

    @Test
    fun `a higher priority pre-empts the running search, which then restarts`() =
        runTest {
            val background = mutableListOf<EngineEvent>()
            val job = collectLong(EnginePriority.BACKGROUND, fenA, background)
            runCurrent()
            assertEquals(1, background.size)

            val opponent = short(EnginePriority.OPPONENT, fenB)
            runCurrent()

            assertEquals(EngineEvent.BestMove("e2e4", "e7e5"), opponent.last())
            // stop is sent before the opponent's search starts, and the stopped bestmove is dropped.
            val stop = transport.sent.indexOf("stop")
            assertTrue(stop in 0 until transport.sent.indexOf("position fen $fenB"))
            assertTrue(background.none { it is EngineEvent.BestMove })
            assertEquals(EngineEvent.Restarted, background[1])
            assertTrue(background[2] is EngineEvent.Info)
            assertEquals(listOf(fenA, fenB, fenA), positionsSent())
            job.cancel()
        }

    @Test
    fun `a lower or equal priority waits without pre-empting`() =
        runTest {
            val live = mutableListOf<EngineEvent>()
            val liveJob = collectLong(EnginePriority.LIVE, fenA, live)
            runCurrent()
            val background = mutableListOf<EngineEvent>()
            val other = mutableListOf<EngineEvent>()
            val backgroundJob = collectLong(EnginePriority.BACKGROUND, fenB, background)
            val otherLive = collectLong(EnginePriority.LIVE, fenC, other)
            runCurrent()
            assertTrue("stop" !in transport.sent)
            assertTrue(background.isEmpty() && other.isEmpty())

            liveJob.cancel()
            runCurrent()
            // The waiting LIVE search goes before the older BACKGROUND one.
            assertEquals(listOf(fenA, fenC), positionsSent())
            assertEquals(1, other.size)
            otherLive.cancel()
            runCurrent()
            assertEquals(listOf(fenA, fenC, fenB), positionsSent())
            backgroundJob.cancel()
        }

    @Test
    fun `equal priorities run in arrival order`() =
        runTest {
            val first = mutableListOf<EngineEvent>()
            val firstJob = collectLong(EnginePriority.LIVE, fenA, first)
            runCurrent()
            val jobs =
                listOf(fenB, fenC).map { fen ->
                    launch { short(EnginePriority.BACKGROUND, fen) }.also { runCurrent() }
                }
            firstJob.cancel()
            jobs.forEach { it.join() }
            assertEquals(listOf(fenA, fenB, fenC), positionsSent())
        }

    @Test
    fun `a cancelled waiting search never reaches the engine`() =
        runTest {
            val live = mutableListOf<EngineEvent>()
            val liveJob = collectLong(EnginePriority.LIVE, fenA, live)
            runCurrent()
            val waiting = launch { short(EnginePriority.BACKGROUND, fenB) }
            runCurrent()
            waiting.cancel()
            liveJob.cancel()
            runCurrent()
            short(EnginePriority.BACKGROUND, fenC)
            assertEquals(listOf(fenA, fenC), positionsSent())
        }

    @Test
    fun `cancelling the running search stops it and frees the engine`() =
        runTest {
            val live = mutableListOf<EngineEvent>()
            val liveJob = collectLong(EnginePriority.LIVE, fenA, live)
            runCurrent()
            liveJob.cancel()
            runCurrent()
            assertEquals(1, transport.sent.count { it == "stop" })
            val next = short(EnginePriority.BACKGROUND, fenB)
            assertEquals(EngineEvent.BestMove("e2e4", "e7e5"), next.last())
        }

    @Test
    fun `each search applies its own strength`() =
        runTest {
            short(EnginePriority.OPPONENT, fenA, EngineStrength.Elo(1500))
            short(EnginePriority.LIVE, fenB)
            val strength = transport.sent.filter { it.contains("UCI_LimitStrength") }
            assertEquals(
                listOf("setoption name UCI_LimitStrength value true", "setoption name UCI_LimitStrength value false"),
                strength,
            )
            // UCI_LimitStrength=false is sent before the position of the full-strength search.
            assertTrue(transport.sent.lastIndexOf(strength.last()) < transport.sent.indexOf("position fen $fenB"))
        }

    @Test
    fun `a restarted search keeps its options after the pre-empting one`() =
        runTest {
            val background = mutableListOf<EngineEvent>()
            val job = collectLong(EnginePriority.BACKGROUND, fenA, background, EngineStrength.Skill(5))
            runCurrent()
            short(EnginePriority.OPPONENT, fenB, EngineStrength.Elo(1400))
            runCurrent()
            val skills = transport.sent.filter { it.startsWith("setoption name Skill Level") }
            assertEquals(
                listOf(
                    "setoption name Skill Level value 5",
                    "setoption name Skill Level value 20",
                    "setoption name Skill Level value 5",
                ),
                skills,
            )
            job.cancel()
        }

    @Test
    fun `global options reach the engine`() =
        runTest {
            scheduler.setOption("Threads", "2")
            assertTrue("setoption name Threads value 2" in transport.sent)
        }
}
