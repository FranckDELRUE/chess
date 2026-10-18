package com.chesscompanion.core.engine.maia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PolicyDecoderTest {
    private val fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1"

    private fun logits(vararg values: Pair<String, Float>) =
        FloatArray(Lc0PolicyMap.SIZE) { 5f }.also { array ->
            // Illegal moves get a high logit on purpose: masking must ignore them.
            values.forEach { (move, v) -> array[Lc0PolicyMap.moves.indexOf(move)] = v }
        }

    @Test
    fun `softmax is taken over legal moves only`() {
        val priors =
            PolicyDecoder.decode(
                logits("e2e4" to 2f, "e2e3" to 1f, "e1d1" to 0f),
                fen,
                setOf("e2e4", "e2e3", "e1d1"),
            )
        assertEquals(listOf("e2e4", "e2e3", "e1d1"), priors.map { it.move })
        assertEquals(1f, priors.sumOf { it.prior.toDouble() }.toFloat(), 1e-6f)
        assertEquals(Math.E.toFloat(), priors[0].prior / priors[1].prior, 1e-4f)
    }

    @Test
    fun `no legal move gives no prior and no choice`() {
        assertEquals(emptyList<MovePrior>(), PolicyDecoder.decode(logits(), fen, emptySet()))
        assertNull(PolicyDecoder.choose(emptyList(), 0.0))
    }

    @Test
    fun `temperature 0 plays the most likely move, otherwise it samples`() {
        val priors = listOf(MovePrior("a", 0.7f), MovePrior("b", 0.2f), MovePrior("c", 0.1f))
        assertEquals("a", PolicyDecoder.choose(priors, 0.0))
        val random = Random(42)
        val counts = List(10_000) { PolicyDecoder.choose(priors, 1.0, random) }.groupingBy { it }.eachCount()
        assertEquals(0.7, counts.getValue("a") / 10_000.0, 0.02)
        assertEquals(0.1, counts.getValue("c") / 10_000.0, 0.02)
    }
}
