package com.parrotworks.redreamer.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the one thing that must never break: upgrading the app keeps every dream.
 *
 * Schema validation alone isn't enough — a migration can satisfy Room and still lose rows — so
 * these tests write real data at the old version and assert it's still readable afterwards.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2_keepsExistingDreamsAndTags() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO dreams
                    (id, title, content, notes, dreamDate, createdAt, updatedAt, deletedAt,
                     isLucid, lucidity, clarity, isNightmare, isRecurring, moods)
                VALUES
                    (1, 'Flying', 'Above the sea', 'Felt free', '2026-07-20', 100, 100, NULL,
                     1, 7, 8, 0, 0, 'JOYFUL'),
                    (2, 'Binned one', 'Should not be indexed', '', '2026-07-19', 90, 90, 95,
                     0, NULL, 5, 0, 0, '')
                """.trimIndent(),
            )
            execSQL("INSERT INTO tags (id, name, color) VALUES (1, 'flying', NULL)")
            execSQL("INSERT INTO dream_tag_cross_ref (dreamId, tagId) VALUES (1, 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT id, title FROM dreams ORDER BY id").use { cursor ->
            assertEquals("both dreams must survive the upgrade", 2, cursor.count)
            cursor.moveToFirst()
            assertEquals("Flying", cursor.getString(1))
        }

        db.query("SELECT tagId FROM dream_tag_cross_ref WHERE dreamId = 1").use { cursor ->
            assertEquals("tag links must survive", 1, cursor.count)
        }
    }

    @Test
    fun migrate1To2_backfillsSearchIndexForLiveDreamsOnly() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO dreams
                    (id, title, content, notes, dreamDate, createdAt, updatedAt, deletedAt,
                     isLucid, lucidity, clarity, isNightmare, isRecurring, moods)
                VALUES
                    (1, 'Flying', 'Above the sea', '', '2026-07-20', 100, 100, NULL,
                     0, NULL, 5, 0, 0, ''),
                    (2, 'Binned', 'Deleted already', '', '2026-07-19', 90, 90, 95,
                     0, NULL, 5, 0, 0, '')
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Pre-existing dreams should be searchable immediately, not only after being edited.
        db.query("SELECT rowid FROM dream_fts WHERE dream_fts MATCH 'sea'").use { cursor ->
            assertTrue("existing dreams must be searchable after upgrade", cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
        }

        db.query("SELECT COUNT(*) FROM dream_fts").use { cursor ->
            cursor.moveToFirst()
            assertEquals("binned dreams must stay out of the index", 1, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
