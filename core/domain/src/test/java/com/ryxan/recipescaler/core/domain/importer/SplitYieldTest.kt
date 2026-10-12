package com.ryxan.recipescaler.core.domain.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SplitYieldTest {

    @Test
    fun `bare quantity has no label`() {
        assertEquals(YieldSplit("4", null), splitYield("4"))
    }

    @Test
    fun `quantity with label`() {
        assertEquals(YieldSplit("12", "cookies"), splitYield("12 cookies"))
        assertEquals(YieldSplit("4", "servings"), splitYield("4 servings"))
    }

    @Test
    fun `leading serves or makes prefix is stripped`() {
        assertEquals(YieldSplit("4", null), splitYield("Serves 4"))
        assertEquals(YieldSplit("2", "dozen"), splitYield("Makes 2 dozen"))
        assertEquals(YieldSplit("24", "cookies"), splitYield("Yield 24 cookies"))
        assertEquals(YieldSplit("4", null), splitYield("servings: 4"))
    }

    @Test
    fun `ranges and fractions parse as the quantity token`() {
        assertEquals(YieldSplit("4-6", "servings"), splitYield("4-6 servings"))
        assertEquals(YieldSplit("1 1/2", "cups"), splitYield("Makes 1 1/2 cups"))
        assertEquals(YieldSplit("½", null), splitYield("½"))
    }

    @Test
    fun `no leading quantity returns null`() {
        assertNull(splitYield("a lot"))
        assertNull(splitYield(""))
        assertNull(splitYield("Serves"))
    }
}
