package com.example.myapplication.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.Recipe
import com.example.myapplication.model.RecipeComplex
import com.example.myapplication.network.RetrofitInstance
import com.example.myapplication.network.RetrofitInstance.api
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    var ingredientsList = mutableStateOf(listOf<String>())
        private set

    fun addIngredient(ingredient: String) {
        ingredientsList.value = ingredientsList.value + ingredient
    }

    fun removeIngredient(index: Int) {
        ingredientsList.value = ingredientsList.value.toMutableList().apply { removeAt(index) }
    }

    private var currentOffset = 0
    private val pageSize = 5

    var recipes = mutableStateOf<List<RecipeComplex>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    fun resetSearch() {
        currentOffset = 0
        recipes.value = emptyList()
    }

    fun fetchRecipes(
        ingredients: List<String>,
        maxReadyTime: Int,
        diet: String?,
        intolerances: String?,
        cuisine: String?,
        sort: String,
        sortDirection: String
    ) {
        viewModelScope.launch {
            val response = api.searchRecipesComplex(
                ingredients = ingredients.joinToString(","),
                maxReadyTime = maxReadyTime,
                diet = diet,
                intolerances = intolerances,
                cuisine = cuisine,
                sort = sort,
                sortDirection = sortDirection
            )
            recipes.value = response.results
        }
    }

    fun loadMoreRecipes(
        maxReadyTime: Int? = null,
        diet: String? = null,
        intolerances: String? = null,
        cuisine: String? = null,
        sort: String? = null,
        sortDirection: String? = null
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = api.searchRecipesComplex(
                    ingredients = ingredientsList.value.joinToString(","),
                    maxReadyTime = maxReadyTime,
                    number = pageSize,
                    offset = currentOffset,
                    diet = diet,
                    intolerances = intolerances,
                    cuisine = cuisine,
                    sort = sort,
                    sortDirection = sortDirection
                )
                recipes.value = recipes.value + response.results
                currentOffset += pageSize
            } catch (e: Exception) {

            }
            isLoading.value = false
        }
    }
}