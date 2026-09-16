package com.ailivebear.app.tiktok

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fake [TikTokCommentProvider] that emits a small looping script of sample
 * comments on a timer. Lets the comment -> AI -> TTS -> lip-sync pipeline
 * (Stage 3/4) be built and tested end-to-end before real TikTok LIVE
 * access exists. Not used by any UI in Stage 1 yet - it's here so that
 * wiring can start immediately in the next stage.
 */
class MockCommentProvider(
    private val intervalMillis: Long = 6000L
) : TikTokCommentProvider {

    private val sampleComments = listOf(
        "halo beruang",
        "kamu umur berapa?",
        "bisa nyanyi?",
        "lucu banget",
        "kamu makan apa?",
        "punya teman gak?"
    )
    private val sampleUsers = listOf("andi", "rani", "budi", "dina", "citra", "eko")

    private var connected = false
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private var commentListener: ((Comment) -> Unit)? = null
    private var giftListener: ((GiftEvent) -> Unit)? = null
    private var followListener: ((FollowEvent) -> Unit)? = null

    override suspend fun connect(username: String) {
        connected = true
        job?.cancel()
        job = scope.launch {
            var index = 0
            while (connected) {
                delay(intervalMillis)
                val user = sampleUsers[index % sampleUsers.size]
                val text = sampleComments[index % sampleComments.size]
                commentListener?.invoke(
                    Comment(
                        id = "mock-${System.currentTimeMillis()}-$index",
                        username = user,
                        displayName = user.replaceFirstChar { it.uppercase() },
                        text = text,
                        timestamp = System.currentTimeMillis()
                    )
                )
                index++
            }
        }
    }

    override suspend fun disconnect() {
        connected = false
        job?.cancel()
        job = null
    }

    override fun isConnected(): Boolean = connected

    override fun onComment(listener: (Comment) -> Unit) {
        commentListener = listener
    }

    override fun onGift(listener: (GiftEvent) -> Unit) {
        giftListener = listener
    }

    override fun onFollow(listener: (FollowEvent) -> Unit) {
        followListener = listener
    }
}
