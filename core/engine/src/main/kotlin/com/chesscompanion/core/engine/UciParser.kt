package com.chesscompanion.core.engine

/** Analyse les lignes de sortie du protocole UCI. Fonctions pures, testées en JVM. */
object UciParser {
    /** Retourne null pour les lignes sans profondeur ni score (`info string`, `currmove`…). */
    fun parseInfo(line: String): EngineInfo? {
        val tokens = tokenize(line)
        val pvIndex = tokens.indexOf("pv")
        val head = if (pvIndex >= 0) tokens.subList(0, pvIndex) else tokens
        val depth = head.valueAfter("depth")?.toIntOrNull()
        val score = parseScore(head)
        if (tokens.firstOrNull() != "info" || depth == null || score == null) return null
        return EngineInfo(
            depth = depth,
            multiPv = head.valueAfter("multipv")?.toIntOrNull() ?: 1,
            score = score,
            nodes = head.valueAfter("nodes")?.toLongOrNull(),
            nodesPerSecond = head.valueAfter("nps")?.toLongOrNull(),
            timeMs = head.valueAfter("time")?.toLongOrNull(),
            pv = if (pvIndex >= 0) tokens.drop(pvIndex + 1) else emptyList(),
        )
    }

    fun parseBestMove(line: String): EngineEvent.BestMove? {
        val tokens = tokenize(line)
        if (tokens.firstOrNull() != "bestmove" || tokens.size < 2) return null
        return EngineEvent.BestMove(tokens[1], tokens.valueAfter("ponder"))
    }

    private fun tokenize(line: String) = line.trim().split(' ').filter { it.isNotEmpty() }

    private fun List<String>.valueAfter(key: String): String? =
        indexOf(key).takeIf { it >= 0 }?.let { getOrNull(it + 1) }

    private fun parseScore(tokens: List<String>): Score? {
        val at = tokens.indexOf("score")
        if (at < 0) return null
        val value = tokens.getOrNull(at + 2)?.toIntOrNull() ?: return null
        return when (tokens.getOrNull(at + 1)) {
            "cp" -> Score.Centipawns(value)
            "mate" -> Score.Mate(value)
            else -> null
        }
    }
}
