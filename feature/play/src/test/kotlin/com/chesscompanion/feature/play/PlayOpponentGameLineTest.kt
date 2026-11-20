package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.engine.EngineStrength
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * S2-8: defects found in the S2 demo. After a take-back the game played is the main line; a game
 * opened or pasted ends the game against Stockfish.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayOpponentGameLineTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val settings = OpponentSettings(PlayerColor.WHITE, EngineStrength.Elo(1500), 1_000)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vsEngine(
        opponent: FakeOpponent,
        saved: SavedStateHandle = SavedStateHandle(mapOf("vs" to "engine")),
        documents: FakeDocuments = FakeDocuments(),
        queue: FakeQueue = FakeQueue(),
    ) = PlayViewModel(saved, documents, FakeAnalyzer(), opponent, queue)

    private fun PlayViewModel.tap(vararg squares: String) =
        squares.forEach { onAction(PlayAction.SquareTap(Square.parse(it)!!)) }

    private fun mainline(pgn: String): List<String> = PgnReader.read(pgn).mainline.map { it.san }

    @Test
    fun `after a take-back the game played is the main line and the moves taken back a variation`() {
        val queue = FakeQueue()
        val vm = vsEngine(FakeOpponent("e7e5", "b8c6", "g8f6"), queue = queue)
        vm.onAction(PlayAction.StartOpponentGame(settings))
        vm.tap("e2", "e4", "g1", "f3")
        vm.onAction(PlayAction.TakeBack)
        vm.tap("d2", "d4")
        vm.onAction(PlayAction.Resign)

        val pgn = vm.exportPgn()
        assertEquals(listOf("e4", "e5", "d4", "Nf6"), mainline(pgn))
        assertTrue(pgn.contains("( 2. Nf3 Nc6 )"), pgn)
        assertTrue(pgn.contains("[Result \"0-1\"]"))
        assertTrue(pgn.trimEnd().endsWith("0-1"))
        // The game analysis gets the game actually played.
        vm.onAction(PlayAction.AnalyseGame)
        assertEquals(listOf("e4", "e5", "d4", "Nf6"), mainline(queue.enqueued.single()))
    }

    @Test
    fun `replaying the move taken back keeps a single main line`() {
        val vm = vsEngine(FakeOpponent("e7e5", "b8c6", "b8c6"))
        vm.onAction(PlayAction.StartOpponentGame(settings))
        vm.tap("e2", "e4", "g1", "f3")
        vm.onAction(PlayAction.TakeBack)
        vm.tap("g1", "f3")
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6"), mainline(vm.exportPgn()))
        assertTrue(!vm.exportPgn().contains("( "))
    }

    @Test
    fun `browsing without playing does not reorder the game`() {
        val vm = vsEngine(FakeOpponent("e7e5", "b8c6", "g8f6"))
        vm.onAction(PlayAction.StartOpponentGame(settings))
        vm.tap("e2", "e4", "g1", "f3")
        vm.onAction(PlayAction.TakeBack)
        vm.tap("d2", "d4")
        // Into the variation 2. Nf3 Nc6, without playing.
        vm.onAction(PlayAction.GoTo(listOf(0, 0, 1)))
        assertEquals(listOf("e4", "e5", "d4", "Nf6"), mainline(vm.exportPgn()))
    }

    @Test
    fun `opening a game after a resigned game against stockfish keeps the file as it is`() {
        val file =
            """
            [Event "Club"]
            [White "Heidoji"]
            [Black "Rival"]
            [Result "1-0"]

            1. e4 e5 2. Nf3 1-0
            """.trimIndent()
        val documents = FakeDocuments(mutableMapOf("content://game.pgn" to file))
        val saved = SavedStateHandle(mapOf("vs" to "engine"))
        val vm = vsEngine(FakeOpponent("c7c5"), saved, documents)
        vm.onAction(PlayAction.StartOpponentGame(settings))
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.Resign)

        vm.onAction(PlayAction.OpenFile("content://game.pgn"))

        assertNull(vm.state.value.opponent)
        val pgn = vm.exportPgn()
        assertTrue(pgn.contains("[White \"Heidoji\"]"), pgn)
        assertTrue(pgn.contains("[Result \"1-0\"]"), pgn)
        assertTrue(!pgn.contains("Stockfish"))
        // Still a two-player game after process death, despite the engine route.
        val restored = vsEngine(FakeOpponent(), saved, documents)
        assertNull(restored.state.value.opponent)
        assertTrue(restored.exportPgn().contains("[Result \"1-0\"]"))
    }

    @Test
    fun `pasting a position while stockfish thinks stops it`() {
        val engine = FakeOpponent("e7e5")
        engine.gate = CompletableDeferred()
        val vm = vsEngine(engine)
        vm.onAction(PlayAction.StartOpponentGame(settings))
        vm.tap("e2", "e4")
        vm.onAction(PlayAction.ImportText("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"))
        engine.gate!!.complete(Unit)
        assertEquals(1, engine.cancelled)
        assertNull(vm.state.value.opponent)
        assertEquals("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1", vm.state.value.fen)
        assertEquals(1, engine.requests.size)
    }

    @Test
    fun `a new game against stockfish after an import is a game against stockfish again`() {
        val saved = SavedStateHandle(mapOf("vs" to "engine"))
        val vm = vsEngine(FakeOpponent("e7e5"), saved)
        vm.onAction(PlayAction.ImportText("1. d4 d5 *"))
        assertNull(vm.state.value.opponent)
        vm.onAction(PlayAction.StartOpponentGame(settings))
        vm.tap("e2", "e4")
        assertEquals(listOf("e4", "e5"), mainline(vm.exportPgn()))
        assertEquals(
            settings,
            vsEngine(FakeOpponent(), saved)
                .state.value.opponent!!
                .settings,
        )
    }
}
