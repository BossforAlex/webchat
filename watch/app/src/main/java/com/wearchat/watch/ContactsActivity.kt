package com.wearchat.watch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import kotlinx.coroutines.*

class ContactsActivity : ComponentActivity() {

    private val api = WechatApi()
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        val recyclerView = findViewById<RecyclerView>(R.id.contact_list)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)

        loadContacts()
    }

    private fun loadContacts() {
        scope.launch {
            try {
                val contacts = api.getContacts()
                // Update RecyclerView adapter
                // On item click: startActivity(Intent(this, ChatActivity::class.java).putExtra("contact_id", id))
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}