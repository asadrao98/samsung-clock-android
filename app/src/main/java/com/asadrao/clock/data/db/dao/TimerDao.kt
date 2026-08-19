package com.asadrao.clock.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.asadrao.clock.data.db.entity.TimerEntity
import com.asadrao.clock.data.db.entity.TimerPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Query("SELECT * FROM timers ORDER BY position ASC, id ASC")
    fun observeTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers ORDER BY position ASC, id ASC")
    suspend fun getTimers(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun getTimer(id: Long): TimerEntity?

    @Insert
    suspend fun insertTimer(timer: TimerEntity): Long

    @Update
    suspend fun updateTimer(timer: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteTimer(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM timers")
    suspend fun nextTimerPosition(): Int

    @Query("SELECT * FROM timer_presets ORDER BY position ASC, id ASC")
    fun observePresets(): Flow<List<TimerPresetEntity>>

    @Insert
    suspend fun insertPreset(preset: TimerPresetEntity): Long

    @Query("DELETE FROM timer_presets WHERE id = :id")
    suspend fun deletePreset(id: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM timer_presets")
    suspend fun nextPresetPosition(): Int
}
