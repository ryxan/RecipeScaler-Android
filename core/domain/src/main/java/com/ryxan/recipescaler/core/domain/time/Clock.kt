package com.ryxan.recipescaler.core.domain.time

/** Wall-clock abstraction so repositories can be tested with fake time. */
fun interface Clock {
    fun now(): Long
}
