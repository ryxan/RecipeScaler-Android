package com.ryxan.recipescaler.core.data.db

import com.ryxan.recipescaler.core.domain.model.Ingredient
import com.ryxan.recipescaler.core.domain.model.KitchenUnit
import com.ryxan.recipescaler.core.domain.model.Recipe
import com.ryxan.recipescaler.core.domain.model.RecipeSummary

internal fun RecipeWithIngredients.toDomain(): Recipe = Recipe(
    id = recipe.id,
    title = recipe.title,
    yieldAmount = recipe.yieldAmount,
    yieldLabel = recipe.yieldLabel,
    createdAt = recipe.createdAt,
    updatedAt = recipe.updatedAt,
    ingredients = ingredients.sortedBy { it.position }.map { it.toDomain() },
)

internal fun IngredientEntity.toDomain(): Ingredient = Ingredient(
    id = id,
    rawQuantity = rawQuantity,
    unit = KitchenUnit.fromKey(unitKey) ?: KitchenUnit.NONE,
    name = name,
    note = note,
)

internal fun Ingredient.toEntity(recipeId: Long, position: Int): IngredientEntity = IngredientEntity(
    id = if (id == 0L) 0 else id,
    recipeId = recipeId,
    position = position,
    rawQuantity = rawQuantity,
    unitKey = unit.key,
    name = name,
    note = note,
)

internal fun RecipeSummaryTuple.toDomain(): RecipeSummary = RecipeSummary(
    id = id,
    title = title,
    yieldAmount = yieldAmount,
    yieldLabel = yieldLabel,
    ingredientCount = ingredientCount,
    updatedAt = updatedAt,
)
