package com.wearchat.phone

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Bundle
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.tencent.mm") return

        val title = sbn.notification.extras.getString("android.title") ?: return
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: return
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // Skip system messages (WeChat login, etc.)
        if (title.contains("微信") && text.contains("登录")) return
        if (title in listOf("Android System", "WeChat")) return

        MessageStore.addMessage(
            contactId = title.hashCode().toString(),
            contactName = title,
            content = text,
            time = time
        )

        // Notify Bluetooth clients
        val data = JSONObject().apply {
            put("contact", title)
            put("content", text)
            put("time", time)
        }.toString()
        BluetoothServer.broadcastEvent("new_message", data)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No action needed
    }
}