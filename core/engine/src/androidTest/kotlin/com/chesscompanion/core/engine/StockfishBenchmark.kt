package com.chesscompanion.core.engine

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * S2-2: time for Stockfish to complete depths 16, 18 and 20 on the 20 Heidoji middlegames, with the
 * S0-4 method (4 threads, Hash 64 MB, `ucinewgame` before each search, wall time to `bestmove`).
 * Depth 18 is run three times per position (multi-threaded search is not deterministic). The engine
 * is started by an untimed search first.
 * Excluded by default; run with `gradlew :core:engine:connectedDebugAndroidTest -PengineBenchmark`
 * and read the results with `adb logcat -d -s SF_BENCH` (enlarge the log buffer first with
 * `adb logcat -G 4M`: the default 256 KiB loses the first lines).
 */
@RunWith(AndroidJUnit4::class)
class StockfishBenchmark {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun timeToDepthOnHeidojiMiddlegames() =
        runBlocking {
            val fens =
                instrumentation.context.assets
                    .open(POSITIONS)
                    .bufferedReader()
                    .readLines()
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { it.substringBefore(" ;") }
            // Untimed: the scheduler starts Stockfish (and installs the NNUE network) on first use.
            search(fens.first(), WARM_UP_DEPTH)
            log("start positions=${fens.size} temp=${batteryTempC()}C pss=${pssMb()}MB")
            fens.forEachIndexed { index, fen ->
                RUNS.forEach { (depth, runs) ->
                    repeat(runs) { run -> log("pos=${index + 1} depth=$depth run=${run + 1} ${search(fen, depth)}") }
                }
            }
            log("done temp=${batteryTempC()}C pss=${pssMb()}MB")
        }

    private suspend fun search(
        fen: String,
        depth: Int,
    ): String {
        val request = SearchRequest(fen, SearchLimit.Depth(depth), newGame = true)
        var last: EngineInfo? = null
        var best: String? = null
        var restarted = false
        val start = System.nanoTime()
        TestEngine.scheduler.search(EnginePriority.BACKGROUND, request).collect { event ->
            when (event) {
                is EngineEvent.Info -> last = event.info
                is EngineEvent.BestMove -> best = event.move
                EngineEvent.Restarted -> restarted = true
            }
        }
        val wallMs = (System.nanoTime() - start) / NS_PER_MS
        val score =
            when (val s = last?.score) {
                is Score.Centipawns -> "cp ${s.value}"
                is Score.Mate -> "mate ${s.moves}"
                null -> "-"
            }
        return "wall_ms=$wallMs nodes=${last?.nodes} nps=${last?.nodesPerSecond} score=$score best=$best" +
            if (restarted) " restarted" else ""
    }

    private fun batteryTempC(): Float? {
        val intent = instrumentation.targetContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        return if (tenths == Int.MIN_VALUE) null else tenths / TENTHS
    }

    private fun pssMb() = Debug.getPss() / KB_PER_MB

    private fun log(line: String) {
        Log.i(TAG, line)
    }

    private companion object {
        const val TAG = "SF_BENCH"
        const val POSITIONS = "heidoji-middlegames.txt"
        val RUNS = listOf(16 to 1, 18 to 3, 20 to 1)
        const val WARM_UP_DEPTH = 1
        const val NS_PER_MS = 1_000_000L
        const val KB_PER_MB = 1024L
        const val TENTHS = 10f
    }
}
