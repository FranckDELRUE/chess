package com.chesscompanion.core.data.explorer

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.json.JSONException
import java.io.IOException

/**
 * [ExplorerFetcher] on a Ktor [client]. The token (ADR 0013) goes in the `Authorization` header of
 * this request only. Without a token no request is made at all.
 */
class KtorExplorerFetcher(
    private val client: HttpClient,
    private val tokens: TokenStore,
    private val baseUrl: String = EXPLORER,
) : ExplorerFetcher {
    override suspend fun fetch(
        fen: String,
        level: ExplorerLevel,
    ): ExplorerResult {
        val token = tokens.get()?.takeIf { it.isNotBlank() } ?: return ExplorerResult.Failed(ExplorerProblem.NO_TOKEN)
        return try {
            answer(
                client.get("$baseUrl/lichess") {
                    parameter("fen", fen)
                    parameter("ratings", level.ratings.joinToString(","))
                    parameter("speeds", "blitz,rapid,classical")
                    parameter("moves", MOVES)
                    parameter("topGames", 0)
                    parameter("recentGames", 0)
                    header(HttpHeaders.Authorization, "Bearer $token")
                },
            )
        } catch (_: IOException) {
            ExplorerResult.Failed(ExplorerProblem.OFFLINE)
        }
    }

    private suspend fun answer(response: HttpResponse): ExplorerResult =
        when (response.status) {
            HttpStatusCode.OK ->
                try {
                    ExplorerResult.Found(ExplorerParser.parse(response.bodyAsText()))
                } catch (_: JSONException) {
                    ExplorerResult.Failed(ExplorerProblem.SERVER)
                }
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ExplorerResult.Failed(ExplorerProblem.BAD_TOKEN)
            HttpStatusCode.TooManyRequests -> ExplorerResult.Failed(ExplorerProblem.RATE_LIMITED)
            else -> ExplorerResult.Failed(ExplorerProblem.SERVER)
        }

    companion object {
        const val EXPLORER = "https://explorer.lichess.ovh"
        private const val MOVES = 12
    }
}
