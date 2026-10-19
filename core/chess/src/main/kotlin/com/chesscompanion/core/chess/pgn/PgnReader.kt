package com.chesscompanion.core.chess.pgn

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.InvalidFenException
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.San
import com.chesscompanion.core.chess.Side

class PgnException(
    /** What is wrong, without the line number. */
    val detail: String,
    /** 1-based line in the file. */
    val line: Int,
    cause: Throwable? = null,
) : IllegalArgumentException("Line $line: $detail", cause)

/** An error or warning about the [gameNumber]-th game of a file (1-based). */
data class PgnIssue(
    val gameNumber: Int,
    val line: Int,
    val message: String,
)

data class PgnImport(
    val games: List<Game>,
    val errors: List<PgnIssue>,
    val warnings: List<PgnIssue>,
)

/**
 * PGN reader. Every move is checked by the rules (rule 2); a faulty game is skipped and reported
 * with its line, and reading goes on with the next game.
 */
object PgnReader {
    /** Suffix annotations and their NAG (PGN standard, section 10). */
    private val SUFFIX_NAGS = mapOf("!" to 1, "?" to 2, "!!" to 3, "??" to 4, "!?" to 5, "?!" to 6)
    private val SUFFIX_REGEX = Regex("[!?]{1,2}$")

    fun readAll(text: String): PgnImport {
        val games = mutableListOf<Game>()
        val errors = mutableListOf<PgnIssue>()
        val warnings = mutableListOf<PgnIssue>()
        splitGames(text.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n'))
            .forEachIndexed { index, chunk ->
                try {
                    val tokens = PgnTokenizer(chunk.text, chunk.firstLine).tokenize()
                    val parsed = GameParser(tokens, chunk.firstLine).parse()
                    games += parsed.game
                    parsed.warnings.forEach { warnings += PgnIssue(index + 1, it.line, it.detail) }
                } catch (e: PgnException) {
                    errors += PgnIssue(index + 1, e.line, e.detail)
                }
            }
        return PgnImport(games, errors, warnings)
    }

    /** Reads exactly one game; throws [PgnException] on any error. */
    fun read(text: String): Game {
        val import = readAll(text)
        import.errors.firstOrNull()?.let { throw PgnException(it.message, it.line) }
        return import.games.singleOrNull() ?: throw PgnException("Expected one game, found ${import.games.size}", 1)
    }

    private class Chunk(
        val text: String,
        val firstLine: Int,
    )

    /** Cuts the file before each tag section that follows movetext, ignoring brackets inside comments. */
    private fun splitGames(text: String): List<Chunk> {
        val chunks = mutableListOf<Chunk>()
        val current = StringBuilder()
        var firstLine = 1
        var inComment = false
        var seenMovetext = false
        text.split('\n').forEachIndexed { index, line ->
            val trimmed = line.trimStart()
            val isTagLine = !inComment && trimmed.startsWith("[")
            if (isTagLine && seenMovetext) {
                chunks += Chunk(current.toString(), firstLine)
                current.clear()
                firstLine = index + 1
                seenMovetext = false
            }
            current.append(line).append('\n')
            if (!isTagLine) {
                if (trimmed.isNotEmpty() && !trimmed.startsWith("%")) seenMovetext = true
                inComment = commentStateAfter(line, inComment)
            }
        }
        if (current.isNotBlank()) chunks += Chunk(current.toString(), firstLine)
        return chunks.filter { it.text.isNotBlank() }
    }

    private fun commentStateAfter(
        line: String,
        startsInComment: Boolean,
    ): Boolean {
        var inComment = startsInComment
        for (c in line) {
            when {
                inComment && c == '}' -> inComment = false
                !inComment && c == '{' -> inComment = true
                !inComment && c == ';' -> return false
            }
        }
        return inComment
    }

    private class ParsedGame(
        val game: Game,
        val warnings: List<PgnException>,
    )

    /** Mutable node used while reading; frozen into [MoveNode] at the end. */
    private class Builder(
        val move: Move?,
        val san: String,
        val position: Position,
        val parent: Builder?,
        val commentsBefore: List<String>,
    ) {
        val commentsAfter = mutableListOf<String>()
        val nags = mutableListOf<Int>()
        val children = mutableListOf<Builder>()

        fun freeze(): MoveNode =
            MoveNode(
                move = checkNotNull(move),
                san = san,
                position = position,
                commentsBefore = commentsBefore,
                commentsAfter = commentsAfter.toList(),
                nags = nags.toList(),
                children = children.map { it.freeze() },
            )
    }

    private class GameParser(
        private val tokens: List<PgnToken>,
        private val firstLine: Int,
    ) {
        private val warnings = mutableListOf<PgnException>()
        private var position = 0

        fun parse(): ParsedGame {
            val rawTags = mutableListOf<Tag>()
            while (position < tokens.size) {
                val token = tokens[position] as? PgnToken.TagPair ?: break
                rawTags += Tag(token.name, token.value)
                position++
            }
            val start = startPosition(rawTags)
            val root = Builder(null, "", start, null, emptyList())
            val (marker, markerLine) = readMovetext(root)
            val result = reconcileResult(rawTags, marker, markerLine, warnings)
            val tags = normaliseTags(rawTags, result, firstLine, warnings)
            val game =
                Game(
                    tags = tags,
                    startPosition = start,
                    moves = root.children.map { it.freeze() },
                    result = result,
                    comments = root.commentsAfter.toList(),
                )
            return ParsedGame(game, warnings)
        }

        private fun startPosition(tags: List<Tag>): Position {
            val fen = tags.lastOrNull { it.name == "FEN" }?.value ?: return Position.START
            return try {
                Fen.parse(fen)
            } catch (e: InvalidFenException) {
                throw PgnException("Invalid FEN tag: ${e.message}", firstLine, e)
            }
        }

        // Movetext state: the node the next move is played from, comments waiting for the next
        // move, and the nodes to come back to when a variation closes.
        private lateinit var cursor: Builder
        private var afterMove = false
        private val pending = mutableListOf<String>()
        private val variations = ArrayDeque<Builder>()

        /** Reads the movetext into [root]; returns the result marker (null if missing) and its line. */
        private fun readMovetext(root: Builder): Pair<GameResult?, Int> {
            cursor = root
            while (position < tokens.size) {
                when (val token = tokens[position++]) {
                    is PgnToken.MoveNumber -> Unit
                    is PgnToken.San -> onMove(token)
                    is PgnToken.Comment -> if (afterMove) cursor.commentsAfter += token.text else pending += token.text
                    is PgnToken.Nag -> onNag(token)
                    is PgnToken.Open -> onOpen(token)
                    is PgnToken.Close -> onClose(token)
                    is PgnToken.Termination -> return onTermination(token)
                    is PgnToken.TagPair -> throw PgnException("Tag '${token.name}' inside the moves", token.line)
                }
            }
            if (variations.isNotEmpty()) throw PgnException("Unclosed variation", lastLine())
            cursor.commentsAfter += pending
            return null to lastLine()
        }

        private fun onMove(token: PgnToken.San) {
            cursor = playMove(cursor, token, pending.toList())
            pending.clear()
            afterMove = true
        }

        private fun onNag(token: PgnToken.Nag) {
            if (cursor.move == null) throw PgnException("Annotation \$${token.value} before any move", token.line)
            cursor.nags += token.value
        }

        private fun onOpen(token: PgnToken.Open) {
            val parent = cursor.parent ?: throw PgnException("Variation before any move", token.line)
            variations.addLast(cursor)
            cursor = parent
            afterMove = false
        }

        private fun onClose(token: PgnToken.Close) {
            if (variations.isEmpty()) throw PgnException("Unexpected ')'", token.line)
            cursor.commentsAfter += pending
            pending.clear()
            cursor = variations.removeLast()
            afterMove = false
        }

        private fun onTermination(token: PgnToken.Termination): Pair<GameResult?, Int> {
            if (variations.isNotEmpty()) throw PgnException("Unclosed variation", token.line)
            if (position < tokens.size) throw PgnException("Text after the result", tokens[position].line)
            cursor.commentsAfter += pending
            return token.result to token.line
        }

        private fun playMove(
            cursor: Builder,
            token: PgnToken.San,
            commentsBefore: List<String>,
        ): Builder {
            val suffix = SUFFIX_REGEX.find(token.text)?.value
            val before = cursor.position
            val move =
                try {
                    San.parse(before, token.text)
                } catch (e: IllegalArgumentException) {
                    // IllegalMoveException or InvalidMoveNotationException.
                    throw PgnException("Move ${moveLabel(before, token.text)}: ${e.message}", token.line, e)
                }
            val node = Builder(move, San.write(before, move), before.play(move), cursor, commentsBefore)
            suffix?.let { SUFFIX_NAGS[it] }?.let { node.nags += it }
            cursor.children += node
            return node
        }

        private fun lastLine(): Int = tokens.lastOrNull()?.line ?: firstLine
    }

    private fun moveLabel(
        position: Position,
        san: String,
    ): String = "${position.fullmoveNumber}${if (position.sideToMove == Side.WHITE) "." else "..."} $san"

    /** The marker at the end of the moves wins over the Result tag; the tag is used if the marker is missing. */
    private fun reconcileResult(
        tags: List<Tag>,
        marker: GameResult?,
        line: Int,
        warnings: MutableList<PgnException>,
    ): GameResult {
        val tagValue = tags.lastOrNull { it.name == "Result" }?.value
        if (marker == null) {
            warnings += PgnException("Missing result at the end of the moves", line)
            return tagValue?.let { GameResult.fromToken(it) } ?: GameResult.UNKNOWN
        }
        if (tagValue != null && tagValue != marker.token) {
            warnings += PgnException("Result tag '$tagValue' differs from '${marker.token}' in the moves", line)
        }
        return marker
    }

    /** Seven Tag Roster first (missing ones filled), then the other tags in their original order. */
    private fun normaliseTags(
        raw: List<Tag>,
        result: GameResult,
        line: Int,
        warnings: MutableList<PgnException>,
    ): List<Tag> {
        val values = LinkedHashMap<String, String>()
        raw.forEach { tag ->
            if (values.put(tag.name, tag.value) != null) {
                warnings += PgnException("Duplicate tag '${tag.name}', last value kept", line)
            }
        }
        values["Result"] = result.token
        val roster = Game.SEVEN_TAG_ROSTER.map { Tag(it.name, values[it.name] ?: it.value) }
        val others = values.filterKeys { name -> Game.SEVEN_TAG_ROSTER.none { it.name == name } }
        return roster + others.map { (name, value) -> Tag(name, value) }
    }
}
