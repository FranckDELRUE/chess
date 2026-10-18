package com.chesscompanion.core.chess

enum class PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }

data class Piece(
    val type: PieceType,
    val side: Side,
)
