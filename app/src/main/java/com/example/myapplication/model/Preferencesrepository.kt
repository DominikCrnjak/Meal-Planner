package com.example.myapplication.model

class PreferencesRepository(private val dao: UserPreferencesDao) {
    suspend fun getPreferences(): UserPreferences? = dao.getPreferences()
    suspend fun savePreferences(entity: UserPreferences) = dao.savePreferences(entity)
}