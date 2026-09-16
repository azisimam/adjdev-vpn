package com.ailivebear.app.core.model

/**
 * High-level state machine for the character. Stage 1 only ever reaches
 * IDLE. The remaining states are declared now so AnimationController, and
 * later the AI/TTS pipeline, can drive the state machine without changing
 * this contract in Stage 2-4.
 */
enum class CharacterState {
    IDLE,
    LISTENING,
    THINKING,
    READING_COMMENT,
    ANSWERING,
    LAUGHING,
    SURPRISED,
    ERROR
}
