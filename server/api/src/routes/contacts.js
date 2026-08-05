const express = require('express');
const router = express.Router();
const { getContacts } = require('../wechat');

// GET /api/contacts
router.get('/', (_req, res) => {
  const contacts = getContacts();
  res.json(contacts);
});

module.exports = router;