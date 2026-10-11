package com.ryxan.recipescaler.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RecipeEntity::class, IngredientEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}
