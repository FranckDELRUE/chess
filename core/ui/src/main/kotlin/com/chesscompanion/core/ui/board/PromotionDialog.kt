package com.chesscompanion.core.ui.board

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.chess.Piece
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.ui.R

/** Test tag of a promotion choice, e.g. `promotion-queen`. */
fun promotionTestTag(type: PieceType): String = "promotion-${type.name.lowercase()}"

/** Lets the player pick the piece a pawn of [side] promotes to. */
@Composable
fun PromotionDialog(
    side: Side,
    onPick: (PieceType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.promotion_title)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PieceType.PROMOTIONS.forEach { type ->
                    val piece = Piece(type, side)
                    Image(
                        painter = painterResource(piece.drawableRes()),
                        contentDescription = stringResource(piece.nameRes()),
                        modifier =
                            Modifier
                                .size(
                                    PIECE_SIZE.dp,
                                ).testTag(promotionTestTag(type))
                                .clickable { onPick(type) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.promotion_cancel)) } },
    )
}

private const val PIECE_SIZE = 64
