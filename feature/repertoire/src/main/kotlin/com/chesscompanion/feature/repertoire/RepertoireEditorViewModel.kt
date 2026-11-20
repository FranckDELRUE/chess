package com.chesscompanion.feature.repertoire

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesscompanion.core.data.RepertoireStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Navigation argument: the id of the repertoire to edit. */
const val REPERTOIRE_ID_ARG = "id"

/**
 * The editor of one repertoire (S7-4). Moves played on the board are added to it, comments, marks
 * and source are written on the position shown. Changes are saved a moment after the last one, and
 * when the screen is left. One entry point, [onAction].
 */
@HiltViewModel
class RepertoireEditorViewModel
    @Inject
    constructor(
        savedState: SavedStateHandle,
        private val store: RepertoireStore,
    ) : ViewModel() {
        private val id: String = savedState.get<String>(REPERTOIRE_ID_ARG).orEmpty()
        private val _state = MutableStateFlow(RepertoireEditorUiState())
        val state: StateFlow<RepertoireEditorUiState> = _state.asStateFlow()

        private var model: EditorModel? = null
        private var name = ""
        private var dirty = false
        private var saveJob: Job? = null

        /** Delay between the last change and the save; tests set 0. */
        internal var saveDelayMs = SAVE_DELAY_MS

        init {
            viewModelScope.launch {
                val stored = store.load(id)
                if (stored == null) {
                    _state.value = RepertoireEditorUiState(status = EditorStatus.NOT_FOUND)
                } else {
                    name = stored.name
                    show(EditorModel.start(stored.repertoire))
                }
            }
        }

        fun onAction(action: EditorAction) {
            val current = model ?: return
            val next = current.reduce(action)
            show(next)
            if (next.repertoire !== current.repertoire) scheduleSave()
        }

        /** Saves now what has not been saved yet. */
        suspend fun flush() {
            saveJob?.cancel()
            write()
        }

        private suspend fun write() {
            val current = model
            if (dirty && current != null) {
                dirty = false
                store.save(id, current.repertoire)
            }
        }

        /** Saves what is pending without waiting (the app goes to the background). */
        fun saveNow() {
            viewModelScope.launch { flush() }
        }

        override fun onCleared() {
            // The scope of this ViewModel is cancelled: the last changes are written on their own scope.
            if (dirty) {
                val last = model?.repertoire
                if (last != null) CoroutineScope(Dispatchers.IO).launch { store.save(id, last) }
            }
        }

        private fun show(next: EditorModel) {
            model = next
            _state.value = next.toUiState(name)
        }

        private fun scheduleSave() {
            dirty = true
            saveJob?.cancel()
            saveJob =
                viewModelScope.launch {
                    delay(saveDelayMs)
                    write()
                }
        }

        private companion object {
            const val SAVE_DELAY_MS = 600L
        }
    }
