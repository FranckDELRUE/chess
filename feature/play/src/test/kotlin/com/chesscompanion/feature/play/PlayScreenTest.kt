package com.chesscompanion.feature.play

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.squareTestTag
import com.chesscompanion.core.ui.theme.ChessTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Compose UI test on the JVM (Robolectric), tablet landscape size. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w1280dp-h800dp-land")
class PlayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun tap(vararg squares: String) =
        squares.forEach { compose.onNodeWithTag(squareTestTag(Square.parse(it)!!)).performClick() }

    @Test
    fun scholarsMateByTaps() {
        compose.setContent { ChessTheme { PlayScreen(PlayViewModel(SavedStateHandle())) } }
        compose.onNodeWithTag(PLAY_STATUS_TAG).assertTextEquals("Aux Blancs de jouer")

        tap("e2", "e4", "e7", "e5", "f1", "c4", "b8", "c6", "d1", "h5", "g8", "f6", "h5", "f7")

        compose.onNodeWithTag(PLAY_STATUS_TAG).assertTextEquals("Échec et mat : les Blancs gagnent")
    }
}

/** Robolectric does not run the newest SDKs right away; the UI under test does not depend on it. */
private const val ROBOLECTRIC_SDK = 35
