package com.asadrao.clock.data.repository

import com.asadrao.clock.data.db.dao.WorldCityDao
import com.asadrao.clock.data.db.entity.WorldCityEntity
import com.asadrao.clock.domain.repository.WorldClockRepository
import com.asadrao.clock.domain.worldclock.City
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomWorldClockRepository(private val dao: WorldCityDao) : WorldClockRepository {

    override fun observeCities(): Flow<List<City>> =
        dao.observeCities().map { list ->
            list.map { entity ->
                City(
                    zoneId = entity.zoneId,
                    cityName = entity.cityName,
                    countryName = entity.countryName,
                    region = entity.zoneId.substringBefore('/'),
                )
            }
        }

    override suspend fun addCity(city: City) {
        dao.insert(
            WorldCityEntity(
                zoneId = city.zoneId,
                cityName = city.cityName,
                countryName = city.countryName,
                position = dao.nextPosition(),
            )
        )
    }

    override suspend fun removeCity(zoneId: String) = dao.delete(zoneId)

    override suspend fun reorder(zoneIdsInOrder: List<String>) {
        // Rewrites every row's position from the given order, rather than trying to patch
        // individual indices — far easier to reason about, and a reorder is rare enough that the
        // cost does not matter.
        val existing = dao.getCities().associateBy { it.zoneId }
        val renumbered = zoneIdsInOrder.mapIndexedNotNull { index, zoneId ->
            existing[zoneId]?.copy(position = index)
        }
        dao.updateAll(renumbered)
    }
}
