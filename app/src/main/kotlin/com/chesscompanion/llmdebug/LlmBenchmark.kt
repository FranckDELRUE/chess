package com.chesscompanion.llmdebug

import android.content.Context
import com.chesscompanion.enginedebug.DeviceProbe
import com.chesscompanion.spike.llm.FactFidelity
import com.chesscompanion.spike.llm.GenerationEvent
import com.chesscompanion.spike.llm.GenerationParams
import com.chesscompanion.spike.llm.GenerationStats
import com.chesscompanion.spike.llm.LlamaTextGenerator
import com.chesscompanion.spike.llm.LlmModel
import com.chesscompanion.spike.llm.PromptVariant
import com.chesscompanion.spike.llm.ReasoningFilter
import com.chesscompanion.spike.llm.TeacherFact
import com.chesscompanion.spike.llm.TeacherPrompt
import com.chesscompanion.spike.llm.TextGenerator
import java.io.File

/**
 * S0-6 measurements, one log line per result (Logcat tag LLM_BENCH):
 * - per model: load time and memory, a cold greedy answer, a thread sweep on the main fact;
 * - per model, prompt variant and fact: [RUNS] sampled answers with timings, the text and its fidelity;
 * - [thermal]: back-to-back answers for 5 minutes with one model.
 */
