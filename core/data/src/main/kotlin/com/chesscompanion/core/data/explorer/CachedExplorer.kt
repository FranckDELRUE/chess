package com.chesscompanion.core.data.explorer

import java.util.concurrent.TimeUnit

/**
 * [ExplorerLookup] with a Room cache (ADR 0013). A position seen less than [maxAgeMs] ago is answered
 * from the cache without network; an older one is asked again, and served as it is (from the cache) if
 * the request fails. Without token or network, a position already consulted still displays.
 */
class CachedExplorer(
    private val fetcher: ExplorerFetcher,
    private val dao: ExplorerDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val maxAgeMs: Long = TimeUnit.DAYS.toMillis(DEFAULT_DAYS),
) : ExplorerLookup {
    override suspend fun lookup(
        fen: String,
        level: ExplorerLevel,
    ): ExplorerOutcome {
        val key = canonicalFen(fen)
        val cached = dao.get(key, level.name)
        if (cached != null && now() - cached.fetchedAt < maxAgeMs) {
            return ExplorerOutcome.Loaded(cached.toStats(), fromCache = true)
        }
        return when (val result = fetcher.fetch(key, level)) {
            is ExplorerResult.Found -> {
                dao.upsert(result.stats.toEntity(key, level, now()))
                ExplorerOutcome.Loaded(result.stats, fromCache = false)
            }
            is ExplorerResult.Failed ->
                if (cached != null) {
                    ExplorerOutcome.Loaded(cached.toStats(), fromCache = true)
                } else {
                    ExplorerOutcome.Failed(result.problem)
                }
        }
    }

    private companion object {
        const val DEFAULT_DAYS = 7L
        const val FEN_FIELDS = 4

        /** Placement, side, castling and en passant: the move counters do not change the statistics. */
        fun canonicalFen(fen: String): String =
            fen
                .trim()
                .split(' ')
                .take(FEN_FIELDS)
                .joinToString(" ")
    }
}
