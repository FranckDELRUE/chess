package com.chesscompanion.core.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Query
import androidx.room.Upsert
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.srs.FsrsCard
import java.time.Instant

/**
 * Review state of one repertoire position (S8-2). Keyed by (repertoire, Zobrist [key], canonical [fen]),
 * the same identity as a repertoire node (ADR 0012), but with no foreign key: `RepertoireStore.save`
 * replaces the whole content of a repertoire and must not erase what was learned. Instants are epoch millis.
 */
@Entity(
    tableName = "review_card",
    primaryKeys = ["repertoireId", "key", "fen"],
    indices = [Index("repertoireId", "due")],
)
data class ReviewCardEntity(
    val repertoireId: String,
    val key: Long,
    val fen: String,
    val stability: Double?,
    val difficulty: Double?,
    val due: Long?,
    val lastReview: Long?,
    val reps: Int,
    val lapses: Int,
)

@Dao
interface ReviewCardDao {
    @Query("SELECT * FROM review_card WHERE repertoireId = :repertoireId AND `key` = :key AND fen = :fen")
    suspend fun get(
        repertoireId: String,
        key: Long,
        fen: String,
    ): ReviewCardEntity?

    @Query("SELECT * FROM review_card WHERE repertoireId = :repertoireId")
    suspend fun all(repertoireId: String): List<ReviewCardEntity>

    @Upsert
    suspend fun upsert(entity: ReviewCardEntity)

    @Query("SELECT COUNT(*) FROM review_card WHERE repertoireId = :repertoireId AND due <= :now")
    suspend fun dueCount(
        repertoireId: String,
        now: Long,
    ): Int

    @Query("DELETE FROM review_card WHERE repertoireId = :repertoireId AND `key` = :key AND fen = :fen")
    suspend fun delete(
        repertoireId: String,
        key: Long,
        fen: String,
    )

    @Query("DELETE FROM review_card WHERE repertoireId = :repertoireId")
    suspend fun deleteAll(repertoireId: String)
}

/** Review cards of the repertoires; a position with no card has never been reviewed. */
interface ReviewCardStore {
    /** The card of [position], or null if it was never reviewed. */
    suspend fun get(
        repertoireId: String,
        position: PositionId,
    ): FsrsCard?

    /** Every reviewed position of the repertoire. */
    suspend fun cards(repertoireId: String): Map<PositionId, FsrsCard>

    /** Creates or replaces the card of [position]. */
    suspend fun put(
        repertoireId: String,
        position: PositionId,
        card: FsrsCard,
    )

    /** Number of reviewed positions due at [now] (new positions are not counted). */
    suspend fun dueCount(
        repertoireId: String,
        now: Instant,
    ): Int

    /** Deletes the cards of positions no longer in [keep] (after moves were removed); returns how many. */
    suspend fun retainOnly(
        repertoireId: String,
        keep: Set<PositionId>,
    ): Int

    suspend fun deleteAll(repertoireId: String)
}

/** [ReviewCardStore] on Room. */
class RoomReviewCardStore(
    database: ChessDatabase,
) : ReviewCardStore {
    private val dao = database.reviewCardDao()

    override suspend fun get(
        repertoireId: String,
        position: PositionId,
    ): FsrsCard? = dao.get(repertoireId, position.key, position.fen)?.toCard()

    override suspend fun cards(repertoireId: String): Map<PositionId, FsrsCard> =
        dao.all(repertoireId).associate { PositionId(it.key, it.fen) to it.toCard() }

    override suspend fun put(
        repertoireId: String,
        position: PositionId,
        card: FsrsCard,
    ) = dao.upsert(
        ReviewCardEntity(
            repertoireId = repertoireId,
            key = position.key,
            fen = position.fen,
            stability = card.stability,
            difficulty = card.difficulty,
            due = card.due?.toEpochMilli(),
            lastReview = card.lastReview?.toEpochMilli(),
            reps = card.reps,
            lapses = card.lapses,
        ),
    )

    override suspend fun dueCount(
        repertoireId: String,
        now: Instant,
    ): Int = dao.dueCount(repertoireId, now.toEpochMilli())

    override suspend fun retainOnly(
        repertoireId: String,
        keep: Set<PositionId>,
    ): Int {
        val orphans = dao.all(repertoireId).filter { PositionId(it.key, it.fen) !in keep }
        orphans.forEach { dao.delete(repertoireId, it.key, it.fen) }
        return orphans.size
    }

    override suspend fun deleteAll(repertoireId: String) = dao.deleteAll(repertoireId)

    private fun ReviewCardEntity.toCard() =
        FsrsCard(
            stability = stability,
            difficulty = difficulty,
            due = due?.let(Instant::ofEpochMilli),
            lastReview = lastReview?.let(Instant::ofEpochMilli),
            reps = reps,
            lapses = lapses,
        )
}
