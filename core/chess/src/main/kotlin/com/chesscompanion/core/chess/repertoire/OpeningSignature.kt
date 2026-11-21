package com.chesscompanion.core.chess.repertoire

/** A move carries the opening when it leads to at least this share of the positions below its siblings (S8b-5). */
private const val DOMINANT_PERCENT = 75

/**
 * The starting moves that make an opening what it is (S8b-5), for instance 1. e4 e5 2. Nf3 Nc6 3. d4 for the Scotch.
 * From the root, the repertoire is followed while one move carries the opening: the only move of a position, or the
 * move that leads to at least [DOMINANT_PERCENT] % of the positions below the moves of that position (a stray line,
 * such as 1...c5 in a Scotch repertoire, does not stop the walk). It stops at the first real fork, here after 3. d4.
 * A reply the opponent could play before that leads to another opening, so it is not a gap of this repertoire.
 *
 * @property edges the moves of the signature, from the root.
 */
class OpeningSignature internal constructor(
    val edges: List<RepertoireEdge>,
    /** The positions reached from the end of the signature (it included); null when the signature is empty. */
    private val inside: Set<PositionId>?,
) {
    val isEmpty: Boolean get() = edges.isEmpty()

    /** True if [id] belongs to the opening: it comes after the signature. Everything does when there is none. */
    fun covers(id: PositionId): Boolean = inside?.contains(id) ?: true
}

/** The signature of this repertoire: empty when its first position already forks (or has no move). */
fun Repertoire.signature(): OpeningSignature {
    val edges = mutableListOf<RepertoireEdge>()
    val seen = mutableSetOf(root)
    // A position seen before would loop for ever (a repetition in the graph): `seen` stops the walk there.
    var next = carryingMove(root)
    while (next != null && seen.add(next.to)) {
        edges += next
        next = carryingMove(next.to)
    }
    return OpeningSignature(edges, edges.lastOrNull()?.let { reachableFrom(it.to) })
}

/** The move of [id] that carries the opening, null when the position has none or forks without a clear main move. */
private fun Repertoire.carryingMove(id: PositionId): RepertoireEdge? {
    val moves = movesFrom(id)
    if (moves.size <= 1) return moves.singleOrNull()
    val sizes = moves.map { reachableFrom(it.to).size }
    val best = sizes.indices.maxBy { sizes[it] }
    return moves[best].takeIf { sizes[best] * PERCENT >= sizes.sum() * DOMINANT_PERCENT }
}

private const val PERCENT = 100

/** [from] and every position that follows it in the repertoire. */
private fun Repertoire.reachableFrom(from: PositionId): Set<PositionId> {
    val seen = mutableSetOf(from)
    val pending = ArrayDeque(listOf(from))
    while (pending.isNotEmpty()) {
        movesFrom(pending.removeFirst()).forEach { if (seen.add(it.to)) pending.add(it.to) }
    }
    return seen
}
