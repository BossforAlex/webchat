package com.wearchat.watch

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private val api = WechatApi()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.message_list)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)
        // TODO: set adapter with message data

        loadMessages()
        connectWebSocket()
    }

    private fun loadMessages() {
        scope.launch {
            try {
                val messages = api.getMessages()
                // Update RecyclerView adapter
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectWebSocket() {
        scope.launch {
            api.connectWebSocket { event ->
                runOnUiThread {
                    when (event.type) {
                        "new_message" -> loadMessages()
                        "login_status" -> {
                            Toast.makeText(this@MainActivity, "Login status: ${event.data}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        api.disconnect()
        super.onDestroy()
    }
}