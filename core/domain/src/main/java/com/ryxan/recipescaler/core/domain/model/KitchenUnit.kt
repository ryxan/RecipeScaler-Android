package com.ryxan.recipescaler.core.domain.model

enum class UnitFamily { VOLUME, WEIGHT, COUNT }
enum class UnitSystem { US, METRIC, NONE }

/**
 * Canonical unit catalogue — replaces the old Pair<String,String> ALL_UNITS list.
 * `key` is the stored/parsed form ("tbsp", "fl oz"); `label` is the display form
 * ("Tbsp", "fl oz"). Order matches the historical ALL_UNITS order.
 */
enum class KitchenUnit(
    val key: String,
    val label: String,
    val family: UnitFamily,
    val system: UnitSystem,
) {
    NONE("none", "-", UnitFamily.COUNT, UnitSystem.NONE),

    // US volume
    TSP("tsp", "tsp", UnitFamily.VOLUME, UnitSystem.US),
    TBSP("tbsp", "Tbsp", UnitFamily.VOLUME, UnitSystem.US),
    CUP("cup", "cup", UnitFamily.VOLUME, UnitSystem.US),
    PINT("pt", "pt", UnitFamily.VOLUME, UnitSystem.US),
    QUART("qt", "qt", UnitFamily.VOLUME, UnitSystem.US),
    GALLON("gal", "gal", UnitFamily.VOLUME, UnitSystem.US),
    FL_OZ("fl oz", "fl oz", UnitFamily.VOLUME, UnitSystem.US),

    // Metric volume
    ML("ml", "mL", UnitFamily.VOLUME, UnitSystem.METRIC),
    L("L", "L", UnitFamily.VOLUME, UnitSystem.METRIC),

    // Weight
    G("g", "g", UnitFamily.WEIGHT, UnitSystem.METRIC),
    KG("kg", "kg", UnitFamily.WEIGHT, UnitSystem.METRIC),
    MG("mg", "mg", UnitFamily.WEIGHT, UnitSystem.METRIC),
    OZ("oz", "oz", UnitFamily.WEIGHT, UnitSystem.US),
    LB("lb", "lb", UnitFamily.WEIGHT, UnitSystem.US),

    // Other / countable
    EACH("each", "each", UnitFamily.COUNT, UnitSystem.NONE),
    WHOLE("whole", "whole", UnitFamily.COUNT, UnitSystem.NONE),
    PINCH("pinch", "pinch", UnitFamily.COUNT, UnitSystem.NONE),
    DASH("dash", "dash", UnitFamily.COUNT, UnitSystem.NONE),
    CLOVE("clove", "clove", UnitFamily.COUNT, UnitSystem.NONE),
    CAN("can", "can", UnitFamily.COUNT, UnitSystem.NONE),
    PKG("pkg", "pkg", UnitFamily.COUNT, UnitSystem.NONE);

    companion object {
        /** Case-insensitive lookup on key or label; null when unrecognised. */
        fun fromKey(raw: String): KitchenUnit? = entries.find {
            it.key.equals(raw.trim(), ignoreCase = true) ||
                it.label.equals(raw.trim(), ignoreCase = true)
        }
    }
}
