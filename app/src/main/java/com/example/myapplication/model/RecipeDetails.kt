package com.example.myapplication.model

import com.google.gson.annotations.SerializedName

data class RecipeDetails(
    val title: String,
    val image: String,
    val instructions: String,
    val readyInMinutes: Int,
    val missedIngredientCount: Int,
    val nutrition: Nutrition?,
    @SerializedName("extendedIngredients")
    val ingredients: List<Ingredient>
)

data class Ingredient(
    val name: String,
    val amount: Double,
    val unit: String
)