package com.ryxan.recipescaler.core.data.importer

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiRecipeDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a full response`() {
        val dto = json.decodeFromString<AiRecipeDto>(
            """{"title":"Cake","servings":"8","ingredients":[{"qty":"1","unit":"cup","name":"flour"}]}""",
        )
        val recipe = dto.toDomain()
        assertEquals("Cake", recipe.title)
        assertEquals("8", recipe.servings)
        assertEquals(1, recipe.ingredients.size)
        assertEquals("flour", recipe.ingredients[0].name)
    }

    @Test
    fun `unknown keys are ignored`() {
        val dto = json.decodeFromString<AiRecipeDto>(
            """{"title":"Cake","unknownField":42,"ingredients":[{"qty":"1","name":"flour","extra":"x"}]}""",
        )
        assertEquals("Cake", dto.title)
        assertEquals(1, dto.ingredients.size)
    }

    @Test
    fun `missing optional fields decode as null or empty`() {
        val dto = json.decodeFromString<AiRecipeDto>("""{"ingredients":[{"name":"salt"}]}""")
        assertNull(dto.title)
        assertNull(dto.servings)
        assertNull(dto.ingredients[0].qty)
        assertNull(dto.ingredients[0].unit)
    }

    @Test
    fun `empty ingredients array decodes`() {
        val dto = json.decodeFromString<AiRecipeDto>("""{"ingredients":[]}""")
        assertEquals(0, dto.ingredients.size)
    }
}
