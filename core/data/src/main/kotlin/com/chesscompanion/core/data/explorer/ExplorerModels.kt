package com.chesscompanion.core.data.explorer

/** Rating bands of the Lichess database that the statistics are limited to (Lichess `ratings` values). */
enum class ExplorerLevel(
    val ratings: List<Int>,
) {
    BEGINNER(Band.LOW),
    CLUB(Band.CLUB),
    STRONG(Band.STRONG),
    MASTER(Band.TOP),
    ;

    private object Band {
        val LOW = listOf(400, 1000)
        val CLUB = listOf(1200, 1400, 1600)
        val STRONG = listOf(1800, 2000, 2200)
        val TOP = listOf(2500)
    }
}

/** One move of a position, with the games that played it. */
data class ExplorerMove(
    val uci: String,
    val white: Long,
    val draws: Long,
    val black: Long,
    val averageRating: Int?,
) {
    val games: Long get() = white + draws + black
}

/** Statistics of one position: the games that reached it and the moves played from it. */
data class ExplorerStats(
    val white: Long,
    val draws: Long,
    val black: Long,
    val moves: List<ExplorerMove>,
) {
    val games: Long get() = white + draws + black
}

enum class ExplorerProblem { NO_TOKEN, BAD_TOKEN, RATE_LIMITED, OFFLINE, SERVER }

/** What a request to the explorer gave. A network failure is a result, not an exception. */
sealed interface ExplorerResult {
    data class Found(
        val stats: ExplorerStats,
    ) : ExplorerResult

    data class Failed(
        val problem: ExplorerProblem,
    ) : ExplorerResult
}

/** Asks the Lichess explorer about a position (FEN) for a [ExplorerLevel]. */
interface ExplorerFetcher {
    suspend fun fetch(
        fen: String,
        level: ExplorerLevel,
    ): ExplorerResult
}

/** Where the Lichess token is kept. It is secret: never logged, never sent anywhere but the explorer. */
interface TokenStore {
    fun get(): String?

    fun set(token: String?)
}

/** What the explorer view shows: [stats], and whether they come from the cache rather than the network. */
sealed interface ExplorerOutcome {
    data class Loaded(
        val stats: ExplorerStats,
        val fromCache: Boolean,
    ) : ExplorerOutcome

    data class Failed(
        val problem: ExplorerProblem,
    ) : ExplorerOutcome
}

/** Cache first, then the network; the single entry point of the UI. */
interface ExplorerLookup {
    suspend fun lookup(
        fen: String,
        level: ExplorerLevel,
    ): ExplorerOutcome
}
