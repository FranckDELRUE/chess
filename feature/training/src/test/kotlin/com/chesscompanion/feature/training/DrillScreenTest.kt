package com.chesscompanion.feature.training

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.srs.Fsrs
import com.chesscompanion.core.srs.FsrsCard
import com.chesscompanion.core.srs.Rating
import com.chesscompanion.core.srs.ReviewQueueBuilder
import com.chesscompanion.core.ui.board.squareTestTag
import com.chesscompanion.core.ui.theme.ChessTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Compose UI tests on the JVM (Robolectric), tablet landscape size, in French. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fr-rFR-w1280dp-h800dp-land")
class DrillScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var closed = false

    private fun sq(name: String) = checkNotNull(Square.parse(name))

    private fun launch(reviewed: Boolean) {
        val repertoire = repertoireOf(Side.WHITE, "1. d4 d5 2. c4 e6 *")
        val now = Instant.parse("2026-03-10T08:00:00Z")
        val cards = FakeReviewCardStore()
        if (reviewed) {
            val card = Fsrs().review(FsrsCard(), Rating.Good, now - Duration.ofDays(30))
            ReviewQueueBuilder.build(repertoire, emptyMap(), now, ZoneId.of("Europe/Paris")).items.forEach {
                runBlocking { cards.put("r", it.id, card) }
            }
        }
        val viewModel =
            DrillViewModel(
                SavedStateHandle(mapOf(DRILL_REPERTOIRE_ID_ARG to "r")),
                FakeRepertoireStore("r", "Anglaise", repertoire),
                cards,
                scheduler(),
                Clock.fixed(now, ZoneId.of("Europe/Paris")),
            )
        viewModel.replyPauseMs = 0
        compose.setContent { ChessTheme { DrillScreen({ closed = true }, viewModel) } }
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
        launch(reviewed = true)
        compose.onNodeWithTag(DRILL_QUESTION_TAG).assertTextContains("Les Blancs jouent", substring = true)
        compose.onNodeWithTag(DRILL_LINE_TAG).assertTextContains("Début du répertoire", substring = true)
    }

    @Test
    fun aWrongMoveShowsExpectedMoveLineAndEvaluations() {
        launch(reviewed = true)
        tapMove("e2", "e4")
        compose.waitForIdle()
        compose.onNodeWithTag(DRILL_FEEDBACK_TAG).assertIsDisplayed()
        compose.onNodeWithTag(DRILL_EVAL_PLAYED_TAG).assertTextContains("e4", substring = true)
        compose.onNodeWithTag(DRILL_EVAL_EXPECTED_TAG).assertTextContains("d4", substring = true)
        compose.onNodeWithTag(DRILL_CONTINUE_TAG).assertIsDisplayed()
    }

    @Test
    fun theHintThenARightMoveThenContinue() {
        launch(reviewed = true)
        compose.onNodeWithTag(DRILL_HINT_BUTTON_TAG).performClick()
        compose.onNodeWithTag(DRILL_HINT_TAG).assertTextContains("d4", substring = true)
        tapMove("d2", "d4")
        compose.onNodeWithTag(DRILL_CONTINUE_TAG).performClick()
        compose.onNodeWithTag(DRILL_LINE_TAG).assertTextContains("1. d4 d5", substring = true)
    }

    @Test
    fun aNewPositionShowsTheMoveToLearnThenIsLearned() {
        launch(reviewed = false)
        compose.onNodeWithTag(DRILL_LEARN_TAG).assertIsDisplayed()
        compose.onNodeWithTag(DRILL_LEARN_MOVES_TAG).assertTextContains("d4", substring = true)

        tapMove("e2", "e4")
        compose.onNodeWithTag(DRILL_LEARN_MISS_TAG).assertIsDisplayed()

        tapMove("d2", "d4")
        compose.onNodeWithTag(DRILL_LEARNED_TAG).assertTextContains("Appris", substring = true)
        compose.onNodeWithTag(DRILL_CONTINUE_TAG).performClick()
        compose.onNodeWithTag(DRILL_LEARN_MOVES_TAG).assertTextContains("c4", substring = true)
    }

    @Test
    fun theSessionCanBeLeftAtAnyTime() {
        launch(reviewed = true)
        compose.onNodeWithTag(DRILL_QUIT_TAG).performClick()
        assertTrue(closed)
    }
}
