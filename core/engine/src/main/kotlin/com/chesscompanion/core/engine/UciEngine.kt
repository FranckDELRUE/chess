package com.chesscompanion.core.engine

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Implémentation de [ChessEngine] pour tout moteur parlant UCI via un [UciTransport].
 * [defaultOptions] sont envoyées à chaque `start`, avant les options de l'appelant.
 * Les coups légaux viennent des règles (`RulesLegalMoveProvider`), plus de Stockfish (S1-5).
 */
class UciEngine(
    private val transport: UciTransport,
    private val defaultOptions: Map<String, String> = emptyMap(),
) : ChessEngine {
    private val searchLock = Mutex()

    override suspend fun start(options: Map<String, String>) {
        transport.send("uci")
        readUntil("uciok")
        (defaultOptions + options).forEach { (name, value) -> transport.send("setoption name $name value $value") }
        sync()
    }

    private suspend fun sync() {
        transport.send("isready")
        readUntil("readyok")
    }

    override suspend fun setOption(
        name: String,
        value: String,
    ) {
        transport.send("setoption name $name value $value")
        sync()
    }

    override suspend fun newGame() {
        transport.send("ucinewgame")
        sync()
    }

    override fun analyze(
        fen: String,
        depth: Int,
    ): Flow<EngineEvent> = analyze(fen, depth, emptyList())

    /** Comme [analyze], limité à [searchMoves] (UCI `searchmoves`) quand la liste n'est pas vide. */
    fun analyze(
        fen: String,
        depth: Int,
        searchMoves: List<String>,
    ): Flow<EngineEvent> =
        flow {
            searchLock.withLock {
                transport.send("position fen $fen")
                val restrict = if (searchMoves.isEmpty()) "" else " searchmoves ${searchMoves.joinToString(" ")}"
                transport.send("go depth $depth$restrict")
                var finished = false
                try {
                    while (!finished) {
                        val line = transport.readLine() ?: break
                        val best = UciParser.parseBestMove(line)
                        if (best != null) {
                            finished = true
                            emit(best)
                        } else {
                            UciParser.parseInfo(line)?.let { emit(EngineEvent.Info(it)) }
                        }
                    }
                } finally {
                    if (!finished) withContext(NonCancellable) { stopSearch() }
                }
            }
        }

    override suspend fun close() {
        transport.send("quit")
        while (transport.readLine() != null) {
            // vide la sortie jusqu'à l'arrêt du moteur
        }
    }

    private suspend fun stopSearch() {
        transport.send("stop")
        while (true) {
            val line = transport.readLine() ?: return
            if (UciParser.parseBestMove(line) != null) return
        }
    }

    private suspend fun readUntil(token: String) {
        while (true) {
            val line = transport.readLine() ?: error("Engine stopped before '$token'")
            if (line.trim() == token) return
        }
    }
}
