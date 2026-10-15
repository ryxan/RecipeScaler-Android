package com.ryxan.recipescaler.core.domain.model

/**
 * The last Scale View inputs the user entered for one recipe, persisted so
 * reopening the recipe restores exactly what they left. Both fields are kept:
 * the scaled result depends on the Serves / Scale To pair, so restoring only
 * one would silently change the factor.
 */
data class SavedScaleInputs(
    val servesText: String,
    val scaleToText: String,
)
