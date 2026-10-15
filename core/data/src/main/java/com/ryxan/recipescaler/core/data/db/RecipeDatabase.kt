package com.ryxan.recipescaler.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2: per-ingredient "Promote to" unit override on the Scale screen. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ingredients ADD COLUMN promotedUnitKey TEXT")
    }
}

@Database(
    entities = [RecipeEntity::class, IngredientEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}
