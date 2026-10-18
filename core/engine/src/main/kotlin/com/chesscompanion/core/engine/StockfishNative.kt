package com.chesscompanion.core.engine

/** Pont JNI vers `libstockfish_jni.so` (voir `src/main/cpp/stockfish_jni.cpp`). */
internal class StockfishNative {
    external fun nativeInit(): Boolean

    /** Bloque jusqu'à `quit` : à appeler depuis un thread dédié. */
    external fun nativeRun()

    external fun nativeSend(line: String)

    /** Bloque jusqu'à la prochaine ligne ; null en fin de flux. */
    external fun nativeReadLine(): String?

    companion object {
        init {
            System.loadLibrary("stockfish_jni")
        }
    }
}
