package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.repertoire.Repertoire
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepertoireListViewModelTest {
    private val store = FakeRepertoireStore()

    // Built after the main dispatcher is replaced: its scope runs on it.
    private lateinit var viewModel: RepertoireListViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = RepertoireListViewModel(store)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val ui get() = viewModel.state.value

    private fun create(
        name: String,
        side: Side = Side.WHITE,
    ) = viewModel.run {
        onAction(RepertoireListAction.StartCreate)
        onAction(RepertoireListAction.EditName(name))
        onAction(RepertoireListAction.ChooseSide(side))
        onAction(RepertoireListAction.Confirm)
    }

    @Test
    fun `the list starts loaded and empty`() {
        assertTrue(ui.loaded)
        assertTrue(ui.items.isEmpty())
        assertNull(ui.dialog)
    }

    @Test
    fun `creating a repertoire adds it with its side and closes the dialog`() {
        create("  Italienne  ", Side.BLACK)
        assertEquals(listOf("Italienne"), ui.items.map { it.name })
        assertEquals(Side.BLACK, ui.items.single().side)
        assertNull(ui.dialog)
    }

    @Test
    fun `a blank name cannot be confirmed`() {
        viewModel.onAction(RepertoireListAction.StartCreate)
        assertFalse(ui.dialog!!.canConfirm)
        viewModel.onAction(RepertoireListAction.EditName("   "))
        viewModel.onAction(RepertoireListAction.Confirm)
        assertTrue(ui.items.isEmpty())
        assertTrue(ui.dialog is RepertoireDialog.Create)
    }

    @Test
    fun `a name is cut at the maximum length`() {
        viewModel.onAction(RepertoireListAction.StartCreate)
        viewModel.onAction(RepertoireListAction.EditName("a".repeat(MAX_NAME_LENGTH + 20)))
        assertEquals(MAX_NAME_LENGTH, (ui.dialog as RepertoireDialog.Create).name.length)
    }

    @Test
    fun `dismissing a dialog creates nothing`() {
        viewModel.onAction(RepertoireListAction.StartCreate)
        viewModel.onAction(RepertoireListAction.EditName("Sicilienne"))
        viewModel.onAction(RepertoireListAction.Dismiss)
        assertNull(ui.dialog)
        assertTrue(store.names().isEmpty())
    }

    @Test
    fun `renaming starts from the current name`() {
        create("Ancien")
        val id = ui.items.single().id
        viewModel.onAction(RepertoireListAction.StartRename(id))
        assertEquals(RepertoireDialog.Rename(id, "Ancien"), ui.dialog)
        viewModel.onAction(RepertoireListAction.EditName("Nouveau"))
        viewModel.onAction(RepertoireListAction.Confirm)
        assertEquals(listOf("Nouveau"), ui.items.map { it.name })
    }

    @Test
    fun `deleting asks first and then removes the repertoire`() {
        create("A")
        create("B")
        val id = ui.items.first().id
        viewModel.onAction(RepertoireListAction.StartDelete(id))
        assertEquals(RepertoireDialog.Delete(id, "A"), ui.dialog)
        assertEquals(2, ui.items.size)
        viewModel.onAction(RepertoireListAction.Confirm)
        assertEquals(listOf("B"), ui.items.map { it.name })
    }

    @Test
    fun `an unknown repertoire opens no dialog`() {
        viewModel.onAction(RepertoireListAction.StartRename("none"))
        viewModel.onAction(RepertoireListAction.StartDelete("none"))
        assertNull(ui.dialog)
    }

    @Test
    fun `the side can only be chosen while creating`() {
        create("A")
        viewModel.onAction(RepertoireListAction.StartRename(ui.items.single().id))
        viewModel.onAction(RepertoireListAction.ChooseSide(Side.BLACK))
        assertTrue(ui.dialog is RepertoireDialog.Rename)
    }

    private fun graphOf(pgn: String) =
        PgnReader.readAll(pgn).games.fold(Repertoire.empty(Side.WHITE)) { r, g -> r.merge(g).repertoire }

    @Test
    fun `a repertoire is exported as PGN that reads back into the same graph`() {
        create("Ma Italienne")
        val id = ui.items.single().id
        val pgn = "[Event \"x\"]" + System.lineSeparator().repeat(2) + "1. e4 e5 2. Nf3 (2. Bc4 Nc6) 2... Nc6 *"
        val graph = graphOf(pgn)
        runBlocking { store.save(id, graph) }

        val exported = checkNotNull(runBlocking { viewModel.exportPgn(id) })
        assertTrue(exported.contains("[Event \"Ma Italienne\"]"))
        val back = graphOf(exported)
        assertEquals(graph.nodeCount, back.nodeCount)
        assertEquals(graph.edgeCount, back.edgeCount)
    }

    @Test
    fun `an unknown repertoire exports nothing`() {
        assertNull(runBlocking { viewModel.exportPgn("nope") })
    }

    @Test
    fun `the suggested file name is safe`() {
        assertEquals("Ma-Sicilienne-Najdorf.pgn", exportFileName("  Ma Sicilienne / Najdorf ?"))
        assertEquals("repertoire.pgn", exportFileName("???"))
        assertEquals("repertoire.pgn", exportFileName(null))
    }
}
