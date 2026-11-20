package com.chesscompanion.core.data.explorer

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** S7-7: a position already consulted shows without network; the cache falls back when the request fails. */
class CachedExplorerTest {
    private class MemoryDao : ExplorerDao {
        val rows = mutableMapOf<Pair<String, String>, ExplorerPositionEntity>()

        override suspend fun get(
            fen: String,
            level: String,
        ) = rows[fen to level]

        override suspend fun upsert(entity: ExplorerPositionEntity) {
            rows[entity.fen to entity.level] = entity
        }
    }

    private class FakeFetcher(
        var result: ExplorerResult,
    ) : ExplorerFetcher {
        val asked = mutableListOf<String>()

        override suspend fun fetch(
            fen: String,
            level: ExplorerLevel,
        ): ExplorerResult {
            asked += fen
            return result
        }
    }

    private val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private val key = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    private val stats =
        ExplorerStats(6, 2, 2, listOf(ExplorerMove("e2e4", 3, 1, 1, 1450), ExplorerMove("d2d4", 3, 1, 1, null)))
    private val dao = MemoryDao()
    private val fetcher = FakeFetcher(ExplorerResult.Found(stats))
    private var clock = 1_000L
    private val explorer = CachedExplorer(fetcher, dao, { clock }, maxAgeMs = 100)

    @Test
    fun `a fetched position is stored and answered without network afterwards`() =
        runTest {
            assertEquals(ExplorerOutcome.Loaded(stats, fromCache = false), explorer.lookup(fen, ExplorerLevel.CLUB))
            fetcher.result = ExplorerResult.Failed(ExplorerProblem.OFFLINE)

            clock += 50
            assertEquals(ExplorerOutcome.Loaded(stats, fromCache = true), explorer.lookup(fen, ExplorerLevel.CLUB))

            assertEquals(listOf(key), fetcher.asked)
        }

    @Test
    fun `the move counters of the fen do not make another entry, the level does`() =
        runTest {
            explorer.lookup(fen, ExplorerLevel.CLUB)
            explorer.lookup(key.plus(" 5 9"), ExplorerLevel.CLUB)
            explorer.lookup(fen, ExplorerLevel.STRONG)

            assertEquals(listOf(key, key), fetcher.asked)
            assertEquals(setOf(key to "CLUB", key to "STRONG"), dao.rows.keys)
        }

    @Test
    fun `an old entry is asked again and replaced`() =
        runTest {
            explorer.lookup(fen, ExplorerLevel.CLUB)
            val newer = stats.copy(white = 7)
            fetcher.result = ExplorerResult.Found(newer)

            clock += 500
            assertEquals(ExplorerOutcome.Loaded(newer, fromCache = false), explorer.lookup(fen, ExplorerLevel.CLUB))
            assertEquals(1_500L, dao.rows.getValue(key to "CLUB").fetchedAt)
        }

    @Test
    fun `an old entry is still shown, as cached, when the request fails`() =
        runTest {
            explorer.lookup(fen, ExplorerLevel.CLUB)
            fetcher.result = ExplorerResult.Failed(ExplorerProblem.OFFLINE)

            clock += 500
            assertEquals(ExplorerOutcome.Loaded(stats, fromCache = true), explorer.lookup(fen, ExplorerLevel.CLUB))
        }

    @Test
    fun `without cache a failure is reported as it is`() =
        runTest {
            ExplorerProblem.entries.forEach { problem ->
                fetcher.result = ExplorerResult.Failed(problem)
                assertEquals(ExplorerOutcome.Failed(problem), explorer.lookup(fen, ExplorerLevel.MASTER))
            }
            assertTrue(dao.rows.isEmpty())
        }
}
