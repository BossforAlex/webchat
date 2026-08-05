package com.wearchat.phone

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WechatAccessibility : AccessibilityService() {

    companion object {
        private const val TAG = "wearchat-acc"
        private const val WECHAT_PKG = "com.tencent.mm"
    }

    private val pendingReplies = mutableListOf<Pair<String, String>>()
    private var receiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val contactId = intent?.getStringExtra("contactId") ?: return
                val content = intent?.getStringExtra("content") ?: return
                pendingReplies.add(contactId to content)
                processNextReply()
            }
        }
        registerReceiver(receiver, IntentFilter(BluetoothServer.BROADCAST_REQUEST_REPLY))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName != WECHAT_PKG) return
        // Auto-process pending replies when WeChat window state changes
        if (pendingReplies.isNotEmpty()) {
            processNextReply()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        receiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    private fun processNextReply() {
        if (pendingReplies.isEmpty()) return
        val (contactId, content) = pendingReplies.removeAt(0)

        // Get the contact name from MessageStore
        val contactName = MessageStore.getMessages(contactId).firstOrNull()?.contactName ?: return

        Thread {
            try {
                sendWechatMessage(contactName, content)
            } catch (e: Exception) {
                Log.e(TAG, "Reply failed: ${e.message}")
            }
        }.start()
    }

    private fun sendWechatMessage(contactName: String, content: String) {
        // Step 1: Open WeChat
        openWeChat()

        // Step 2: Search for the contact
        Thread.sleep(800)
        searchContact(contactName)

        // Step 3: Open the chat
        Thread.sleep(600)
        openChatWithContact(contactName)

        // Step 4: Type the message
        Thread.sleep(500)
        typeMessage(content)

        // Step 5: Send
        Thread.sleep(300)
        clickSend()
    }

    private fun openWeChat() {
        // Use performGlobalAction BACK to ensure we're at launcher, then open WeChat
        performGlobalAction(GLOBAL_ACTION_BACK)
        Thread.sleep(300)

        val intent = packageManager.getLaunchIntentForPackage(WECHAT_PKG)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun searchContact(contactName: String) {
        val root = rootInActiveWindow ?: return

        // Look for search button or input field
        val searchNodes = findNodesByText(root, listOf("搜索", "Search"))
        if (searchNodes.isNotEmpty()) {
            searchNodes.first().performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Thread.sleep(400)

            // Find the search input
            val editTexts = findEditTexts(rootInActiveWindow)
            if (editTexts.isNotEmpty()) {
                val input = editTexts.first()
                input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                Thread.sleep(200)

                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, contactName)
                }
                input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                Thread.sleep(500)

                // Press search/enter
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun openChatWithContact(contactName: String) {
        val root = rootInActiveWindow ?: return

        // Find the contact in the list
        val contactNodes = findNodesByText(root, listOf(contactName))
        for (node in contactNodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
            // Try parent
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
                parent = parent.parent
            }
        }
    }

    private fun typeMessage(content: String) {
        val root = rootInActiveWindow ?: return

        // Find the message input field
        val editTexts = findEditTexts(root)
        if (editTexts.isEmpty()) {
            // Try clicking the input area first
            val inputAreas = findNodesByClass(root, "android.widget.EditText")
            if (inputAreas.isNotEmpty()) {
                inputAreas.first().performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(300)
            }
        }

        val inputs = findEditTexts(rootInActiveWindow)
        if (inputs.isNotEmpty()) {
            val input = inputs.first()
            input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            Thread.sleep(200)

            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content)
            }
            input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
    }

    private fun clickSend() {
        val root = rootInActiveWindow ?: return

        // Look for send button
        val sendButtons = findNodesByText(root, listOf("发送", "Send"))
        for (btn in sendButtons) {
            if (btn.isClickable) {
                btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }

        // Try to find by content description
        val descButtons = findNodesByDesc(root, listOf("发送", "send"))
        for (btn in descButtons) {
            if (btn.isClickable) {
                btn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    // --- Node search helpers ---

    private fun findNodesByText(node: AccessibilityNodeInfo?, texts: List<String>): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        if (node == null) return results
        val nodeText = node.text?.toString() ?: ""
        val nodeDesc = node.contentDescription?.toString() ?: ""
        if (texts.any { nodeText.contains(it) || nodeDesc.contains(it) }) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            results.addAll(findNodesByText(node.getChild(i), texts))
        }
        return results
    }

    private fun findNodesByDesc(node: AccessibilityNodeInfo?, descs: List<String>): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        if (node == null) return results
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (descs.any { desc.contains(it.lowercase()) }) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            results.addAll(findNodesByDesc(node.getChild(i), descs))
        }
        return results
    }

    private fun findNodesByClass(node: AccessibilityNodeInfo?, className: String): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        if (node == null) return results
        if (node.className?.toString() == className) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            results.addAll(findNodesByClass(node.getChild(i), className))
        }
        return results
    }

    private fun findEditTexts(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        if (node == null) return results
        if (node.className?.toString()?.contains("EditText") == true) {
            results.add(node)
        }
        for (i in 0 until node.childCount) {
            results.addAll(findEditTexts(node.getChild(i)))
        }
        return results
    }
}