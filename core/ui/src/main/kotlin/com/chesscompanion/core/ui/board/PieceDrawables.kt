package com.chesscompanion.core.ui.board

import androidx.annotation.DrawableRes
import com.chesscompanion.core.chess.Piece
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.ui.R

private val whiteDrawables =
    mapOf(
        PieceType.KING to R.drawable.piece_wk,
        PieceType.QUEEN to R.drawable.piece_wq,
        PieceType.ROOK to R.drawable.piece_wr,
        PieceType.BISHOP to R.drawable.piece_wb,
        PieceType.KNIGHT to R.drawable.piece_wn,
        PieceType.PAWN to R.drawable.piece_wp,
    )

private val blackDrawables =
    mapOf(
        PieceType.KING to R.drawable.piece_bk,
        PieceType.QUEEN to R.drawable.piece_bq,
        PieceType.ROOK to R.drawable.piece_br,
        PieceType.BISHOP to R.drawable.piece_bb,
        PieceType.KNIGHT to R.drawable.piece_bn,
        PieceType.PAWN to R.drawable.piece_bp,
    )

@DrawableRes
internal fun Piece.drawableRes(): Int = getValue(if (side == Side.WHITE) whiteDrawables else blackDrawables)

private fun Piece.getValue(table: Map<PieceType, Int>): Int = table.getValue(type)
