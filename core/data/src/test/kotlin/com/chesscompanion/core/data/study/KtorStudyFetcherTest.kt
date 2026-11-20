package com.chesscompanion.core.data.study

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.UnknownHostException

/** The fetcher on a fake transport: no network in these tests. */
class KtorStudyFetcherTest {
    private val ref = StudyRef("Y1yXP80U", "oxgS1aRW")

    private fun fetcher(
        maxBytes: Long = KtorStudyFetcher.MAX_BYTES,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = KtorStudyFetcher(HttpClient(MockEngine(handler)), maxBytes = maxBytes)

    private fun failing(status: HttpStatusCode) = fetcher { respondError(status) }

    @Test
    fun `the pgn is asked on the study path of the api, without credentials`() =
        runTest {
            var asked: HttpRequestData? = null
            val pgn = "[Event \"x\"]\n\n1. e4 *\n"
            val result =
                fetcher {
                    asked = it
                    respond(pgn, HttpStatusCode.OK)
                }.fetch(ref)
            assertEquals(StudyResult.Found(pgn), result)
            assertEquals("https://lichess.org/api/study/Y1yXP80U/oxgS1aRW.pgn", asked?.url.toString())
            assertEquals(false, asked?.headers?.contains(HttpHeaders.Authorization))
            assertEquals(false, asked?.headers?.contains(HttpHeaders.Cookie))
        }

    @Test
    fun `http errors are told apart`() =
        runTest {
            assertEquals(StudyResult.NotFound, failing(HttpStatusCode.NotFound).fetch(ref))
            assertEquals(StudyResult.Private, failing(HttpStatusCode.Forbidden).fetch(ref))
            assertEquals(StudyResult.Private, failing(HttpStatusCode.Unauthorized).fetch(ref))
            assertEquals(StudyResult.RateLimited, failing(HttpStatusCode.TooManyRequests).fetch(ref))
            assertEquals(StudyResult.ServerError(500), failing(HttpStatusCode.InternalServerError).fetch(ref))
            assertEquals(StudyResult.ServerError(503), failing(HttpStatusCode.ServiceUnavailable).fetch(ref))
            assertEquals(StudyResult.ServerError(400), failing(HttpStatusCode.BadRequest).fetch(ref))
        }

    @Test
    fun `a network failure is offline, not an exception`() =
        runTest {
            assertEquals(StudyResult.Offline, fetcher { throw IOException("no route") }.fetch(ref))
            assertEquals(StudyResult.Offline, fetcher { throw UnknownHostException("lichess.org") }.fetch(ref))
        }

    @Test
    fun `a declared size over the limit is refused before reading`() =
        runTest {
            assertEquals(
                StudyResult.TooLarge,
                fetcher(maxBytes = 10) { respond("x".repeat(11), HttpStatusCode.OK) }.fetch(ref),
            )
            assertEquals(
                StudyResult.Found("x".repeat(10)),
                fetcher(maxBytes = 10) { respond("x".repeat(10), HttpStatusCode.OK) }.fetch(ref),
            )
        }

    @Test
    fun `an empty answer is a found study with no text`() =
        runTest { assertEquals(StudyResult.Found(""), fetcher { respond("", HttpStatusCode.OK) }.fetch(ref)) }
}
