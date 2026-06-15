package com.omerkurdi.stednetlock.data

import kotlinx.coroutines.flow.Flow

class NetworkProfileRepository(private val dao: NetworkProfileDao) {
    val allProfilesFlow: Flow<List<NetworkProfile>> = dao.getAllProfilesFlow()

    suspend fun getProfileById(id: Int): NetworkProfile? = dao.getProfileById(id)

    suspend fun insert(profile: NetworkProfile): Long = dao.insertProfile(profile)

    suspend fun delete(profile: NetworkProfile) = dao.deleteProfile(profile)

    suspend fun deleteCustomProfileById(id: Int) = dao.deleteCustomProfileById(id)

    suspend fun updateShowOnWidget(id: Int, show: Boolean) = dao.updateShowOnWidget(id, show)
}
