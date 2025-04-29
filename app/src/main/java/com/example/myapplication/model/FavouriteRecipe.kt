package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_recipes")
data class FavoriteRecipe(
    @PrimaryKey val id: String,
    val title: String,
    val image: String
)