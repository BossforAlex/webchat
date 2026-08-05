# ADR-001: Architecture Decision — Native Linux WeChat Client + API Server

**Status:** Accepted  
**Date:** 2026-08-05  
**Decision:** Use the native Linux WeChat client (via Docker) as the backend, with a REST + WebSocket API server, and a Wear OS APK as the frontend client.

---

## Context

We need to build a WeChat client for Android watches that supports viewing and replying to messages. Three existing approaches were evaluated:

1. **weixin-watch** ([gitee.com/cqupt/weixin-watch](https://gitee.com/cqupt/weixin-watch)) — wraps the web WeChat API (`wx.qq.com`) in an Android WebView. Simple but fragile: most accounts cannot log into web WeChat anymore.

2. **wechat-need-web** ([github.com/lqzhgood/wechat-need-web](https://github.com/lqzhgood/wechat-need-web)) — Chrome/Firefox extension that bypasses web WeChat access restrictions. [Issue #43](https://github.com/lqzhgood/wechat-need-web/issues/43) confirms Tencent warns/bans accounts using this approach.

3. **docker-wechat** ([github.com/RICwang/docker-wechat](https://github.com/RICwang/docker-wechat)) — runs the official native Linux WeChat client in Docker with VNC/web access. Uses the real Linux client, not the web version. Supports amd64 and arm64 multi-arch builds via GitHub Actions.

## Forces

| Force | Weight | Rationale |
|---|---|---|
| Account safety | Critical | Web WeChat approaches risk account warnings or bans (confirmed by Issue #43) |
| API stability | High | The native Linux client is maintained by Tencent; web WeChat APIs are deprecated |
| Multi-architecture | High | Android watches are arm64; server can be amd64 or arm64 |
| Message reliability | Medium | Must deliver messages to the watch reliably |
| Build automation | Medium | GitHub Actions CI/CD for reproducible builds |
| Offline resilience | Low | Watch may lose connection; local cache is a nice-to-have |

## Decision

**Use the native Linux WeChat client (docker-wechat-based) as the backend, with a lightweight REST + WebSocket API server, and a Wear OS APK as the frontend.**

### Architecture

```
WeChat Servers <--> [Native Linux WeChat Client] <--> [wearchat-server API] <--> [wearchat-watch APK]
                    (Docker, arm64/amd64)              (REST + WebSocket)        (Wear OS)
```

### Bounded Contexts

| Context | Responsibility | Model | Relationships |
|---|---|---|---|
| WeChat Client | Native WeChat Linux process, login session, message sync | WeChat internal model | Upstream of API Gateway |
| API Gateway | REST + WebSocket server, message polling, send commands | JSON message/contact models | Anti-corruption layer between WeChat Client and Watch Client |
| Watch Client | Wear OS UI, message rendering, voice/keyboard input | Watch-optimized UI models | Downstream of API Gateway |

### Interaction Style

- **Synchronous REST** for contacts, message history, and sending messages
- **WebSocket push** for real-time new message notifications
- Avoids polling from the watch (battery-sensitive device)

## Alternatives Considered

| Alternative | Rejected Because |
|---|---|
| **A: Direct web WeChat API wrapper** (weixin-watch approach) | Web WeChat is gate-kept; most accounts cannot log in. High risk of account restriction. |
| **B: Browser extension to bypass restrictions** (wechat-need-web) | Issue #43 confirms accounts get warned/banned. Unsustainable. |
| **C: Native WeChat Android app on watch** | WeChat does not provide a Wear OS version. Sideloading the phone APK is not viable on most watches. |
| **D: SSH tunnel to a phone running WeChat** | Requires a phone always online; complex setup; not a standalone solution. |

## Consequences

### Positive
- **Account safety**: Uses the official Linux WeChat client — no risk of warnings or bans
- **Multi-arch**: Docker images for both amd64 (server) and arm64 (edge devices)
- **CI/CD**: GitHub Actions builds both Docker images and Android APK automatically
- **Extensible API**: REST + WebSocket API can serve other clients (web, desktop, etc.)
- **Offline-capable**: Watch APK can cache messages locally

### Negative
- **Server dependency**: Requires running the Docker backend (server or home NAS)
- **Resource overhead**: Docker container needs ~1-2 GB RAM for the GUI + WeChat client
- **Session persistence**: WeChat login sessions need to be maintained; re-login requires QR code scan
- **Latency**: Network round-trip between watch and server adds latency

## Reversibility

**Cost to undo**: Medium. The API server and watch APK are loosely coupled — the server backend could be swapped for a different WeChat access method without changing the watch client.

**Reconsideration trigger**: If Tencent releases an official Wear OS WeChat client, or if a headless WeChat protocol becomes available, the Docker-based approach can be replaced.

## Fitness Functions

| Property | Metric | Threshold | Measurement | Cadence |
|---|---|---|---|---|
| Message delivery latency | Time from WeChat receipt to WebSocket push | < 5 seconds | Server-side timing | Per deployment |
| API availability | HTTP 200 on /api/health | 99% uptime | Health check probe | Every 30s |
| APK build success | CI job status | Pass on every push to main | GitHub Actions | Per push |
| Multi-arch image build | Docker manifest for amd64 + arm64 | Both architectures present | Docker buildx inspect | Per build |

## Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| WeChat Linux client update breaks compatibility | Medium | High | Daily CI checks for new versions; pin working version tag |
| Docker daemon not available on target device | Low | Medium | Provide pre-built binaries; support running API server directly on Linux |
| WeChat session expires frequently | Medium | Medium | Session heartbeat; notify watch user to re-scan QR code |
| Network latency degrades UX | Low | Medium | Local message cache on watch; optimistic UI updates |

## Responsibility

- **Owner**: User + local checks (this is a solo/personal project)
- **Check path**: `docs/adr/001-architecture-decision.md`
- **Review**: After first working end-to-end message send/receive

## Follow-up Decisions

1. **[ADR-002]** WeChat message extraction strategy — DBus hook vs. xdotool automation vs. filesystem polling
2. **[ADR-003]** Watch APK authentication and pairing with the server