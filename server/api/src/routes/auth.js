const express = require('express');
const router = express.Router();

// GET /api/auth/status — check if WeChat is logged in
router.get('/status', (_req, res) => {
  // Stub: check if WeChat process is running and logged in
  res.json({ loggedIn: true, method: 'qr-code' });
});

// GET /api/auth/qrcode — get QR code for login (stub)
router.get('/qrcode', (_req, res) => {
  res.json({ qrcodeUrl: '/api/auth/qrcode-image' });
});

module.exports = router;