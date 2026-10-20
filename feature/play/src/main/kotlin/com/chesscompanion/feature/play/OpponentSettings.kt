package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.engine.EngineStrength

/** The side the player takes against Stockfish; [RANDOM] is drawn when the game starts. */
enum class PlayerColor { WHITE, BLACK, RANDOM }

/**
 * How Stockfish plays (S2-4): the player's colour, a strength (an ELO limit or a skill level, never
 * full strength) and the time it thinks per move.
 */
data class OpponentSettings(
    val color: PlayerColor = PlayerColor.WHITE,
    val strength: EngineStrength = EngineStrength.Elo(DEFAULT_ELO),
    val moveTimeMs: Long = DEFAULT_MOVE_TIME_MS,
) {
    init {
        require(strength !is EngineStrength.Full) { "The opponent always plays with a limited strength" }
        require(moveTimeMs > 0) { "Move time must be positive: $moveTimeMs" }
    }

    /** Text form kept with the game in the saved state. */
    fun encode(): String {
        val level =
            when (strength) {
                is EngineStrength.Elo -> "elo:${strength.elo}"
                is EngineStrength.Skill -> "skill:${strength.level}"
                EngineStrength.Full -> error("unreachable: checked at construction")
            }
        return "${color.name}|$level|$moveTimeMs"
    }

    /** "ELO 1500" or "niveau 5 sur 20", for the PGN `Event` tag and the screen. */
    val strengthLabel: String
        get() =
            when (strength) {
                is EngineStrength.Elo -> "Elo ${strength.elo}"
                is EngineStrength.Skill -> "skill ${strength.level}"
                EngineStrength.Full -> "full strength"
            }

    companion object {
        const val DEFAULT_ELO = 1500
        const val DEFAULT_MOVE_TIME_MS = 1_000L

        /** The thinking times offered, in milliseconds. */
        val MOVE_TIMES_MS = listOf(500L, 1_000L, 3_000L, 10_000L)

        /** Reads what [encode] wrote; null if the text is missing or malformed. */
        fun decode(text: String?): OpponentSettings? {
            val parts = text?.split('|') ?: return null
            if (parts.size != PARTS) return null
            val color = PlayerColor.entries.firstOrNull { it.name == parts[0] }
            val strength = parseStrength(parts[1])
            val time = parts[2].toLongOrNull()?.takeIf { it > 0 }
            return if (color == null ||
                strength == null ||
                time == null
            ) {
                null
            } else {
                OpponentSettings(color, strength, time)
            }
        }

        private fun parseStrength(text: String): EngineStrength? {
            val value = text.substringAfter(':', "").toIntOrNull() ?: return null
            return when (text.substringBefore(':')) {
                "elo" -> value.takeIf { it in EngineStrength.MIN_ELO..EngineStrength.MAX_ELO }?.let(EngineStrength::Elo)
                "skill" ->
                    value.takeIf { it in EngineStrength.MIN_SKILL..EngineStrength.MAX_SKILL }?.let(
                        EngineStrength::Skill,
                    )
                else -> null
            }
        }

        private const val PARTS = 3
    }
}

/** What the game screen shows of the game against Stockfish; null in the two-player game. */
data class OpponentUi(
    val settings: OpponentSettings,
    /** The side the player plays (the colour drawn when it was [PlayerColor.RANDOM]). */
    val humanSide: Side,
    /** Stockfish is searching its move. */
    val thinking: Boolean,
    val resigned: Boolean,
    /** The setup dialog, with its initial values; null when it is closed. */
    val setup: OpponentSettings?,
    /** The game is over (mate, draw or resignation): the analysis may be used again. */
    val over: Boolean,
)
