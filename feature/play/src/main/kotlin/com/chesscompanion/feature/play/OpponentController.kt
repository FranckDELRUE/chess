package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.Game
import com.chesscompanion.core.chess.pgn.GameResult
import com.chesscompanion.core.chess.pgn.GameSession
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.SearchLimit
import com.chesscompanion.core.engine.SearchRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Runs Stockfish for the player's opponent (`EnginePriority.OPPONENT` in production); a fake in tests. */
fun interface OpponentEngine {
    fun search(request: SearchRequest): Flow<EngineEvent>
}

/** What [OpponentController] tells the game screen. */
class OpponentCallbacks(
    /** Stockfish chose a move for the position the controller was last synchronised with (already checked). */
    val onEngineMove: (Move) -> Unit,
    /** Stockfish gave no usable move. */
    val onEngineFailed: () -> Unit,
    /** The controller's state changed on its own (a search ended, the setup dialog opened). */
    val onChanged: () -> Unit,
)

/**
 * The player's opponent (S2-4): the settings of the game against Stockfish, who plays which side,
 * and the search of Stockfish's move. The game itself stays in the screen's `GameSession`; the
 * controller is told of every change through [sync] and answers actions with a new session.
 *
 * Stockfish moves when it is its turn, the game is not over and the player has not resigned, and
 * either the position shown ends the line or the player has just moved ([humanMoved]): browsing an
 * old position never makes it play. Its move is checked by the rules before it is played.
 */
class OpponentController(
    private val engine: OpponentEngine,
    private val scope: CoroutineScope,
    private val saved: SavedStateHandle,
    private val host: OpponentCallbacks,
    vsEngine: Boolean,
    private val random: Random = Random.Default,
) {
    private var settings: OpponentSettings? = OpponentSettings.decode(saved[KEY_SETTINGS])
    private var humanSide: Side = if (saved.get<String>(KEY_SIDE) == BLACK) Side.BLACK else Side.WHITE
    private var resigned: Boolean = saved[KEY_RESIGNED] ?: false
    private var setup: OpponentSettings? = null
    private var thinking = false
    private var due = false
    private var current: GameSession? = null
    private var job: Job? = null
    private var searched: String? = null
    private var failed: String? = null

    init {
        if (settings == null && vsEngine) {
            settings = OpponentSettings()
            setup = settings
            persist()
        }
    }

    /** True when the game is against Stockfish. */
    val active: Boolean get() = settings != null

    fun ui(session: GameSession): OpponentUi? =
        settings?.let {
            OpponentUi(it, humanSide, thinking, resigned, setup, over = resigned || session.status().isOver)
        }

    /** The player may move: always in the two-player game, on its own turn against Stockfish. */
    fun canHumanMove(position: Position): Boolean = !active || (!resigned && position.sideToMove == humanSide)

    /** The analysis is hidden while a game against Stockfish is being played. */
    fun analysisAllowed(session: GameSession): Boolean = !active || resigned || session.status().isOver

    /** The player has just played: Stockfish answers even if the move was already in the tree. */
    fun humanMoved() {
        due = true
    }

    /**
     * Handles an opponent action on the game shown; returns the session to show next when the action
     * changes the game (a new game, a take-back), null otherwise.
     */
    fun accept(
        action: PlayAction.Opponent,
        session: GameSession,
    ): GameSession? {
        val result =
            when (action) {
                PlayAction.ShowOpponentSetup -> {
                    setup = settings ?: OpponentSettings()
                    null
                }
                PlayAction.DismissOpponentSetup -> {
                    setup = null
                    null
                }
                is PlayAction.StartOpponentGame -> start(action.settings)
                PlayAction.TakeBack -> {
                    cancel()
                    resigned = false
                    failed = null
                    takeBackTarget(session, humanSide)
                }
                PlayAction.Resign -> {
                    resigned = true
                    cancel()
                    null
                }
            }
        persist()
        host.onChanged()
        return result
    }

    /** The game shown changed: starts, keeps or cancels the search of Stockfish's move. */
    fun sync(session: GameSession) {
        current = session
        val chosen = settings
        val position = session.position
        val engineTurn =
            chosen != null &&
                setup == null &&
                !resigned &&
                !session.status().isOver &&
                position.sideToMove != humanSide &&
                (due || !session.canGoForward)
        if (!engineTurn || chosen == null) {
            cancel()
            return
        }
        val key = gameKey(session)
        if (key == searched || key == failed) return
        search(session, chosen, key)
    }

    /** The game ready to export: a resignation gives the win to Stockfish. */
    fun finish(game: Game): Game = if (resigned && active) resignedGame(game, humanSide) else game

    private fun start(chosen: OpponentSettings): GameSession {
        cancel()
        settings = chosen
        humanSide = sideOf(chosen.color, random)
        resigned = false
        setup = null
        failed = null
        due = false
        return namedGame(GameSession.new(), chosen, humanSide)
    }

    private fun search(
        session: GameSession,
        chosen: OpponentSettings,
        key: String,
    ) {
        cancel()
        searched = key
        due = false
        thinking = true
        host.onChanged()
        val request =
            SearchRequest(
                fen = session.game.startPosition.toFen(),
                limit = SearchLimit.MoveTime(chosen.moveTimeMs),
                moves = session.line.map { it.move.uci },
                strength = chosen.strength,
            )
        val expected = session.position
        job =
            scope.launch {
                val uci = engine.bestMove(request).getOrNull()
                thinking = false
                val move = uci?.let(Move::parseUci)
                val still = current?.position == expected
                when {
                    !still -> host.onChanged()
                    move != null && expected.isLegal(move) -> host.onEngineMove(move)
                    else -> {
                        failed = key
                        host.onEngineFailed()
                    }
                }
            }
    }

    private fun cancel() {
        job?.cancel()
        job = null
        searched = null
        if (thinking) {
            thinking = false
            host.onChanged()
        }
    }

    private fun persist() {
        saved[KEY_SETTINGS] = settings?.encode()
        saved[KEY_SIDE] = if (humanSide == Side.BLACK) BLACK else WHITE
        saved[KEY_RESIGNED] = resigned
    }

    private companion object {
        const val KEY_SETTINGS = "opponent.settings"
        const val KEY_SIDE = "opponent.side"
        const val KEY_RESIGNED = "opponent.resigned"
        const val BLACK = "b"
        const val WHITE = "w"
    }
}

