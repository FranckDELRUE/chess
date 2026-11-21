package com.chesscompanion.core.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S7-2: the migration from version 1 (analyses) to version 2 (repertoires) keeps the analyses. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            ChessDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun version1DataSurvivesAndTheRepertoireTablesExist() {
        // The Robolectric driver wants the full path of the file, not just its name.
        val db1 = ApplicationProvider.getApplicationContext<Context>().getDatabasePath(DB).path
        helper.createDatabase(db1, 1).apply {
            execSQL(
                "INSERT INTO position_analysis (`key`, fen, depth, scoreCp, mateIn, bestMove, pv, analysedAt) " +
                    "VALUES (7, 'startpos', 18, 30, NULL, 'e2e4', 'e2e4 e7e5', 99)",
            )
            execSQL(
                "INSERT INTO analysis_job (id, pgn, state, done, total, createdAt, updatedAt) " +
                    "VALUES ('job', '1. e4 *', 'DONE', 2, 2, 1, 2)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(db1, 2, true, Migrations.V1_TO_V2)

        db.query("SELECT depth, bestMove FROM position_analysis WHERE `key` = 7").use {
            it.moveToFirst()
            assertEquals(18, it.getInt(0))
            assertEquals("e2e4", it.getString(1))
        }
        db.query("SELECT state FROM analysis_job WHERE id = 'job'").use {
            it.moveToFirst()
            assertEquals("DONE", it.getString(0))
        }
        listOf("repertoire", "repertoire_node", "repertoire_edge").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
        }
    }

    @Test
    fun version2DataSurvivesAndTheExplorerCacheExists() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("migration-test-3").path
        helper.createDatabase(path, 2).apply {
            execSQL(
                "INSERT INTO repertoire (id, name, side, startFen, createdAt, updatedAt) " +
                    "VALUES ('r', 'Scotch', 'WHITE', 'startpos', 1, 2)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(path, 3, true, Migrations.V2_TO_V3)

        db.query("SELECT name FROM repertoire WHERE id = 'r'").use {
            it.moveToFirst()
            assertEquals("Scotch", it.getString(0))
        }
        db.query("SELECT COUNT(*) FROM explorer_position").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun version3DataSurvivesAndTheReviewCardsExist() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("migration-test-4").path
        helper.createDatabase(path, 3).apply {
            execSQL(
                "INSERT INTO repertoire (id, name, side, startFen, createdAt, updatedAt) " +
                    "VALUES ('r', 'Scotch', 'WHITE', 'startpos', 1, 2)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(path, 4, true, Migrations.V3_TO_V4)

        db.query("SELECT name FROM repertoire WHERE id = 'r'").use {
            it.moveToFirst()
            assertEquals("Scotch", it.getString(0))
        }
        db.execSQL(
            "INSERT INTO review_card (repertoireId, `key`, fen, reps, lapses) VALUES ('r', 1, 'f', 0, 0)",
        )
        db.query("SELECT COUNT(*) FROM review_card").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun version4NotesSurviveAndGetEmptyIdeaPlanAndTrap() {
        val path = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("migration-test-5").path
        helper.createDatabase(path, 4).apply {
            execSQL(
                "INSERT INTO repertoire (id, name, side, startFen, createdAt, updatedAt) " +
                    "VALUES ('r', 'Scotch', 'WHITE', 'startpos', 1, 2)",
            )
            execSQL(
                "INSERT INTO repertoire_node (repertoireId, `key`, fen, positionFen, comment, arrows, squares) " +
                    "VALUES ('r', 1, 'f', 'p', 'Mon commentaire', '', '')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(path, 5, true, Migrations.V4_TO_V5)

        db.query("SELECT comment, idea, plan, trap FROM repertoire_node WHERE repertoireId = 'r'").use {
            it.moveToFirst()
            assertEquals("Mon commentaire", it.getString(0))
            assertEquals("", it.getString(1))
            assertEquals("", it.getString(2))
            assertEquals("", it.getString(3))
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
