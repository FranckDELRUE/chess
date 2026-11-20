package com.chesscompanion.core.engine

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implémentation de [ChessEngine] pour tout moteur parlant UCI via un [UciTransport].
 * [defaultOptions] sont envoyées à chaque `start`, avant les options de l'appelant.
 * Les coups légaux viennent des règles (`RulesLegalMoveProvider`), plus de Stockfish (S1-5).
 *
 * Chaque recherche applique ses propres options (MultiPV, force) : seules celles qui diffèrent de
 * la recherche précédente sont envoyées. Les échanges avec le moteur ne sont jamais interrompus au
 * milieu (lecture d'une ligne, attente de `readyok`) : une annulation envoie `stop` puis lit la
 * sortie jusqu'au `bestmove`, pour que la recherche suivante parte d'un flux propre.
 */
class UciEngine(
    private val transport: UciTransport,
    private val defaultOptions: Map<String, String> = emptyMap(),
) : ChessEngine {
    private val searchLock = Mutex()
    private val applied = mutableMapOf<String, String>()

    override suspend fun start(options: Map<String, String>) =
        withContext(NonCancellable) {
            transport.send("uci")
            readUntil("uciok")
            (defaultOptions + options).forEach { (name, value) -> sendOption(name, value) }
            sync()
        }

    private suspend fun sync() {
        transport.send("isready")
        readUntil("readyok")
    }

    override suspend fun setOption(
        name: String,
        value: String,
    ) = searchLock.withLock {
        withContext(NonCancellable) {
            sendOption(name, value)
            sync()
        }
    }

    override suspend fun newGame() =
        searchLock.withLock {
            withContext(NonCancellable) {
                transport.send("ucinewgame")
                sync()
            }
        }

    override fun analyze(request: SearchRequest): Flow<EngineEvent> =
        channelFlow {
            searchLock.withLock {
                withContext(NonCancellable) { begin(request) }
                readSearch()
            }
        }

    /** Envoie la position et `go`, après les options qui changent et un éventuel `ucinewgame`. */
    private suspend fun begin(request: SearchRequest) {
        val changed = searchOptions(request).filter { (name, value) -> applied[name] != value }
        changed.forEach { (name, value) -> sendOption(name, value) }
        if (request.newGame) transport.send("ucinewgame")
        if (changed.isNotEmpty() || request.newGame) sync()
        val moves = if (request.moves.isEmpty()) "" else " moves ${request.moves.joinToString(" ")}"
        transport.send("position fen ${request.fen}$moves")
        transport.send(goCommand(request))
    }

    /**
     * Relaie les `info` jusqu'au `bestmove`. Les lectures sont non annulables (une ligne lue n'est
     * jamais perdue) ; un observateur envoie `stop` dès l'annulation pour ne pas attendre la
     * prochaine ligne du moteur.
     */
    private suspend fun ProducerScope<EngineEvent>.readSearch() {
        val finished = AtomicBoolean(false)
        val stopSent = AtomicBoolean(false)
        val sendStop: suspend () -> Unit = {
            if (!finished.get() && stopSent.compareAndSet(false, true)) {
                withContext(NonCancellable) { transport.send("stop") }
            }
        }
        val watcher =
            launch {
                try {
                    awaitCancellation()
                } finally {
                    sendStop()
                }
            }
        try {
            while (!finished.get()) {
                val line = withContext(NonCancellable) { transport.readLine() } ?: break
                val best = UciParser.parseBestMove(line)
                if (best != null) {
                    finished.set(true)
                    send(best)
                } else {
                    UciParser.parseInfo(line)?.let { send(EngineEvent.Info(it)) }
                }
            }
        } finally {
            val drain = !finished.get()
            watcher.cancel()
            if (drain) {
                withContext(NonCancellable) {
                    sendStop()
                    readUntilBestMove()
                }
            }
        }
    }

    override suspend fun close() {
        transport.send("quit")
        while (transport.readLine() != null) {
            // vide la sortie jusqu'à l'arrêt du moteur
        }
    }

    private suspend fun sendOption(
        name: String,
        value: String,
    ) {
        transport.send("setoption name $name value $value")
        applied[name] = value
    }

    private suspend fun readUntilBestMove() {
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

    companion object {
        const val MULTI_PV = "MultiPV"
        const val SKILL_LEVEL = "Skill Level"
        const val LIMIT_STRENGTH = "UCI_LimitStrength"
        const val ELO = "UCI_Elo"

        /** Options UCI propres à une recherche ; la force reste maximale hors [EngineStrength.Skill]. */
        fun searchOptions(request: SearchRequest): Map<String, String> {
            val strength = request.strength
            val skill = if (strength is EngineStrength.Skill) strength.level else EngineStrength.MAX_SKILL
            return buildMap {
                put(MULTI_PV, request.multiPv.toString())
                put(SKILL_LEVEL, skill.toString())
                put(LIMIT_STRENGTH, (strength is EngineStrength.Elo).toString())
                if (strength is EngineStrength.Elo) put(ELO, strength.elo.toString())
            }
        }

        fun goCommand(request: SearchRequest): String {
            val limit =
                when (val l = request.limit) {
                    is SearchLimit.Depth -> "depth ${l.plies}"
                    is SearchLimit.MoveTime -> "movetime ${l.millis}"
                    is SearchLimit.Nodes -> "nodes ${l.count}"
                }
            val restrict =
                if (request.searchMoves.isEmpty()) {
                    ""
                } else {
                    " searchmoves ${request.searchMoves.joinToString(
                        " ",
                    )}"
                }
            return "go $limit$restrict"
        }
    }
}
