package com.ailivebear.app.character.engine

import android.content.Context
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.ailivebear.app.character.CharacterManager

/**
 * The single visible surface for the character + background. Main screen
 * must contain ONLY this - do not add any other view on top of it.
 *
 * Drives the render loop off [Choreographer], which paces callbacks to the
 * display's vsync (typically 60 Hz, sometimes 90/120), satisfying the
 * 30-60 FPS target for Stage 1's simple scene without a manual frame-rate
 * limiter.
 */
class CharacterRenderView(
    context: Context,
    private val characterManager: CharacterManager
) : SurfaceView(context), SurfaceHolder.Callback, Choreographer.FrameCallback {

    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var lastFrameTimeNanos = 0L

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        characterManager.onSurfaceCreated()
        running = true
        lastFrameTimeNanos = 0L
        choreographer.postFrameCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        characterManager.onSurfaceChanged(holder.surface, width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        choreographer.removeFrameCallback(this)
        characterManager.onSurfaceDestroyed()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return

        val deltaSeconds = if (lastFrameTimeNanos == 0L) {
            1f / 60f
        } else {
            ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000.0)
                .toFloat()
                .coerceIn(0f, 1f / 15f) // clamp so a stall (e.g. app backgrounded) doesn't jump the animation
        }
        lastFrameTimeNanos = frameTimeNanos

        characterManager.onFrame(deltaSeconds, frameTimeNanos)

        choreographer.postFrameCallback(this)
    }

    fun onPause() {
        running = false
        choreographer.removeFrameCallback(this)
    }

    fun onResume() {
        if (!running) {
            running = true
            lastFrameTimeNanos = 0L
            choreographer.postFrameCallback(this)
        }
    }
}
