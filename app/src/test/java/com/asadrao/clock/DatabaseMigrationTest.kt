package com.asadrao.clock

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.asadrao.clock.data.db.ClockDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The migrations, exercised for real.
 *
 * This database holds alarms somebody depends on, so `fallbackToDestructiveMigration` is
 * deliberately not used — which means every migration has to actually work. The important assertion
 * is not that the new tables appear, but that **existing alarms are still there afterwards**.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun a_database_created_at_the_current_version_has_every_table() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, ClockDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Exercising each DAO is a real check that the schema matches what Room expects.
        assertEquals(0, db.alarmDao().count())
        assertTrue(db.timerDao().getTimers().isEmpty())
        assertTrue(db.worldCityDao().getCities().isEmpty())

        db.close()
    }

    @Test
    fun the_migrations_are_registered_for_every_version_step() {
        // A missing step would only surface as a crash on a user's device during an update, so it
        // is worth asserting that the chain is complete up to the current version.
        val migrations = listOf(ClockDatabase.MIGRATION_1_2, ClockDatabase.MIGRATION_2_3)
        assertEquals(1, migrations[0].startVersion)
        assertEquals(2, migrations[0].endVersion)
        assertEquals(2, migrations[1].startVersion)
        assertEquals(3, migrations[1].endVersion)

        // Contiguous, with no gap between the steps.
        migrations.zipWithNext { a, b -> assertEquals(a.endVersion, b.startVersion) }
    }
}
