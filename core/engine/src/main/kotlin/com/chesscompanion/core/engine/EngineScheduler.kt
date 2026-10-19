package com.chesscompanion.core.engine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

/** Du moins prioritaire au plus prioritaire. */
enum class EnginePriority { BACKGROUND, LIVE, OPPONENT }

/**
 * Seul accès au moteur partagé (Stockfish : une session par processus, ADR 0004 et 0008).
 *
 * Une recherche à la fois, la plus prioritaire d'abord, puis la plus ancienne à priorité égale.
 * Une recherche plus prioritaire interrompt celle en cours (`stop`, son `bestmove` est jeté) ;
 * la recherche interrompue repart ensuite du début et émet [EngineEvent.Restarted]. Chaque
 * recherche porte ses options ([SearchRequest.multiPv], [SearchRequest.strength]).
 * Le moteur est créé par [create], démarré compris, au premier besoin.
 */
class EngineScheduler(
    private val create: suspend () -> ChessEngine,
) {
    private val startLock = Mutex()

    @Volatile
    private var engine: ChessEngine? = null

    private val lock = Any()
    private val waiting = mutableListOf<Ticket>()
    private var running: Ticket? = null
    private val sequence = AtomicLong()

    private class Ticket(
        val priority: EnginePriority,
        val order: Long,
    ) {
        var turn = CompletableDeferred<Unit>()
        var search: Job? = null
        var preempted = false
    }

    /** Lance [request] dès que le moteur est libre pour [priority] ; annuler la collecte l'abandonne. */
    fun search(
        priority: EnginePriority,
        request: SearchRequest,
    ): Flow<EngineEvent> =
        channelFlow {
            val ticket = Ticket(priority, sequence.incrementAndGet())
            try {
                var restarted = false
                while (true) {
                    enqueue(ticket)
                    ticket.turn.await()
                    if (restarted) send(EngineEvent.Restarted)
                    val finished = runTurn(ticket, request)
                    release(ticket)
                    if (finished) break
                    restarted = true
                }
            } finally {
                release(ticket)
            }
        }

    /** Option globale du moteur (`Threads`, `Hash`), appliquée entre deux recherches. */
    suspend fun setOption(
        name: String,
        value: String,
    ) = engine().setOption(name, value)

    /** Arrête le moteur ; le planificateur ne peut plus servir ensuite. */
    suspend fun close() {
        engine?.close()
    }

    /** Vrai si la recherche est allée jusqu'à son `bestmove`, faux si elle a été interrompue. */
    private suspend fun ProducerScope<EngineEvent>.runTurn(
        ticket: Ticket,
        request: SearchRequest,
    ): Boolean {
        var finished = false
        coroutineScope {
            val job =
                launch {
                    engine().analyze(request).collect { event ->
                        if (event is EngineEvent.BestMove) {
                            finished = true
                            withContext(NonCancellable) { send(event) }
                        } else {
                            send(event)
                        }
                    }
                }
            synchronized(lock) {
                ticket.search = job
                if (ticket.preempted) job.cancel()
            }
        }
        return finished
    }

    private fun enqueue(ticket: Ticket) =
        synchronized(lock) {
            ticket.turn = CompletableDeferred()
            ticket.search = null
            ticket.preempted = false
            waiting += ticket
            val current = running
            if (current == null) {
                grantNext()
            } else if (ticket.priority > current.priority) {
                current.preempted = true
                current.search?.cancel()
            }
        }

    /** Libère le moteur si [ticket] le tient, et retire [ticket] de la file sinon. */
    private fun release(ticket: Ticket) =
        synchronized(lock) {
            waiting.remove(ticket)
            if (running === ticket) {
                running = null
                grantNext()
            }
        }

    private fun grantNext() {
        val next = waiting.maxWithOrNull(compareBy<Ticket> { it.priority }.thenByDescending { it.order }) ?: return
        waiting.remove(next)
        running = next
        next.turn.complete(Unit)
    }

    private suspend fun engine(): ChessEngine =
        engine ?: startLock.withLock {
            engine ?: withContext(NonCancellable) { create() }.also { engine = it }
        }
}
