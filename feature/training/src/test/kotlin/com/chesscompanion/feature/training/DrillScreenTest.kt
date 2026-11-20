package com.chesscompanion.feature.training

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.squareTestTag
import com.chesscompanion.core.ui.theme.ChessTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Compose UI tests on the JVM (Robolectric), tablet landscape size, in French. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fr-rFR-w1280dp-h800dp-land")
class DrillScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun sq(name: String) = checkNotNull(Square.parse(name))

    @Before
    fun setUp() {
        val viewModel =
            DrillViewModel(
                SavedStateHandle(mapOf(DRILL_REPERTOIRE_ID_ARG to "r")),
                FakeRepertoireStore("r", "Anglaise", repertoireOf(Side.WHITE, "1. d4 d5 2. c4 e6 *")),
                FakeReviewCardStore(),
                scheduler(),
                Clock.fixed(Instant.parse("2026-03-10T08:00:00Z"), ZoneId.of("Europe/Paris")),
            )
        compose.setContent { ChessTheme { DrillScreen(viewModel) } }
        compose.waitForIdle()
    }

    private fun tapMove(
        from: String,
        to: String,
    ) {
        compose.onNodeWithTag(squareTestTag(sq(from))).performClick()
        compose.onNodeWithTag(squareTestTag(sq(to))).performClick()
    }

    @Test
    fun theQuestionAndTheStartOfTheLineAreShown() {
        compose.onNodeWithTag(DRILL_QUESTION_TAG).assertTextContains("Les Blancs jouent", substring = true)
        compose.onNodeWithTag(DRILL_LINE_TAG).assertTextContains("Début du répertoire", substring = true)
    }

    @Test
    fun aWrongMoveShowsExpectedMoveLineAndEvaluations() {
        tapMove("e2", "e4")
        compose.waitForIdle()
        compose.onNodeWithTag(DRILL_FEEDBACK_TAG).assertIsDisplayed()
        compose.onNodeWithTag(DRILL_EVAL_PLAYED_TAG).assertTextContains("e4", substring = true)
        compose.onNodeWithTag(DRILL_EVAL_EXPECTED_TAG).assertTextContains("d4", substring = true)
        compose.onNodeWithTag(DRILL_CONTINUE_TAG).assertIsDisplayed()
    }

    @Test
    fun theHintThenARightMoveThenContinue() {
        compose.onNodeWithTag(DRILL_HINT_BUTTON_TAG).performClick()
        compose.onNodeWithTag(DRILL_HINT_TAG).assertTextContains("d4", substring = true)
        tapMove("d2", "d4")
        compose.onNodeWithTag(DRILL_CONTINUE_TAG).performClick()
        compose.onNodeWithTag(DRILL_LINE_TAG).assertTextContains("1. d4 d5", substring = true)
    }
}
