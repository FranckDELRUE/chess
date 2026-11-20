package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.data.RepertoireStore
import com.chesscompanion.core.data.RepertoireSummary
import com.chesscompanion.core.data.StoredRepertoire
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** [RepertoireStore] in memory: repertoires in creation order. */
class FakeRepertoireStore : RepertoireStore {
    private data class Entry(
        val id: String,
        val name: String,
        val repertoire: Repertoire,
    )

    private val entries = MutableStateFlow<List<Entry>>(emptyList())
    private var counter = 0

    /** Number of calls to [save]. */
    var saves = 0
        private set

    fun names(): List<String> = entries.value.map { it.name }

    fun repertoireOf(id: String): Repertoire = entries.value.first { it.id == id }.repertoire

    override fun repertoires(): Flow<List<RepertoireSummary>> =
        entries.map { list ->
            list.map { RepertoireSummary(it.id, it.name, it.repertoire.side, it.repertoire.edgeCount, 0L) }
        }

    override suspend fun create(
        name: String,
        side: Side,
        start: Position,
    ): String {
        val id = "r${++counter}"
        entries.value = entries.value + Entry(id, name, Repertoire.empty(side, start))
        return id
    }

    override suspend fun load(id: String): StoredRepertoire? =
        entries.value.firstOrNull { it.id == id }?.let { StoredRepertoire(it.id, it.name, it.repertoire) }

    override suspend fun save(
        id: String,
        repertoire: Repertoire,
    ) {
        saves++
        entries.value = entries.value.map { if (it.id == id) it.copy(repertoire = repertoire) else it }
    }

    override suspend fun rename(
        id: String,
        name: String,
    ) {
        entries.value = entries.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun delete(id: String) {
        entries.value = entries.value.filterNot { it.id == id }
    }
}
