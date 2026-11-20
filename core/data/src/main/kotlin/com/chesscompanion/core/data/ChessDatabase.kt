package com.chesscompanion.core.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** One analysed position, keyed by its Zobrist key; [fen] holds the four identifying FEN fields. */
@Entity(tableName = "position_analysis")
data class PositionAnalysisEntity(
    @PrimaryKey val key: Long,
    val fen: String,
    val depth: Int,
    val scoreCp: Int?,
    val mateIn: Int?,
    val bestMove: String,
    /** UCI moves separated by spaces. */
    val pv: String,
    val analysedAt: Long,
)

@Entity(tableName = "analysis_job", indices = [Index("createdAt")])
data class AnalysisJobEntity(
    @PrimaryKey val id: String,
    val pgn: String,
    val state: String,
    val done: Int,
    val total: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface PositionAnalysisDao {
    @Query("SELECT * FROM position_analysis WHERE `key` = :key")
    suspend fun get(key: Long): PositionAnalysisEntity?

    @Upsert
    suspend fun upsert(entity: PositionAnalysisEntity)
}

@Dao
interface AnalysisJobDao {
    @Upsert
    suspend fun upsert(entity: AnalysisJobEntity)

    @Query("SELECT * FROM analysis_job WHERE id = :id")
    suspend fun get(id: String): AnalysisJobEntity?

    @Query("UPDATE analysis_job SET state = :state, done = :done, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(
        id: String,
        state: String,
        done: Int,
        updatedAt: Long,
    )

    @Query("UPDATE analysis_job SET state = :cancelled, updatedAt = :updatedAt WHERE state IN (:unfinished)")
    suspend fun cancelAll(
        cancelled: String,
        unfinished: List<String>,
        updatedAt: Long,
    )

    @Query("SELECT * FROM analysis_job ORDER BY createdAt DESC, rowid DESC LIMIT 1")
    fun latest(): Flow<AnalysisJobEntity?>
}

/**
 * The app's database: background analyses (S2-5, version 1) and repertoires (S7-2, version 2).
 * Schemas are exported to `core/data/schemas`; migrations are in [Migrations].
 */
@Database(
    entities =
        [
            PositionAnalysisEntity::class,
            AnalysisJobEntity::class,
            RepertoireEntity::class,
            RepertoireNodeEntity::class,
            RepertoireEdgeEntity::class,
        ],
    version = 2,
    exportSchema = true,
)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun positionAnalysisDao(): PositionAnalysisDao

    abstract fun analysisJobDao(): AnalysisJobDao

    abstract fun repertoireDao(): RepertoireDao

    abstract fun repertoireContentDao(): RepertoireContentDao

    companion object {
        const val NAME = "chess-companion.db"
    }
}
