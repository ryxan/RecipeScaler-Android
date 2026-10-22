package com.ryxan.recipescaler.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * "Culinary Material" (v2) shape scale — the expressive end of M3 curvature:
 * 8dp badges, 12dp menus/snackbars, 16dp cards & inputs, 28dp hero cards,
 * dialogs and sheets. Chips and buttons use the full pill shape.
 */
val RescaleShapesV2 = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
