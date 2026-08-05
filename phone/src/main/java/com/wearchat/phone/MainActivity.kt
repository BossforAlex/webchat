package com.wearchat.phone

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.status_text)
        val ipText = findViewById<TextView>(R.id.ip_text)
        val enableBtn = findViewById<Button>(R.id.enable_btn)

        // Show local IP
        ipText.text = "手机 IP: ${getLocalIpAddress()}\n端口: 8765"

        // Start API server
        startService(Intent(this, ApiServer::class.java))
        statusText.text = "服务器运行中"

        enableBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                val addresses = ni.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is InetAddress && addr.address.size == 4) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (_: Exception) {}
        return "0.0.0.0"
    }
}