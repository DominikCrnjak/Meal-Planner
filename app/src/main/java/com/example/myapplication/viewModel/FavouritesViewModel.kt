package com.example.myapplication.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.database.RecipeRepository
import com.example.myapplication.model.FavoriteRecipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FavoritesViewModel(private val repository: RecipeRepository) : ViewModel() {

    val favorites: Flow<List<FavoriteRecipe>> = repository.favorites

    fun addToFavorites(recipe: FavoriteRecipe) {
        viewModelScope.launch {
            repository.addToFavorites(recipe)
        }
    }

    fun removeFromFavorites(recipeId: String) {
        viewModelScope.launch {
            repository.removeFromFavorites(recipeId)
        }
    }

    suspend fun isFavorite(recipeId: String): Boolean {
        return repository.isFavorite(recipeId)
    }
}