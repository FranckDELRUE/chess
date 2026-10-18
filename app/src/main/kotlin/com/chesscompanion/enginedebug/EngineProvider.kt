package com.chesscompanion.enginedebug

import android.content.Context
import com.chesscompanion.core.engine.ChessEngine
import com.chesscompanion.core.engine.StockfishEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Stockfish allows one session per process, so the engine lives at application scope. */
@Singleton
class EngineProvider
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val lock = Mutex()
        private var engine: ChessEngine? = null

        suspend fun get(): ChessEngine =
            lock.withLock {
                engine ?: StockfishEngine.create(context).also {
                    it.start(mapOf("Threads" to DEFAULT_THREADS.toString(), "Hash" to DEFAULT_HASH_MB.toString()))
                    engine = it
                }
            }

        companion object {
            const val DEFAULT_THREADS = 4
            const val DEFAULT_HASH_MB = 64
        }
    }
