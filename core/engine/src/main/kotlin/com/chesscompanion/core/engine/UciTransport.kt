package com.chesscompanion.core.engine

/** Canal texte vers un moteur UCI (JNI en production, faux transport en test). */
interface UciTransport {
    suspend fun send(line: String)

    /** Bloque jusqu'à la prochaine ligne ; null quand le moteur s'est arrêté. */
    suspend fun readLine(): String?
}
