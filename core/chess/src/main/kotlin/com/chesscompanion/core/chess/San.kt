package com.chesscompanion.core.chess

import kotlin.math.abs

/** Thrown for move text that is malformed or matches several legal moves. */
class InvalidMoveNotationException(
    message: String,
) : IllegalArgumentException(message)

/**
 * Piece letters used when writing SAN. PGN and engines use [ENGLISH]; [FRENCH] (Roi, Dame, Tour,
 * Fou, Cavalier) is for display only and is never parsed: its R would read as an English rook.
 */
enum class PieceLetters(
    private val king: Char,
    private val queen: Char,
    private val rook: Char,
    private val bishop: Char,
    private val knight: Char,
) {
    ENGLISH('K', 'Q', 'R', 'B', 'N'),
    FRENCH('R', 'D', 'T', 'F', 'C'),
    ;

    /** Letter of a piece; pawns have none. */
    fun letter(type: PieceType): Char? =
        when (type) {
            PieceType.KING -> king
            PieceType.QUEEN -> queen
            PieceType.ROOK -> rook
            PieceType.BISHOP -> bishop
            PieceType.KNIGHT -> knight
            PieceType.PAWN -> null
        }
}

/** Standard Algebraic Notation: exact writer and tolerant reader, always checked against the legal moves. */
object San {
    private const val KINGSIDE = "O-O"
    private const val QUEENSIDE = "O-O-O"
    private const val GROUP_PIECE = 1
    private const val GROUP_FILE = 2
    private const val GROUP_RANK = 3
    private const val GROUP_TARGET = 4
    private const val GROUP_PROMOTION = 5

    // Piece, origin file, origin rank, separator (- x :), target, promotion as =Q, Q, (Q) or =q.
    private val moveRegex =
        Regex("^([KQRBN])?([a-h])?([1-8])?[-x:]?([a-h][1-8])(?:=?\\(?([QRBNqrbn])\\)?)?$")
    private val castlingRegex = Regex("^([O0o])-\\1(-\\1)?$")
    private val trailingNoise = Regex("(\\s*e\\.?p\\.?|[+#!?])+$")

    /** Writes [move] in SAN; throws [IllegalMoveException] if it is not legal in [position]. */
    fun write(
        position: Position,
        move: Move,
        letters: PieceLetters = PieceLetters.ENGLISH,
    ): String {
        if (!position.isLegal(move)) throw IllegalMoveException("Illegal move $move in ${position.toFen()}")
        val piece = checkNotNull(position.board[move.from])
        val body =
            when {
                piece.type == PieceType.KING && abs(move.to.file - move.from.file) == 2 ->
                    if (move.to.file > move.from.file) KINGSIDE else QUEENSIDE
                piece.type == PieceType.PAWN -> pawnMove(move, letters)
                else -> pieceMove(position, move, piece, letters)
            }
        val next = position.play(move)
        val suffix =
            when {
                !next.isCheck -> ""
                next.legalMoves.isEmpty() -> "#"
                else -> "+"
            }
        return body + suffix
    }

    /**
     * Reads SAN leniently: `0-0`, missing or wrong `+`/`#`, annotations (`!?`), `e.p.`, `e8Q`,
     * `e8=q`, `e8(Q)`, extra disambiguation (`Ng1f3`, `Ng1-f3`) and a missing `x` are accepted.
     * Piece letters are English and case-sensitive (`b` is a file, `B` a bishop).
     *
     * Throws [InvalidMoveNotationException] if the text is malformed or ambiguous, and
     * [IllegalMoveException] if no legal move matches it.
     */
    fun parse(
        position: Position,
        text: String,
    ): Move {
        val candidates = candidates(position, text)
        return when (candidates.size) {
            1 -> candidates.single()
            0 -> throw IllegalMoveException("No legal move matches '$text' in ${position.toFen()}")
            else -> throw InvalidMoveNotationException(
                "Ambiguous SAN '$text' (${candidates.joinToString { it.uci }}) in ${position.toFen()}",
            )
        }
    }