/** The move Stockfish ends on (null if it gave none), or the failure of the search. */
private suspend fun OpponentEngine.bestMove(request: SearchRequest): Result<String?> =
    try {
        var move: String? = null
        search(request).collect { if (it is EngineEvent.BestMove) move = it.move }
        Result.success(move)
    } catch (e: CancellationException) {
        throw e
    } catch (
        @Suppress("TooGenericExceptionCaught") e: Exception,
    ) {
        Result.failure(e)
    }

private fun sideOf(
    color: PlayerColor,
    random: Random,
): Side =
    when (color) {
        PlayerColor.WHITE -> Side.WHITE
        PlayerColor.BLACK -> Side.BLACK
        PlayerColor.RANDOM -> if (random.nextBoolean()) Side.WHITE else Side.BLACK
    }

/** Back to the last position before the current one where it is the player's turn. */
private fun takeBackTarget(
    session: GameSession,
    humanSide: Side,
): GameSession {
    var target = session.back()
    while (target.canGoBack && target.position.sideToMove != humanSide) target = target.back()
    if (target.position.sideToMove != humanSide) target = target.forward()
    return target
}

/** Identifies a search: the start position and the moves played. */
private fun gameKey(session: GameSession): String =
    session.game.startPosition.toFen() + session.line.joinToString(" ") { it.move.uci }

/** The new game with the players and the strength written in its PGN tags. */
private fun namedGame(
    session: GameSession,
    chosen: OpponentSettings,
    humanSide: Side,
): GameSession {
    val seconds = chosen.moveTimeMs / MS_PER_S
    val tags =
        mapOf(
            "Event" to "Casual game against Stockfish (${chosen.strengthLabel}, $seconds s a move)",
            "White" to if (humanSide == Side.WHITE) HUMAN else STOCKFISH,
            "Black" to if (humanSide == Side.BLACK) HUMAN else STOCKFISH,
        )
    val game = session.game
    return session.copy(
        game =
            game.copy(
                tags =
                    game.tags.map { tag ->
                        tags[tag.name]?.let { tag.copy(value = it) }
                            ?: tag
                    },
            ),
    )
}

/** A resignation of the player gives the win to Stockfish. */
private fun resignedGame(
    game: Game,
    humanSide: Side,
): Game {
    val result = if (humanSide == Side.WHITE) GameResult.BLACK_WINS else GameResult.WHITE_WINS
    return game.copy(
        result = result,
        tags = game.tags.map { if (it.name == "Result") it.copy(value = result.token) else it },
    )
}

private const val HUMAN = "Player"
private const val STOCKFISH = "Stockfish"
private const val MS_PER_S = 1_000L
