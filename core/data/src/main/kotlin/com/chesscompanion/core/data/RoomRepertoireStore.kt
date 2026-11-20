package com.chesscompanion.core.data

import androidx.room.withTransaction
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.NodeNotes
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import com.chesscompanion.core.chess.repertoire.RepertoireNode
import com.chesscompanion.core.chess.repertoire.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** [RepertoireStore] on Room. [clock] gives the time of each write (tests pass a fixed one). */
class RoomRepertoireStore(
    private val database: ChessDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : RepertoireStore {
    private val dao = database.repertoireDao()
    private val cards = database.reviewCardDao()
    private val content = database.repertoireContentDao()

    override fun repertoires(): Flow<List<RepertoireSummary>> =
        dao.summaries().map { rows ->
            rows.map { RepertoireSummary(it.id, it.name, sideOf(it.side), it.moveCount, it.updatedAt) }
        }

    override suspend fun create(
        name: String,
        side: Side,
        start: Position,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = clock()
        val repertoire = Repertoire.empty(side, start)
        dao.insert(RepertoireEntity(id, name, side.name, start.toFen(), now, now))
        content.replaceContent(id, nodeRows(id, repertoire), emptyList(), now)
        return id
    }

    override suspend fun load(id: String): StoredRepertoire? {
        val entity = dao.get(id) ?: return null
        val rows = content.nodes(id)
        val nodes = rows.map { RepertoireNode(PositionId(it.key, it.fen), Fen.parse(it.positionFen)) }
        val notes = rows.associate { PositionId(it.key, it.fen) to it.toNotes() }
        val moves =
            content.edges(id).map {
                RepertoireEdge(
                    from = PositionId(it.fromKey, it.fromFen),
                    to = PositionId(it.toKey, it.toFen),
                    move = checkNotNull(Move.parseUci(it.uci)) { "Stored move ${it.uci} is malformed" },
                    san = it.san,
                )
            }
        val root = PositionId.of(Fen.parse(entity.startFen))
        return StoredRepertoire(id, entity.name, Repertoire.restore(sideOf(entity.side), root, nodes, moves, notes))
    }

    override suspend fun save(
        id: String,
        repertoire: Repertoire,
    ) {
        val entity = requireNotNull(dao.get(id)) { "Unknown repertoire $id" }
        require(repertoire.root == PositionId.of(Fen.parse(entity.startFen))) {
            "The repertoire does not start from the stored position"
        }
        require(repertoire.side.name == entity.side) { "The repertoire is not on the stored side" }
        val edges =
            repertoire.allMoves.mapIndexed { index, edge ->
                RepertoireEdgeEntity(
                    repertoireId = id,
                    fromKey = edge.from.key,
                    fromFen = edge.from.fen,
                    uci = edge.move.uci,
                    toKey = edge.to.key,
                    toFen = edge.to.fen,
                    san = edge.san,
                    ordinal = index,
                )
            }
        content.replaceContent(id, nodeRows(id, repertoire), edges, clock())
    }

    override suspend fun rename(
        id: String,
        name: String,
    ) = dao.rename(id, name, clock())

    // Cards have no foreign key (ADR 0012), so deleting a repertoire removes them explicitly.
    override suspend fun delete(id: String) =
        database.withTransaction {
            cards.deleteAll(id)
            dao.delete(id)
        }

    private fun nodeRows(
        id: String,
        repertoire: Repertoire,
    ): List<RepertoireNodeEntity> =
        repertoire.allNodes.map { node ->
            val notes = repertoire.notes(node.id)
            RepertoireNodeEntity(
                repertoireId = id,
                key = node.id.key,
                fen = node.id.fen,
                positionFen = node.position.toFen(),
                comment = notes.comment,
                arrows = RepertoireCodec.writeArrows(notes.arrows),
                squares = RepertoireCodec.writeSquares(notes.squares),
                sourceBook = notes.source?.book,
                sourceChapter = notes.source?.chapter,
                sourcePage = notes.source?.page,
            )
        }

    private fun RepertoireNodeEntity.toNotes() =
        NodeNotes(
            comment = comment,
            arrows = RepertoireCodec.readArrows(arrows),
            squares = RepertoireCodec.readSquares(squares),
            source = sourceBook?.let { Source(it, sourceChapter, sourcePage) },
        )

    private fun sideOf(name: String): Side = Side.entries.first { it.name == name }
}
