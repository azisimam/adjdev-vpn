package com.ailivebear.app.character.animation

import com.ailivebear.app.core.model.CharacterState

/**
 * Combines the individual idle sub-controllers into the single set of
 * per-frame outputs CharacterManager applies to the model.
 *
 * A later stage extends this, it does not replace it: an EmotionController
 * and MouthController/LipSyncController plug in alongside
 * Breathing/Blink/Head as additional producers feeding the same per-frame
 * apply step, and [CharacterState] (already defined in core.model) gates
 * which sub-controllers are active - e.g. suppressing idle head sway while
 * ANSWERING and instead driving head pose from speech emphasis.
 */
class AnimationController {
    private val breathing = BreathingController()
    private val blink = BlinkController()
    private val headMovement = HeadMovementController()

    var state: CharacterState = CharacterState.IDLE
        private set

    fun setState(newState: CharacterState) {
        state = newState
    }

    fun update(deltaSeconds: Float): FrameAnimation {
        val breatheValue = breathing.update(deltaSeconds)
        val blinkValue = blink.update(deltaSeconds)
        val (yaw, pitch) = when (state) {
            CharacterState.IDLE -> headMovement.update(deltaSeconds)
            else -> 0f to 0f // Later stages: other states drive head pose differently.
        }
        return FrameAnimation(
            breathe = breatheValue,
            blink = blinkValue,
            headYawDegrees = yaw,
            headPitchDegrees = pitch
        )
    }
}

/** One frame's worth of idle animation outputs, ready for CharacterManager to apply to the model. */
data class FrameAnimation(
    val breathe: Float,
    val blink: Float,
    val headYawDegrees: Float,
    val headPitchDegrees: Float
)
