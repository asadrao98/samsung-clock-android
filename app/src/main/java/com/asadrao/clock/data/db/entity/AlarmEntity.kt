package com.asadrao.clock.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.asadrao.clock.domain.model.Alarm
import com.asadrao.clock.domain.model.RepeatDays

/**
 * Storage shape of an alarm. Kept separate from the domain [Alarm] so a schema change never
 * ripples straight into the UI, and so the domain model can keep types Room has no opinion
 * about (the [RepeatDays] value class).
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    /** [RepeatDays] flattened to its seven-bit integer. */
    val repeatDayBits: Int,
    val label: String,
    val soundUri: String?,
    val vibrationEnabled: Boolean,
    val snoozeEnabled: Boolean,
    val snoozeDurationMinutes: Int,
    val snoozeRepeatLimit: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    enabled = enabled,
    repeatDays = RepeatDays(repeatDayBits),
    label = label,
    soundUri = soundUri,
    vibrationEnabled = vibrationEnabled,
    snoozeEnabled = snoozeEnabled,
    snoozeDurationMinutes = snoozeDurationMinutes,
    snoozeRepeatLimit = snoozeRepeatLimit,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    enabled = enabled,
    repeatDayBits = repeatDays.bits,
    label = label,
    soundUri = soundUri,
    vibrationEnabled = vibrationEnabled,
    snoozeEnabled = snoozeEnabled,
    snoozeDurationMinutes = snoozeDurationMinutes,
    snoozeRepeatLimit = snoozeRepeatLimit,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
