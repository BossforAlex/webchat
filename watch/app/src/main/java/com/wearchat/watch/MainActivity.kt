package com.wearchat.watch

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    companion object {
        val api = WechatApi()
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                connectBluetooth(findViewById<TextView>(R.id.status_text))
            } else {
                val statusText = findViewById<TextView>(R.id.status_text)
                statusText.text = "Permission denied"
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.message_list)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) permissions.add(Manifest.permission.BLUETOOTH_CONNECT)

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (permissions.isEmpty()) {
            connectBluetooth(findViewById<TextView>(R.id.status_text))
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun connectBluetooth(statusText: TextView) {
        api.connect { event ->
            runOnUiThread {
                when (event.type) {
                    "connected" -> {
                        statusText.text = "Connected"
                        statusText.setTextColor(0xFF4CAF50.toInt())
                        loadMessages()
                    }
                    "new_message" -> loadMessages()
                    "disconnected" -> {
                        statusText.text = "Disconnected. Reconnecting..."
                        statusText.setTextColor(0xFFFF5722.toInt())
                    }
                    "error" -> {
                        statusText.text = "Error: ${event.data}"
                        statusText.setTextColor(0xFFFF5722.toInt())
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
                Toast.makeText(this@MainActivity, "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        // Don't disconnect - other activities may use the connection
        super.onDestroy()
    }
}