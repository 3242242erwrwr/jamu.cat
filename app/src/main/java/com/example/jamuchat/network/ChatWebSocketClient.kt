package com.example.jamuchat.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class UserStatus(
    val name: String,
    val displayName: String = "",
    val profileImageUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: String = ""
) {
    fun getEffectiveName(): String {
        return displayName.ifBlank { name }
    }
}

class ChatWebSocketClient(
    private val onConnectionStatusChanged: (isOnline: Boolean) -> Unit,
    private val onUserListUpdated: (users: List<UserStatus>) -> Unit,
    private val onPrivateMessageReceived: (sender: String, receiver: String, message: String, imageUrl: String?, timestamp: String) -> Unit,
    private val onChatHistoryReceived: (targetUser: String, messages: List<com.example.jamuchat.ChatMessage>) -> Unit,
    private val onChatHistoryCleared: (targetUser: String) -> Unit = {},
    private val onNameTaken: () -> Unit = {},
    private val onErrorOccurred: (errorMsg: String) -> Unit
) {
    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var currentUsername: String = ""
    private var currentBaseUrl: String = ""
    private var currentDeviceId: String = ""
    private var isIntentionallyClosed: Boolean = false
    private var pingRunnable: Runnable? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .pingInterval(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun startPingLoop() {
        stopPingLoop()
        pingRunnable = Runnable {
            try {
                if (webSocket != null && !isIntentionallyClosed) {
                    val json = JSONObject().apply { put("type", "ping") }
                    webSocket?.send(json.toString())
                }
            } catch (e: Exception) {
                Log.e("JAMU_WS", "Ping loop error: ${e.message}")
            }
            if (!isIntentionallyClosed) {
                mainHandler.postDelayed(pingRunnable!!, 8000)
            }
        }
        mainHandler.postDelayed(pingRunnable!!, 8000)
    }

    private fun stopPingLoop() {
        pingRunnable?.let { mainHandler.removeCallbacks(it) }
        pingRunnable = null
    }

    fun connect(baseUrl: String, username: String, deviceId: String) {
        currentUsername = username
        currentBaseUrl = baseUrl
        currentDeviceId = deviceId
        isIntentionallyClosed = false

        var formatted = baseUrl.trim().trimEnd('/')
        if (formatted.startsWith("http://")) {
            formatted = "ws://" + formatted.substring(7)
        } else if (formatted.startsWith("https://")) {
            formatted = "wss://" + formatted.substring(8)
        } else if (!formatted.startsWith("ws://") && !formatted.startsWith("wss://")) {
            formatted = "wss://$formatted"
        }

        var cleanBase = formatted.trimEnd('/')
        if (cleanBase.endsWith("/ws")) {
            cleanBase = cleanBase.substring(0, cleanBase.length - 3).trimEnd('/')
        }

        val encodedUsername = try {
            URLEncoder.encode(username.trim(), "UTF-8")
        } catch (_: Exception) {
            username.trim()
        }

        val wsUrl = "$cleanBase/ws/$encodedUsername/$deviceId"
        Log.d("JAMU_WS", "WebSocket connecting: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        closeInternal()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("JAMU_WS", "WebSocket successfully connected: $username")
                mainHandler.post {
                    onConnectionStatusChanged(true)
                    startPingLoop()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("JAMU_WS", "WebSocket message received: $text")
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "user_list" -> {
                            val array = json.optJSONArray("users")
                            val userStatusList = mutableListOf<UserStatus>()
                            if (array != null) {
                                for (i in 0 until array.length()) {
                                    val item = array.optJSONObject(i)
                                    if (item != null) {
                                        val uName = item.optString("name")
                                        val dName = item.optString("display_name").takeIf { it.isNotEmpty() } ?: uName
                                        val pImg = item.optString("profile_image_url").takeIf { it.isNotEmpty() }
                                        val lSeen = item.optString("last_seen", "")
                                        userStatusList.add(
                                            UserStatus(
                                                name = uName,
                                                displayName = dName,
                                                profileImageUrl = pImg,
                                                isOnline = item.optBoolean("is_online", false),
                                                lastSeen = lSeen
                                            )
                                        )
                                    } else {
                                        val nameStr = array.getString(i)
                                        userStatusList.add(
                                            UserStatus(
                                                name = nameStr,
                                                displayName = nameStr,
                                                profileImageUrl = null,
                                                isOnline = true,
                                                lastSeen = ""
                                            )
                                        )
                                    }
                                }
                            }
                            mainHandler.post {
                                onUserListUpdated(userStatusList)
                            }
                        }

                        "private_message" -> {
                            val sender = json.optString("sender")
                            val receiver = json.optString("receiver")
                            val message = json.optString("message")
                            val imageUrl = json.optString("image_url").takeIf { it.isNotEmpty() }
                            val timestamp = json.optString("timestamp")
                            mainHandler.post {
                                onPrivateMessageReceived(sender, receiver, message, imageUrl, timestamp)
                            }
                        }

                        "chat_history" -> {
                            val targetUser = json.optString("target_user")
                            val array = json.optJSONArray("messages")
                            val messages = mutableListOf<com.example.jamuchat.ChatMessage>()
                            if (array != null) {
                                for (i in 0 until array.length()) {
                                    val msgObj = array.getJSONObject(i)
                                    val sender = msgObj.optString("sender")
                                    val isMe = sender.trim().equals(currentUsername.trim(), ignoreCase = true)
                                    messages.add(
                                        com.example.jamuchat.ChatMessage(
                                            id = msgObj.optString("id"),
                                            senderName = sender,
                                            text = msgObj.optString("text"),
                                            imageUrl = msgObj.optString("image_url").takeIf { it.isNotEmpty() },
                                            isCurrentUser = isMe,
                                            timestamp = msgObj.optString("timestamp")
                                        )
                                    )
                                }
                            }
                            mainHandler.post {
                                onChatHistoryReceived(targetUser, messages)
                            }
                        }

                        "chat_history_cleared" -> {
                            val targetUser = json.optString("target_user")
                            mainHandler.post {
                                onChatHistoryCleared(targetUser)
                            }
                        }

                        "error" -> {
                            if (json.optString("message") == "NAME_TAKEN") {
                                mainHandler.post { onNameTaken() }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("JAMU_WS", "JSON parsing error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorDetails = t.localizedMessage ?: "Serverga ulanib bo'lmadi"
                Log.e("JAMU_WS", "WebSocket error: $errorDetails")
                mainHandler.post {
                    stopPingLoop()
                    onConnectionStatusChanged(false)
                    onErrorOccurred("$errorDetails ($wsUrl)")
                    scheduleReconnect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("JAMU_WS", "WebSocket closed: $reason")
                mainHandler.post {
                    stopPingLoop()
                    onConnectionStatusChanged(false)
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (!isIntentionallyClosed && currentUsername.isNotEmpty() && currentBaseUrl.isNotEmpty()) {
            mainHandler.postDelayed({
                if (!isIntentionallyClosed) {
                    Log.d("JAMU_WS", "Auto-reconnecting to WebSocket...")
                    connect(currentBaseUrl, currentUsername, currentDeviceId)
                }
            }, 2000)
        }
    }

    fun updateProfile(username: String, displayName: String, profileImageUrl: String?) {
        val json = JSONObject().apply {
            put("type", "update_profile")
            put("username", username)
            put("display_name", displayName)
            put("profile_image_url", profileImageUrl ?: "")
        }
        webSocket?.send(json.toString())
    }

    fun fetchHistory(targetUser: String) {
        val json = JSONObject().apply {
            put("type", "fetch_history")
            put("target_user", targetUser)
        }
        webSocket?.send(json.toString())
    }

    fun clearHistory(targetUser: String) {
        val json = JSONObject().apply {
            put("type", "clear_history")
            put("target_user", targetUser)
        }
        webSocket?.send(json.toString())
    }

    fun sendPrivateMessage(msgId: String, sender: String, receiver: String, message: String, imageUrl: String? = null, timestamp: String) {
        val json = JSONObject().apply {
            put("type", "private_message")
            put("id", msgId)
            put("sender", sender)
            put("receiver", receiver)
            put("message", message)
            if (!imageUrl.isNullOrEmpty()) {
                put("image_url", imageUrl)
            }
            put("timestamp", timestamp)
        }
        val sent = webSocket?.send(json.toString()) ?: false
        if (!sent) {
            Log.e("JAMU_WS", "Failed to send private message")
        }
    }

    fun sendFcmToken(fcmToken: String) {
        val json = JSONObject().apply {
            put("type", "register_fcm")
            put("fcm_token", fcmToken)
        }
        webSocket?.send(json.toString())
    }

    private fun closeInternal() {
        try {
            stopPingLoop()
            webSocket?.close(1000, "User disconnect")
            webSocket = null
        } catch (e: Exception) {
            Log.e("JAMU_WS", "Disconnect error: ${e.message}")
        }
    }

    fun disconnect() {
        isIntentionallyClosed = true
        closeInternal()
        mainHandler.post {
            onConnectionStatusChanged(false)
        }
    }
}
