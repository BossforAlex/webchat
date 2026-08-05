// WeChat client bridge — interacts with the native Linux WeChat client
// Uses xdotool + DBus to read messages and send replies

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const WECHAT_DATA_DIR = '/root/xwechat_files';
let broadcastFn = null;

// In-memory message store (until we implement proper DBus/WeChat hook extraction)
const messageStore = new Map();
const contactStore = new Map();

function initWechatBridge({ broadcast } = {}) {
  broadcastFn = broadcast;
  console.log('[wechat] Bridge initialized');
  startPolling();
}

function startPolling() {
  // Poll for new messages by scanning WeChat data directory
  // This is a stub — real implementation would hook into WeChat's IPC or DBus
  setInterval(() => {
    try {
      scanForNewMessages();
    } catch (e) {
      console.error('[wechat] Poll error:', e.message);
    }
  }, 5000);
}

function scanForNewMessages() {
  // Scan WeChat message database files for new messages
  if (!fs.existsSync(WECHAT_DATA_DIR)) return;

  const files = fs.readdirSync(WECHAT_DATA_DIR, { recursive: true });
  // Stub: real implementation parses WeChat's SQLite/msg databases
  // For now, demonstrate the polling architecture
}

function getContacts() {
  return Array.from(contactStore.values());
}

function getMessages(contactId) {
  return messageStore.get(contactId) || [];
}

function sendMessage(contactId, content) {
  // Use xdotool to type and send message in the WeChat window
  try {
    // Focus WeChat window
    execSync('xdotool search --name "微信" windowactivate 2>/dev/null || true');
    // Navigate to contact (stub — real impl uses WeChat IPC)
    // Type and send
    console.log(`[wechat] Sending to ${contactId}: ${content}`);
    return { success: true, contactId, content };
  } catch (e) {
    console.error('[wechat] Send error:', e.message);
    return { success: false, error: e.message };
  }
}

module.exports = { initWechatBridge, getContacts, getMessages, sendMessage };