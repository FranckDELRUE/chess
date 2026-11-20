package com.chesscompanion.core.engine.maia

import java.io.File

/** One row of `maia-reference.tsv`, produced by `tools/maia2onnx/maia2onnx.py` from lc0 itself. */
data class ReferenceRow(
    val net: String,
    val fen: String,
    val move: String,
    val index: Int,
    val prior: Float,
)

object MaiaReference {
    val rows: List<ReferenceRow> by lazy {
        resource("maia-reference.tsv")
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line ->
                val cells = line.split('\t')
                ReferenceRow(cells[0], cells[1], cells[2], cells[3].toInt(), cells[4].toFloat())
            }
    }

    fun resource(name: String): List<String> =
        requireNotNull(MaiaReference::class.java.classLoader!!.getResource(name)) { "Missing test resource $name" }
            .readText()
            .lines()

    /** ONNX model converted by the tool, absent on CI (weights are not versioned). */
    fun model(net: String): File = File("src/main/assets/maia-$net.onnx")
}
