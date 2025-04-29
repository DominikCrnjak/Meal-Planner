package com.example.myapplication.model

data class ComplexSearchResponse(
    val results: List<RecipeComplex>,
    val offset: Int,
    val number: Int,
    val totalResults: Int
)
