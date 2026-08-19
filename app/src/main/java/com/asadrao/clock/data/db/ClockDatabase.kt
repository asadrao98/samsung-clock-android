package com.asadrao.clock.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.asadrao.clock.data.db.dao.AlarmDao
import com.asadrao.clock.data.db.dao.TimerDao
import com.asadrao.clock.data.db.dao.WorldCityDao
import com.asadrao.clock.data.db.entity.AlarmEntity
import com.asadrao.clock.data.db.entity.TimerEntity
import com.asadrao.clock.data.db.entity.TimerPresetEntity
import com.asadrao.clock.data.db.entity.WorldCityEntity

@Database(
    entities = [
        AlarmEntity::class,
        TimerEntity::class,
        TimerPresetEntity::class,
        WorldCityEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class ClockDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun timerDao(): TimerDao
    abstract fun worldCityDao(): WorldCityDao

    companion object {
        private const val NAME = "clock.db"

        /**
         * Adds the timer tables.
         *
         * Written out by hand rather than relying on a destructive fallback: someone's alarms live
         * in this database, and dropping them to add a timer table would be indefensible.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `timers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `totalMillis` INTEGER NOT NULL,
                        `endsAtRealtime` INTEGER NOT NULL,
                        `remainingWhenPausedMillis` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        `bootMarker` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `timer_presets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `totalMillis` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `position` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds the World clock's saved-city table. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `world_cities` (
                        `zoneId` TEXT PRIMARY KEY NOT NULL,
                        `cityName` TEXT NOT NULL,
                        `countryName` TEXT NOT NULL,
                        `position` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun build(context: Context): ClockDatabase =
            Room.databaseBuilder(context, ClockDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                // No fallbackToDestructiveMigration, on purpose: an alarm someone relies on must
                // never be dropped by a schema bump.
                .build()
    }
}
