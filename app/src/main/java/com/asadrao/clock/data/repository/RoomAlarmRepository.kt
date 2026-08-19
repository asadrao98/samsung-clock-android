package com.asadrao.clock.data.repository

import com.asadrao.clock.data.db.dao.AlarmDao
import com.asadrao.clock.data.db.entity.toDomain
import com.asadrao.clock.data.db.entity.toEntity
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [AlarmRepository].
 *
 * [now] is injected rather than read from [System] so that timestamp behaviour is testable,
 * and so a single write cannot end up with `createdAt` and `updatedAt` a millisecond apart.
 */
class RoomAlarmRepository(
    private val dao: AlarmDao,
    private val now: () -> Long = System::currentTimeMillis,
) : AlarmRepository {

    override fun observeAlarms(): Flow<List<Alarm>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeAlarm(id: Long): Flow<Alarm?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getAlarm(id: Long): Alarm? = dao.getById(id)?.toDomain()

    override suspend fun getAllAlarms(): List<Alarm> = dao.getAll().map { it.toDomain() }

    override suspend fun getEnabledAlarms(): List<Alarm> =
        dao.getEnabled().map { it.toDomain() }

    override suspend fun addAlarm(alarm: Alarm): Long {
        val timestamp = now()
        // id is left at its default so Room assigns one; passing a caller-supplied id here
        // would silently overwrite an existing row.
        return dao.insert(
            alarm.copy(id = Alarm.NO_ID, createdAt = timestamp, updatedAt = timestamp).toEntity()
        )
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        dao.update(alarm.copy(updatedAt = now()).toEntity())
    }

    override suspend fun deleteAlarm(id: Long) = dao.deleteById(id)

    override suspend fun setEnabled(id: Long, enabled: Boolean) =
        dao.setEnabled(id, enabled, now())
}
