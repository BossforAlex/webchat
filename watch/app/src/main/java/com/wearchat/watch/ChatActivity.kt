package com.wearchat.watch

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import kotlinx.coroutines.*

class ChatActivity : ComponentActivity() {

    private val api = MainActivity.api
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var contactId: String? = null
    private var contactName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactId = intent.getStringExtra("contact_id")
        contactName = intent.getStringExtra("contact_name")

        findViewById<TextView>(R.id.chat_title).text = contactName ?: "Chat"

        val recyclerView = findViewById<RecyclerView>(R.id.chat_messages)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)
        recyclerView.isEdgeItemsCenteringEnabled = true

        val input = findViewById<EditText>(R.id.message_input)
        val sendBtn = findViewById<Button>(R.id.send_btn)

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(input.text.toString())
                input.text.clear()
                true
            } else false
        }

        sendBtn.setOnClickListener {
            sendMessage(input.text.toString())
            input.text.clear()
        }

        loadChatHistory()
    }

    private fun loadChatHistory() {
        scope.launch {
            try {
                val messages = api.getChatMessages(contactId ?: return@launch)
                // Update RecyclerView adapter
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(content: String) {
        if (content.isBlank()) return
        scope.launch {
            try {
                api.sendMessage(contactId ?: return@launch, content)
                loadChatHistory()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Send failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}