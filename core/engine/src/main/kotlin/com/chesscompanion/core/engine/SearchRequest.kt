package com.chesscompanion.core.engine

/**
 * Une recherche : position de départ [fen] suivie des coups [moves] (UCI), ce qui laisse le moteur
 * voir l'historique (répétitions) ; [limit] arrête la recherche.
 */
data class SearchRequest(
    val fen: String,
    val limit: SearchLimit,
    val moves: List<String> = emptyList(),
    /** Restreint la recherche à ces coups (UCI `searchmoves`) quand la liste n'est pas vide. */
    val searchMoves: List<String> = emptyList(),
    /** Nombre de lignes principales (UCI `MultiPV`). */
    val multiPv: Int = 1,
    val strength: EngineStrength = EngineStrength.Full,
    /** Envoie `ucinewgame` avant la recherche : tables vidées, mesures reproductibles. */
    val newGame: Boolean = false,
) {
    init {
        require(multiPv >= 1) { "multiPv must be at least 1: $multiPv" }
    }

    /** Camp au trait dans la position analysée (après [moves]) : le point de vue des scores UCI. */
    val whiteToMove: Boolean
        get() = (fen.trim().split(' ').getOrNull(1) == "w") == (moves.size % 2 == 0)
}

sealed interface SearchLimit {
    data class Depth(
        val plies: Int,
    ) : SearchLimit

    data class MoveTime(
        val millis: Long,
    ) : SearchLimit

    data class Nodes(
        val count: Long,
    ) : SearchLimit
}

/** Force de jeu de Stockfish. Les bornes viennent de `engine.cpp` et `search.h` (Stockfish 19). */
sealed interface EngineStrength {
    data object Full : EngineStrength

    /** Option `Skill Level`, de 0 (le plus faible) à 20 (pleine force). */
    data class Skill(
        val level: Int,
    ) : EngineStrength {
        init {
            require(level in MIN_SKILL..MAX_SKILL) { "Skill level out of range: $level" }
        }
    }

    /** Options `UCI_LimitStrength` et `UCI_Elo`. */
    data class Elo(
        val elo: Int,
    ) : EngineStrength {
        init {
            require(elo in MIN_ELO..MAX_ELO) { "Elo out of range: $elo" }
        }
    }

    companion object {
        const val MIN_SKILL = 0
        const val MAX_SKILL = 20
        const val MIN_ELO = 1320
        const val MAX_ELO = 3190
    }
}
