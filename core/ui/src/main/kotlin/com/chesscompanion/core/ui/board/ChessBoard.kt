package com.chesscompanion.core.ui.board

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.FenParser

private const val FILE_LETTERS = "abcdefgh"

/**
 * Square board drawn from [fen], sized to the largest square that fits the incoming constraints.
 * An invalid FEN renders an empty board.
 */
@Composable
fun ChessBoard(
    fen: String,
    flipped: Boolean,
    modifier: Modifier = Modifier,
    colors: BoardColors = BoardColors(),
) {
    val board = remember(fen) { runCatching { FenParser.parseBoard(fen) }.getOrNull() }
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val cell = side / Board.SIZE
        Column(Modifier.size(side)) {
            repeat(Board.SIZE) { row ->
                Row {
                    repeat(Board.SIZE) { column ->
                        BoardCell(board, squareAt(row, column, flipped), row, column, cell, colors)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardCell(
    board: Board?,
    square: BoardSquare,
    row: Int,
    column: Int,
    cell: Dp,
    colors: BoardColors,
) {
    val light = isLightSquare(square)
    val background = if (light) colors.light else colors.dark
    val labelColor = if (light) colors.dark else colors.light
    val labelSize = with(LocalDensity.current) { (cell * LABEL_RATIO).toSp() }
    Box(Modifier.size(cell).background(background)) {
        board?.pieceAt(square.file, square.rank)?.let { piece ->
            Image(
                painter = painterResource(piece.drawableRes()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (column == Board.SIZE - 1) {
            CoordinateLabel("${square.rank + 1}", labelColor, labelSize, Modifier.align(Alignment.TopEnd))
        }
        if (row == Board.SIZE - 1) {
            CoordinateLabel(
                "${FILE_LETTERS[square.file]}",
                labelColor,
                labelSize,
                Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

@Composable
private fun CoordinateLabel(
    text: String,
    color: Color,
    size: TextUnit,
    modifier: Modifier,
) {
    Text(text = text, color = color, fontSize = size, modifier = modifier.padding(2.dp))
}

private const val LABEL_RATIO = 0.2f
