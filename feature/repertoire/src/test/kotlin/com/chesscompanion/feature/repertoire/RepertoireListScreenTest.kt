package com.chesscompanion.feature.repertoire

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.ui.theme.ChessTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Compose UI tests on the JVM (Robolectric), tablet landscape size, in French (plurals). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "fr-rFR-w1280dp-h800dp-land")
class RepertoireListScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = FakeRepertoireStore()
    private var opened: String? = null

    private fun show(canOpen: Boolean = false) =
        compose.setContent {
            ChessTheme {
                RepertoireListScreen(
                    onOpen = if (canOpen) ({ opened = it }) else null,
                    viewModel = RepertoireListViewModel(store),
                )
            }
        }

    private fun create(
        name: String,
        side: Side = Side.WHITE,
    ) {
        compose.onNodeWithTag(REPERTOIRE_NEW_TAG).performClick()
        compose.onNodeWithTag(REPERTOIRE_NAME_FIELD_TAG).performTextInput(name)
        compose.onNodeWithTag(repertoireSideTag(side)).performClick()
        compose.onNodeWithTag(REPERTOIRE_CONFIRM_TAG).performClick()
    }

    @Test
    fun anEmptyListInvitesToCreateOne() {
        show()
        compose.onNodeWithTag(REPERTOIRE_EMPTY_TAG).assertTextContains("Aucun répertoire", substring = true)
    }

    @Test
    fun creatingARepertoireShowsItWithItsSideAndMoveCount() {
        show()
        create("Italienne", Side.BLACK)
        compose.onNodeWithTag(REPERTOIRE_EMPTY_TAG).assertDoesNotExist()
        compose.onNodeWithText("Italienne").assertExists()
        compose.onNodeWithText("Noirs · 0 coup").assertExists()
    }

    @Test
    fun theCreateButtonIsDisabledUntilThereIsAName() {
        show()
        compose.onNodeWithTag(REPERTOIRE_NEW_TAG).performClick()
        compose.onNodeWithTag(REPERTOIRE_CONFIRM_TAG).assertIsNotEnabled()
        compose.onNodeWithTag(REPERTOIRE_NAME_FIELD_TAG).performTextInput("x")
        compose.onNodeWithTag(REPERTOIRE_CONFIRM_TAG).assertIsEnabled()
        compose.onNodeWithTag(REPERTOIRE_NAME_FIELD_TAG).performTextClearance()
        compose.onNodeWithTag(REPERTOIRE_CONFIRM_TAG).assertIsNotEnabled()
    }

    @Test
    fun renamingChangesTheName() {
        show()
        create("Ancien")
        compose.onNodeWithTag(repertoireRenameTag("r1")).performClick()
        compose.onNodeWithTag(REPERTOIRE_NAME_FIELD_TAG).performTextClearance()
        compose.onNodeWithTag(REPERTOIRE_NAME_FIELD_TAG).performTextInput("Nouveau")
        compose.onNodeWithTag(REPERTOIRE_CONFIRM_TAG).performClick()
        compose.onNodeWithText("Nouveau").assertExists()
        compose.onNodeWithText("Ancien").assertDoesNotExist()
    }

    @Test
    fun deletingAsksForConfirmationAndCancelKeepsTheRepertoire() {
        show()
        create("A")
        compose.onNodeWithTag(repertoireDeleteTag("r1")).performClick()
        compose.onNodeWithText("Supprimer « A » ?").assertExists()
        compose.onNodeWithText("Annuler").performClick()
        compose.onNodeWithText("A").assertExists()
        assertEquals(listOf("A"), store.names())

        compose.onNodeWithTag(repertoireDeleteTag("r1")).performClick()
        compose.onNodeWithTag(REPERTOIRE_CONFIRM_TAG).performClick()
        assertEquals(emptyList<String>(), store.names())
        compose.onNodeWithTag(REPERTOIRE_EMPTY_TAG).assertExists()
    }

    @Test
    fun tappingARowOpensItOnlyWhenOpeningIsAvailable() {
        show(canOpen = true)
        create("A")
        compose.onNodeWithTag(repertoireRowTag("r1")).performClick()
        assertEquals("r1", opened)
    }
}
