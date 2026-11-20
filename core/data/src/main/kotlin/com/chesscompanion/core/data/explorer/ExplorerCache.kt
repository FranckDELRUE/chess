package com.chesscompanion.core.data.explorer

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/** Statistics of one position for one level (S7-7); [fen] is the canonical four-field FEN. */
@Entity(tableName = "explorer_position", primaryKeys = ["fen", "level"])
data class ExplorerPositionEntity(
    val fen: String,
    val level: String,
    val white: Long,
    val draws: Long,
    val black: Long,
    /** One move per line: `uci`, `white`, `draws`, `black`, `averageRating` (empty if unknown), tab-separated. */
    val moves: String,
    val fetchedAt: Long,
)

@Dao
interface ExplorerDao {
    @Query("SELECT * FROM explorer_position WHERE fen = :fen AND level = :level")
    suspend fun get(
        fen: String,
        level: String,
    ): ExplorerPositionEntity?

    @Upsert
    suspend fun upsert(entity: ExplorerPositionEntity)
}

internal fun ExplorerStats.toEntity(
    fen: String,
    level: ExplorerLevel,
    now: Long,
) = ExplorerPositionEntity(
    fen = fen,
    level = level.name,
    white = white,
    draws = draws,
    black = black,
    moves =
        moves.joinToString("\n") {
            listOf(it.uci, it.white, it.draws, it.black, it.averageRating ?: "").joinToString("\t")
        },
    fetchedAt = now,
)

internal fun ExplorerPositionEntity.toStats(): ExplorerStats =
    ExplorerStats(
        white = white,
        draws = draws,
        black = black,
        moves =
            moves.lines().filter { it.isNotBlank() }.map { line ->
                val part = line.split('\t')
                ExplorerMove(
                    part[UCI],
                    part[WHITE].toLong(),
                    part[DRAWS].toLong(),
                    part[BLACK].toLong(),
                    part[RATING].toIntOrNull(),
                )
            },
    )

// Columns of a line of `ExplorerPositionEntity.moves`.
private const val UCI = 0
private const val WHITE = 1
private const val DRAWS = 2
private const val BLACK = 3
private const val RATING = 4
