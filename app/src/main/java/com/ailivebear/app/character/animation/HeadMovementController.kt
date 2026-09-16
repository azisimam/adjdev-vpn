package com.ailivebear.app.character.animation

import kotlin.random.Random

/**
 * Very small, slow random head sway. Stage 1 applies this to the whole
 * character root, since the placeholder asset has no isolated head bone
 * yet. Once a rigged skeleton exists, retarget the output onto the head
 * joint only (via TransformManager on that bone's transform instance)
 * instead of the root.
 */
class HeadMovementController(
    private val maxAngleDegrees: Float = 3.5f,
    private val retargetIntervalSeconds: Float = 3.5f
) {
    private var targetYaw = 0f
    private var targetPitch = 0f
    private var currentYaw = 0f
    private var currentPitch = 0f
    private var timeUntilRetarget = 0f

    fun update(deltaSeconds: Float): Pair<Float, Float> {
        timeUntilRetarget -= deltaSeconds
        if (timeUntilRetarget <= 0f) {
            targetYaw = randomAngle()
            targetPitch = randomAngle() * 0.5f
            timeUntilRetarget = retargetIntervalSeconds * (0.6f + Random.nextFloat())
        }
        val smoothing = 1f - Math.exp(-deltaSeconds.toDouble() * 1.5).toFloat()
        currentYaw += (targetYaw - currentYaw) * smoothing
        currentPitch += (targetPitch - currentPitch) * smoothing
        return currentYaw to currentPitch
    }

    private fun randomAngle() = (Random.nextFloat() * 2f - 1f) * maxAngleDegrees
}
