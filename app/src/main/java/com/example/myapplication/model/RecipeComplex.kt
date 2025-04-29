package com.example.myapplication.model

data class RecipeComplex(
    val id: String,
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val missedIngredientCount: Int,
    val nutrition: Nutrition?
)
