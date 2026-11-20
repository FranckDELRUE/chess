package com.chesscompanion.core.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.concurrent.thread

/**
 * Stockfish embarqué (arm64-v8a) derrière [ChessEngine].
 * Une seule session par processus : Stockfish garde un état global, donc `close()` est définitif.
 */
object StockfishEngine {
    const val NETWORK_FILE = "nn-1a298aa575a0.nnue"

    /** Réglages des mesures de S0-4 sur la tablette (`docs/spikes/rapport-s0.md`). */
    const val DEFAULT_THREADS = 4
    const val DEFAULT_HASH_MB = 64

    /** Crée le moteur et le démarre avec [DEFAULT_THREADS] et [DEFAULT_HASH_MB]. */
    suspend fun start(context: Context): UciEngine =
        create(context).also {
            it.start(mapOf("Threads" to DEFAULT_THREADS.toString(), "Hash" to DEFAULT_HASH_MB.toString()))
        }

    /** Copie le réseau NNUE depuis les assets (au premier lancement) et crée le moteur. */
    suspend fun create(context: Context): UciEngine {
        val network = withContext(Dispatchers.IO) { installNetwork(context) }
        return UciEngine(JniTransport(StockfishNative()), mapOf("EvalFile" to network.absolutePath))
    }

    private fun installNetwork(context: Context): File {
        val target = File(context.filesDir, NETWORK_FILE)
        if (!target.exists()) {
            val tmp = File(context.filesDir, "$NETWORK_FILE.tmp")
            context.assets.open(NETWORK_FILE).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            check(tmp.renameTo(target)) { "Cannot install $NETWORK_FILE" }
        }
        return target
    }

    private class JniTransport(
        private val jni: StockfishNative,
    ) : UciTransport {
        init {
            check(jni.nativeInit()) { "Stockfish is already running in this process" }
            thread(name = "stockfish-uci", isDaemon = true) { jni.nativeRun() }
        }

        override suspend fun send(line: String) = withContext(Dispatchers.IO) { jni.nativeSend(line) }

        override suspend fun readLine(): String? = withContext(Dispatchers.IO) { jni.nativeReadLine() }
    }
}
