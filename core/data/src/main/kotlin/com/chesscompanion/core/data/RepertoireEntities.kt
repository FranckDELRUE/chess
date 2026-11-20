package com.chesscompanion.core.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** One repertoire; [startFen] is the full FEN of its root position and [side] a `Side` name. */
@Entity(tableName = "repertoire")
data class RepertoireEntity(
    @PrimaryKey val id: String,
    val name: String,
    val side: String,
    val startFen: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * A position of a repertoire, identified by its Zobrist [key] and canonical [fen] (ADR 0012).
 * [positionFen] is the full FEN of the position reached the first time; [arrows] and [squares] are
 * written by [RepertoireCodec].
 */
@Entity(
    tableName = "repertoire_node",
    primaryKeys = ["repertoireId", "key", "fen"],
    foreignKeys = [
        ForeignKey(
            entity = RepertoireEntity::class,
            parentColumns = ["id"],
            childColumns = ["repertoireId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RepertoireNodeEntity(
    val repertoireId: String,
    val key: Long,
    val fen: String,
    val positionFen: String,
    val comment: String,
    val arrows: String,
    val squares: String,
    val sourceBook: String?,
    val sourceChapter: String?,
    val sourcePage: String?,
)

/** A move between two nodes of a repertoire; [ordinal] keeps the order in which moves were added. */
@Entity(
    tableName = "repertoire_edge",
    primaryKeys = ["repertoireId", "fromKey", "fromFen", "uci"],
    indices = [Index("repertoireId", "ordinal")],
    foreignKeys = [
        ForeignKey(
            entity = RepertoireEntity::class,
            parentColumns = ["id"],
            childColumns = ["repertoireId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class RepertoireEdgeEntity(
    val repertoireId: String,
    val fromKey: Long,
    val fromFen: String,
    val uci: String,
    val toKey: Long,
    val toFen: String,
    val san: String,
    val ordinal: Int,
)

/** Row of the repertoire list. */
data class RepertoireSummaryRow(
    val id: String,
    val name: String,
    val side: String,
    val moveCount: Int,
    val updatedAt: Long,
)

@Dao
interface RepertoireDao {
    @Query(
        "SELECT r.id, r.name, r.side, r.updatedAt, " +
            "(SELECT COUNT(*) FROM repertoire_edge e WHERE e.repertoireId = r.id) AS moveCount " +
            "FROM repertoire r ORDER BY r.createdAt, r.rowid",
    )
    fun summaries(): Flow<List<RepertoireSummaryRow>>

    @Insert
    suspend fun insert(entity: RepertoireEntity)

    @Query("SELECT * FROM repertoire WHERE id = :id")
    suspend fun get(id: String): RepertoireEntity?

    @Query("UPDATE repertoire SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(
        id: String,
        name: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM repertoire WHERE id = :id")
    suspend fun delete(id: String)
}

/** Content of the repertoires: positions and moves. */
@Dao
interface RepertoireContentDao {
    @Query("SELECT * FROM repertoire_node WHERE repertoireId = :id")
    suspend fun nodes(id: String): List<RepertoireNodeEntity>

    @Query("SELECT * FROM repertoire_edge WHERE repertoireId = :id ORDER BY ordinal")
    suspend fun edges(id: String): List<RepertoireEdgeEntity>

    @Insert
    suspend fun insertNodes(nodes: List<RepertoireNodeEntity>)

    @Insert
    suspend fun insertEdges(edges: List<RepertoireEdgeEntity>)

    @Query("DELETE FROM repertoire_node WHERE repertoireId = :id")
    suspend fun deleteNodes(id: String)

    @Query("DELETE FROM repertoire_edge WHERE repertoireId = :id")
    suspend fun deleteEdges(id: String)

    @Query("UPDATE repertoire SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(
        id: String,
        updatedAt: Long,
    )

    /** Replaces the whole content of a repertoire in one transaction. */
    @Transaction
    suspend fun replaceContent(
        id: String,
        nodes: List<RepertoireNodeEntity>,
        edges: List<RepertoireEdgeEntity>,
        updatedAt: Long,
    ) {
        deleteEdges(id)
        deleteNodes(id)
        insertNodes(nodes)
        insertEdges(edges)
        touch(id, updatedAt)
    }
}
