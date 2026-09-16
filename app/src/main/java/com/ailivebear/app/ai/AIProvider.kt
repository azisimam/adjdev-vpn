package com.ailivebear.app.ai

import com.ailivebear.app.core.model.EmotionType

/**
 * Structured AI reply. Defined next to the interface so Stage 4's real
 * provider and its callers agree on one exact shape from the start:
 * a short "read" (repeats/acknowledges the comment), an "answer", the
 * emotion to play it with, and an intensity for how strongly to express it.
 */
data class AIResponse(
    val read: String,
    val answer: String,
    val emotion: EmotionType,
    val intensity: Float
)

/**
 * Abstraction over "whatever generates the character's replies". No
 * implementation ships in Stage 1 - this interface exists purely so the
 * settings screen and later CharacterManager wiring have a stable contract
 * to point at, and so a provider can be swapped (OpenAI, a custom backend,
 * etc.) without touching the rest of the app.
 */
interface AIProvider {
    suspend fun generateResponse(commentText: String, username: String): AIResponse
}
