package com.ailivebear.app.character.animation

import kotlin.random.Random

/**
 * Natural, non-periodic blinking. Emits a 0f..1f "closedness" value with a
 * short blink duration and a randomized interval between blinks, so it
 * never reads as a fixed loop.
 *
 * Stage 1 has no eye morph target to drive yet (the placeholder asset may
 * not define one), so this value is computed every frame but not applied
 * to anything visible. Stage 2's EyeController should wire this straight
 * into a morph-target weight without changing the timing logic here.
 */
class BlinkController(
    private val minIntervalSeconds: Float = 2.0f,
    private val maxIntervalSeconds: Float = 6.5f,
    private val blinkDurationSeconds: Float = 0.14f
) {
    private var timeUntilNextBlink = randomInterval()
    private var blinkTimeRemaining = 0f

    fun update(deltaSeconds: Float): Float {
        if (blinkTimeRemaining > 0f) {
            blinkTimeRemaining -= deltaSeconds
            val progress = 1f - (blinkTimeRemaining / blinkDurationSeconds).coerceIn(0f, 1f)
            // Triangle-shaped close/open envelope.
            return if (progress < 0.5f) progress * 2f else (1f - progress) * 2f
        }
        timeUntilNextBlink -= deltaSeconds
        if (timeUntilNextBlink <= 0f) {
            blinkTimeRemaining = blinkDurationSeconds
            timeUntilNextBlink = randomInterval()
        }
        return 0f
    }

    private fun randomInterval() =
        Random.nextFloat() * (maxIntervalSeconds - minIntervalSeconds) + minIntervalSeconds
}
