package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1, // uvijek samo jedan red
    val diet: String,
    val intolerances: String,
    val cuisine: String
)
