package com.chesscompanion.feature.repertoire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesscompanion.core.data.RepertoireStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The list of repertoires (S7-3): create, rename, delete. One entry point, [onAction]. */
@HiltViewModel
class RepertoireListViewModel
    @Inject
    constructor(
        private val store: RepertoireStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow(RepertoireListUiState())
        val state: StateFlow<RepertoireListUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                store.repertoires().collect { items -> _state.update { it.copy(items = items, loaded = true) } }
            }
        }

        fun onAction(action: RepertoireListAction) {
            when (action) {
                RepertoireListAction.StartCreate -> open(RepertoireDialog.Create())
                is RepertoireListAction.StartRename ->
                    item(action.id)?.let { open(RepertoireDialog.Rename(it.id, it.name)) }
                is RepertoireListAction.StartDelete ->
                    item(action.id)?.let { open(RepertoireDialog.Delete(it.id, it.name)) }
                is RepertoireListAction.EditName -> editName(action.name.take(MAX_NAME_LENGTH))
                is RepertoireListAction.ChooseSide -> chooseSide(action)
                RepertoireListAction.Confirm -> confirm()
                RepertoireListAction.Dismiss -> open(null)
            }
        }

        private fun item(id: String) = _state.value.items.firstOrNull { it.id == id }

        private fun open(dialog: RepertoireDialog?) = _state.update { it.copy(dialog = dialog) }

        private fun editName(name: String) =
            _state.update {
                it.copy(
                    dialog =
                        when (val dialog = it.dialog) {
                            is RepertoireDialog.Create -> dialog.copy(name = name)
                            is RepertoireDialog.Rename -> dialog.copy(name = name)
                            else -> dialog
                        },
                )
            }

        private fun chooseSide(action: RepertoireListAction.ChooseSide) =
            _state.update {
                val dialog = it.dialog
                it.copy(dialog = if (dialog is RepertoireDialog.Create) dialog.copy(side = action.side) else dialog)
            }

        private fun confirm() {
            val dialog = _state.value.dialog ?: return
            if (!dialog.canConfirm) return
            open(null)
            viewModelScope.launch {
                when (dialog) {
                    is RepertoireDialog.Create -> store.create(checkNotNull(cleanName(dialog.name)), dialog.side)
                    is RepertoireDialog.Rename -> store.rename(dialog.id, checkNotNull(cleanName(dialog.name)))
                    is RepertoireDialog.Delete -> store.delete(dialog.id)
                }
            }
        }
    }
