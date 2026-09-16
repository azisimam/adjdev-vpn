package com.ailivebear.app.tiktok

/** A single normalized LIVE comment. */
data class Comment(
    val id: String,
    val username: String,
    val displayName: String,
    val text: String,
    val timestamp: Long
)

/** A gift event. Not wired up until a later stage; declared now to keep the interface stable. */
data class GiftEvent(
    val username: String,
    val giftName: String,
    val count: Int,
    val timestamp: Long
)

/** A follow event. Not wired up until a later stage. */
data class FollowEvent(val username: String, val timestamp: Long)

/**
 * Abstraction over "wherever LIVE comments come from". Stage 1 ships only
 * [MockCommentProvider]. A real implementation can be dropped in later
 * without touching CharacterManager or the AI pipeline, since both only
 * ever depend on this interface.
 */
interface TikTokCommentProvider {
    suspend fun connect(username: String)
    suspend fun disconnect()
    fun isConnected(): Boolean
    fun onComment(listener: (Comment) -> Unit)
    fun onGift(listener: (GiftEvent) -> Unit)
    fun onFollow(listener: (FollowEvent) -> Unit)
}
