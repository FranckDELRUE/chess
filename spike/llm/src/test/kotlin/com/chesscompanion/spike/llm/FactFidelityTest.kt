package com.chesscompanion.spike.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FactFidelityTest {
    private val fact = TeacherPrompt.MAIN_FACT.json

    @Test
    fun `faithful sentence with the French move`() {
        val r = FactFidelity.check(fact, "Ton cavalier en f3 n'est pas défendu : joue Cd2 pour le mettre à l'abri !")
        assertTrue(r.faithful)
        assertTrue(r.mentionsBest)
        assertTrue(r.namesPiece)
    }

    @Test
    fun `English move and a square taken from the move are accepted`() {
        val r = FactFidelity.check(fact, "Le cavalier f3 est en prise, Nd2 le ramène en d2.")
        assertTrue(r.faithful)
        assertTrue(r.mentionsBest)
    }

    @Test
    fun `invented square and move are reported`() {
        val r = FactFidelity.check(fact, "Ton fou en c4 est en prise, joue Fe2.")
        assertEquals(setOf("c4"), r.inventedSquares)
        assertEquals(setOf("Fe2"), r.inventedMoves)
        assertFalse(r.faithful)
        assertFalse(r.mentionsBest)
        assertFalse(r.namesPiece)
    }

    @Test
    fun `check and mate suffixes are ignored`() {
        val fork = TeacherPrompt.FACTS[1].json
        assertTrue(FactFidelity.check(fork, "Cc7 fourchette le roi en e8 et la tour en a8.").mentionsBest)
        val mate = TeacherPrompt.FACTS[3].json
        assertTrue(FactFidelity.check(mate, "Te8# : mat du couloir !").faithful)
    }

    @Test
    fun `ordinary French words are not squares`() {
        val r = FactFidelity.check(fact, "Attention, ta pièce est seule : protège-la vite.")
        assertTrue(r.inventedSquares.isEmpty())
        assertTrue(r.inventedMoves.isEmpty())
    }

    @Test
    fun `wrong piece is reported, as in a real Qwen3 answer`() {
        val r = FactFidelity.check(fact, "Tu as bien vu, tu as capturé la dame en f3, et c'est une pièce en prise !")
        assertEquals(setOf("dame"), r.inventedPieces)
        assertFalse(r.faithful)
        assertFalse(r.namesPiece)
    }

    @Test
    fun `turn is not a rook and a mate implies the king`() {
        assertTrue(FactFidelity.check(fact, "À ton tour : ton cavalier f3 est seul, joue Cd2.").faithful)
        assertTrue(FactFidelity.check(TeacherPrompt.FACTS[3].json, "Ta tour en e8 fait mat au roi : Te8# !").faithful)
        assertEquals(setOf("tour"), FactFidelity.check(fact, "La tour est en prise.").inventedPieces)
    }

    @Test
    fun `pieces named anywhere in the fact are allowed`() {
        val pin = TeacherPrompt.FACTS[2].json
        val r = FactFidelity.check(pin, "Ton fou en g5 cloue le cavalier f6 devant la dame d8 : prends avec Fxf6.")
        assertTrue(r.faithful, r.toString())
        assertTrue(r.mentionsBest)
    }

    @Test
    fun `the piece of the best move is allowed`() {
        val free = TeacherPrompt.FACTS[4].json
        assertTrue(FactFidelity.check(free, "Le fou en d4 est gratuit : prends-le avec ton cavalier, Cxd4 !").faithful)
    }

    @Test
    fun `sentences are counted`() {
        assertEquals(1, FactFidelity.countSentences("Ton cavalier en f3 est en prise, joue Cd2 !"))
        assertEquals(3, FactFidelity.countSentences("Bravo. Joue Cd2 ! C'est solide…"))
        assertEquals(0, FactFidelity.countSentences("  "))
    }

    @Test
    fun `toFrench translates the piece letter only`() {
        assertEquals("Cd2", FactFidelity.toFrench("Nd2"))
        assertEquals("Fxf6", FactFidelity.toFrench("Bxf6"))
        assertEquals("Te8#", FactFidelity.toFrench("Re8#"))
        assertEquals("e4", FactFidelity.toFrench("e4"))
    }
}
