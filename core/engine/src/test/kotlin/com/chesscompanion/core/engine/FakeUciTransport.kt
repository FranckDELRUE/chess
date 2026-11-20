package com.chesscompanion.core.engine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.hours

/**
 * Faux moteur UCI. `go depth N` répond aussitôt N lignes `info` puis `bestmove` ; `go movetime` et
 * `go nodes` émettent une ligne `info` puis attendent `stop`, comme une longue recherche. Comme
 * Stockfish, `stop` sans recherche en cours est ignoré. [readLine] suspend tant que rien n'est à lire.
 */
class FakeUciTransport : UciTransport {
    val sent = mutableListOf<String>()
    private val output = Channel<String>(Channel.UNLIMITED)
    private var searching = false

    /** When set, `go` waits for it before the engine starts: a window in which a search can be cancelled. */
    var goGate: CompletableDeferred<Unit>? = null
    private var position = ""
    private var stopSent = false

    /** Lines read while a search was running and before `stop`: zero when a cancelled search stops at once. */
    var readsWhileSearching = 0
        private set

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
                goGate?.await()
                searching = true
                stopSent = false
                emit("info depth 1 multipv 1 score cp 10 nodes 20 pv d2d4")
            }
            line == "stop" && searching -> {
                searching = false
                stopSent = true
                emit("bestmove d2d4")
            }
            line == "quit" -> output.close()
        }
    }

    /** A silent engine fails the test (in virtual time) instead of hanging it. */
    override suspend fun readLine(): String? {
        val line = withTimeout(1.hours) { output.receiveCatching().getOrNull() }
        if (line != null && searching && !stopSent) readsWhileSearching++
        return line
    }

    /** Commandes `go` et `position` envoyées, dans l'ordre. */
    val searches: List<String> get() = sent.filter { it.startsWith("go") || it.startsWith("position") }

    private fun emit(vararg lines: String) = lines.forEach { output.trySend(it) }
}
