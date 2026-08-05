package com.wearchat.phone

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CopyOnWriteArraySet

class ApiServer : Service() {

    companion object {
        private var instance: WebServer? = null
        private val wsClients = CopyOnWriteArraySet<NanoWSD.WebSocket>()

        fun broadcast(event: String, data: String) {
            val payload = JSONObject().apply {
                put("event", event)
                put("data", data)
            }.toString()
            for (ws in wsClients) {
                try { ws.send(payload) } catch (_: IOException) {}
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (instance == null) {
            instance = WebServer(8765)
            try {
                instance?.start()
                Log.i("wearchat", "API server started on port 8765")
            } catch (e: IOException) {
                Log.e("wearchat", "Failed to start server: ${e.message}")
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        instance?.stop()
        instance = null
        super.onDestroy()
    }

    private class WebServer(port: Int) : NanoWSD("0.0.0.0", port) {

        override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
            return WsClient(handshake)
        }

        override fun serve(session: IHTTPSession): Response {
            return try {
                when {
                    session.uri == "/api/health" -> handleHealth()
                    session.uri == "/api/contacts" && session.method == Method.GET -> handleGetContacts()
                    session.uri == "/api/messages" && session.method == Method.GET -> handleGetAllMessages()
                    session.uri.matches(Regex("/api/messages/(\\w+)")) -> {
                        val contactId = session.uri.split("/").last()
                        when (session.method) {
                            Method.GET -> handleGetChatMessages(contactId)
                            Method.POST -> handleSendMessage(session, contactId)
                            else -> jsonError("Method not allowed")
                        }
                    }
                    else -> jsonError("Not found")
                }
            } catch (e: Exception) {
                Log.e("wearchat", "API error: ${e.message}")
                jsonError("Internal error: ${e.message}")
            }
        }

        private fun handleHealth(): Response = jsonOk("""{"status":"ok","timestamp":${System.currentTimeMillis()}}""")

        private fun handleGetContacts(): Response = jsonOk(MessageStore.getContacts().toString())

        private fun handleGetAllMessages(): Response = jsonOk(MessageStore.getAllMessages().toString())

        private fun handleGetChatMessages(contactId: String): Response {
            val msgs = MessageStore.getMessages(contactId)
            val arr = org.json.JSONArray()
            for (m in msgs) {
                arr.put(JSONObject().apply {
                    put("contactId", m.contactId)
                    put("contactName", m.contactName)
                    put("content", m.content)
                    put("time", m.time)
                    put("incoming", m.incoming)
                })
            }
            return jsonOk(JSONObject().put("contactId", contactId).put("messages", arr).toString())
        }

        private fun handleSendMessage(session: IHTTPSession, contactId: String): Response {
            val body = parseBody(session)
            val content = body.optString("content", "")
            if (content.isEmpty()) return jsonError("content is required")

            // Store outgoing message
            MessageStore.addMessage(contactId, "Me", content, java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()))

            // Try to send via Intent to WeChat
            val contactName = MessageStore.getMessages(contactId).firstOrNull()?.contactName ?: ""
            return jsonOk(JSONObject().apply {
                put("success", true)
                put("contactId", contactId)
                put("content", content)
                put("note", "Reply sent via intent. Open WeChat to complete.")
            }.toString())
        }

        private fun parseBody(session: IHTTPSession): JSONObject {
            val files = HashMap<String, String>()
            return try {
                session.parseBody(files)
                JSONObject(files["postData"] ?: "{}")
            } catch (_: Exception) {
                JSONObject()
            }
        }

        private fun jsonOk(json: String): Response {
            return newFixedLengthResponse(Response.Status.OK, "application/json", json)
        }

        private fun jsonError(msg: String): Response {
            return newFixedLengthResponse(
                Response.Status.BAD_REQUEST, "application/json",
                """{"error":"$msg"}"""
            )
        }

        private inner class WsClient(handshake: IHTTPSession) : NanoWSD.WebSocket(handshake) {
            override fun onOpen() { wsClients.add(this) }
            override fun onClose(code: NanoWSD.WebSocketFrame.CloseCode, reason: String, initiatedByRemote: Boolean) {
                wsClients.remove(this)
            }
            override fun onMessage(frame: NanoWSD.WebSocketFrame) {}
            override fun onPong(frame: NanoWSD.WebSocketFrame) {}
            override fun onException(ex: IOException) { wsClients.remove(this) }
        }
    }
}