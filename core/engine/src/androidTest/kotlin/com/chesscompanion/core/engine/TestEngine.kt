package com.chesscompanion.core.engine

import androidx.test.platform.app.InstrumentationRegistry

/** Stockfish allows one session per process: every instrumented test shares this scheduler. */
object TestEngine {
    val scheduler by lazy {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        EngineScheduler { StockfishEngine.start(context) }
    }
}