    private fun candidates(
        position: Position,
        text: String,
    ): List<Move> {
        val cleaned = text.trim().replace(trailingNoise, "")
        val castling = castlingRegex.matchEntire(cleaned)
        if (castling != null) return castlingCandidates(position, kingside = castling.groupValues[2].isEmpty())
        val match = moveRegex.matchEntire(cleaned) ?: throw InvalidMoveNotationException("Malformed SAN '$text'")
        return moveCandidates(position, match)
    }

    private fun pawnMove(
        move: Move,
        letters: PieceLetters,
    ): String =
        buildString {
            if (move.from.file != move.to.file) append('a' + move.from.file).append('x')
            append(move.to.name)
            move.promotion?.let { append('=').append(letters.letter(it)) }
        }

    private fun pieceMove(
        position: Position,
        move: Move,
        piece: Piece,
        letters: PieceLetters,
    ): String {
        val rivals =
            position.legalMoves
                .filter { it.to == move.to && it.from != move.from && position.board[it.from]?.type == piece.type }
                .map { it.from }
        val disambiguation =
            when {
                rivals.isEmpty() -> ""
                rivals.none { it.file == move.from.file } -> ('a' + move.from.file).toString()
                rivals.none { it.rank == move.from.rank } -> ('1' + move.from.rank).toString()
                else -> move.from.name
            }
        val capture = if (position.board[move.to] != null) "x" else ""
        return "${letters.letter(piece.type)}$disambiguation$capture${move.to}"
    }

    private fun castlingCandidates(
        position: Position,
        kingside: Boolean,
    ): List<Move> =
        position.legalMoves.filter { move ->
            position.board[move.from]?.type == PieceType.KING &&
                abs(move.to.file - move.from.file) == 2 &&
                (move.to.file > move.from.file) == kingside
        }

    private fun moveCandidates(
        position: Position,
        match: MatchResult,
    ): List<Move> {
        val groups = match.groupValues
        val pieceText = groups[GROUP_PIECE]
        val fileText = groups[GROUP_FILE]
        val rankText = groups[GROUP_RANK]
        val targetText = groups[GROUP_TARGET]
        val promotionText = groups[GROUP_PROMOTION]
        val type =
            pieceText.firstOrNull()?.let { letter ->
                PieceType.entries.first { PieceLetters.ENGLISH.letter(it) == letter }
            } ?: PieceType.PAWN
        val target = checkNotNull(Square.parse(targetText))
        val promotion = promotionText.firstOrNull()?.let { PieceType.fromLetter(it) }
        return position.legalMoves.filter { move ->
            move.to == target &&
                position.board[move.from]?.type == type &&
                (fileText.isEmpty() || move.from.file == fileText[0] - 'a') &&
                (rankText.isEmpty() || move.from.rank == rankText[0] - '1') &&
                move.promotion == promotion
        }
    }
}

/** Reads UCI move text (`e2e4`, `e7e8q`, castling as `e1g1`) and checks it against the legal moves. */
object Uci {
    /**
     * Throws [InvalidMoveNotationException] if the text is not UCI, and [IllegalMoveException] if
     * the move is not legal in [position].
     */
    fun parse(
        position: Position,
        text: String,
    ): Move {
        val move = Move.parseUci(text.trim()) ?: throw InvalidMoveNotationException("Malformed UCI move '$text'")
        if (!position.isLegal(move)) throw IllegalMoveException("Illegal move $move in ${position.toFen()}")
        return move
    }
}

fun Position.toSan(
    move: Move,
    letters: PieceLetters = PieceLetters.ENGLISH,
): String = San.write(this, move, letters)

fun Position.playSan(text: String): Position = play(San.parse(this, text))

fun Position.playUci(text: String): Position = play(Uci.parse(this, text))
