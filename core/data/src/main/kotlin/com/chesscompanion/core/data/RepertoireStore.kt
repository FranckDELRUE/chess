package com.chesscompanion.core.data

import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.Repertoire
import kotlinx.coroutines.flow.Flow

/** A repertoire in the list: no content, only what the list shows. */
data class RepertoireSummary(
    val id: String,
    val name: String,
    val side: Side,
    val moveCount: Int,
    val updatedAt: Long,
)

/** A repertoire read back from the store. */
data class StoredRepertoire(
    val id: String,
    val name: String,
    val repertoire: Repertoire,
)

/** Stored repertoires (S7-2); Room in the app, a fake in tests. */
interface RepertoireStore {
    /** The repertoires, oldest first, as they change. */
    fun repertoires(): Flow<List<RepertoireSummary>>

    /** Creates an empty repertoire and returns its id. */
    suspend fun create(
        name: String,
        side: Side,
        start: Position = Position.START,
    ): String

    /** The repertoire with its content, or null if [id] is unknown. */
    suspend fun load(id: String): StoredRepertoire?

    /**
     * Replaces the content of [id] with [repertoire], which must start from the same position.
     * Throws [IllegalArgumentException] if [id] is unknown or the start differs.
     */
    suspend fun save(
        id: String,
        repertoire: Repertoire,
    )

    suspend fun rename(
        id: String,
        name: String,
    )

    /** Deletes the repertoire and everything in it. */
    suspend fun delete(id: String)
}
