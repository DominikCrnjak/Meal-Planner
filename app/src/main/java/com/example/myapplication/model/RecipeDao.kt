package com.example.myapplication.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Insert
    suspend fun addToFavorites(recipe: FavoriteRecipe)

    @Query("DELETE FROM favorite_recipes WHERE id = :recipeId")
    suspend fun removeFromFavorites(recipeId: String)

    @Query("SELECT * FROM favorite_recipes")
    fun getFavorites(): Flow<List<FavoriteRecipe>>

    @Query("SELECT * FROM favorite_recipes WHERE id = :recipeId")
    suspend fun isFavorite(recipeId: String): FavoriteRecipe?
}