package com.example.myapplication.database

import com.example.myapplication.model.FavoriteRecipe
import kotlinx.coroutines.flow.Flow

class RecipeRepository(private val recipeDao: RecipeDao) {

    val favorites: Flow<List<FavoriteRecipe>> = recipeDao.getFavorites()

    suspend fun addToFavorites(recipe: FavoriteRecipe) {
        recipeDao.addToFavorites(recipe)
    }

    suspend fun removeFromFavorites(recipeId: String) {
        recipeDao.removeFromFavorites(recipeId)
    }

    suspend fun isFavorite(recipeId: String): Boolean {
        return recipeDao.isFavorite(recipeId) != null
    }
}