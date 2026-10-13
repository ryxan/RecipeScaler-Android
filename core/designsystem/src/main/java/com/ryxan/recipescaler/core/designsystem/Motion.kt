package com.ryxan.recipescaler.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing

object RescaleMotion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val DurationEnter = 400
    const val DurationExit = 200
    const val DurationShort = 150
}
