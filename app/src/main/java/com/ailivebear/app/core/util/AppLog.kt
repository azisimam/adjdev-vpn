package com.ailivebear.app.core.util

import android.util.Log

/**
 * Internal-only logging wrapper. Never surfaced in the UI - Stage 1's main
 * screen has no debug/FPS/status overlay by design, and that should stay
 * true in later stages too (add a separate developer-mode screen instead
 * of piping this into the character screen).
 */
object AppLog {
    private const val DEFAULT_TAG = "AILiveBear"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        Log.d(tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }
}
