package com.chesscompanion.core.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min

/** Le même score vu par les blancs ; [whiteToMove] est le camp au trait de la position analysée. */
fun Score.forWhite(whiteToMove: Boolean): Score =
    if (whiteToMove) {
        this
    } else {
        when (this) {
            is Score.Centipawns -> Score.Centipawns(-value)
            is Score.Mate -> Score.Mate(-moves)
        }
    }

/**
 * Chances de gain dans [-1, 1] du camp dont le score est le point de vue, selon la formule de
 * Lichess (`ui/lib/src/ceval/winningChances.ts`, lila PR 11148) : sigmoïde des centipions bornés
 * à ±1000 ; un mat en n vaut (21 - min(10, |n|)) × 100 centipions.
 */
fun Score.winningChances(): Double {
    val cp =
        when (this) {
            is Score.Centipawns -> value.coerceIn(-CP_CAP, CP_CAP)
            is Score.Mate -> (MATE_BASE - min(MATE_CAP, abs(moves))) * CP_PER_PAWN * if (moves > 0) 1 else -1
        }
    return 2 / (1 + exp(WIN_MULTIPLIER * cp)) - 1
}

/** Probabilité de gain dans [0, 1], pour la barre d'évaluation. */
fun Score.winProbability(): Double = (winningChances() + 1) / 2

private const val WIN_MULTIPLIER = -0.00368208
private const val CP_CAP = 1000
private const val CP_PER_PAWN = 100
private const val MATE_BASE = 21
private const val MATE_CAP = 10
