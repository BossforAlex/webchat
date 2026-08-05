package com.wearchat.watch

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WsEvent(val type: String, val data: String)

class WechatApi(
    // Connect to phone's wearchat Phone app (port 8765)
    // Change this IP to your phone's local IP address
    private var baseUrl: String = "http://192.168.1.100:8765"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun updateBaseUrl(url: String) {
        baseUrl = url
    }

    // --- REST API ---

    suspend fun getContacts(): List<JSONObject> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/contacts").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "[]"
        val arr = JSONArray(body)
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun getMessages(): List<JSONObject> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/messages").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "[]"
        val arr = JSONArray(body)
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun getChatMessages(contactId: String): List<JSONObject> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/api/messages/$contactId").build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "{}"
        val obj = JSONObject(body)
        val arr = obj.optJSONArray("messages") ?: JSONArray()
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun sendMessage(contactId: String, content: String): JSONObject = withContext(Dispatchers.IO) {
        val json = JSONObject().put("content", content)
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$baseUrl/api/messages/$contactId").post(body).build()
        val response = client.newCall(request).execute()
        JSONObject(response.body?.string() ?: "{}")
    }

    // --- WebSocket ---

    fun connectWebSocket(onEvent: (WsEvent) -> Unit) {
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws"
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                onEvent(WsEvent(json.getString("event"), json.optString("data", "")))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Reconnect logic
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Activity destroyed")
        webSocket = null
    }
}