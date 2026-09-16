package com.ailivebear.app.tiktok

import com.ailivebear.app.core.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Connection lifecycle, exposed separately from comment/gift/follow events for UI status display. */
enum class TikTokConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR }

/**
 * Real [TikTokCommentProvider] backed by a plain WebSocket to a
 * TikTok-Live-Connector relay server (default host: ws.adjdev.site).
 *
 * This app does NOT talk to TikTok directly - it talks to your own relay,
 * which runs TikTok-Live-Connector server-side and forwards events over a
 * WebSocket. That split is intentional and out of this class's scope to
 * change.
 *
 * CONTRACT (reverse-engineered from a known-working reference client
 * against this exact relay):
 *  - Connect: `GET wss://<host>?username=<tiktok username, no @>`
 *  - The relay pushes JSON text frames. Each frame is either the raw event
 *    object, or `{"event" or "type": "...", "data": {...}}`.
 *  - Chat/comment events: the event/type field matches /chat|comment|message/i,
 *    OR there is no event/type field at all (some relays send bare chat
 *    objects). Username is read from, in order: `uniqueId`, `username`,
 *    `user.uniqueId`, `user.nickname`, `nickname`, `userInfo.uniqueId`,
 *    `userInfo.nickname`. Text is read from, in order: `comment`,
 *    `message`, `text`, `content`.
 *  - Everything else (gift/follow/like/join/roomUser/...) is ignored for
 *    chat purposes. Gift/follow parsing below is BEST-EFFORT, based on
 *    common tiktok-live-connector field names, and has not been confirmed
 *    against this specific relay - verify against real traffic (AppLog
 *    tag "AILiveBear") before relying on onGift/onFollow.
 *  - No client-side heartbeat/ack protocol observed. On close/failure, the
 *    reference client reconnects after a fixed delay if it's still
 *    supposed to be connected - mirrored here (default 3s).
 */
class WebSocketTikTokCommentProvider(
    private val host: String = "ws.adjdev.site",
    private val reconnectDelayMillis: Long = 3000L
) : TikTokCommentProvider {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // long-lived stream - no read timeout
        .build()

    private var webSocket: WebSocket? = null
    private var shouldStayConnected = false
    private var connected = false

    private val scope = CoroutineScope(Dispatchers.Default)
    private var reconnectJob: Job? = null

    private var commentListener: ((Comment) -> Unit)? = null
    private var giftListener: ((GiftEvent) -> Unit)? = null
    private var followListener: ((FollowEvent) -> Unit)? = null
    private var statusListener: ((TikTokConnectionStatus) -> Unit)? = null

    /** Optional: observe connection status separately from events, e.g. to drive a UI indicator. */
    fun onStatusChanged(listener: (TikTokConnectionStatus) -> Unit) {
        statusListener = listener
    }

    override suspend fun connect(username: String) {
        shouldStayConnected = true
        openSocket(username)
    }

    override suspend fun disconnect() {
        shouldStayConnected = false
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "client disconnect")
        webSocket = null
        connected = false
        statusListener?.invoke(TikTokConnectionStatus.DISCONNECTED)
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

    private fun openSocket(username: String) {
        webSocket?.cancel()
        val cleanUsername = username.trim().removePrefix("@")
        val url = "wss://$host?username=$cleanUsername"
        val request = Request.Builder().url(url).build()

        statusListener?.invoke(TikTokConnectionStatus.CONNECTING)
        AppLog.d(message = "[TikTok] Connecting to $url")

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected = true
                statusListener?.invoke(TikTokConnectionStatus.CONNECTED)
                AppLog.d(message = "[TikTok] Connected as @$cleanUsername")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
                AppLog.d(message = "[TikTok] Closed ($code $reason)")
                scheduleReconnectIfNeeded(cleanUsername)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false
                statusListener?.invoke(TikTokConnectionStatus.ERROR)
                AppLog.w(message = "[TikTok] WebSocket error: ${t.message}")
                scheduleReconnectIfNeeded(cleanUsername)
            }
        })
    }

    private fun scheduleReconnectIfNeeded(username: String) {
        if (!shouldStayConnected) return
        statusListener?.invoke(TikTokConnectionStatus.RECONNECTING)
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelayMillis)
            if (shouldStayConnected) openSocket(username)
        }
    }

    private fun handleMessage(text: String) {
        val payload = try {
            JSONObject(text)
        } catch (e: Exception) {
            AppLog.w(message = "[TikTok] Non-JSON message ignored: $text")
            return
        }

        val evType = payload.optString("event", "").ifBlank { payload.optString("type", "") }.lowercase()
        val data = payload.optJSONObject("data") ?: payload

        when {
            evType.isEmpty() || Regex("chat|comment|message").containsMatchIn(evType) -> {
                extractChat(data)?.let { (username, displayName, commentText) ->
                    commentListener?.invoke(
                        Comment(
                            id = "tt-${System.currentTimeMillis()}-${username.hashCode()}",
                            username = username,
                            displayName = displayName,
                            text = commentText,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            evType.contains("gift") -> extractGift(data)?.let { giftListener?.invoke(it) }
            evType.contains("follow") -> extractFollow(data)?.let { followListener?.invoke(it) }
            // like / join / roomUser / etc: ignored, same as the reference client.
        }
    }

    private fun extractChat(data: JSONObject): Triple<String, String, String>? {
        val userObject = data.optJSONObject("user")
        val userInfo = data.optJSONObject("userInfo")

        val username = data.optString("uniqueId")
            .ifBlank { data.optString("username") }
            .ifBlank { userObject?.optString("uniqueId").orEmpty() }
            .ifBlank { userObject?.optString("nickname").orEmpty() }
            .ifBlank { data.optString("nickname") }
            .ifBlank { userInfo?.optString("uniqueId").orEmpty() }
            .ifBlank { userInfo?.optString("nickname").orEmpty() }
            .ifBlank { "Penonton" }

        val text = data.optString("comment")
            .ifBlank { data.optString("message") }
            .ifBlank { data.optString("text") }
            .ifBlank { data.optString("content") }

        if (text.isBlank()) return null

        val displayName = userObject?.optString("nickname")?.takeIf { it.isNotBlank() } ?: username
        return Triple(username, displayName, text)
    }

    // Best-effort - based on common tiktok-live-connector "gift" event field names.
    // Confirm against real traffic before relying on this in production.
    private fun extractGift(data: JSONObject): GiftEvent? {
        val username = data.optString("uniqueId").ifBlank { data.optString("username") }.ifBlank { "Penonton" }
        val giftName = data.optString("giftName").ifBlank { "gift" }
        val count = data.optInt("repeatCount", data.optInt("count", 1))
        return GiftEvent(username = username, giftName = giftName, count = count, timestamp = System.currentTimeMillis())
    }

    // Best-effort - same caveat as extractGift.
    private fun extractFollow(data: JSONObject): FollowEvent? {
        val username = data.optString("uniqueId").ifBlank { data.optString("username") }
        if (username.isBlank()) return null
        return FollowEvent(username = username, timestamp = System.currentTimeMillis())
    }
}
