package com.asadrao.clock.domain.repository

import com.asadrao.clock.domain.timer.ClockTimer
import com.asadrao.clock.domain.timer.TimerPreset
import kotlinx.coroutines.flow.Flow

/** Reading and writing timers and their saved presets. */
interface TimerRepository {
    fun observeTimers(): Flow<List<ClockTimer>>
    suspend fun getTimers(): List<ClockTimer>
    suspend fun getTimer(id: Long): ClockTimer?
    suspend fun addTimer(totalMillis: Long, label: String): Long
    suspend fun updateTimer(timer: ClockTimer)
    suspend fun deleteTimer(id: Long)

    fun observePresets(): Flow<List<TimerPreset>>
    suspend fun addPreset(totalMillis: Long, label: String): Long
    suspend fun deletePreset(id: Long)
}
