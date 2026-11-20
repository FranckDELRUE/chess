package com.chesscompanion.core.engine

import kotlinx.coroutines.channels.Channel

/**
 * Faux moteur UCI. `go depth N` répond aussitôt N lignes `info` puis `bestmove` ; `go movetime` et
 * `go nodes` émettent une ligne `info` puis attendent `stop`, comme une longue recherche. Comme
 * Stockfish, `stop` sans recherche en cours est ignoré. [readLine] suspend tant que rien n'est à lire.
 */
class FakeUciTransport : UciTransport {
    val sent = mutableListOf<String>()
    private val output = Channel<String>(Channel.UNLIMITED)
    private var searching = false
    private var position = ""

    override suspend fun send(line: String) {
        sent += line
        when {
            line == "uci" -> emit("id name Fake", "uciok")
            line == "isready" -> emit("readyok")
            line.startsWith("position") -> position = line
            line.startsWith("go depth") -> {
                val depth = line.split(' ')[2].toInt()
                emit(*Array(depth) { "info depth ${it + 1} multipv 1 score cp ${it + 10} nodes 20 pv e2e4" })
                emit("bestmove e2e4 ponder e7e5")
            }
            line.startsWith("go") -> {
                searching = true
                emit("info depth 1 multipv 1 score cp 10 nodes 20 pv d2d4")
            }
            line == "stop" && searching -> {
                searching = false
                emit("bestmove d2d4")
            }
            line == "quit" -> output.close()
        }
    }

    override suspend fun readLine(): String? = output.receiveCatching().getOrNull()

    /** Commandes `go` et `position` envoyées, dans l'ordre. */
    val searches: List<String> get() = sent.filter { it.startsWith("go") || it.startsWith("position") }

    private fun emit(vararg lines: String) = lines.forEach { output.trySend(it) }
}
