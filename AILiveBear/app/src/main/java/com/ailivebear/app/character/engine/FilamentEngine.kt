package com.ailivebear.app.character.engine

import android.view.Surface
import com.google.android.filament.Camera
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport

/**
 * Owns the core Filament objects: engine, scene, view, renderer, camera,
 * and lighting. One instance lives for the lifetime of the character
 * screen's surface.
 *
 * Kept deliberately dumb: this class only sets up engine/scene/camera/light
 * plumbing. Model loading lives in [ModelLoader]; per-frame animation lives
 * in character.animation + CharacterManager. That split is what lets a
 * facial rig and lip sync be added later without touching this file.
 */
class FilamentEngine {

    val engine: Engine = Engine.create()
    val scene: Scene = engine.createScene()
    val view: View = engine.createView()
    val renderer: Renderer = engine.createRenderer()
    private val cameraEntity = EntityManager.get().create()
    val camera: Camera = engine.createCamera(cameraEntity)

    private var swapChain: SwapChain? = null
    private val keyLight = EntityManager.get().create()
    private val fillLight = EntityManager.get().create()

    init {
        view.scene = scene
        view.camera = camera
        setupLighting()
        setupCamera()
    }

    /** Simple key + fill directional lighting so the character reads as soft/friendly, not flat-shaded. */
    private fun setupLighting() {
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.98f, 0.92f)
            .intensity(90_000.0f)
            .direction(-0.35f, -0.9f, -0.25f)
            .castShadows(true)
            .build(engine, keyLight)
        scene.addEntity(keyLight)

        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(0.85f, 0.9f, 1.0f)
            .intensity(28_000.0f)
            .direction(0.5f, -0.2f, 0.6f)
            .castShadows(false)
            .build(engine, fillLight)
        scene.addEntity(fillLight)
    }

    private fun setupCamera() {
        // Head + upper-body framing with a little breathing room around the character.
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)
        camera.lookAt(
            0.0, 1.55, 3.2,   // eye
            0.0, 1.35, 0.0,   // target
            0.0, 1.0, 0.0     // up
        )
    }

    fun attachSurface(surface: Surface, width: Int, height: Int) {
        detachSurface()
        swapChain = engine.createSwapChain(surface)
        updateViewport(width, height)
    }

    fun updateViewport(width: Int, height: Int) {
        if (width == 0 || height == 0) return
        view.viewport = Viewport(0, 0, width, height)
        val aspect = width.toDouble() / height.toDouble()
        camera.setProjection(38.0, aspect, 0.05, 50.0, Camera.Fov.VERTICAL)
    }

    fun renderFrame(frameTimeNanos: Long) {
        val swap = swapChain ?: return
        if (renderer.beginFrame(swap, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    fun detachSurface() {
        swapChain?.let { engine.destroySwapChain(it) }
        swapChain = null
    }

    fun destroy() {
        detachSurface()
        engine.destroyEntity(keyLight)
        engine.destroyEntity(fillLight)
        engine.destroyCameraComponent(cameraEntity)
        EntityManager.get().destroy(cameraEntity)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyRenderer(renderer)
        Engine.destroy(engine)
    }
}