class LlmBenchmark(
    private val context: Context,
    private val log: (String) -> Unit,
) {
    class Answer(
        val text: String,
        val stats: GenerationStats,
    )

    suspend fun run(models: List<LlmModel>) {
        log("llm start temp=${DeviceProbe.batteryTempC(context)}C")
        log("system ${LlamaTextGenerator.systemInfo()}")
        models.forEach { measureModel(it) }
        log("llm done temp=${DeviceProbe.batteryTempC(context)}C")
    }

    suspend fun thermal(
        model: LlmModel,
        threads: Int,
    ) {
        val generator = LlamaTextGenerator.load(modelDir(context), model, threads)
        try {
            val start = System.nanoTime()
            log("thermal start model=${model.label} threads=$threads temp=${DeviceProbe.batteryTempC(context)}C")
            var round = 0
            while (System.nanoTime() - start < THERMAL_DURATION_NS) {
                val fact = TeacherPrompt.FACTS[round % TeacherPrompt.FACTS.size]
                val answer = answer(generator, fact, PromptVariant.FRENCH, GenerationParams(seed = round))
                round++
                val seconds = (System.nanoTime() - start) / NS_PER_S
                log("round=$round t=${seconds}s ${answer.stats.summary()} temp=${DeviceProbe.batteryTempC(context)}C")
            }
            log("thermal done rounds=$round temp=${DeviceProbe.batteryTempC(context)}C pss=${DeviceProbe.pssMb()}MB")
        } finally {
            generator.close()
        }
    }

    private suspend fun measureModel(model: LlmModel) {
        val file = File(modelDir(context), model.fileName)
        val pss0 = DeviceProbe.pssMb()
        val t0 = System.nanoTime()
        val generator = LlamaTextGenerator.load(modelDir(context), model)
        val loadMs = (System.nanoTime() - t0) / NS_PER_MS
        log(
            "model=${model.label} file=${file.length() / BYTES_PER_MB}MB load=${loadMs}ms " +
                "pss $pss0 -> ${DeviceProbe.pssMb()}MB",
        )
        try {
            val cold = answer(generator, TeacherPrompt.MAIN_FACT, PromptVariant.JSON, GREEDY)
            log("model=${model.label} cold ${cold.stats.summary()} pss=${DeviceProbe.pssMb()}MB")
            log("model=${model.label} cold text=${cold.text.replace('\n', ' ')}")
            THREAD_SWEEP.forEach { threads ->
                generator.setThreads(threads)
                val a = answer(generator, TeacherPrompt.MAIN_FACT, PromptVariant.JSON, GREEDY)
                log("model=${model.label} threads=$threads ${a.stats.summary()}")
            }
            generator.setThreads(LlamaTextGenerator.DEFAULT_THREADS)
            PromptVariant.entries.forEach { measureFacts(model, it, generator) }
            log(
                "model=${model.label} after answers pss=${DeviceProbe.pssMb()}MB temp=${DeviceProbe.batteryTempC(
                    context,
                )}C",
            )
        } finally {
            generator.close()
        }
        log("model=${model.label} closed pss=${DeviceProbe.pssMb()}MB")
    }

    private suspend fun measureFacts(
        model: LlmModel,
        variant: PromptVariant,
        generator: TextGenerator,
    ) {
        val tag = "model=${model.label} prompt=$variant"
        val answers = mutableListOf<Answer>()
        var faithful = 0
        var best = 0
        var oneSentence = 0
        TeacherPrompt.FACTS.forEachIndexed { i, fact ->
            repeat(RUNS) { run ->
                val a = answer(generator, fact, variant, GenerationParams(seed = run + 1))
                val report = FactFidelity.check(fact.json, a.text)
                answers += a
                if (report.faithful) faithful++
                if (report.mentionsBest) best++
                if (report.sentences == 1) oneSentence++
                log(
                    "$tag fact=${i + 1} run=${run + 1} ${a.stats.summary()} " +
                        "faithful=${report.faithful} " +
                        "best=${report.mentionsBest} piece=${report.namesPiece} sentences=${report.sentences} " +
                        "invented=${report.invented}",
                )
                log("$tag fact=${i + 1} run=${run + 1} text=${a.text.replace('\n', ' ')}")
            }
        }
        val stats = answers.map { it.stats }
        log(
            "$tag summary n=${answers.size} faithful=$faithful best=$best one_sentence=$oneSentence " +
                "ttft_median=%.0fms prefill_median=%.1ftok/s decode_median=%.1ftok/s tokens_median=%d".format(
                    stats.map { it.firstTokenMs }.median(),
                    stats.map { it.prefillTokensPerSecond }.median(),
                    stats.map { it.decodeTokensPerSecond }.median(),
                    stats.map { it.generatedTokens }.sorted()[stats.size / 2],
                ),
        )
    }

    companion object {
        const val TAG = "LLM_BENCH"
        private val GREEDY = GenerationParams(temperature = 0f)
        private val THREAD_SWEEP = listOf(2, 4, 6, 8)
        private const val RUNS = 3
        private const val NS_PER_MS = 1_000_000L
        private const val NS_PER_S = 1_000_000_000L
        private const val THERMAL_DURATION_NS = 5 * 60 * NS_PER_S
        private const val BYTES_PER_MB = 1024 * 1024

        fun modelDir(context: Context): File = File(context.getExternalFilesDir(null), "models")

        /** Collects one answer to [fact], reasoning blocks removed. */
        suspend fun answer(
            generator: TextGenerator,
            fact: TeacherFact,
            variant: PromptVariant,
            params: GenerationParams,
            onToken: (String) -> Unit = {},
        ): Answer {
            val text = StringBuilder()
            var stats: GenerationStats? = null
            generator.generate(TeacherPrompt.messages(fact, variant), params).collect { event ->
                when (event) {
                    is GenerationEvent.Token -> {
                        text.append(event.text)
                        onToken(event.text)
                    }
                    is GenerationEvent.Done -> stats = event.stats
                }
            }
            return Answer(ReasoningFilter.strip(text.toString()), checkNotNull(stats))
        }

        fun GenerationStats.summary(): String =
            "prompt=$promptTokens ttft=%.0fms prefill=%.1ftok/s gen=$generatedTokens decode=%.1ftok/s".format(
                firstTokenMs,
                prefillTokensPerSecond,
                decodeTokensPerSecond,
            )

        private fun List<Double>.median(): Double = sorted()[size / 2]
    }
}
