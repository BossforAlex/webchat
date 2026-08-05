package com.wearchat.watch

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import kotlinx.coroutines.*

class ChatActivity : ComponentActivity() {

    private val api = WechatApi()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var contactId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        contactId = intent.getStringExtra("contact_id")

        val recyclerView = findViewById<RecyclerView>(R.id.chat_messages)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)

        val input = findViewById<EditText>(R.id.message_input)
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(input.text.toString())
                input.text.clear()
                true
            } else false
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