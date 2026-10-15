package com.ryxan.recipescaler.core.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val yieldAmount: Double,
    val yieldLabel: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val position: Int,
    val rawQuantity: String,
    val unitKey: String,
    val name: String,
    val note: String?,
    val promotedUnitKey: String? = null,
)

data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientEntity>,
)

data class RecipeSummaryTuple(
    val id: Long,
    val title: String,
    val yieldAmount: Double,
    val yieldLabel: String,
    val updatedAt: Long,
    val ingredientCount: Int,
)
