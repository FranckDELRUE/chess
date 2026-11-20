package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.data.explorer.ExplorerLevel
import com.chesscompanion.core.data.explorer.ExplorerProblem

/** One move of the explorer: [label] in French SAN, shares of the games in whole percent. */
data class ExplorerRow(
    val uci: String,
    val label: String,
    val games: Long,
    val whitePercent: Int,
    val drawPercent: Int,
    val blackPercent: Int,
    /** The repertoire already has this move from the position shown. */
    val known: Boolean,
)

sealed interface ExplorerContent {
    data object Loading : ExplorerContent

    data class Loaded(
        val rows: List<ExplorerRow>,
        val games: Long,
        /** Answered from the cache, without network. */
        val fromCache: Boolean,
    ) : ExplorerContent

    data class Problem(
        val problem: ExplorerProblem,
    ) : ExplorerContent
}

/** The explorer panel (S7-7): statistics of the position shown and the engine's evaluation. */
data class ExplorerUiState(
    val level: ExplorerLevel = ExplorerLevel.CLUB,
    val hasToken: Boolean = false,
    val content: ExplorerContent = ExplorerContent.Loading,
    /** Evaluation from White's point of view (`+0,35`, `#3`), null until the engine has answered. */
    val evaluation: String? = null,
)

sealed interface ExplorerAction {
    data class SetLevel(
        val level: ExplorerLevel,
    ) : ExplorerAction

    data class SaveToken(
        val text: String,
    ) : ExplorerAction

    data object ClearToken : ExplorerAction
}
