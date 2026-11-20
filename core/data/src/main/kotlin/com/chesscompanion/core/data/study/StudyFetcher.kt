package com.chesscompanion.core.data.study

/** What fetching a study gave. Anything but [Found] leaves the rest of the app untouched. */
sealed interface StudyResult {
    data class Found(
        val pgn: String,
    ) : StudyResult

    /** The study or chapter does not exist. */
    data object NotFound : StudyResult

    /** The study is not public. */
    data object Private : StudyResult

    /** Too many requests to Lichess: try again in a moment. */
    data object RateLimited : StudyResult

    /** No network, or Lichess cannot be reached. */
    data object Offline : StudyResult

    data class ServerError(
        val status: Int,
    ) : StudyResult

    /** The answer is bigger than what is accepted. */
    data object TooLarge : StudyResult
}

/** Gets the PGN of a public Lichess study (S7-6). */
interface StudyFetcher {
    suspend fun fetch(ref: StudyRef): StudyResult
}
