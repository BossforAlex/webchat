package com.wearchat.phone

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BluetoothServer : Service() {

    companion object {
        private const val TAG = "wearchat-bt"
        private val SERVICE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        const val SERVICE_NAME = "wearchat"
        const val BROADCAST_NEW_MESSAGE = "com.wearchat.NEW_MESSAGE"
        const val BROADCAST_REQUEST_REPLY = "com.wearchat.REQUEST_REPLY"

        private var serverThread: AcceptThread? = null
        private val clients = ConcurrentHashMap<String, BluetoothSocket>()

        fun broadcastEvent(event: String, data: String) {
            val payload = JSONObject().apply {
                put("type", "event")
                put("event", event)
                put("data", data)
            }.toString() + "\n"
            for ((_, socket) in clients) {
                try { socket.outputStream.write(payload.toByteArray()) } catch (_: Exception) {}
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServer()
        return START_STICKY
    }

    private fun startServer() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Log.e(TAG, "Bluetooth not available")
            return
        }
        if (!adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not enabled")
            return
        }

        if (serverThread != null) return

        try {
            val serverSocket = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
            serverThread = AcceptThread(serverSocket)
            serverThread?.start()
            Log.i(TAG, "Bluetooth server started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Bluetooth server: ${e.message}")
        }
    }

    override fun onDestroy() {
        serverThread?.cancel()
        serverThread = null
        for ((_, socket) in clients) {
            try { socket.close() } catch (_: Exception) {}
        }
        clients.clear()
        super.onDestroy()
    }

    private inner class AcceptThread(private val serverSocket: BluetoothServerSocket) : Thread() {
        private var running = true

        override fun run() {
            while (running) {
                try {
                    val socket = serverSocket.accept()
                    val device = socket.remoteDevice
                    Log.i(TAG, "Accepted connection from ${device.name} (${device.address})")
                    clients[device.address] = socket
                    ClientThread(socket, device.address).start()
                } catch (e: Exception) {
                    if (running) Log.e(TAG, "Accept error: ${e.message}")
                    break
                }
            }
        }

        fun cancel() {
            running = false
            try { serverSocket.close() } catch (_: Exception) {}
        }
    }

    private inner class ClientThread(
        private val socket: BluetoothSocket,
        private val address: String
    ) : Thread() {
        override fun run() {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                val writer = OutputStreamWriter(socket.outputStream)
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    handleRequest(line!!, writer)
                }
            } catch (e: Exception) {
                Log.d(TAG, "Client disconnected: $address")
            } finally {
                clients.remove(address)
                try { socket.close() } catch (_: Exception) {}
            }
        }

        private fun handleRequest(json: String, writer: OutputStreamWriter) {
            try {
                val req = JSONObject(json)
                val method = req.optString("method")
                val id = req.optString("id", "0")
                val params = req.optJSONObject("params") ?: JSONObject()

                val result = when (method) {
                    "getContacts" -> MessageStore.getContacts()
                    "getMessages" -> MessageStore.getAllMessages()
                    "getChatMessages" -> {
                        val contactId = params.optString("contactId")
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
                        JSONObject().put("contactId", contactId).put("messages", arr)
                    }
                    "sendMessage" -> {
                        val contactId = params.optString("contactId")
                        val content = params.optString("content")
                        // Trigger AccessibilityService to send the reply
                        val intent = Intent(BROADCAST_REQUEST_REPLY).apply {
                            putExtra("contactId", contactId)
                            putExtra("content", content)
                        }
                        sendBroadcast(intent)
                        JSONObject().put("success", true).put("note", "Reply queued")
                    }
                    "health" -> JSONObject().put("status", "ok").put("type", "bluetooth")
                    else -> JSONObject().put("error", "Unknown method: $method")
                }

                val response = JSONObject().apply {
                    put("type", "response")
                    put("id", id)
                    put("result", result)
                }.toString() + "\n"
                writer.write(response)
                writer.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Handle error: ${e.message}")
            }
        }
    }
}