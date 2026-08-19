package com.asadrao.clock.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.asadrao.clock.data.db.entity.WorldCityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldCityDao {

    @Query("SELECT * FROM world_cities ORDER BY position ASC")
    fun observeCities(): Flow<List<WorldCityEntity>>

    @Query("SELECT * FROM world_cities ORDER BY position ASC")
    suspend fun getCities(): List<WorldCityEntity>

    /** Replaces on conflict, so adding a city already in the list is a harmless no-op. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(city: WorldCityEntity)

    @Update
    suspend fun updateAll(cities: List<WorldCityEntity>)

    @Query("DELETE FROM world_cities WHERE zoneId = :zoneId")
    suspend fun delete(zoneId: String)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM world_cities")
    suspend fun nextPosition(): Int
}
