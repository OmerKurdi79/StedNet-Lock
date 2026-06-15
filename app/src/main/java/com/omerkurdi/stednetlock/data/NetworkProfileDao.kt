package com.omerkurdi.stednetlock.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkProfileDao {
    @Query("SELECT * FROM network_profiles ORDER BY isDefaultAuto DESC, isSystemDefault DESC, name ASC")
    fun getAllProfilesFlow(): Flow<List<NetworkProfile>>

    @Query("SELECT * FROM network_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): NetworkProfile?

    @Query("SELECT * FROM network_profiles WHERE showOnWidget = 1 ORDER BY isDefaultAuto DESC, isSystemDefault DESC, name ASC")
    suspend fun getWidgetProfiles(): List<NetworkProfile>

    @Query("SELECT * FROM network_profiles ORDER BY isDefaultAuto DESC, isSystemDefault DESC, name ASC LIMIT 3")
    suspend fun getDefaultWidgetProfiles(): List<NetworkProfile>

    @Query("UPDATE network_profiles SET showOnWidget = :show WHERE id = :id")
    suspend fun updateShowOnWidget(id: Int, show: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: NetworkProfile): Long

    @Delete
    suspend fun deleteProfile(profile: NetworkProfile)

    @Query("DELETE FROM network_profiles WHERE id = :id AND isSystemDefault = 0")
    suspend fun deleteCustomProfileById(id: Int)
}
