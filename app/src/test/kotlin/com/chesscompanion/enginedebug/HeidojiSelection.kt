package com.chesscompanion.enginedebug

import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.Game
import kotlin.math.abs

/**
 * Stage 1 of the S2-2 selection, without an engine: the middlegames of plies 30 to 40 of each game
 * of the corpus, in file order. Stage 2 (`station-pc/bench/select_positions.py`) keeps the first
 * balanced one of each game.
 */
object HeidojiSelection {
    const val FIRST_PLY = 30
    const val LAST_PLY = 40
    private const val MAX_MATERIAL_GAP = 1

    data class Candidate(
        /** 1-based index of the game in the PGN file. */
        val game: Int,
        /** Number of half-moves played to reach the position. */
        val ply: Int,
        val fen: String,
    ) {
        /** Line of `testdata/positions/heidoji-candidates.txt`. */
        fun line(): String = "$fen ; game $game ply $ply"
    }

    /** Every middlegame of plies [FIRST_PLY]..[LAST_PLY] of each game, by game then ply. */
    fun candidates(games: List<Game>): List<Candidate> =
        games.flatMapIndexed { index, game ->
            val line = game.mainline
            (FIRST_PLY..minOf(LAST_PLY, line.size))
                .filter { ply -> isMiddlegame(line[ply - 1].position) }
                .map { ply -> Candidate(index + 1, ply, line[ply - 1].position.toFen()) }
        }

    /** Not in check, material equal within a pawn (1/3/3/5/9), a queen or two rooks on each side. */
    fun isMiddlegame(position: Position): Boolean {
        if (position.isCheck) return false
        val pieces = position.board.pieces().map { it.second }

        fun count(
            side: Side,
            type: PieceType,
        ) = pieces.count { it.side == side && it.type == type }

        fun material(side: Side) = PieceType.entries.sumOf { count(side, it) * value(it) }

        fun heavy(side: Side) = count(side, PieceType.QUEEN) >= 1 || count(side, PieceType.ROOK) >= 2
        return abs(material(Side.WHITE) - material(Side.BLACK)) <= MAX_MATERIAL_GAP &&
            heavy(Side.WHITE) &&
            heavy(Side.BLACK)
    }

    @Suppress("MagicNumber")
    private fun value(type: PieceType): Int =
        when (type) {
            PieceType.PAWN -> 1
            PieceType.KNIGHT, PieceType.BISHOP -> 3
            PieceType.ROOK -> 5
            PieceType.QUEEN -> 9
            PieceType.KING -> 0
        }
}
