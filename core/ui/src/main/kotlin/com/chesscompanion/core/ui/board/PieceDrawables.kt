package com.chesscompanion.core.ui.board

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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

private val whiteNames =
    mapOf(
        PieceType.KING to R.string.piece_white_king,
        PieceType.QUEEN to R.string.piece_white_queen,
        PieceType.ROOK to R.string.piece_white_rook,
        PieceType.BISHOP to R.string.piece_white_bishop,
        PieceType.KNIGHT to R.string.piece_white_knight,
        PieceType.PAWN to R.string.piece_white_pawn,
    )

private val blackNames =
    mapOf(
        PieceType.KING to R.string.piece_black_king,
        PieceType.QUEEN to R.string.piece_black_queen,
        PieceType.ROOK to R.string.piece_black_rook,
        PieceType.BISHOP to R.string.piece_black_bishop,
        PieceType.KNIGHT to R.string.piece_black_knight,
        PieceType.PAWN to R.string.piece_black_pawn,
    )

@DrawableRes
internal fun Piece.drawableRes(): Int = (if (side == Side.WHITE) whiteDrawables else blackDrawables).getValue(type)

/** Spoken name, e.g. "cavalier blanc". */
@StringRes
internal fun Piece.nameRes(): Int = (if (side == Side.WHITE) whiteNames else blackNames).getValue(type)
