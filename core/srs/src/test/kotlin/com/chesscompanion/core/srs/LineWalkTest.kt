package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class LineWalkTest {
    private val repertoire = repertoireOf(Side.WHITE, "1. d4 d5 2. c4 e6 *", "1. d4 Nf6 2. c4 g6 *")

    private fun edgesFrom(walk: LineWalk): List<RepertoireEdge> = walk.moves

    @Test
    fun theLineStartsAtTheRootWithThePlayerToMove() {
        val walk = LineWalk(repertoire, emptyList())
        assertEquals(repertoire.root, walk.position)
        assertTrue(walk.isPlayerTurn)
        assertFalse(walk.isOver)
        assertTrue(walk.played.isEmpty())
    }

    @Test
    fun theOpponentFollowsThePlanWhileThePlayerDoes() {
        val plan =
            ReviewQueueBuilder
                .build(repertoire, emptyMap(), java.time.Instant.EPOCH, java.time.ZoneOffset.UTC)
                .items
                .first { it.path.size == 2 }
                .path
        repeat(10) { seed ->
            val walk = LineWalk(repertoire, plan, Random(seed))
            walk.advance(plan[0])
            assertEquals(plan[1], walk.opponentReply())
        }
    }

    @Test
    fun onceThePlayerLeavesThePlanTheOpponentIsDrawnAmongTheRepertoireReplies() {
        val plan =
            ReviewQueueBuilder
                .build(repertoire, emptyMap(), java.time.Instant.EPOCH, java.time.ZoneOffset.UTC)
                .items
                .first { it.path.size == 2 }
                .path
        val walk = LineWalk(repertoire, plan, Random(1))
        walk.advance(plan[0])
        walk.advance(plan[1])
        walk.advance(walk.preferred(edgesFrom(walk))!!)
        val reply = walk.opponentReply()
        assertTrue(reply in walk.moves)
    }

    @Test
    fun aLeafEndsTheLine() {
        val walk = LineWalk(repertoire, emptyList())
        while (!walk.isOver) {
            walk.advance(
                checkNotNull(if (walk.isPlayerTurn) walk.preferred(walk.moves) else walk.opponentReply()),
            )
        }
        assertTrue(walk.isOver)
        assertNull(walk.opponentReply())
        assertEquals(4, walk.played.size)
    }
}
