package com.chesscompanion.llmdebug

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesscompanion.llmdebug.LlmBenchmark.Companion.summary
import com.chesscompanion.spike.llm.FactFidelity
import com.chesscompanion.spike.llm.GenerationParams
import com.chesscompanion.spike.llm.LlamaTextGenerator
import com.chesscompanion.spike.llm.LlmModel
import com.chesscompanion.spike.llm.PromptVariant
import com.chesscompanion.spike.llm.TeacherPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LlmStatus { IDLE, LOADING, GENERATING, BATCH, THERMAL, ERROR }

data class LlmDebugState(
    val model: LlmModel = LlmModel.QWEN3_1_7B,
    val threads: Int = LlamaTextGenerator.DEFAULT_THREADS,
    val variant: PromptVariant = PromptVariant.FRENCH,
    val status: LlmStatus = LlmStatus.IDLE,
    val output: String = "",
    val log: List<String> = emptyList(),
    val error: String? = null,
) {
    val busy: Boolean get() = status != LlmStatus.IDLE && status != LlmStatus.ERROR
}

/** S0-6 debug screen: one streamed answer, the benchmark batch and the 5-minute heat run. */
@HiltViewModel
class LlmDebugViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LlmDebugState())
        val state: StateFlow<LlmDebugState> = _state.asStateFlow()
        private var job: Job? = null

        fun setModel(model: LlmModel) = _state.update { it.copy(model = model) }

        fun setThreads(threads: Int) = _state.update { it.copy(threads = threads) }

        fun setVariant(variant: PromptVariant) = _state.update { it.copy(variant = variant) }

        fun stop() {
            job?.cancel()
        }

        /** Streams one sampled answer to the main fact with the selected model (loaded for this run only). */
        fun generate() =
            launchJob(LlmStatus.GENERATING) {
                val s = _state.value
                _state.update { it.copy(status = LlmStatus.LOADING) }
                val generator = LlamaTextGenerator.load(LlmBenchmark.modelDir(context), s.model, s.threads)
                try {
                    _state.update { it.copy(status = LlmStatus.GENERATING) }
                    val answer =
                        LlmBenchmark.answer(
                            generator,
                            TeacherPrompt.MAIN_FACT,
                            s.variant,
                            GenerationParams(),
                        ) { token ->
                            _state.update { it.copy(output = it.output + token) }
                        }
                    val report = FactFidelity.check(TeacherPrompt.MAIN_FACT.json, answer.text)
                    _state.update { it.copy(output = answer.text) }
                    log("${s.model.label} prompt=${s.variant} ${answer.stats.summary()} faithful=${report.faithful}")
                } finally {
                    generator.close()
                }
            }

        fun runBatch() = launchJob(LlmStatus.BATCH) { benchmark().run(LlmModel.entries) }

        fun runThermal() =
            launchJob(LlmStatus.THERMAL) { benchmark().thermal(_state.value.model, _state.value.threads) }

        private fun benchmark() = LlmBenchmark(context, ::log)

        private fun launchJob(
            status: LlmStatus,
            block: suspend () -> Unit,
        ) {
            if (_state.value.busy) return
            _state.update { it.copy(status = status, error = null, output = "", log = emptyList()) }
            job =
                viewModelScope.launch {
                    try {
                        block()
                        _state.update { it.copy(status = LlmStatus.IDLE) }
                    } catch (e: CancellationException) {
                        _state.update { it.copy(status = LlmStatus.IDLE) }
                        throw e
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        _state.update { it.copy(status = LlmStatus.ERROR, error = e.message ?: e.toString()) }
                    }
                }
        }

        private fun log(line: String) {
            Log.i(LlmBenchmark.TAG, line)
            _state.update { it.copy(log = it.log + line) }
        }
    }
