package com.example.myapplication.database

import com.example.myapplication.model.UserPreferences

class PreferencesRepository(private val dao: UserPreferencesDao) {
    suspend fun getPreferences(): UserPreferences? = dao.getPreferences()
    suspend fun savePreferences(entity: UserPreferences) = dao.savePreferences(entity)
}