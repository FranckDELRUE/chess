package com.chesscompanion.core.chess.pgn

internal sealed interface PgnToken {
    val line: Int

    data class TagPair(
        val name: String,
        val value: String,
        override val line: Int,
    ) : PgnToken

    /** `{...}` or `;` comment, whitespace collapsed. */
    data class Comment(
        val text: String,
        override val line: Int,
    ) : PgnToken

    data class Nag(
        val value: Int,
        override val line: Int,
    ) : PgnToken

    data class Open(
        override val line: Int,
    ) : PgnToken

    data class Close(
        override val line: Int,
    ) : PgnToken

    data class MoveNumber(
        override val line: Int,
    ) : PgnToken

    data class Termination(
        val result: GameResult,
        override val line: Int,
    ) : PgnToken

    /** A move in SAN, with any suffix annotation (`!?`) still attached. */
    data class San(
        val text: String,
        override val line: Int,
    ) : PgnToken
}

/** Splits the text of one game into tokens; [firstLine] is the line number of its first line in the file. */
internal class PgnTokenizer(
    private val text: String,
    firstLine: Int,
) {
    private var index = 0
    private var line = firstLine
    private val tokens = mutableListOf<PgnToken>()

    fun tokenize(): List<PgnToken> {
        while (index < text.length) {
            val c = text[index]
            when {
                c == '\n' -> {
                    line++
                    index++
                }
                c.isWhitespace() -> index++
                c == '%' && (index == 0 || text[index - 1] == '\n') -> skipToEndOfLine()
                c == ';' -> lineComment()
                c == '{' -> braceComment()
                c == '[' -> tagPair()
                c == '(' -> single(PgnToken.Open(line))
                c == ')' -> single(PgnToken.Close(line))
                c == '$' -> nag()
                else -> symbol()
            }
        }
        return tokens
    }

    private fun single(token: PgnToken) {
        tokens += token
        index++
    }

    private fun skipToEndOfLine() {
        while (index < text.length && text[index] != '\n') index++
    }

    private fun lineComment() {
        val start = index + 1
        skipToEndOfLine()
        tokens += PgnToken.Comment(normalise(text.substring(start, index)), line)
    }

    private fun braceComment() {
        val startLine = line
        val end = text.indexOf('}', index)
        if (end < 0) throw PgnException("Unterminated comment", startLine)
        val body = text.substring(index + 1, end)
        line += body.count { it == '\n' }
        index = end + 1
        tokens += PgnToken.Comment(normalise(body), startLine)
    }

    private fun tagPair() {
        val match =
            TAG_REGEX.matchAt(text, index)
                ?: throw PgnException("Malformed tag '${text.substring(index).substringBefore('\n')}'", line)
        val value = match.groupValues[2].replace(ESCAPE_REGEX) { it.value.substring(1) }
        tokens += PgnToken.TagPair(match.groupValues[1], value, line)
        index = match.range.last + 1
    }

    private fun nag() {
        val start = ++index
        while (index < text.length && text[index].isDigit()) index++
        val value = text.substring(start, index).toIntOrNull() ?: throw PgnException("Malformed NAG", line)
        tokens += PgnToken.Nag(value, line)
    }

    private fun symbol() {
        val start = index
        while (index < text.length && !text[index].isWhitespace() && text[index] !in DELIMITERS) index++
        var symbol = text.substring(start, index)
        // Move number, possibly glued to the move: "12.", "12...", "1.e4".
        MOVE_NUMBER_REGEX.find(symbol)?.let {
            tokens += PgnToken.MoveNumber(line)
            symbol = symbol.substring(it.range.last + 1)
        }
        if (symbol.isEmpty()) return
        val result = GameResult.fromToken(symbol)
        tokens += if (result != null) PgnToken.Termination(result, line) else PgnToken.San(symbol, line)
    }

    private fun normalise(comment: String): String = comment.trim().replace(WHITESPACE_REGEX, " ")

    private companion object {
        const val DELIMITERS = "{}()[];$"
        val TAG_REGEX = Regex("""\[\s*([A-Za-z0-9_]+)\s+"((?:[^"\\]|\\.)*)"\s*]""")
        val ESCAPE_REGEX = Regex("""\\.""")
        val MOVE_NUMBER_REGEX = Regex("""^\d+\.+""")
        val WHITESPACE_REGEX = Regex("""\s+""")
    }
}
