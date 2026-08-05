package com.wearchat.phone

import org.json.JSONArray
import org.json.JSONObject

object MessageStore {
    private val contacts = LinkedHashMap<String, ContactInfo>()
    private val messages = LinkedHashMap<String, MutableList<Message>>()

    data class ContactInfo(
        val id: String,
        val name: String,
        var lastMessage: String = "",
        var lastTime: String = ""
    )

    data class Message(
        val contactId: String,
        val contactName: String,
        val content: String,
        val time: String,
        val incoming: Boolean = true
    )

    @Synchronized
    fun addMessage(contactId: String, contactName: String, content: String, time: String) {
        contacts[contactId] = ContactInfo(contactId, contactName, content, time)
        messages.getOrPut(contactId) { mutableListOf() }
            .add(Message(contactId, contactName, content, time))
    }

    @Synchronized
    fun getContacts(): JSONArray {
        val arr = JSONArray()
        for (c in contacts.values) {
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("lastMessage", c.lastMessage)
                put("lastTime", c.lastTime)
            })
        }
        return arr
    }

    @Synchronized
    fun getMessages(contactId: String): List<Message> {
        return messages[contactId] ?: emptyList()
    }

    @Synchronized
    fun getAllMessages(): JSONArray {
        val arr = JSONArray()
        for ((contactId, msgs) in messages) {
            for (m in msgs) {
                arr.put(JSONObject().apply {
                    put("contactId", m.contactId)
                    put("contactName", m.contactName)
                    put("content", m.content)
                    put("time", m.time)
                    put("incoming", m.incoming)
                })
            }
        }
        return arr
    }
}