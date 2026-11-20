package com.chesscompanion.feature.repertoire

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.squareTestTag
import com.chesscompanion.core.ui.theme.ChessTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Compose UI tests on the JVM (Robolectric), tablet landscape size, in French. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fr-rFR-w1280dp-h800dp-land")
class RepertoireEditorScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = FakeRepertoireStore()
    private lateinit var id: String

    @Before
    fun setUp() {
        id = runBlocking { store.create("Italienne", Side.WHITE) }
        compose.setContent {
            ChessTheme {
                RepertoireEditorScreen(
                    RepertoireEditorViewModel(SavedStateHandle(mapOf(REPERTOIRE_ID_ARG to id)), store)
                        .also {
                            it.saveDelayMs = 0
                            it.importDispatcher = Dispatchers.Main
                        },
                )
            }
        }
    }

    private fun tap(vararg squares: String) =
        squares.forEach { compose.onNodeWithTag(squareTestTag(Square.parse(it)!!)).performClick() }

    private fun line() = compose.onNodeWithTag(EDITOR_LINE_TAG)

    @Test
    fun theEditorOpensOnTheStartPositionOfTheRepertoire() {
        compose.onNodeWithTag(EDITOR_LINE_TAG).assertTextEquals("Position de départ")
        compose.onNodeWithTag(EDITOR_BACK_TAG).assertIsNotEnabled()
        compose.onNodeWithTag(EDITOR_DELETE_TAG).assertIsNotEnabled()
    }

    @Test
    fun playingMovesOnTheBoardBuildsALineThatIsSaved() {
        tap("e2", "e4", "e7", "e5", "g1", "f3")
        line().assertTextEquals("1. e4 e5 2. Cf3")
        compose.onNodeWithTag(EDITOR_BACK_TAG).assertIsEnabled()
        assertEquals(3, store.repertoireOf(id).edgeCount)
    }

    @Test
    fun goingBackThenPlayingAnotherMoveOffersBothContinuations() {
        tap("e2", "e4")
        compose.onNodeWithTag(EDITOR_START_TAG).performClick()
        tap("d2", "d4")
        compose.onNodeWithTag(EDITOR_START_TAG).performClick()
        compose.onNodeWithTag(editorContinuationTag("e2e4")).assertTextContains("e4")
        compose.onNodeWithTag(editorContinuationTag("d2d4")).assertTextContains("d4")
        compose.onNodeWithTag(editorContinuationTag("e2e4")).performClick()
        line().assertTextEquals("1. e4")
    }

    @Test
    fun aCommentIsKeptWhenComingBackToThePosition() {
        tap("e2", "e4")
        compose.onNodeWithTag(EDITOR_COMMENT_TAG).performScrollTo().performTextInput("Coup du roi")
        compose.onNodeWithTag(EDITOR_START_TAG).performClick()
        compose.onNodeWithTag(editorContinuationTag("e2e4")).performClick()
        compose.onNodeWithTag(EDITOR_COMMENT_TAG).assertTextContains("Coup du roi")
        val saved = store.repertoireOf(id)
        assertEquals("Coup du roi", saved.notes(saved.movesFrom(saved.root).single().to).comment)
    }

    @Test
    fun theSourceIsWrittenOnThePosition() {
        compose.onNodeWithTag(editorSourceTag(SourceField.BOOK)).performScrollTo().performTextInput("Mon livre")
        compose.onNodeWithTag(editorSourceTag(SourceField.PAGE)).performScrollTo().performTextInput("12")
        val saved = store.repertoireOf(id)
        assertEquals("Mon livre", saved.notes(saved.root).source?.book)
        assertEquals("12", saved.notes(saved.root).source?.page)
    }

    @Test
    fun annotatingMarksTheBoardInsteadOfPlaying() {
        compose.onNodeWithTag(EDITOR_ANNOTATE_TAG).performScrollTo().performClick()
        compose.onNodeWithTag(editorColorTag(AnnotationColor.BLUE)).performScrollTo().performClick()
        tap("e4")
        line().assertTextEquals("Position de départ")
        val saved = store.repertoireOf(id)
        assertEquals(1, saved.notes(saved.root).squares.size)
        compose
            .onNodeWithTag(EDITOR_CLEAR_MARKS_TAG)
            .performScrollTo()
            .assertIsEnabled()
            .performClick()
        assertEquals(0, store.repertoireOf(id).let { it.notes(it.root).squares.size })
    }

    @Test
    fun pastingAPgnImportsItAndShowsTheReport() {
        compose.onNodeWithTag(EDITOR_IMPORT_TAG).performScrollTo().performClick()
        compose.onNodeWithTag(IMPORT_RUN_TAG).assertIsNotEnabled()
        compose.onNodeWithTag(IMPORT_TEXT_TAG).performTextInput("1. e4 e5 2. Nf3 Nc6 *")
        compose.onNodeWithTag(IMPORT_RUN_TAG).assertIsEnabled().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(IMPORT_REPORT_TAG).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Coups ajoutés : 4").assertExists()
        compose.onNodeWithTag(IMPORT_CLOSE_TAG).performClick()
        compose.onNodeWithTag(EDITOR_IMPORT_TAG).performScrollTo().assertIsEnabled()
        compose.onNodeWithTag(editorContinuationTag("e2e4")).assertTextContains("e4")
    }

    @Test
    fun deletingAMoveAsksForConfirmation() {
        tap("e2", "e4")
        compose.onNodeWithTag(EDITOR_DELETE_TAG).performScrollTo().performClick()
        compose.onNodeWithTag(EDITOR_DELETE_CONFIRM_TAG).performClick()
        line().assertTextEquals("Position de départ")
        assertEquals(0, store.repertoireOf(id).edgeCount)
    }
}
