package com.chesscompanion.core.data.study

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.io.IOException

/**
 * [StudyFetcher] on a Ktor [client]: one GET of the PGN of the study, without account or token. A
 * network failure is an [StudyResult.Offline] result, not an exception.
 */
class KtorStudyFetcher(
    private val client: HttpClient,
    private val baseUrl: String = LICHESS,
    private val maxBytes: Long = MAX_BYTES,
) : StudyFetcher {
    override suspend fun fetch(ref: StudyRef): StudyResult =
        try {
            answer(
                client.get(baseUrl + ref.pgnPath) { header(HttpHeaders.Accept, "application/x-chess-pgn, text/plain") },
            )
        } catch (_: IOException) {
            StudyResult.Offline
        }

    private suspend fun answer(response: HttpResponse): StudyResult {
        val declared = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
        return when {
            response.status == HttpStatusCode.OK -> if (declared > maxBytes) StudyResult.TooLarge else read(response)
            response.status == HttpStatusCode.NotFound -> StudyResult.NotFound
            response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden ->
                StudyResult.Private
            response.status == HttpStatusCode.TooManyRequests -> StudyResult.RateLimited
            else -> StudyResult.ServerError(response.status.value)
        }
    }

    private suspend fun read(response: HttpResponse): StudyResult {
        val text = response.bodyAsText()
        return if (text.length > maxBytes) StudyResult.TooLarge else StudyResult.Found(text)
    }

    companion object {
        const val LICHESS = "https://lichess.org"

        /** The biggest accepted study, in bytes (a study of hundreds of chapters is a few megabytes). */
        const val MAX_BYTES = 32L * 1024 * 1024
    }
}
