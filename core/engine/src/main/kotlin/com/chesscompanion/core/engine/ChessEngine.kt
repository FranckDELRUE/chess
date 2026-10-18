package com.chesscompanion.core.engine

import kotlinx.coroutines.flow.Flow

/** Point d'entrée unique vers les moteurs (Stockfish, Maia, station PC). */
interface ChessEngine {
    /** Démarre le moteur et applique [options] (noms UCI, ex. `Threads`, `Hash`). */
    suspend fun start(options: Map<String, String> = emptyMap())

    /** Change une option UCI (ex. `Threads`) sur un moteur démarré. */
    suspend fun setOption(
        name: String,
        value: String,
    )

    /** Oublie la partie en cours et vide les tables de hachage (mesures reproductibles). */
    suspend fun newGame()

    /**
     * Analyse [fen] jusqu'à la profondeur [depth]. Émet les [EngineEvent.Info] puis un
     * [EngineEvent.BestMove] final. Annuler la collecte arrête la recherche.
     * Les analyses sont sérialisées : une seule recherche à la fois.
     */
    fun analyze(
        fen: String,
        depth: Int,
    ): Flow<EngineEvent>

    /** Arrête le moteur. Un moteur fermé ne peut pas être redémarré. */
    suspend fun close()
}

sealed interface EngineEvent {
    data class Info(
        val info: EngineInfo,
    ) : EngineEvent

    /** Coup en notation UCI (ex. `e2e4`) ; `(none)` si la position n'a aucun coup légal. */
    data class BestMove(
        val move: String,
        val ponder: String? = null,
    ) : EngineEvent
}

data class EngineInfo(
    val depth: Int,
    val multiPv: Int = 1,
    val score: Score,
    val nodes: Long? = null,
    val nodesPerSecond: Long? = null,
    val timeMs: Long? = null,
    val pv: List<String> = emptyList(),
)

sealed interface Score {
    /** Évaluation en centipions, du point de vue du camp au trait. */
    data class Centipawns(
        val value: Int,
    ) : Score

    /** Mat en [moves] coups (négatif si le camp au trait est maté). */
    data class Mate(
        val moves: Int,
    ) : Score
}
