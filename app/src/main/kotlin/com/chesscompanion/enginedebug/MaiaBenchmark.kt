package com.chesscompanion.enginedebug

import android.content.Context
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.UciEngine
import com.chesscompanion.core.engine.maia.Lc0Encoder
import com.chesscompanion.core.engine.maia.MaiaEngine
import com.chesscompanion.core.engine.maia.OnnxMaiaNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

/**
 * S0-5 measurements, one log line per result:
 * - per net and runtime config: session load, first (cold) inference, median and p90 of warm inferences;
 * - per net: chosen move of each position, its prior, legality, and Stockfish depth-16 loss in centipawns;
 * - memory with the three nets loaded at once.
 */
class MaiaBenchmark(
    private val context: Context,
    private val log: (String) -> Unit,
) {
    private class Config(
        val name: String,
        val threads: Int,
        val xnnpack: Boolean,
    )

    suspend fun run(stockfish: UciEngine) {
        val fens = BenchmarkPositions.MAIA_FENS
        log("maia start pss=${DeviceProbe.pssMb()}MB temp=${DeviceProbe.batteryTempC(context)}C")
        val bestScores =
            fens.map { fen ->
                stockfish.newGame()
                score(stockfish, fen, emptyList())
            }
        log("stockfish d$DEPTH reference done")

        NETS.forEach { net ->
            val bytes = withContext(Dispatchers.IO) { context.assets.open("maia-$net.onnx").use { it.readBytes() } }
            log("net=$net onnx=${bytes.size / BYTES_PER_KB}KB")
            CONFIGS.forEach { config -> measureSpeed(net, bytes, config) }
            measureMoves(net, bytes, stockfish, bestScores)
        }

        val before = DeviceProbe.pssMb()
        val loaded = NETS.map { net -> OnnxMaiaNetwork(context.assets.open("maia-$net.onnx").use { it.readBytes() }) }
        loaded.forEach { it.policy(Lc0Encoder.encode(fens[0])) }
        log("memory 3 nets loaded: pss $before -> ${DeviceProbe.pssMb()}MB")
        loaded.forEach { it.close() }
        log("maia done temp=${DeviceProbe.batteryTempC(context)}C")
    }

    private suspend fun measureSpeed(
        net: String,
        bytes: ByteArray,
        config: Config,
    ) = withContext(Dispatchers.Default) {
        val inputs = BenchmarkPositions.MAIA_FENS.map { Lc0Encoder.encode(it) }
        val t0 = System.nanoTime()
        val network = OnnxMaiaNetwork(bytes, config.threads, config.xnnpack)
        val t1 = System.nanoTime()
        network.policy(inputs[0])
        val t2 = System.nanoTime()
        val times =
            List(WARM_ROUNDS) {
                inputs.map { input ->
                    val start = System.nanoTime()
                    network.policy(input)
                    (System.nanoTime() - start) / NS_PER_MS
                }
            }.flatten().sorted()
        val encode = measureMs { inputs.indices.forEach { Lc0Encoder.encode(BenchmarkPositions.MAIA_FENS[it]) } }
        network.close()
        log(
            "net=$net cfg=${config.name} load=%.1fms cold=%.1fms median=%.2fms p90=%.2fms encode=%.3fms".format(
                (t1 - t0) / NS_PER_MS,
                (t2 - t1) / NS_PER_MS,
                times[times.size / 2],
                times[times.size * P90 / PERCENT],
                encode / BenchmarkPositions.MAIA_FENS.size,
            ),
        )
    }

    private suspend fun measureMoves(
        net: String,
        bytes: ByteArray,
        stockfish: UciEngine,
        bestScores: List<Int>,
    ) {
        val maia = MaiaEngine(OnnxMaiaNetwork(bytes), stockfish)
        var legal = 0
        var rawLegal = 0
        var agree = 0
        val losses = mutableListOf<Int>()
        val legalMs = mutableListOf<Double>()
        BenchmarkPositions.MAIA_FENS.forEachIndexed { i, fen ->
            val evaluation = maia.evaluate(fen)
            val top = evaluation.priors.first()
            val isLegal = top.move in stockfish.legalMoves(fen)
            stockfish.newGame()
            val loss = (bestScores[i] - score(stockfish, fen, listOf(top.move))).coerceAtLeast(0)
            if (isLegal) legal++
            if (evaluation.rawBestIsLegal) rawLegal++
            if (loss == 0) agree++
            losses += loss
            legalMs += evaluation.legalMovesMs
            log(
                "net=$net pos=${i + 1} move=${top.move} prior=%.3f legal=$isLegal raw_legal=${evaluation.rawBestIsLegal} loss=${loss}cp"
                    .format(top.prior),
            )
        }
        maia.close()
        log(
            "net=$net summary legal=$legal/20 raw_legal=$rawLegal/20 sf_agree=$agree/20 " +
                "mean_loss=${losses.average().toInt()}cp median_loss=${losses.sorted()[losses.size / 2]}cp " +
                "perft_median=%.1fms".format(legalMs.sorted()[legalMs.size / 2]),
        )
    }

    /** Stockfish score (centipawns, side to move) at depth [DEPTH], optionally restricted to [moves]. */
    private suspend fun score(
        stockfish: UciEngine,
        fen: String,
        moves: List<String>,
    ): Int {
        var last: Score? = null
        stockfish.analyze(fen, DEPTH, moves).collect { if (it is EngineEvent.Info) last = it.info.score }
        return when (val s = last) {
            is Score.Centipawns -> s.value.coerceIn(-SCORE_CAP, SCORE_CAP)
            is Score.Mate -> if (s.moves > 0) SCORE_CAP - min(abs(s.moves), MATE_SPREAD) else -SCORE_CAP
            null -> 0
        }
    }

    private inline fun measureMs(block: () -> Unit): Double {
        val start = System.nanoTime()
        block()
        return (System.nanoTime() - start) / NS_PER_MS
    }

    companion object {
        const val TAG = "MAIA_BENCH"
        private val NETS = listOf("1100", "1500", "1900")
        private val CONFIGS =
            listOf(Config("cpu1", 1, false), Config("cpu4", 4, false), Config("xnnpack1", 1, true))
        private const val DEPTH = 16
        private const val WARM_ROUNDS = 5
        private const val SCORE_CAP = 1000
        private const val MATE_SPREAD = 50
        private const val NS_PER_MS = 1e6
        private const val BYTES_PER_KB = 1024
        private const val P90 = 90
        private const val PERCENT = 100
    }
}
