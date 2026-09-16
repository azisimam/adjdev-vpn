package com.ailivebear.app.character.animation

import kotlin.math.sin

/**
 * Continuous slow breathing motion. Produces a normalized [-1, 1] value
 * driven by a sine wave; the caller maps this to a small scale/position
 * offset on the character root (Stage 1) or chest bone (once a skeleton
 * exists).
 */
class BreathingController(
    private val cyclesPerSecond: Float = 0.22f // ~4.5s per breath cycle
) {
    private var elapsedSeconds = 0f

    fun update(deltaSeconds: Float): Float {
        elapsedSeconds += deltaSeconds
        return sin(elapsedSeconds * cyclesPerSecond * 2f * Math.PI.toFloat())
    }
}
