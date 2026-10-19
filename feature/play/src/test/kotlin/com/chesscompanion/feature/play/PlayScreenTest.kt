package com.chesscompanion.feature.play

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.ui.board.squareTestTag
import com.chesscompanion.core.ui.theme.ChessTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Compose UI tests on the JVM (Robolectric), tablet landscape size. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], qualifiers = "w1280dp-h800dp-land")
class PlayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val clipboard: ClipboardManager
        get() =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private fun showScreen() =
        compose.setContent {
            ChessTheme { PlayScreen(PlayViewModel(SavedStateHandle(), FakeDocuments())) }
        }

    private fun tap(vararg squares: String) =
        squares.forEach { compose.onNodeWithTag(squareTestTag(Square.parse(it)!!)).performClick() }

    private fun status() = compose.onNodeWithTag(PLAY_STATUS_TAG)

    @Test
    fun scholarsMateByTaps() {
        showScreen()
        status().assertTextEquals("Aux Blancs de jouer")

        tap("e2", "e4", "e7", "e5", "f1", "c4", "b8", "c6", "d1", "h5", "g8", "f6", "h5", "f7")

        status().assertTextEquals("Échec et mat : les Blancs gagnent")
    }

    /** S1-7 acceptance: paste a game of Heidoji, browse it, copy it back as PGN. */
    @Test
    fun importBrowseAndExportAHeidojiGame() {
        val original = File("../../testdata/pgn/chesscom-heidoji-100.pgn").readText().split("\n\n[Event").first()
        val game = PgnReader.read(original)
        clipboard.setPrimaryClip(ClipData.newPlainText("PGN", original))
        showScreen()

        compose.onNodeWithText("Coller (PGN ou FEN)").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(NAV_START_TAG).assertIsNotEnabled()

        compose.onNodeWithTag(NAV_FORWARD_TAG).performClick()
        compose.onNodeWithTag(NAV_END_TAG).performClick()
        // Third half-move (2. Nf3): Black to play.
        compose.onNodeWithTag(moveTestTag(listOf(0, 0, 0))).performScrollTo().performClick()
        status().assertTextEquals("Aux Noirs de jouer")
        compose.onNodeWithTag(NAV_START_TAG).performClick()
        compose.onNodeWithTag(NAV_START_TAG).assertIsNotEnabled()

        compose.onNodeWithText("Copier le PGN").performClick()
        compose.waitForIdle()
        val copied =
            clipboard.primaryClip!!
                .getItemAt(0)
                .text
                .toString()
        assertEquals(game, PgnReader.read(copied))
    }
}

/** Robolectric does not run the newest SDKs right away; the UI under test does not depend on it. */
private const val ROBOLECTRIC_SDK = 35
