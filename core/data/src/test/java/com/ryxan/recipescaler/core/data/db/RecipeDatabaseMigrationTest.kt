package com.ryxan.recipescaler.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecipeDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RecipeDatabase::class.java,
    )

    @Test
    fun `create database at version 1`() {
        helper.createDatabase("migration-test.db", 1).close()
    }

    @Test
    fun `migrate 1 to 2 adds promotedUnitKey and keeps rows`() {
        helper.createDatabase("migration-test.db", 1).apply {
            execSQL(
                "INSERT INTO recipes (id, title, yieldAmount, yieldLabel, createdAt, updatedAt) " +
                    "VALUES (7, 'Pancakes', 4.0, 'servings', 100, 200)"
            )
            execSQL(
                "INSERT INTO ingredients (id, recipeId, position, rawQuantity, unitKey, name, note) " +
                    "VALUES (70, 7, 0, '44', 'cup', 'stock', NULL)"
            )
            close()
        }

        helper.runMigrationsAndValidate("migration-test.db", 2, true, MIGRATION_1_2).apply {
            query("SELECT promotedUnitKey, name FROM ingredients WHERE id = 70").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("stock", c.getString(c.getColumnIndexOrThrow("name")))
                assertTrue(c.isNull(c.getColumnIndexOrThrow("promotedUnitKey")))
            }
            query("SELECT title FROM recipes WHERE id = 7").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Pancakes", c.getString(0))
            }
            close()
        }
    }
}
