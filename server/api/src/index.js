const express = require('express');
const http = require('http');
const { WebSocketServer } = require('ws');
const cors = require('cors');

const messages = require('./routes/messages');
const contacts = require('./routes/contacts');
const auth = require('./routes/auth');
const { initWechatBridge } = require('./wechat');

const app = express();
const server = http.createServer(app);
const wss = new WebSocketServer({ server, path: '/ws' });

app.use(cors());
app.use(express.json());

// REST API routes
app.get('/api/health', (_req, res) => res.json({ status: 'ok', timestamp: Date.now() }));
app.use('/api/contacts', contacts);
app.use('/api/messages', messages);
app.use('/api/auth', auth);

// WebSocket — real-time message push
const clients = new Set();

wss.on('connection', (ws) => {
  clients.add(ws);
  ws.on('close', () => clients.delete(ws));
});

function broadcast(event, data) {
  const payload = JSON.stringify({ event, data });
  for (const ws of clients) {
    if (ws.readyState === 1) ws.send(payload);
  }
}

// Initialize WeChat bridge
initWechatBridge({ broadcast });

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`wearchat API server listening on port ${PORT}`);
});