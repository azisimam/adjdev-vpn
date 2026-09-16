package com.ailivebear.app.background

import com.google.android.filament.Engine
import com.google.android.filament.Scene
import com.google.android.filament.Skybox

/**
 * Owns the background shown behind the character.
 *
 * Stage 1 supports solid-color backgrounds only, implemented as a Filament
 * [Skybox]. The public surface (applyBackground) is written so a later
 * stage can add image/video/HDR backgrounds by branching inside
 * applyBackground, without any caller (CharacterManager, SettingsScreen)
 * needing to change.
 */
class BackgroundManager(private val engine: Engine, private val scene: Scene) {

    private var currentSkybox: Skybox? = null

    fun applyBackground(backgroundId: String) {
        currentSkybox?.let {
            scene.skybox = null
            engine.destroySkybox(it)
        }

        val color = colorFor(backgroundId)
        val skybox = Skybox.Builder()
            .color(color[0], color[1], color[2], 1.0f)
            .build(engine)

        currentSkybox = skybox
        scene.skybox = skybox
    }

    fun destroy() {
        currentSkybox?.let { engine.destroySkybox(it) }
        currentSkybox = null
    }

    private fun colorFor(backgroundId: String): FloatArray = when (backgroundId) {
        "solid_dark" -> floatArrayOf(0.07f, 0.07f, 0.08f)
        "solid_light" -> floatArrayOf(0.96f, 0.96f, 0.94f)
        else -> floatArrayOf(0.53f, 0.64f, 0.62f) // "studio_soft" default: soft neutral green-grey
    }
}
