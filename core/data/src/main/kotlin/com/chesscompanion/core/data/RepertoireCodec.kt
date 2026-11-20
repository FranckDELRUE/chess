package com.chesscompanion.core.data

import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.repertoire.MarkColor
import com.chesscompanion.core.chess.repertoire.RepertoireArrow
import com.chesscompanion.core.chess.repertoire.RepertoireSquareMark

/**
 * Text form of the marks of a node: items separated by a space, each `<squares>:<colour letter>`,
 * e.g. `e2e4:G` for an arrow and `d4:R` for a square.
 */
internal object RepertoireCodec {
    private const val SQUARE_LENGTH = 2
    private val letters = mapOf(MarkColor.GREEN to 'G', MarkColor.RED to 'R', MarkColor.BLUE to 'B')

    fun writeArrows(arrows: List<RepertoireArrow>): String =
        arrows.joinToString(" ") { "${it.from}${it.to}:${letters.getValue(it.color)}" }

    fun writeSquares(squares: List<RepertoireSquareMark>): String =
        squares.joinToString(" ") { "${it.square}:${letters.getValue(it.color)}" }

    fun readArrows(text: String): List<RepertoireArrow> =
        items(text).mapNotNull { (squares, color) ->
            val from = Square.parse(squares.take(SQUARE_LENGTH))
            val to = Square.parse(squares.drop(SQUARE_LENGTH))
            if (from == null || to == null) null else RepertoireArrow(from, to, color)
        }

    fun readSquares(text: String): List<RepertoireSquareMark> =
        items(text).mapNotNull { (squares, color) -> Square.parse(squares)?.let { RepertoireSquareMark(it, color) } }

    private fun items(text: String): List<Pair<String, MarkColor>> =
        text.split(' ').filter(String::isNotEmpty).mapNotNull { item ->
            val parts = item.split(':')
            val letter = parts.getOrNull(1)?.singleOrNull()
            val color = letters.entries.firstOrNull { it.value == letter }?.key
            if (parts.size == 2 && color != null) parts[0] to color else null
        }
}
