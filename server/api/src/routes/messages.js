const express = require('express');
const router = express.Router();
const { getMessages, sendMessage } = require('../wechat');

// GET /api/messages — list recent messages across all chats
router.get('/', (_req, res) => {
  // Returns all messages grouped by contactId
  const allMessages = getMessages();
  res.json(allMessages);
});

// GET /api/messages/:contactId — chat history
router.get('/:contactId', (req, res) => {
  const msgs = getMessages(req.params.contactId);
  res.json({ contactId: req.params.contactId, messages: msgs });
});

// POST /api/messages/:contactId — send message
router.post('/:contactId', (req, res) => {
  const { content } = req.body;
  if (!content) {
    return res.status(400).json({ error: 'content is required' });
  }
  const result = sendMessage(req.params.contactId, content);
  res.json(result);
});

module.exports = router;