package com.ryxan.recipescaler.core.domain.formatting

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultMeasurementSystemTest {

    @Test
    fun `customary countries default to US`() {
        assertEquals(MeasurementSystem.US, defaultMeasurementSystemFor("US"))
        assertEquals(MeasurementSystem.US, defaultMeasurementSystemFor("LR"))
        assertEquals(MeasurementSystem.US, defaultMeasurementSystemFor("MM"))
    }

    @Test
    fun `country matching is case-insensitive`() {
        assertEquals(MeasurementSystem.US, defaultMeasurementSystemFor("us"))
    }

    @Test
    fun `other countries default to METRIC`() {
        assertEquals(MeasurementSystem.METRIC, defaultMeasurementSystemFor("GB"))
        assertEquals(MeasurementSystem.METRIC, defaultMeasurementSystemFor("DE"))
    }

    @Test
    fun `blank or unknown country defaults to METRIC`() {
        assertEquals(MeasurementSystem.METRIC, defaultMeasurementSystemFor(""))
        assertEquals(MeasurementSystem.METRIC, defaultMeasurementSystemFor("XX"))
    }
}
