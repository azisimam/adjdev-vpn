package com.ailivebear.app.character

import android.content.Context
import android.view.Surface
import com.ailivebear.app.background.BackgroundManager
import com.ailivebear.app.character.animation.AnimationController
import com.ailivebear.app.character.animation.FrameAnimation
import com.ailivebear.app.character.engine.FilamentEngine
import com.ailivebear.app.character.engine.ModelLoader
import com.google.android.filament.gltfio.FilamentAsset

/**
 * Top-level coordinator for "the character on screen": owns the Filament
 * engine, the loaded model, the background, and the idle animation
 * controller, and wires them together every frame.
 *
 * This is the class a facial rig / EmotionController / EyeController /
 * MouthController, and later a LipSyncController, should extend. They plug
 * into [onFrame] alongside animationController, and into [applySettings]'s
 * character-loading branch - they should not replace this class outright.
 */
class CharacterManager(private val context: Context) {

    private val filamentEngine = FilamentEngine()
    private val modelLoader = ModelLoader(filamentEngine.engine, filamentEngine.scene)
    val backgroundManager = BackgroundManager(filamentEngine.engine, filamentEngine.scene)
    val animationController = AnimationController()

    private var currentAsset: FilamentAsset? = null
    private var rootTransformInstance = -1
    private var baseRootY = 0f

    var characterId: String = "bubu_bear"
        private set
    var backgroundId: String = "studio_soft"
        private set

    /**
     * Loads the character + background for the given settings. Stage 1
     * always loads the single placeholder asset regardless of
     * [characterId]; a later stage maps characterId -> a specific asset
     * path here once more than one character exists.
     */
    fun applySettings(characterId: String, backgroundId: String) {
        this.characterId = characterId
        this.backgroundId = backgroundId

        backgroundManager.applyBackground(backgroundId)

        currentAsset = modelLoader.loadCharacter(context)
        val tm = filamentEngine.engine.transformManager
        rootTransformInstance = currentAsset?.let { tm.getInstance(it.root) } ?: -1
        baseRootY = 0f
    }

    fun onSurfaceCreated() {
        applySettings(characterId, backgroundId)
    }

    fun onSurfaceChanged(surface: Surface, width: Int, height: Int) {
        filamentEngine.attachSurface(surface, width, height)
    }

    fun onSurfaceDestroyed() {
        filamentEngine.detachSurface()
    }

    fun onFrame(deltaSeconds: Float, frameTimeNanos: Long) {
        val frame = animationController.update(deltaSeconds)
        applyFrameAnimation(frame)
        filamentEngine.renderFrame(frameTimeNanos)
    }

    /**
     * Applies breathing/head-sway to the character root transform.
     * Stage 1 has no skeleton/morph targets to target individually, so all
     * of this lands on the root node: a tiny vertical bob + scale for
     * breathing, and a small Y-axis rotation for head sway.
     *
     * [FrameAnimation.blink] and [FrameAnimation.headPitchDegrees] are
     * computed every frame but intentionally not applied to anything here -
     * once a rigged asset with an eye morph target and a separate head bone
     * exists, an EyeController should consume blink as a morph-target
     * weight, and head pitch should move to that bone's own transform
     * instead of the whole body's.
     */
    private fun applyFrameAnimation(frame: FrameAnimation) {
        if (rootTransformInstance < 0) return
        val tm = filamentEngine.engine.transformManager

        val breatheOffset = frame.breathe * 0.012f
        val breatheScale = 1f + frame.breathe * 0.008f
        val yawRad = Math.toRadians(frame.headYawDegrees.toDouble())
        val cosY = Math.cos(yawRad).toFloat()
        val sinY = Math.sin(yawRad).toFloat()

        // Column-major 4x4: Y-axis rotation (head sway) combined with a uniform
        // breathing scale, translated by a small vertical bob.
        val matrix = floatArrayOf(
            cosY * breatheScale, 0f, sinY * breatheScale, 0f,
            0f, breatheScale, 0f, 0f,
            -sinY * breatheScale, 0f, cosY * breatheScale, 0f,
            0f, baseRootY + breatheOffset, 0f, 1f
        )
        tm.setTransform(rootTransformInstance, matrix)
    }

    fun destroy() {
        modelLoader.destroy()
        backgroundManager.destroy()
        filamentEngine.destroy()
    }
}
