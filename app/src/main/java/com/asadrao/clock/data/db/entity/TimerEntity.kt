package com.asadrao.clock.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.asadrao.clock.domain.timer.ClockTimer
import com.asadrao.clock.domain.timer.TimerPreset

/**
 * A timer, as stored.
 *
 * `endsAtRealtime` is a monotonic reading, which is only meaningful within one boot session — so
 * [bootMarker] is stored alongside it for the same reason the stopwatch stores one. A timer that
 * was running when the device restarted cannot be resumed honestly, and this is what lets that be
 * detected instead of showing a nonsensical countdown.
 */
@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalMillis: Long,
    /** -1 when paused; DataStore-style sentinel avoids a nullable column. */
    val endsAtRealtime: Long,
    val remainingWhenPausedMillis: Long,
    val label: String,
    val position: Int,
    val bootMarker: Long,
)

@Entity(tableName = "timer_presets")
data class TimerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val totalMillis: Long,
    val label: String,
    val position: Int,
)

fun TimerEntity.toDomain(): ClockTimer = ClockTimer(
    id = id,
    totalMillis = totalMillis,
    endsAtRealtime = endsAtRealtime.takeIf { it >= 0L },
    remainingWhenPausedMillis = remainingWhenPausedMillis,
    label = label,
    position = position,
)

fun ClockTimer.toEntity(bootMarker: Long): TimerEntity = TimerEntity(
    id = id,
    totalMillis = totalMillis,
    endsAtRealtime = endsAtRealtime ?: -1L,
    remainingWhenPausedMillis = remainingWhenPausedMillis,
    label = label,
    position = position,
    bootMarker = bootMarker,
)

fun TimerPresetEntity.toDomain(): TimerPreset = TimerPreset(
    id = id,
    totalMillis = totalMillis,
    label = label,
    position = position,
)

fun TimerPreset.toEntity(): TimerPresetEntity = TimerPresetEntity(
    id = id,
    totalMillis = totalMillis,
    label = label,
    position = position,
)
