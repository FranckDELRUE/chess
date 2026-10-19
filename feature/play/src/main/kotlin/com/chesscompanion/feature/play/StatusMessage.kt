package com.chesscompanion.feature.play

import androidx.annotation.StringRes
import com.chesscompanion.core.chess.DrawReason
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.Side

/** Text shown under the board for the game [status], [sideToMove] being the side to play. */
@StringRes
internal fun statusMessage(
    status: GameStatus,
    sideToMove: Side,
    inCheck: Boolean,
): Int {
    val white = sideToMove == Side.WHITE
    return when (status) {
        is GameStatus.Checkmate -> checkmateMessage(status.winner)
        GameStatus.Stalemate -> R.string.play_status_stalemate
        is GameStatus.Draw ->
            when (status.reason) {
                DrawReason.THREEFOLD_REPETITION -> R.string.play_status_draw_repetition
                DrawReason.FIFTY_MOVES -> R.string.play_status_draw_fifty_moves
                DrawReason.INSUFFICIENT_MATERIAL -> R.string.play_status_draw_material
            }
        GameStatus.Ongoing ->
            when {
                inCheck && white -> R.string.play_status_white_in_check
                inCheck -> R.string.play_status_black_in_check
                white -> R.string.play_status_white_to_move
                else -> R.string.play_status_black_to_move
            }
    }
}

@StringRes
private fun checkmateMessage(winner: Side): Int =
    if (winner == Side.WHITE) R.string.play_status_checkmate_white else R.string.play_status_checkmate_black
