# wearchat

微信消息在安卓手表上查看和回复 — 无需 Docker、无需服务器，手机装一个 APK 中转，手表连过来即可。

## 架构

```
[手机微信 App] → [phone APK: 通知监听 + 内嵌 HTTP 服务器] ←→ [watch APK: Wear OS 客户端]
                     NotificationListenerService                REST/WebSocket
                     NanoHTTPd :8765
```

不走网页版 API（已封堵），不走 Docker（太重），直接利用手机本地微信 App 的通知来获取消息。

## 为什么不用 Docker

网页版微信 API 已被封堵，Docker 方案需要额外部署桌面 Linux 微信客户端。新方案利用手机本地微信 App 的通知监听，零部署、零封号风险。

## 项目结构

```
wearchat/
├── phone/                # 手机端 APK（通知监听 + HTTP 服务器）
│   ├── src/main/java/com/wearchat/phone/
│   │   ├── NotificationListener.kt   # 监听微信通知
│   │   ├── ApiServer.kt             # NanoHTTPd 内嵌服务器
│   │   ├── MessageStore.kt          # 消息存储
│   │   └── MainActivity.kt          # 显示 IP 和状态
│   └── build.gradle.kts
├── watch/                # 手表端 APK（Wear OS）
│   └── app/src/main/java/com/wearchat/watch/
│       ├── api/WechatApi.kt        # 连接手机 API
│       ├── MainActivity.kt         # 消息列表
│       ├── ContactsActivity.kt     # 联系人列表
│       └── ChatActivity.kt         # 聊天界面
└── .github/workflows/
    └── android-build.yml  # 同时编译 phone 和 watch APK
```

## 快速开始

### 1. 安装手机 APK

下载 `wearchat-phone-debug.apk`，安装到手机。

打开应用后：
- 点击「开启通知监听权限」，允许 wearchat 读取微信通知
- 记下显示的 IP 地址（如 `192.168.1.100`）

### 2. 安装手表 APK

下载 `wearchat-watch-debug.apk`，通过 ADB 安装到手表：

```bash
adb install wearchat-watch-debug.apk
```

### 3. 配置连接

修改手表端代码中的 `WechatApi` 默认 IP 地址为手机 IP，或在手表应用设置中配置。

### 本地编译

```bash
# 编译手机端
cd phone && ./gradlew assembleDebug

# 编译手表端
cd watch && ./gradlew assembleDebug
```

## API

手机端在 8765 端口提供以下接口（手表连接用）：

| Endpoint | Method | Description |
|---|---|---|
| `/api/health` | GET | 服务器状态 |
| `/api/contacts` | GET | 联系人列表 |
| `/api/messages` | GET | 所有消息 |
| `/api/messages/:contactId` | GET | 聊天记录 |
| `/api/messages/:contactId` | POST | 发送消息 |
| `/ws` | WebSocket | 实时推送 |

## 注意事项

- 手机和手表需要在同一局域网
- 手机需要开启通知监听权限（设置 → 通知使用权）
- 部分手机系统可能需要在后台保活设置中允许 wearchat 运行

## License

Apache-2.0