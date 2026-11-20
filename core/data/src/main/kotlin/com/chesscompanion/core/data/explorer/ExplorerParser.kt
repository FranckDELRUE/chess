package com.chesscompanion.core.data.explorer

import org.json.JSONObject

/** Reads the JSON of the Lichess explorer. Only counts and UCI moves are kept, never the SAN. */
internal object ExplorerParser {
    /** @throws org.json.JSONException if the text is not an explorer answer. */
    fun parse(json: String): ExplorerStats {
        val root = JSONObject(json)
        val moves = root.optJSONArray("moves")
        return ExplorerStats(
            white = root.getLong("white"),
            draws = root.getLong("draws"),
            black = root.getLong("black"),
            moves =
                List(moves?.length() ?: 0) { i ->
                    val move = checkNotNull(moves).getJSONObject(i)
                    ExplorerMove(
                        uci = move.getString("uci"),
                        white = move.getLong("white"),
                        draws = move.getLong("draws"),
                        black = move.getLong("black"),
                        averageRating = if (move.isNull("averageRating")) null else move.optInt("averageRating"),
                    )
                },
        )
    }
}
