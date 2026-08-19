package com.asadrao.clock.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A city the user has added to their World clock.
 *
 * Only the zone id is authoritative; the names are cached for display so the list can be drawn
 * without rebuilding the whole catalogue, and are refreshed from the catalogue when the locale
 * changes.
 */
@Entity(tableName = "world_cities")
data class WorldCityEntity(
    @PrimaryKey val zoneId: String,
    val cityName: String,
    val countryName: String,
    val position: Int,
)
