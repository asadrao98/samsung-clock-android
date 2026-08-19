package com.asadrao.clock.domain.repository

import com.asadrao.clock.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

/**
 * Reading and writing alarms.
 *
 * Every write returns only once the change is durable, because each one is immediately
 * followed by a change to what the system has scheduled. Persisting after scheduling — or
 * not waiting at all — is how you end up with an alarm that rings but is not in the list, or
 * one in the list that never rings.
 */
interface AlarmRepository {

    fun observeAlarms(): Flow<List<Alarm>>

    fun observeAlarm(id: Long): Flow<Alarm?>

    suspend fun getAlarm(id: Long): Alarm?

    suspend fun getAllAlarms(): List<Alarm>

    suspend fun getEnabledAlarms(): List<Alarm>

    /** Returns the id assigned to the new alarm. */
    suspend fun addAlarm(alarm: Alarm): Long

    suspend fun updateAlarm(alarm: Alarm)

    suspend fun deleteAlarm(id: Long)

    suspend fun setEnabled(id: Long, enabled: Boolean)
}
