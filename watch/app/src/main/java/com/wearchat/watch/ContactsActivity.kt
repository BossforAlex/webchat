package com.wearchat.watch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import kotlinx.coroutines.*

class ContactsActivity : ComponentActivity() {

    private val api = MainActivity.api
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        val recyclerView = findViewById<RecyclerView>(R.id.contact_list)
        val layoutManager = WearableLinearLayoutManager(this)
        layoutManager.isEdgeItemsCenteringEnabled = true
        recyclerView.layoutManager = layoutManager

        loadContacts()
    }

    private fun loadContacts() {
        scope.launch {
            try {
                val contacts = api.getContacts()
                // Update RecyclerView adapter
                // On item click: navigate to chat
            } catch (e: Exception) {
                Toast.makeText(this@ContactsActivity, "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}