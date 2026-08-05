# wearchat

WeChat for Android watches — server-side Docker backend wrapping the native Linux WeChat client, with a Wear OS watch APK for message viewing and replying.

## Why this approach

| Approach | Risk |
|---|---|
| Web WeChat API wrapper ([weixin-watch](https://gitee.com/cqupt/weixin-watch)) | Web WeChat access is being restricted; most accounts cannot log in |
| Browser extension bypass ([wechat-need-web](https://github.com/lqzhgood/wechat-need-web)) | [Issue #43](https://github.com/lqzhgood/wechat-need-web/issues/43): accounts get warned/banned by Tencent |
| **Native Linux client in Docker ([docker-wechat](https://github.com/RICwang/docker-wechat)) + API server** | Uses the official WeChat Linux client — no account risk |

## Architecture

```
WeChat Servers <--> [Native Linux WeChat] <--> [wearchat-server API] <--> [wearchat-watch APK]
                    (Docker, arm64/amd64)       (REST + WebSocket)         (Wear OS)
```

## Project Structure

```
wearchat/
├── server/          # Docker-based WeChat backend + REST/WebSocket API
│   ├── Dockerfile
│   ├── Dockerfile-base
│   ├── api/         # Node.js API server
│   ├── scripts/     # Build & entrypoint scripts
│   └── docker-compose.yml
├── watch/           # Android Wear OS APK
│   └── app/src/     # Kotlin source
├── .github/workflows/
│   ├── docker-build.yml   # Multi-arch Docker image build
│   └── android-build.yml  # Wear OS APK build
└── docs/adr/        # Architecture Decision Records
```

## Quick Start

### Server

```bash
cd server
docker compose up -d
# Access at http://localhost:5800 (VNC) or ws://localhost:3000 (API)
```

### Watch APK

```bash
cd watch
./gradlew assembleDebug
# APK at watch/app/build/outputs/apk/debug/app-debug.apk
```

## API

| Endpoint | Method | Description |
|---|---|---|
| `/api/health` | GET | Server health check |
| `/api/contacts` | GET | List contacts |
| `/api/messages` | GET | List recent messages |
| `/api/messages/:contactId` | GET | Chat history with contact |
| `/api/messages/:contactId` | POST | Send message to contact |
| `/ws` | WebSocket | Real-time message push |

## References

- [weixin-watch](https://gitee.com/cqupt/weixin-watch) - Original Android watch WeChat (web API)
- [wechat-need-web](https://github.com/lqzhgood/wechat-need-web) - Browser extension to bypass web WeChat restrictions
- [docker-wechat](https://github.com/RICwang/docker-wechat) - Docker-based native Linux WeChat client

## License

Apache-2.0