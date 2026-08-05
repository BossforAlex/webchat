# ADR 002: 从 Docker 服务端方案迁移到手机中转方案

## 状态

已采纳

## 背景

原方案（ADR 001）使用 Docker 部署 Linux 微信客户端 + Node.js API 服务器，手表通过 HTTP/WebSocket 连接。用户反馈 Docker 部署繁琐，希望一个 APK 就能运行。

## 决策

**将服务端从 Docker 迁移到手机本地 APK。**

新架构：手机安装 phone APK，通过 Android `NotificationListenerService` 监听微信通知，内嵌 NanoHTTPd 轻量 HTTP 服务器，手表连接手机 IP 即可获取消息。

## 技术选型

- **通知监听**: Android `NotificationListenerService`，过滤 `com.tencent.mm` 包名
- **HTTP 服务器**: NanoHTTPd 2.3.1（内嵌，零依赖，~100KB）
- **WebSocket**: NanoHTTPd WebSocket 扩展，支持实时消息推送
- **消息存储**: 内存存储（`LinkedHashMap`），应用重启后清空
- **端口**: 8765

## 效果

| 对比维度 | Docker 方案 | 手机中转方案 |
|---|---|---|
| 部署 | 需要 Docker 环境 | 安装 APK 即可 |
| 依赖 | Docker + Node.js + Linux 微信客户端 | 仅 Android 手机 |
| 架构 | 外部服务器 | 手机本地 |
| 封号风险 | 无（桌面客户端） | 无（手机微信 App） |
| 消息获取 | 桌面微信客户端 | 通知监听 |

## 后果

- 手表和手机必须在同一局域网
- 手机需开启通知监听权限
- 回复消息功能受限（需通过 Intent 跳转微信，或使用 AccessibilityService 自动化）
- 消息内容仅限通知栏显示的文字，无法获取完整聊天记录