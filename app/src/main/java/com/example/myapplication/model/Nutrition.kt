package com.example.myapplication.model

data class Nutrition(
    val nutrients: List<Nutrient>
)

data class Nutrient(
    val name: String,
    val amount: Float,
    val unit: String
)