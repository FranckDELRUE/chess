package com.chesscompanion.core.engine.maia

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

/** A policy network: 112 x 8 x 8 input planes in, 1858 policy logits out. */
interface MaiaNetwork : AutoCloseable {
    fun policy(planes: FloatArray): FloatArray

    override fun close() = Unit
}

/**
 * Maia network exported by `lc0 leela2onnx`, run with ONNX Runtime (Android or desktop JVM).
 * [threads] sets intra-op threads; [xnnpack] enables the XNNPACK execution provider (Android).
 */
class OnnxMaiaNetwork(
    model: ByteArray,
    threads: Int = 1,
    xnnpack: Boolean = false,
) : MaiaNetwork {
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession =
        OrtSession.SessionOptions().use { options ->
            options.setIntraOpNumThreads(threads)
            if (xnnpack) options.addXnnpack(mapOf("intra_op_num_threads" to threads.toString()))
            env.createSession(model, options)
        }

    override fun policy(planes: FloatArray): FloatArray {
        require(planes.size == Lc0Encoder.PLANES * Lc0Encoder.SQUARES) { "Bad input size ${planes.size}" }
        OnnxTensor.createTensor(env, FloatBuffer.wrap(planes), INPUT_SHAPE).use { input ->
            session.run(mapOf(INPUT to input), setOf(POLICY)).use { result ->
                @Suppress("UNCHECKED_CAST")
                return (result[0].value as Array<FloatArray>)[0]
            }
        }
    }

    override fun close() = session.close()

    companion object {
        const val INPUT = "/input/planes"
        const val POLICY = "/output/policy"
        private val INPUT_SHAPE = longArrayOf(1, Lc0Encoder.PLANES.toLong(), 8, 8)
    }
}
