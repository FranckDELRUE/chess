package com.chesscompanion.core.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.R
import kotlin.math.roundToInt

private const val FILE_LETTERS = "abcdefgh"
private const val LABEL_RATIO = 0.2f
private const val DOT_RATIO = 0.16f
private const val RING_RATIO = 0.08f

/** Test tag of the cell showing [square], e.g. `square-e4`. */
fun squareTestTag(square: Square): String = "square-${square.name}"

/**
 * Display-only board drawn from [fen], sized to the largest square that fits the incoming
 * constraints. An invalid FEN renders an empty board.
 */
@Composable
fun ChessBoard(
    fen: String,
    flipped: Boolean,
    modifier: Modifier = Modifier,
    colors: BoardColors = BoardColors(),
) {
    val board = remember(fen) { runCatching { Fen.parseBoard(fen) }.getOrNull() }
    BoardView(board, flipped, BoardHighlights(), null, modifier, colors)
}

/**
 * Interactive board. It holds no game logic: taps and drops are reported, and the caller (a
 * ViewModel) decides what they mean. Only the finger position of a drag is kept here.
 *
 * @param onSquareTap a square was tapped.
 * @param onDragStart a piece started to be dragged from this square.
 * @param onMove a dragged piece was dropped on another square.
 */
@Composable
fun ChessBoard(
    board: Board,
    flipped: Boolean,
    onSquareTap: (Square) -> Unit,
    onDragStart: (Square) -> Unit,
    onMove: (from: Square, to: Square) -> Unit,
    modifier: Modifier = Modifier,
    highlights: BoardHighlights = BoardHighlights(),
    colors: BoardColors = BoardColors(),
) {
    BoardView(board, flipped, highlights, BoardInput(onSquareTap, onDragStart, onMove), modifier, colors)
}

private class BoardInput(
    val onTap: (Square) -> Unit,
    val onDragStart: (Square) -> Unit,
    val onMove: (Square, Square) -> Unit,
)

private data class Drag(
    val from: Square,
    val position: Offset,
)

@Composable
private fun BoardView(
    board: Board?,
    flipped: Boolean,
    highlights: BoardHighlights,
    input: BoardInput?,
    modifier: Modifier,
    colors: BoardColors,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val cell = side / Board.SIZE
        val cellPx = with(LocalDensity.current) { cell.toPx() }
        var drag by remember { mutableStateOf<Drag?>(null) }
        val currentInput by rememberUpdatedState(input)
        val currentBoard by rememberUpdatedState(board)
        val gestures =
            if (input == null) {
                Modifier
            } else {
                Modifier
                    .pointerInput(flipped, cellPx) {
                        detectTapGestures { offset ->
                            squareAtOffset(offset.x, offset.y, cellPx, flipped)?.let { currentInput?.onTap(it) }
                        }
                    }.pointerInput(flipped, cellPx) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val from = squareAtOffset(offset.x, offset.y, cellPx, flipped)
                                if (from != null && currentBoard?.get(from) != null) {
                                    drag = Drag(from, offset)
                                    currentInput?.onDragStart(from)
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                drag = drag?.let { it.copy(position = it.position + amount) }
                            },
                            onDragEnd = {
                                drag?.let { d ->
                                    val to = squareAtOffset(d.position.x, d.position.y, cellPx, flipped)
                                    if (to != null && to != d.from) currentInput?.onMove(d.from, to)
                                }
                                drag = null
                            },
                            onDragCancel = { drag = null },
                        )
                    }
            }
        Box(Modifier.size(side).then(gestures)) {
            Column {
                repeat(Board.SIZE) { row ->
                    Row {
                        repeat(Board.SIZE) { column ->
                            val square = squareAt(row, column, flipped)
                            BoardCell(
                                cell = CellSpec(square, row, column, cell),
                                board = board,
                                highlights = highlights,
                                hidePiece = drag?.from == square.toSquare(),
                                colors = colors,
                            )
                        }
                    }
                }
            }
            drag?.let { d ->
                board?.get(d.from)?.let { piece ->
                    Image(
                        painter = painterResource(piece.drawableRes()),
                        contentDescription = null,
                        modifier =
                            Modifier.size(cell).offset {
                                IntOffset(
                                    (d.position.x - cellPx / 2).roundToInt(),
                                    (d.position.y - cellPx / 2).roundToInt(),
                                )
                            },
                    )
                }
            }
        }
    }
}

private class CellSpec(
    val square: BoardSquare,
    val row: Int,
    val column: Int,
    val size: Dp,
)

@Composable
private fun BoardCell(
    cell: CellSpec,
    board: Board?,
    highlights: BoardHighlights,
    hidePiece: Boolean,
    colors: BoardColors,
) {
    val square = cell.square.toSquare()
    val light = isLightSquare(cell.square)
    val piece = board?.get(square)
    val label =
        if (piece == null) {
            stringResource(R.string.board_square_empty, square.name)
        } else {
            stringResource(R.string.board_square_piece, square.name, stringResource(piece.nameRes()))
        }
    Box(
        Modifier
            .size(cell.size)
            .background(if (light) colors.light else colors.dark)
            .testTag(squareTestTag(square))
            .semantics { contentDescription = label },
    ) {
        SquareMarks(square, piece != null, highlights, colors)
        if (piece != null && !hidePiece) {
            Image(
                painter = painterResource(piece.drawableRes()),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        TargetMark(square in highlights.targets, piece != null, colors.target)
        val labelColor = if (light) colors.dark else colors.light
        val labelSize = with(LocalDensity.current) { (cell.size * LABEL_RATIO).toSp() }
        if (cell.column == Board.SIZE - 1) {
            CoordinateLabel("${square.rank + 1}", labelColor, labelSize, Modifier.align(Alignment.TopEnd))
        }
        if (cell.row == Board.SIZE - 1) {
            CoordinateLabel(
                "${FILE_LETTERS[square.file]}",
                labelColor,
                labelSize,
                Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

/** Last move, selection and check, drawn under the piece. */
@Composable
private fun SquareMarks(
    square: Square,
    occupied: Boolean,
    highlights: BoardHighlights,
    colors: BoardColors,
) {
    val lastMove = highlights.lastMove?.let { square == it.from || square == it.to } == true
    val selected = highlights.selected == square
    val check = highlights.check == square && occupied
    if (!lastMove && !selected && !check) return
    Canvas(Modifier.fillMaxSize()) {
        if (lastMove) drawRect(colors.lastMove)
        if (selected) drawRect(colors.selected)
        if (check) drawCircle(colors.check, radius = size.minDimension / 2)
    }
}

/** A dot on an empty target square, a ring around a piece that can be captured. */
@Composable
private fun TargetMark(
    target: Boolean,
    occupied: Boolean,
    color: Color,
) {
    if (!target) return
    Canvas(Modifier.fillMaxSize()) {
        if (occupied) {
            val stroke = size.minDimension * RING_RATIO
            drawCircle(color, radius = (size.minDimension - stroke) / 2, style = Stroke(width = stroke))
        } else {
            drawCircle(color, radius = size.minDimension * DOT_RATIO)
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
