package com.wearchat.phone

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.status_text)
        val ipText = findViewById<TextView>(R.id.ip_text)
        val enableBtn = findViewById<Button>(R.id.enable_btn)

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter?.isEnabled == true) {
            statusText.text = "蓝牙服务运行中"
            ipText.text = "设备名: ${adapter.name}\nUUID: wearchat"
        } else {
            statusText.text = "蓝牙未开启"
            ipText.text = "请先开启蓝牙"
        }

        // Start Bluetooth server
        startService(Intent(this, BluetoothServer::class.java))

        enableBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.accessibility_btn).apply {
            visibility = android.view.View.VISIBLE
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
    }
}