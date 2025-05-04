package com.example.myapplication.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.database.PreferencesRepository
import com.example.myapplication.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PreferencesViewModel(private val repository: PreferencesRepository) : ViewModel() {

    private val _preferences = MutableStateFlow<UserPreferences?>(null)
    val preferences: StateFlow<UserPreferences?> = _preferences

    init {
        viewModelScope.launch {
            _preferences.value = repository.getPreferences()
        }
    }

    fun save(diet: String, intolerances: String, cuisine: String) {
        viewModelScope.launch {
            val entity = UserPreferences(
                diet = diet,
                intolerances = intolerances,
                cuisine = cuisine
            )
            repository.savePreferences(entity)
            _preferences.value = entity
        }
    }
}
