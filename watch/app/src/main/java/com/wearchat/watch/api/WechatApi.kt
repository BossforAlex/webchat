package com.wearchat.watch

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class WsEvent(val type: String, val data: String)

class WechatApi {

    companion object {
        private val SERVICE_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    }

    private var socket: BluetoothSocket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var connected = false
    private var eventListener: ((WsEvent) -> Unit)? = null
    private val requestId = AtomicInteger(0)
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun isConnected(): Boolean = connected

    fun connect(onEvent: (WsEvent) -> Unit) {
        eventListener = onEvent
        scope.launch {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                if (adapter == null || !adapter.isEnabled) {
                    onEvent(WsEvent("error", "Bluetooth not available"))
                    return@launch
                }

                // Find paired device named "wearchat" or connect to first paired phone
                val device = findPairedDevice(adapter) ?: run {
                    onEvent(WsEvent("error", "No paired phone found. Pair with phone first."))
                    return@launch
                }

                val btSocket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                adapter.cancelDiscovery()
                btSocket.connect()

                socket = btSocket
                writer = OutputStreamWriter(btSocket.outputStream)
                reader = BufferedReader(InputStreamReader(btSocket.inputStream))
                connected = true
                onEvent(WsEvent("connected", device.name ?: "unknown"))

                // Start reading responses and events
                readLoop()
            } catch (e: Exception) {
                connected = false
                onEvent(WsEvent("error", "Connection failed: ${e.message}"))
                // Retry after delay
                delay(5000)
                connect(onEvent)
            }
        }
    }

    private fun findPairedDevice(adapter: BluetoothAdapter): BluetoothDevice? {
        for (device in adapter.bondedDevices) {
            // Try to connect to each paired device
            return device
        }
        return null
    }

    private suspend fun readLoop() {
        val r = reader ?: return
        try {
            var line: String?
            while (r.readLine().also { line = it } != null) {
                val json = JSONObject(line!!)
                val type = json.optString("type")
                when (type) {
                    "response" -> {
                        val id = json.optString("id")
                        pendingRequests.remove(id)?.complete(json.optJSONObject("result") ?: JSONObject())
                    }
                    "event" -> {
                        val event = json.optString("event")
                        val data = json.optString("data", "")
                        withContext(Dispatchers.Main) {
                            eventListener?.invoke(WsEvent(event, data))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            connected = false
            withContext(Dispatchers.Main) {
                eventListener?.invoke(WsEvent("disconnected", e.message ?: "Connection lost"))
            }
        }
    }

    private suspend fun request(method: String, params: JSONObject = JSONObject()): JSONObject {
        if (!connected) throw IllegalStateException("Not connected")

        val id = "req-${requestId.incrementAndGet()}"
        val req = JSONObject().apply {
            put("type", "request")
            put("id", id)
            put("method", method)
            put("params", params)
        }.toString() + "\n"

        val deferred = CompletableDeferred<JSONObject>()
        pendingRequests[id] = deferred

        writer?.write(req)
        writer?.flush()

        return withTimeout(10000) { deferred.await() }
    }

    // --- Public API ---

    suspend fun getContacts(): List<JSONObject> = withContext(Dispatchers.IO) {
        val result = request("getContacts")
        val arr = result.optJSONArray("contacts") ?: JSONArray()
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun getMessages(): List<JSONObject> = withContext(Dispatchers.IO) {
        val result = request("getMessages")
        val arr = result.optJSONArray("messages") ?: JSONArray()
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun getChatMessages(contactId: String): List<JSONObject> = withContext(Dispatchers.IO) {
        val params = JSONObject().put("contactId", contactId)
        val result = request("getChatMessages", params)
        val arr = result.optJSONArray("messages") ?: JSONArray()
        (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    suspend fun sendMessage(contactId: String, content: String): JSONObject = withContext(Dispatchers.IO) {
        val params = JSONObject().apply {
            put("contactId", contactId)
            put("content", content)
        }
        request("sendMessage", params)
    }

    fun disconnect() {
        connected = false
        scope.cancel()
        try { reader?.close() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        writer = null
        reader = null
        pendingRequests.clear()
    }
}