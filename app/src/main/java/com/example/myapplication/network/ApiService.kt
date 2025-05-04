package com.example.myapplication.network

import com.example.myapplication.BuildConfig
import com.example.myapplication.model.ComplexSearchResponse
import com.example.myapplication.model.Recipe
import com.example.myapplication.model.RecipeDetails
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface ApiService {

    //Pozivi prema API endpointovima
    @GET("recipes/findByIngredients")
    suspend fun getRecipes(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 5,
        @Query("apiKey") apiKey: String = BuildConfig.API_KEY
    ): List<Recipe>

    @GET("recipes/complexSearch")
    suspend fun searchRecipesComplex(
        @Query("includeIngredients") ingredients: String,
        @Query("maxReadyTime") maxReadyTime: Int? = null,
        @Query("number") number: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("diet") diet: String? = null,
        @Query("intolerances") intolerances: String? = null,
        @Query("cuisine") cuisine: String? = null,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("instructionsRequired") instructionsRequired: Boolean = true,
        @Query("addRecipeNutrition") addRecipeNutrition: Boolean = true,
        @Query("sort") sort: String? = null,
        @Query("sortDirection") sortDirection: String? = null,
        @Query("apiKey") apiKey: String = BuildConfig.API_KEY
    ): ComplexSearchResponse

    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") id: Int,
        @Query("includeNutrition") addRecipeNutrition: Boolean = true,
        @Query("fillIngredients") fillIngredients: Boolean = true,
        @Query("apiKey") apiKey: String = BuildConfig.API_KEY
    ): RecipeDetails
}

//Retrofit je HTTP klijent za Android i Javu koji olakšava slanje API poziva i rad s REST servisima.
object RetrofitInstance {
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}