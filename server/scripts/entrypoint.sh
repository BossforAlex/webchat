#!/bin/sh
# Entrypoint: start Xvfb, WeChat, and the API server
set -e

echo "Starting Xvfb on :99..."
Xvfb :99 -screen 0 "${DISPLAY_WIDTH:-1920}x${DISPLAY_HEIGHT:-1080}x24" &
sleep 1

echo "Starting fluxbox window manager..."
fluxbox &
sleep 1

echo "Starting WeChat Linux client..."
/usr/bin/wechat &
sleep 3

echo "Starting wearchat API server on port 3000..."
cd /opt/wearchat-api && node src/index.js