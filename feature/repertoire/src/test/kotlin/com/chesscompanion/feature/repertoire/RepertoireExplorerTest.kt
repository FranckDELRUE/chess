package com.chesscompanion.feature.repertoire

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.data.explorer.ExplorerLevel
import com.chesscompanion.core.data.explorer.ExplorerMove
import com.chesscompanion.core.data.explorer.ExplorerOutcome
import com.chesscompanion.core.data.explorer.ExplorerProblem
import com.chesscompanion.core.data.explorer.ExplorerStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** S7-7: the explorer panel of the editor, with a fake explorer, a fake token store and a fake engine. */
@OptIn(ExperimentalCoroutinesApi::class)
class RepertoireExplorerTest {
    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val start = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private val explorer = FakeExplorer()
    private val tokens = FakeTokens("lip_token")
    private val engine = FakeEngine()
    private val store = FakeRepertoireStore()

    // e7e5 is not legal for White at the start: a wrong answer must not reach the screen.
    private val stats =
        ExplorerStats(
            100,
            20,
            80,
            listOf(
                ExplorerMove("e2e4", 120, 30, 50, 1500),
                ExplorerMove("g1f3", 40, 10, 50, null),
                ExplorerMove("e7e5", 1, 1, 1, null),
            ),
        )

    private fun editor(): RepertoireEditorViewModel {
        val id = runBlocking { store.create("Test", Side.WHITE) }
        return RepertoireEditorViewModel(
            SavedStateHandle(mapOf(REPERTOIRE_ID_ARG to id)),
            store,
            FakeStudyFetcher(),
            explorer,
            tokens,
            scheduler(engine),
        ).also { it.saveDelayMs = 0 }
    }

    private val RepertoireEditorViewModel.panel get() = explorerState.value

    private fun loaded() = ExplorerOutcome.Loaded(stats, fromCache = false)

    @Test
    fun `the start position is asked for the club level and shown with French moves`() {
        explorer.outcome = loaded()

        val panel = editor().panel

        assertEquals(listOf(start to ExplorerLevel.CLUB), explorer.asked)
        val content = panel.content as ExplorerContent.Loaded
        assertEquals(listOf("e2e4", "g1f3"), content.rows.map { it.uci })
        assertEquals(listOf("e4", "Cf3"), content.rows.map { it.label })
        assertEquals(listOf(60, 40), content.rows.map { it.whitePercent })
        assertEquals(listOf(15, 10), content.rows.map { it.drawPercent })
        assertEquals(listOf(25, 50), content.rows.map { it.blackPercent })
        assertEquals(false, content.fromCache)
    }

    @Test
    fun `adding a move of the explorer puts it in the repertoire and shows its position`() {
        explorer.outcome = loaded()
        val vm = editor()

        vm.onAction(EditorAction.PlayUci("e2e4"))

        assertEquals(1, vm.state.value.moveCount)
        assertEquals("1. e4", vm.state.value.lineText)
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", explorer.asked.last().first)
    }

    @Test
    fun `a move that is not legal is refused whoever proposes it`() {
        val vm = editor()

        vm.onAction(EditorAction.PlayUci("e7e5"))
        vm.onAction(EditorAction.PlayUci("zzzz"))

        assertEquals(0, vm.state.value.moveCount)
    }

    @Test
    fun `moves already in the repertoire are marked as known`() {
        explorer.outcome = loaded()
        val vm = editor()
        vm.onAction(EditorAction.PlayUci("e2e4"))
        vm.onAction(EditorAction.ToStart)

        val rows = (vm.panel.content as ExplorerContent.Loaded).rows

        assertEquals(listOf(true, false), rows.map { it.known })
    }

    @Test
    fun `editing a comment does not ask the explorer or the engine again`() {
        explorer.outcome = loaded()
        val vm = editor()

        vm.onAction(EditorAction.EditComment("idée"))
        vm.onAction(EditorAction.EditComment("idée d4"))

        assertEquals(1, explorer.asked.size)
        assertEquals(1, engine.searched.size)
    }

    @Test
    fun `changing the level asks again with that level`() {
        explorer.outcome = loaded()
        val vm = editor()

        vm.onAction(EditorAction.Explorer(ExplorerAction.SetLevel(ExplorerLevel.MASTER)))

        assertEquals(ExplorerLevel.MASTER, vm.panel.level)
        assertEquals(ExplorerLevel.MASTER, explorer.asked.last().second)
    }

    @Test
    fun `without a token the panel says so, and the token entered is kept and used`() {
        tokens.value = null
        explorer.outcome = ExplorerOutcome.Failed(ExplorerProblem.NO_TOKEN)
        val vm = editor()
        assertEquals(ExplorerContent.Problem(ExplorerProblem.NO_TOKEN), vm.panel.content)
        assertEquals(false, vm.panel.hasToken)

        explorer.outcome = loaded()
        vm.onAction(EditorAction.Explorer(ExplorerAction.SaveToken("lip_new")))

        assertEquals("lip_new", tokens.value)
        assertTrue(vm.panel.hasToken)
        assertTrue(vm.panel.content is ExplorerContent.Loaded)

        vm.onAction(EditorAction.Explorer(ExplorerAction.ClearToken))
        assertNull(tokens.value)
        assertEquals(false, vm.panel.hasToken)
    }

    @Test
    fun `a network problem leaves the editor usable`() {
        ExplorerProblem.entries.forEach { problem ->
            explorer.outcome = ExplorerOutcome.Failed(problem)
            val vm = editor()
            assertEquals(ExplorerContent.Problem(problem), vm.panel.content)

            vm.onAction(EditorAction.PlayUci("d2d4"))

            assertEquals(1, vm.state.value.moveCount)
        }
    }

    @Test
    fun `a position already consulted shows as cached`() {
        explorer.outcome = ExplorerOutcome.Loaded(stats, fromCache = true)

        assertEquals(true, (editor().panel.content as ExplorerContent.Loaded).fromCache)
    }

    @Test
    fun `the evaluation is given from the point of view of White`() {
        engine.infos = listOf(centipawns(20), centipawns(35))
        val vm = editor()
        assertEquals("+0,35", vm.panel.evaluation)

        // After 1. e4 Black is to move: +35 for Black is -0,35 for White.
        vm.onAction(EditorAction.PlayUci("e2e4"))
        assertEquals("-0,35", vm.panel.evaluation)
    }

    @Test
    fun `an engine that cannot start only removes the evaluation`() {
        engine.fail = true
        explorer.outcome = loaded()

        val vm = editor()

        assertNull(vm.panel.evaluation)
        assertTrue(vm.panel.content is ExplorerContent.Loaded)
    }
}
