package com.ailivebear.app.tts

/**
 * One synthesized utterance: raw audio bytes plus optional phoneme/viseme
 * timing, consumed by the future LipSyncController. No implementation
 * ships in Stage 1.
 */
data class TTSResult(
    val audioData: ByteArray,
    val phonemeTimings: List<PhonemeTiming> = emptyList()
)

data class PhonemeTiming(
    val viseme: String,
    val startMillis: Long,
    val endMillis: Long
)

/** Abstraction over the text-to-speech backend, swappable independently of AIProvider. */
interface TTSProvider {
    suspend fun synthesize(text: String): TTSResult
}
