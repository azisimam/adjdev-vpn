package com.ailivebear.app.core.model

/**
 * Emotions the character can express. Mirrors the schema the AI layer will
 * output in Stage 4 (see AIResponse). Defined now so Stage 1's interfaces
 * are already shaped correctly and Stage 2's EmotionController can consume
 * this without renegotiating the contract.
 */
enum class EmotionType {
    NEUTRAL,
    HAPPY,
    LAUGH,
    SAD,
    ANGRY,
    SURPRISED,
    CONFUSED,
    SHY,
    EXCITED
}
