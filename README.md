# Mirage for Android

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)

基于 sing-box 的 Android 通用代理客户端。

A universal proxy client for Android, powered by sing-box.

> Mirage 由 [NekoBoxForAndroid](https://github.com/MatsuriDayo/NekoBoxForAndroid) 衍生而来，并持续独立演进。

## 特性 / Features

### 内核 / Core

- **sing-box `1.14.x`**（由上游的 `1.12.19` 升级）。内核补丁维护在
  [justnpcx/sing-box](https://github.com/justnpcx/sing-box) 的 `nb-next-1.14.x` 分支。

- **支持 Xray-core v26+ 的 REALITY（后量子 `X25519MLKEM768`）**：
  Xray-core 自 `v26.9.8` 起要求 REALITY 的 ClientHello 携带后量子混合密钥交换 `X25519MLKEM768`。
  上游 sing-box 会主动剥离该 key share，导致与运行新版 Xray 内核（如 3x-ui 的 `v26.10.10`）的服务端握手失败
  (`reality verification failed`)。Mirage 依据订阅链接中的 `support-x25519mlkem768=true` 决定是否启用，
  并在计算 REALITY 认证密钥时回退到 `MlkemEcdhe`（行为与 Xray-core / mihomo 一致）。

- **WireGuard 使用 endpoint**：sing-box `1.13` 起移除了 WireGuard **outbound**。
  Mirage 生成 `endpoints[]`（单 peer、`allowed_ips: 0.0.0.0/0, ::/0`）。
  由于 endpoint 在 sing-box 中同样按 tag 寻址，selector 与路由规则无需改动。

### 架构 / Architecture

- **Kotlin Multiplatform 共享核心**（`android` + `jvm` 目标）：
  - `:core:model` —— 跨平台领域模型（kotlinx.serialization）
  - `:core:subscription` —— **订阅链接解析**：
    `ProxyUriParser`（无依赖的 URI 解析，语义对齐 okhttp）+
    `V2RayUri`（vless / vmess / trojan → 传输与 TLS 选项，含 REALITY / ML-KEM）+
    `ShadowsocksUri`（ss-android / v2rayN 两种 `ss://` 链接）。
    Android 端不再自己解析，而是委托给该模块，因此同一套逻辑可在其它平台复用。
- **Jetpack Compose + Material 3**：已接入（Kotlin 2.0 Compose 编译器插件 + BOM + 动态取色），
  `ui/compose/` 下有设计系统（`MirageTheme`）与 **Home/Dashboard** 竖切（抽屉 `nav_home`）。
  该页已迁移到 **ViewModel + StateFlow** 的单向数据流（`DashboardViewModel`），
  旧 View 栈与新 Compose 栈在迁移期内共存。
- **全局单例注入化（起步）**：`BuildSettings`（`io.nekohasekai.sagernet.fmt`）把配置构建器需要的全局偏好
  （如 `globalAllowInsecure`）从 `DataStore` 中抽离出来，默认实现为 `DataStoreBuildSettings`，测试可注入假实现。
- **Gradle 版本目录**（`gradle/libs.versions.toml`）统一管理版本。

### 质量 / Quality

- **静态检查**：detekt（配置见 `config/detekt/detekt.yml`，遗留问题用 baseline 固化，新问题会被 CI 拦下）。
- **单元测试**：共享核心为纯 Kotlin 测试（无需 Android）；Android 端用 Robolectric 覆盖 bean 映射与 sing-box JSON 契约。
- **CI**（`.github/workflows/build.yml`）：编译 libcore → 静态检查 + 全部单测 → 构建并签名 APK；推 `v*` tag 自动发布 Release。

## REALITY / X25519MLKEM768

分享链接示例：

```
vless://<uuid>@host:443?type=tcp&encryption=none&security=reality&flow=xtls-rprx-vision\
&sni=intel.com&fp=chrome&pbk=<public-key>&sid=<short-id>&support-x25519mlkem768=true#node
```

链接带 `support-x25519mlkem768=true` 时，客户端会在 ClientHello 中保留 `X25519MLKEM768` 混合 key share；
否则维持旧行为（剥离），以兼容旧版 REALITY 服务端。

## 下载 / Downloads

[GitHub Releases](https://github.com/justnpcx/Mirage/releases) —— 包名 `com.justnpcx.mirage`，可与官方 NekoBox 共存；
手机一般选 `arm64-v8a`。

## 支持的代理协议 / Supported Proxy Protocols

* SOCKS (4/4a/5)
* HTTP(S)
* SSH
* Shadowsocks
* VMess
* Trojan
* VLESS
* AnyTLS
* ShadowTLS
* TUIC
* Hysteria 1/2
* WireGuard
* Trojan-Go (trojan-go-plugin)
* NaïveProxy (naive-plugin)
* Mieru (mieru-plugin)

请到[这里](https://matsuridayo.github.io/nb4a-plugin/)下载插件以获得完整的代理支持。

Please visit [here](https://matsuridayo.github.io/nb4a-plugin/) to download plugins for full proxy
supports.

## 支持的订阅格式 / Supported Subscription Format

* 一些广泛使用的格式 (如 Shadowsocks, ClashMeta 和 v2rayN)
* sing-box 出站

仅支持解析出站，即节点。分流规则等信息会被忽略。

* Some widely used formats (like Shadowsocks, ClashMeta and v2rayN)
* sing-box outbound

Only resolving outbound, i.e. nodes, is supported. Information such as diversion rules are ignored.

## 构建 / Building

```bash
# 1) 编译内核（需要 Go 1.26.x + Android NDK 25.0.8775105）
JAVA_HOME=/path/to/jdk17 \
PATH=$JAVA_HOME/bin:$GOPATH/bin:$PATH \
ANDROID_HOME=/path/to/android-sdk \
ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.0.8775105 \
./run lib core

# 2) 静态检查 + 单测
./gradlew :app:detekt :core:model:detekt :core:subscription:detekt \
          :core:model:allTests :core:subscription:allTests :app:testOssReleaseUnitTest

# 3) 构建 APK / App Bundle
./gradlew app:assembleOssRelease   # 分 ABI 的 APK
./gradlew app:bundleOssRelease     # .aab（用于商店分发）
```

### Baseline Profile（启动性能，可选）

应用已依赖 `androidx.profileinstaller`，只要产出 profile 即会在安装时生效。
生成需要**连接真机或模拟器**（生成过程会实际启动应用）：

```bash
# 需在 app 模块中先声明一个 benchmark 构建类型并加入 :baselineprofile 测试模块
./gradlew :app:generateBaselineProfile
```

该模块尚未随仓库提供（生成依赖设备，无法在 CI 里完成）；在设备上生成后，
profile 会落到 `app/src/<variant>/generated/baselineProfiles/`。

> ⚠️ **Go 版本**：`gomobile` 的 `go.mod` 使用了 `godebug gotypesalias=0`，该 GODEBUG 在 **Go 1.27 被移除**，
> 因此编译内核必须使用 **Go 1.26.x**（CI 已固定）。

签名使用仓库内的 `release.keystore`（自签名，凭据见 `keystore.properties`，CI 通过 `LOCAL_PROPERTIES` 传入）。

## Credits

Core:

- [SagerNet/sing-box](https://github.com/SagerNet/sing-box)
- [MatsuriDayo/sing-box](https://github.com/MatsuriDayo/sing-box)

Android GUI:

- [MatsuriDayo/NekoBoxForAndroid](https://github.com/MatsuriDayo/NekoBoxForAndroid)
- [shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android)
- [SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)

Web Dashboard:

- [Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)
