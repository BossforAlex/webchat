package com.wearchat.watch

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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

        connectBluetooth()
    }

    private fun connectBluetooth() {
        api.connect { event ->
            runOnUiThread {
                when (event.type) {
                    "connected" -> {
                        Toast.makeText(this@MainActivity, "蓝牙已连接", Toast.LENGTH_SHORT).show()
                        loadMessages()
                    }
                    "new_message" -> loadMessages()
                    "disconnected" -> {
                        Toast.makeText(this@MainActivity, "蓝牙断开，自动重连中...", Toast.LENGTH_SHORT).show()
                    }
                    "error" -> {
                        Toast.makeText(this@MainActivity, "连接失败: ${event.data}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadMessages() {
        scope.launch {
            try {
                val messages = api.getMessages()
                // Update RecyclerView adapter
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        api.disconnect()
        super.onDestroy()
    }
}