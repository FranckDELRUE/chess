package com.chesscompanion.core.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migrations of [ChessDatabase]; each one keeps the data of the previous version. */
object Migrations {
    /** S7-2: adds the repertoire tables; the analysis tables are untouched. */
    val V1_TO_V2: Migration =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2_SQL.forEach(db::execSQL)
            }
        }

    /** S7-7: adds the explorer cache; analyses and repertoires are untouched. */
    val V2_TO_V3: Migration =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `explorer_position` (`fen` TEXT NOT NULL, `level` TEXT NOT NULL, " +
                        "`white` INTEGER NOT NULL, `draws` INTEGER NOT NULL, `black` INTEGER NOT NULL, " +
                        "`moves` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`fen`, `level`))",
                )
            }
        }

    // Copied from core/data/schemas/.../2.json (createSql of the three new entities).
    private val MIGRATION_1_2_SQL: List<String> =
        listOf(
            "CREATE TABLE IF NOT EXISTS `repertoire` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`side` TEXT NOT NULL, `startFen` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `repertoire_node` (`repertoireId` TEXT NOT NULL, `key` INTEGER NOT NULL, " +
                "`fen` TEXT NOT NULL, `positionFen` TEXT NOT NULL, `comment` TEXT NOT NULL, " +
                "`arrows` TEXT NOT NULL, `squares` TEXT NOT NULL, `sourceBook` TEXT, `sourceChapter` TEXT, " +
                "`sourcePage` TEXT, PRIMARY KEY(`repertoireId`, `key`, `fen`), " +
                "FOREIGN KEY(`repertoireId`) REFERENCES `repertoire`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `repertoire_edge` (`repertoireId` TEXT NOT NULL, `fromKey` INTEGER NOT NULL, " +
                "`fromFen` TEXT NOT NULL, `uci` TEXT NOT NULL, `toKey` INTEGER NOT NULL, `toFen` TEXT NOT NULL, " +
                "`san` TEXT NOT NULL, `ordinal` INTEGER NOT NULL, " +
                "PRIMARY KEY(`repertoireId`, `fromKey`, `fromFen`, `uci`), " +
                "FOREIGN KEY(`repertoireId`) REFERENCES `repertoire`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE INDEX IF NOT EXISTS `index_repertoire_edge_repertoireId_ordinal` " +
                "ON `repertoire_edge` (`repertoireId`, `ordinal`)",
        )
}
