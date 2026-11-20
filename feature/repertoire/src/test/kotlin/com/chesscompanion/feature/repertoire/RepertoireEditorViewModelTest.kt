package com.chesscompanion.feature.repertoire

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.pgn.PgnWriter
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.toGame
import com.chesscompanion.core.data.study.StudyRef
import com.chesscompanion.core.data.study.StudyResult
import com.chesscompanion.core.ui.board.AnnotationColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepertoireEditorViewModelTest {
    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val store = FakeRepertoireStore()
    private val studies = FakeStudyFetcher()

    private fun sq(name: String) = Square.parse(name)!!

    private fun editor(
        side: Side = Side.WHITE,
        fen: String? = null,
    ): RepertoireEditorViewModel {
        val id =
            kotlinx.coroutines.runBlocking {
                if (fen == null) store.create("Test", side) else store.create("Test", side, Fen.parse(fen))
            }
        return open(id)
    }

    private fun open(id: String) =
        RepertoireEditorViewModel(
            SavedStateHandle(mapOf(REPERTOIRE_ID_ARG to id)),
            store,
            studies,
        ).also {
            it.saveDelayMs =
                0
        }

    private fun RepertoireEditorViewModel.tap(vararg squares: String) =
        squares.forEach { onAction(EditorAction.SquareTap(sq(it))) }

    private val RepertoireEditorViewModel.ui get() = state.value

    @Test
    fun `an unknown repertoire is reported`() {
        assertEquals(EditorStatus.NOT_FOUND, open("none").ui.status)
    }

    @Test
    fun `the editor opens on the start position, from the side of the repertoire`() {
        val white = editor(Side.WHITE)
        assertEquals(EditorStatus.READY, white.ui.status)
        assertFalse(white.ui.flipped)
        assertEquals("", white.ui.lineText)
        assertFalse(white.ui.canGoBack)
        assertTrue(white.ui.continuations.isEmpty())
        assertTrue(editor(Side.BLACK).ui.flipped)
    }

    @Test
    fun `a move played on the board is added to the repertoire and saved`() {
        val vm = editor()
        vm.tap("e2", "e4")
        assertEquals("1. e4", vm.ui.lineText)
        assertEquals(1, vm.ui.moveCount)
        assertTrue(vm.ui.canGoBack)
        assertEquals(1, store.saves)
        assertEquals(1, store.repertoireOf("r1").edgeCount)
    }

    @Test
    fun `a drag and drop plays too, an illegal one does nothing`() {
        val vm = editor()
        vm.onAction(EditorAction.DragStart(sq("g1")))
        vm.onAction(EditorAction.Drop(sq("g1"), sq("g3")))
        assertEquals(0, vm.ui.moveCount)
        vm.onAction(EditorAction.DragStart(sq("g1")))
        vm.onAction(EditorAction.Drop(sq("g1"), sq("f3")))
        assertEquals("1. Cf3", vm.ui.lineText)
    }

    @Test
    fun `selecting a piece shows its targets`() {
        val vm = editor()
        vm.tap("g1")
        assertEquals(sq("g1"), vm.ui.highlights.selected)
        assertEquals(setOf(sq("f3"), sq("h3")), vm.ui.highlights.targets)
    }

    @Test
    fun `replaying a known move follows it and adds nothing`() {
        val vm = editor()
        vm.tap("e2", "e4")
        vm.onAction(EditorAction.Back)
        val saves = store.saves
        vm.tap("e2", "e4")
        assertEquals("1. e4", vm.ui.lineText)
        assertEquals(1, vm.ui.moveCount)
        assertEquals(saves, store.saves)
    }

    @Test
    fun `another move from the same position is a variation and both are offered`() {
        val vm = editor()
        vm.tap("e2", "e4")
        vm.onAction(EditorAction.Back)
        vm.tap("d2", "d4")
        vm.onAction(EditorAction.Back)
        assertEquals(listOf("e4", "d4"), vm.ui.continuations.map { it.label })
        vm.onAction(EditorAction.Follow("e2e4"))
        assertEquals("1. e4", vm.ui.lineText)
        assertEquals(2, vm.ui.moveCount)
    }

    @Test
    fun `navigation goes back, to the start, and follows a continuation`() {
        val vm = editor()
        vm.tap("e2", "e4")
        vm.tap("e7", "e5")
        vm.tap("g1", "f3")
        assertEquals("1. e4 e5 2. Cf3", vm.ui.lineText)
        vm.onAction(EditorAction.Back)
        assertEquals("1. e4 e5", vm.ui.lineText)
        vm.onAction(EditorAction.ToStart)
        assertEquals("", vm.ui.lineText)
        assertFalse(vm.ui.canGoBack)
        vm.onAction(EditorAction.Follow("e2e4"))
        vm.onAction(EditorAction.Follow("e7e5"))
        vm.onAction(EditorAction.Follow("zzzz"))
        assertEquals("1. e4 e5", vm.ui.lineText)
    }

    @Test
    fun `a promotion waits for its piece`() {
        val vm = editor(fen = "8/P6k/8/8/8/8/8/K7 w - - 0 1")
        vm.tap("a7", "a8")
        assertEquals(Side.WHITE, vm.ui.promotionSide)
        assertEquals(0, vm.ui.moveCount)
        vm.onAction(EditorAction.CancelPromotion)
        assertNull(vm.ui.promotionSide)
        vm.tap("a7", "a8")
        vm.onAction(EditorAction.Promote(PieceType.KNIGHT))
        assertNull(vm.ui.promotionSide)
        assertEquals(1, vm.ui.moveCount)
        assertEquals("a8=C", vm.ui.lineText.substringAfter("1. "))
    }

    @Test
    fun `in annotation mode taps and drags mark the position and play nothing`() {
        val vm = editor()
        vm.onAction(EditorAction.ToggleAnnotate)
        vm.onAction(EditorAction.PickAnnotationColor(AnnotationColor.RED))
        vm.tap("e4")
        vm.onAction(EditorAction.Drop(sq("g1"), sq("f3")))
        assertEquals(0, vm.ui.moveCount)
        assertEquals(1, vm.ui.annotations.squares.size)
        assertEquals(1, vm.ui.annotations.arrows.size)
        assertEquals(2, store.saves)
        val notes = store.repertoireOf("r1").let { it.notes(it.root) }
        assertEquals(1, notes.squares.size)
        assertEquals(1, notes.arrows.size)
        vm.tap("e4")
        assertTrue(
            vm.ui.annotations.squares
                .isEmpty(),
        )
        vm.onAction(EditorAction.ClearAnnotations)
        assertTrue(vm.ui.annotations.isEmpty)
    }

    @Test
    fun `marks belong to the position shown`() {
        val vm = editor()
        vm.onAction(EditorAction.ToggleAnnotate)
        vm.tap("e4")
        vm.onAction(EditorAction.ToggleAnnotate)
        vm.tap("e2", "e4")
        assertTrue(vm.ui.annotations.isEmpty)
        vm.onAction(EditorAction.ToStart)
        assertEquals(1, vm.ui.annotations.squares.size)
    }

    @Test
    fun `a comment is written on the position and saved`() {
        val vm = editor()
        vm.tap("e2", "e4")
        vm.onAction(EditorAction.EditComment("Le plus joué."))
        assertEquals("Le plus joué.", vm.ui.comment)
        vm.onAction(EditorAction.ToStart)
        assertEquals("", vm.ui.comment)
        vm.onAction(EditorAction.Follow("e2e4"))
        assertEquals("Le plus joué.", vm.ui.comment)
        val saved = store.repertoireOf("r1")
        assertEquals("Le plus joué.", saved.notes(saved.movesFrom(saved.root).single().to).comment)
    }

    @Test
    fun `a source exists once it has a book, and its other fields are kept while typing`() {
        val vm = editor()
        vm.onAction(EditorAction.EditSource(SourceField.CHAPTER, "3"))
        assertEquals("3", vm.ui.sourceChapter)
        assertNull(store.repertoireOf("r1").notes(store.repertoireOf("r1").root).source)
        vm.onAction(EditorAction.EditSource(SourceField.BOOK, "Mon livre"))
        vm.onAction(EditorAction.EditSource(SourceField.PAGE, "42"))
        val source = store.repertoireOf("r1").let { it.notes(it.root).source }!!
        assertEquals(Triple("Mon livre", "3", "42"), Triple(source.book, source.chapter, source.page))
        vm.onAction(EditorAction.EditSource(SourceField.BOOK, ""))
        assertNull(store.repertoireOf("r1").let { it.notes(it.root).source })
        assertEquals("42", vm.ui.sourcePage)
    }

    @Test
    fun `deleting the last move asks first, then removes it and what only it led to`() {
        val vm = editor()
        vm.tap("e2", "e4")
        vm.tap("e7", "e5")
        vm.onAction(EditorAction.Back)
        vm.onAction(EditorAction.AskDelete)
        assertTrue(vm.ui.confirmDelete)
        vm.onAction(EditorAction.DismissDelete)
        assertFalse(vm.ui.confirmDelete)
        assertEquals(2, vm.ui.moveCount)
        vm.onAction(EditorAction.AskDelete)
        vm.onAction(EditorAction.ConfirmDelete)
        assertEquals(0, vm.ui.moveCount)
        assertEquals("", vm.ui.lineText)
        assertEquals(0, store.repertoireOf("r1").edgeCount)
    }

    @Test
    fun `there is nothing to delete at the start position`() {
        val vm = editor()
        vm.onAction(EditorAction.AskDelete)
        assertFalse(vm.ui.confirmDelete)
    }

    @Test
    fun `flip changes the orientation only`() {
        val vm = editor()
        vm.onAction(EditorAction.Flip)
        assertTrue(vm.ui.flipped)
        assertEquals(0, store.saves)
    }

    @Test
    fun `with a delay, changes are saved together and flush saves at once`() =
        runTest {
            val vm = editor()
            vm.saveDelayMs = 60_000
            vm.tap("e2", "e4")
            vm.onAction(EditorAction.EditComment("x"))
            assertEquals(0, store.saves)
            vm.flush()
            assertEquals(1, store.saves)
            vm.flush()
            assertEquals(1, store.saves)
            assertEquals("x", store.repertoireOf("r1").let { it.notes(it.movesFrom(it.root).single().to).comment })
        }

    private val pgn = "[Event \"x\"]\n\n1. e4 e5 (1... c5) 2. Nf3 *\n\n[Event \"y\"]\n\n1. e4 Ke4 *\n"

    private fun RepertoireEditorViewModel.importing(text: String = pgn) {
        importDispatcher = UnconfinedTestDispatcher()
        onAction(EditorAction.OpenImport)
        onAction(EditorAction.RunImport(text))
    }

    @Test
    fun `the import dialog opens and closes`() {
        val vm = editor()
        assertEquals(ImportUiState.Closed, vm.ui.import)
        vm.onAction(EditorAction.OpenImport)
        assertEquals(ImportUiState.Form, vm.ui.import)
        vm.onAction(EditorAction.CloseImport)
        assertEquals(ImportUiState.Closed, vm.ui.import)
    }

    @Test
    fun `an import adds the lines, saves them at once and reports`() {
        val vm = editor()
        vm.importing()
        val done = vm.ui.import as ImportUiState.Done
        assertEquals(1, done.summary.gamesMerged)
        assertEquals(2, done.summary.gamesInFile)
        assertEquals(4, done.summary.addedMoves)
        assertEquals(listOf(2), done.summary.rejected.map { it.gameNumber })
        assertEquals(4, vm.ui.moveCount)
        assertEquals(1, store.saves)
        assertEquals(4, store.repertoireOf("r1").edgeCount)
        assertEquals(listOf("e4"), vm.ui.continuations.map { it.label })
    }

    @Test
    fun `importing what is already there changes and saves nothing`() {
        val vm = editor()
        vm.importing()
        vm.onAction(EditorAction.CloseImport)
        vm.importing()
        val done = vm.ui.import as ImportUiState.Done
        assertEquals(0, done.summary.addedMoves)
        assertEquals(1, store.saves)
    }

    @Test
    fun `an import keeps the position shown`() {
        val vm = editor()
        vm.tap("e2", "e4")
        vm.importing()
        assertEquals("1. e4", vm.ui.lineText)
        assertEquals(listOf("e5", "c5"), vm.ui.continuations.map { it.label })
    }

    @Test
    fun `an unreadable file is reported and can be closed`() {
        val vm = editor()
        vm.onAction(EditorAction.OpenImport)
        vm.onAction(EditorAction.ImportFileUnreadable)
        assertEquals(ImportUiState.FileUnreadable, vm.ui.import)
        vm.onAction(EditorAction.CloseImport)
        assertEquals(ImportUiState.Closed, vm.ui.import)
    }

    @Test
    fun `an empty text reports no game and saves nothing`() {
        val vm = editor()
        vm.importing("   ")
        assertEquals(0, (vm.ui.import as ImportUiState.Done).summary.gamesInFile)
        assertEquals(0, store.saves)
    }

    @Test
    fun `a fetched study is merged like a pgn`() {
        val vm = editor()
        studies.result = StudyResult.Found(pgn)
        vm.importDispatcher = UnconfinedTestDispatcher()
        vm.onAction(EditorAction.FetchStudy("https://lichess.org/study/Y1yXP80U/oxgS1aRW"))
        assertEquals(listOf(StudyRef("Y1yXP80U", "oxgS1aRW")), studies.asked)
        assertEquals(4, (vm.ui.import as ImportUiState.Done).summary.addedMoves)
        assertEquals(4, store.repertoireOf("r1").edgeCount)
    }

    @Test
    fun `each way a study can fail is reported and changes nothing`() {
        val expected =
            mapOf(
                StudyResult.NotFound to StudyProblem.NOT_FOUND,
                StudyResult.Private to StudyProblem.PRIVATE,
                StudyResult.RateLimited to StudyProblem.RATE_LIMITED,
                StudyResult.Offline to StudyProblem.OFFLINE,
                StudyResult.ServerError(503) to StudyProblem.SERVER,
                StudyResult.TooLarge to StudyProblem.TOO_LARGE,
            )
        expected.forEach { (result, problem) ->
            val vm = editor()
            studies.result = result
            vm.onAction(EditorAction.FetchStudy("Y1yXP80U"))
            assertEquals(ImportUiState.StudyFailed(problem), vm.ui.import)
            assertEquals(0, vm.ui.moveCount)
        }
        assertEquals(0, store.saves)
    }

    @Test
    fun `an unrecognised study reference is refused without asking Lichess`() {
        val vm = editor()
        vm.onAction(EditorAction.FetchStudy("https://example.com/study/Y1yXP80U"))
        assertEquals(ImportUiState.StudyFailed(StudyProblem.INVALID_REFERENCE), vm.ui.import)
        assertTrue(studies.asked.isEmpty())
    }

    @Test
    fun `the saved repertoire exports to PGN with its comment and marks`() {
        val vm = editor()
        vm.tap("e2", "e4")
        vm.onAction(EditorAction.EditComment("Ouverture du roi"))
        vm.onAction(EditorAction.ToggleAnnotate)
        vm.onAction(EditorAction.Drop(sq("g1"), sq("f3")))
        val pgn = PgnWriter.write(store.repertoireOf("r1").toGame("Test"))
        val back = Repertoire.empty(Side.WHITE).merge(PgnReader.read(pgn)).repertoire
        val node = back.movesFrom(back.root).single().to
        assertEquals("Ouverture du roi", back.notes(node).comment)
        assertEquals(1, back.notes(node).arrows.size)
    }
}
