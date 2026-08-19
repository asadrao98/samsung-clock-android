package com.asadrao.clock.domain.repository

import com.asadrao.clock.domain.worldclock.City
import kotlinx.coroutines.flow.Flow

/** The cities on the user's World clock, in their chosen order. */
interface WorldClockRepository {
    fun observeCities(): Flow<List<City>>
    suspend fun addCity(city: City)
    suspend fun removeCity(zoneId: String)
    /** Persists a reorder. [zoneIdsInOrder] is the complete list, top to bottom. */
    suspend fun reorder(zoneIdsInOrder: List<String>)
}
