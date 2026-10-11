package com.ryxan.recipescaler.core.domain.model

/**
 * Conversion constants for [KitchenUnit]. Volumes normalise through teaspoons
 * (US) and millilitres (metric); weights through grams (metric) and ounces (US).
 */
object UnitConversions {

    // ── US volume ─────────────────────────────────────────────────────────────
    //   3 tsp = 1 Tbsp, 6 tsp = 1 fl oz, 48 tsp = 1 cup, 96 tsp = 1 pt,
    //   192 tsp = 1 qt, 768 tsp = 1 gal
    val TSP_PER_UNIT: Map<KitchenUnit, Double> = mapOf(
        KitchenUnit.TSP to 1.0,
        KitchenUnit.TBSP to 3.0,
        KitchenUnit.FL_OZ to 6.0,
        KitchenUnit.CUP to 48.0,
        KitchenUnit.PINT to 96.0,
        KitchenUnit.QUART to 192.0,
        KitchenUnit.GALLON to 768.0,
    )

    const val ML_PER_TSP = 4.92892159375
    const val ML_PER_L = 1000.0
    const val G_PER_OZ = 28.349523125
    const val OZ_PER_LB = 16.0
    private const val G_PER_MG = 0.001
    private const val G_PER_KG = 1000.0

    /** US-volume amount → teaspoons; null if [unit] is not US volume. */
    fun toTsp(amount: Double, unit: KitchenUnit): Double? =
        TSP_PER_UNIT[unit]?.let { amount * it }

    /** Any volume amount → millilitres; null if [unit] is not VOLUME. */
    fun toMl(amount: Double, unit: KitchenUnit): Double? = when (unit) {
        KitchenUnit.ML -> amount
        KitchenUnit.L -> amount * ML_PER_L
        else -> toTsp(amount, unit)?.let { it * ML_PER_TSP }
    }

    /** Any weight amount → grams; null if [unit] is not WEIGHT. */
    fun toG(amount: Double, unit: KitchenUnit): Double? = when (unit) {
        KitchenUnit.MG -> amount * G_PER_MG
        KitchenUnit.G -> amount
        KitchenUnit.KG -> amount * G_PER_KG
        KitchenUnit.OZ -> amount * G_PER_OZ
        KitchenUnit.LB -> amount * OZ_PER_LB * G_PER_OZ
        else -> null
    }

    /** Any weight amount → ounces; null if [unit] is not WEIGHT. */
    fun toOz(amount: Double, unit: KitchenUnit): Double? =
        toG(amount, unit)?.let { it / G_PER_OZ }
}
