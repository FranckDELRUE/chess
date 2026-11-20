package com.chesscompanion.core.data.explorer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.UnknownHostException

/** S7-7: the requests to the explorer and the analysis of its answers, on a fake transport. */
@RunWith(RobolectricTestRunner::class) // org.json needs the Android classes
@Config(sdk = [35])
class KtorExplorerFetcherTest {
    private class Tokens(
        var value: String?,
    ) : TokenStore {
        override fun get() = value

        override fun set(token: String?) {
            value = token
        }
    }

    private val start = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -"
    private val json =
        """{"white":60,"draws":20,"black":20,"moves":[
          {"uci":"e2e4","san":"e4","white":30,"draws":10,"black":10,"averageRating":1450},
          {"uci":"d2d4","san":"d4","white":20,"draws":8,"black":8,"averageRating":null}],"topGames":[]}"""

    private fun fetcher(
        token: String?,
        requests: MutableList<HttpRequestData> = mutableListOf(),
        reply: () -> Pair<HttpStatusCode, String> = { HttpStatusCode.OK to json },
    ) = KtorExplorerFetcher(
        HttpClient(
            MockEngine { request ->
                requests += request
                val (status, body) = reply()
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
        Tokens(token),
        baseUrl = "https://explorer.test",
    )

    private fun fetch(
        fetcher: KtorExplorerFetcher,
        level: ExplorerLevel = ExplorerLevel.CLUB,
    ) = runBlocking { fetcher.fetch(start, level) }

    @Test
    fun `asks for the position and level with the token, and reads the moves`() {
        val requests = mutableListOf<HttpRequestData>()

        val result = fetch(fetcher("lip_secret", requests))

        val request = requests.single()
        assertEquals("explorer.test", request.url.host)
        assertEquals("/lichess", request.url.encodedPath)
        assertEquals(start, request.url.parameters["fen"])
        assertEquals("1200,1400,1600", request.url.parameters["ratings"])
        assertEquals("Bearer lip_secret", request.headers[HttpHeaders.Authorization])
        val stats = (result as ExplorerResult.Found).stats
        assertEquals(100L, stats.games)
        assertEquals(listOf("e2e4", "d2d4"), stats.moves.map { it.uci })
        assertEquals(50L, stats.moves[0].games)
        assertEquals(1450, stats.moves[0].averageRating)
        assertNull(stats.moves[1].averageRating)
    }

    @Test
    fun `without a token nothing is requested`() {
        val requests = mutableListOf<HttpRequestData>()

        listOf(null, "", "  ").forEach {
            assertEquals(ExplorerResult.Failed(ExplorerProblem.NO_TOKEN), fetch(fetcher(it, requests)))
        }

        assertTrue(requests.isEmpty())
    }

    @Test
    fun `each http failure has its own problem`() {
        mapOf(
            HttpStatusCode.Unauthorized to ExplorerProblem.BAD_TOKEN,
            HttpStatusCode.Forbidden to ExplorerProblem.BAD_TOKEN,
            HttpStatusCode.TooManyRequests to ExplorerProblem.RATE_LIMITED,
            HttpStatusCode.InternalServerError to ExplorerProblem.SERVER,
            HttpStatusCode.NotFound to ExplorerProblem.SERVER,
        ).forEach { (status, problem) ->
            assertEquals(
                "status ${status.value}",
                ExplorerResult.Failed(problem),
                fetch(fetcher("t") { status to "" }),
            )
        }
    }

    @Test
    fun `an answer that is not an explorer answer is a server problem`() {
        assertEquals(
            ExplorerResult.Failed(ExplorerProblem.SERVER),
            fetch(fetcher("t") { HttpStatusCode.OK to "<html>oops</html>" }),
        )
    }

    @Test
    fun `a network failure is offline, not an exception`() {
        listOf(IOException("down"), UnknownHostException("explorer.test")).forEach { failure ->
            val offline =
                KtorExplorerFetcher(
                    HttpClient(MockEngine { throw failure }),
                    Tokens("t"),
                    baseUrl = "https://explorer.test",
                )
            assertEquals(ExplorerResult.Failed(ExplorerProblem.OFFLINE), fetch(offline))
        }
    }

    @Test
    fun `respondError helper is not used for the token`() {
        // The token must be sent to the explorer only: a request to another host is never built here.
        val requests = mutableListOf<HttpRequestData>()
        val client =
            HttpClient(
                MockEngine {
                    requests += it
                    respondError(HttpStatusCode.Unauthorized)
                },
            )

        runBlocking {
            KtorExplorerFetcher(
                client,
                Tokens("t"),
                "https://explorer.test",
            ).fetch(start, ExplorerLevel.MASTER)
        }

        assertEquals(setOf("explorer.test"), requests.map { it.url.host }.toSet())
        assertEquals("2500", requests.single().url.parameters["ratings"])
    }
}
