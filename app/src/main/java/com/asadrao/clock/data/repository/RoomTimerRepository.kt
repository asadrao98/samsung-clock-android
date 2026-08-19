package com.asadrao.clock.data.repository

import android.os.SystemClock
import com.asadrao.clock.data.db.dao.TimerDao
import com.asadrao.clock.data.db.entity.TimerEntity
import com.asadrao.clock.data.db.entity.TimerPresetEntity
import com.asadrao.clock.data.db.entity.toDomain
import com.asadrao.clock.data.db.entity.toEntity
import com.asadrao.clock.domain.repository.TimerRepository
import com.asadrao.clock.domain.stopwatch.StopwatchState
import com.asadrao.clock.domain.timer.ClockTimer
import com.asadrao.clock.domain.timer.TimerPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.abs

/**
 * Room-backed timers.
 *
 * Reads reconcile each stored timer against the current boot session. A timer whose monotonic
 * deadline was recorded before a restart cannot be resumed honestly — the reading means nothing
 * now — so it comes back paused at the time it had left, rather than showing a countdown computed
 * from an incomparable clock.
 */
class RoomTimerRepository(
    private val dao: TimerDao,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis,
) : TimerRepository {

    private fun bootMarker(): Long = wallClock() - elapsedRealtime()

    private fun TimerEntity.reconciled(): ClockTimer {
        val timer = toDomain()
        if (!timer.isRunning) return timer
        val rebooted = abs(bootMarker() - bootMarker) > StopwatchState.BOOT_TOLERANCE
        return if (rebooted) {
            timer.copy(
                endsAtRealtime = null,
                remainingWhenPausedMillis = timer.remainingWhenPausedMillis,
            )
        } else {
            timer
        }
    }

    override fun observeTimers(): Flow<List<ClockTimer>> =
        dao.observeTimers().map { list -> list.map { it.reconciled() } }

    override suspend fun getTimers(): List<ClockTimer> =
        dao.getTimers().map { it.reconciled() }

    override suspend fun getTimer(id: Long): ClockTimer? = dao.getTimer(id)?.reconciled()

    override suspend fun addTimer(totalMillis: Long, label: String): Long {
        val timer = ClockTimer(
            id = 0L,
            totalMillis = totalMillis,
            remainingWhenPausedMillis = totalMillis,
            label = label,
            position = dao.nextTimerPosition(),
        )
        return dao.insertTimer(timer.toEntity(bootMarker()).copy(id = 0L))
    }

    override suspend fun updateTimer(timer: ClockTimer) {
        dao.updateTimer(timer.toEntity(bootMarker()))
    }

    override suspend fun deleteTimer(id: Long) = dao.deleteTimer(id)

    override fun observePresets(): Flow<List<TimerPreset>> =
        dao.observePresets().map { list -> list.map { it.toDomain() } }

    override suspend fun addPreset(totalMillis: Long, label: String): Long =
        dao.insertPreset(
            TimerPresetEntity(
                totalMillis = totalMillis,
                label = label,
                position = dao.nextPresetPosition(),
            )
        )

    override suspend fun deletePreset(id: Long) = dao.deletePreset(id)
}
