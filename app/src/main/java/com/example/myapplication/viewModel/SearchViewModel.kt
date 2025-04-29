package com.example.myapplication.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {
    var ingredientsList = mutableStateOf(listOf<String>())
        private set

    fun addIngredient(ingredient: String) {
        ingredientsList.value = ingredientsList.value + ingredient
    }

    fun removeIngredient(index: Int) {
        ingredientsList.value = ingredientsList.value.toMutableList().apply { removeAt(index) }
    }
}