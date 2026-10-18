package com.chesscompanion.spike.llm

/**
 * Checks a teacher sentence against the fact it rewords (rule 3: the AI comments, the engine computes).
 * Counts as invented: a square the fact does not contain, a move other than the fact's best move
 * (English or French piece letters), a piece the fact does not name (the king is implied by a mate).
 * Heuristic: it reads words, not meaning ("ton cavalier est défendu" passes for a hanging knight).
 */
object FactFidelity {
    data class Report(
        val inventedSquares: Set<String>,
        val inventedMoves: Set<String>,
        val inventedPieces: Set<String>,
        val mentionsBest: Boolean,
        val namesPiece: Boolean,
        val sentences: Int,
    ) {
        val faithful: Boolean get() = inventedSquares.isEmpty() && inventedMoves.isEmpty() && inventedPieces.isEmpty()
        val invented: Set<String> get() = inventedSquares + inventedMoves + inventedPieces
    }

    /** A standalone square; one glued to a piece letter is part of a move. */
    private val square = Regex("(?<![a-zA-Z0-9])[a-h][1-8](?![0-9])")
    private val anySquare = Regex("[a-h][1-8]")
    private val move = Regex("(?<![A-Za-z0-9])[KQRBNDTFC]x?[a-h][1-8][+#]?")
    private val piece = Regex(""""piece"\s*:\s*"(\w+)"""")
    private val best = Regex(""""best"\s*:\s*"([^"]+)"""")
    private val englishPiece = Regex("\\b(king|queen|rook|bishop|knight|pawn)\\b")

    /** French piece names; "tour" after a possessive is a turn ("à ton tour"), not a rook. */
    private val frenchPieceWord =
        Regex(
            "(?<![\\p{L}])(roi|dame|(?<!(?:ton|mon|son|chaque|premier) )tour|fou|cavalier|pion)s?(?![\\p{L}])",
            RegexOption.IGNORE_CASE,
        )
    private val sentenceEnd = Regex("[.!?…]+(?=\\s|$)")

    private val frenchLetter = mapOf('K' to 'R', 'Q' to 'D', 'R' to 'T', 'B' to 'F', 'N' to 'C')
    private val pieceOfLetter = mapOf('K' to "roi", 'Q' to "dame", 'R' to "tour", 'B' to "fou", 'N' to "cavalier")
    private val frenchPiece =
        mapOf(
            "king" to "roi",
            "queen" to "dame",
            "rook" to "tour",
            "bishop" to "fou",
            "knight" to "cavalier",
            "pawn" to "pion",
        )

    fun check(
        fact: String,
        sentence: String,
    ): Report {
        // In the fact, squares inside moves count too ("best":"Nd2" allows "en d2").
        val allowedSquares = anySquare.findAll(fact).map { it.value }.toSet()
        val bestMove = best.find(fact)?.groupValues?.get(1)
        val allowedMoves = bestMove?.let { setOf(stripSuffix(it), stripSuffix(toFrench(it))) }.orEmpty()
        // Pieces named in the fact, the piece of the best move (Nxd4: a knight), the king of a mate.
        val allowedPieces =
            englishPiece.findAll(fact).mapNotNull { frenchPiece[it.value] }.toSet() +
                listOfNotNull(bestMove?.firstOrNull()?.let { pieceOfLetter[it] }) +
                if ("mate" in fact) setOf("roi") else emptySet()
        val squares = square.findAll(sentence).map { it.value }.toSet()
        val moves = move.findAll(sentence).map { stripSuffix(it.value) }.toSet()
        val pieces = frenchPieceWord.findAll(sentence).map { it.groupValues[1].lowercase() }.toSet()
        val pieceName =
            piece
                .find(fact)
                ?.groupValues
                ?.get(1)
                ?.let { frenchPiece[it] }
        return Report(
            inventedSquares = squares - allowedSquares,
            inventedMoves = moves - allowedMoves,
            inventedPieces = pieces - allowedPieces,
            mentionsBest = allowedMoves.any { it in moves },
            namesPiece = pieceName != null && pieceName in pieces,
            sentences = countSentences(sentence),
        )
    }

    /** SAN with English piece letters to French (Nd2 → Cd2). */
    fun toFrench(san: String): String =
        san.firstOrNull()?.let { first -> frenchLetter[first]?.let { it + san.drop(1) } } ?: san

    fun countSentences(text: String): Int = text.split(sentenceEnd).count { it.isNotBlank() }

    private fun stripSuffix(san: String) = san.trimEnd('+', '#')
}
