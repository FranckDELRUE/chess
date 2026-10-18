package com.chesscompanion.home

import androidx.lifecycle.ViewModel
import com.chesscompanion.core.chess.FenParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val fen: String = FenParser.START_POSITION,
    val flipped: Boolean = false,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor() : ViewModel() {
        private val _state = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = _state.asStateFlow()

        fun flipBoard() = _state.update { it.copy(flipped = !it.flipped) }

        fun showFen(fen: String) = _state.update { it.copy(fen = fen) }
    }
