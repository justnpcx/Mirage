# Mirage for Android

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)

基于 sing-box 的 Android 通用代理客户端。

A universal proxy client for Android, powered by sing-box.

> Mirage 由 [NekoBoxForAndroid](https://github.com/MatsuriDayo/NekoBoxForAndroid) 衍生而来，并持续独立演进。
> 内核维护在 [justnpcx/sing-box](https://github.com/justnpcx/sing-box) 的 `nb-next-1.14.x` 分支。

---

## 特性 / Features

### 内核 / Core

- **sing-box `1.14.x`**（由上游 `1.12.19` 升级而来）。

- **支持 Xray-core v26+ 的 REALITY 后量子 `X25519MLKEM768`**
  Xray-core 自 `v26.9.8` 起要求 REALITY 的 ClientHello 携带后量子混合密钥交换；上游 sing-box 会主动剥离，
  导致与新版 Xray 服务端（如 3x-ui）握手失败 (`reality verification failed`)。
  Mirage 依据订阅链接的 `support-x25519mlkem768=true` 决定是否保留，并在计算认证密钥时回退 `MlkemEcdhe`。

- **支持 xhttp（Xray `splithttp`）传输** —— sing-box 官方没有这个传输，我们把它实现在了内核里：

  | 模式 | 状态 |
  |---|---|
  | `stream-one` | ✅ |
  | `stream-up` | ✅ |
  | `downloadSettings`（上/下行分离到不同域名） | ✅ |
  | `packet-up` / `xmux` | ⏳ 规划中 |

  实现要点：必须走 **HTTP/2**（无 TLS 时用 **h2c prior-knowledge**），且请求路径必须以 `/` 结尾
  （Xray 默认把 session/seq 放在 path 上）。**已用真实 Xray 服务端做互操作验证。**

- **WireGuard 使用 endpoint**：sing-box `1.13` 起移除了 WireGuard outbound，Mirage 改为生成 `endpoints[]`。

- **DNS**：使用 sing-box 1.12+ 的新 DNS 格式；由于 sing-box 没有 DNS 故障转移，
  客户端会在连接前**探测**所配 DNS 并选用第一个可达的（失败开放 + 缓存，见 `DnsHealth`）。

### 架构 / Architecture

- **Kotlin Multiplatform 共享核心**（`android` + `jvm` 目标）：
  - `:core:model` —— 跨平台领域模型
  - `:core:subscription` —— **订阅 / 分享链接解析**：URI 解析、vless / vmess / trojan 字段映射
    （含 REALITY / ML-KEM / **xhttp**）、Shadowsocks 链接、Clash 选项解析。
    Android 端不再自行解析，全部委托给该模块。

- **Jetpack Compose + Material 3**：设计系统 `MirageTheme`（动态取色）；
  Home 已迁移到 **ViewModel + StateFlow** 的单向数据流；「关于」页也已改为 Compose。

- **Gradle 版本目录**（`gradle/libs.versions.toml`）统一管理版本。

### 质量 / Quality

- **静态检查**：detekt（`config/detekt/detekt.yml`，遗留问题用 baseline 固化，新问题会被 CI 拦下）。
- **单元测试**：共享核心为纯 Kotlin 测试（无需 Android）；Android 端用 Robolectric 覆盖 bean 映射与 sing-box JSON 契约。
- **CI**（`.github/workflows/build.yml`）：编译 libcore → 静态检查 + 全部单测 → 构建并签名 APK / AAB；推 `v*` tag 自动发布。
- **发布前验证**：用 sing-box 自身的解析器 + **真正启动 box**（`box.New` + `Start`）校验生成的配置，
  并对 xhttp 做**真实 Xray 服务端互操作测试**。

---

## 下载 / Downloads

- GitHub Releases：<https://github.com/justnpcx/Mirage/releases>
- 备用镜像（不依赖 GitHub CDN）：<https://edu.justnpc.com/local/>

包名 `com.justnpcx.mirage`，可与官方 NekoBox 共存；手机一般选 `arm64-v8a`。

---

## 支持的代理协议 / Supported Proxy Protocols

* SOCKS (4/4a/5) · HTTP(S) · SSH
* Shadowsocks · VMess · Trojan · VLESS
* AnyTLS · ShadowTLS · TUIC · Hysteria 1/2 · WireGuard
* Trojan-Go / NaïveProxy / Mieru（需插件）

请到[这里](https://matsuridayo.github.io/nb4a-plugin/)下载插件以获得完整的代理支持。

---

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
./gradlew app:bundleOssRelease     # .aab
```

> ⚠️ **Go 版本**：`gomobile` 的 `go.mod` 使用了 `godebug gotypesalias=0`，该 GODEBUG 在 **Go 1.27 被移除**，
> 因此编译内核必须使用 **Go 1.26.x**（CI 已固定）。

> ⚠️ **内核 commit**：`buildScript/lib/core/get_source_env.sh` 里的 `COMMIT_SING_BOX` 固定了 sing-box 的提交，
> 升级内核后需要同步更新。

### Baseline Profile（启动性能）

仓库包含 `:baselineprofile` 模块与 app 的 `benchmark` 构建类型。**生成需要连接真机**（无法在 CI 完成）：

```bash
./gradlew :app:generateOssBenchmarkBaselineProfile
```

---

## 致谢 / Credits

Core:

- [SagerNet/sing-box](https://github.com/SagerNet/sing-box)
- [MatsuriDayo/sing-box](https://github.com/MatsuriDayo/sing-box)

Android GUI:

- [MatsuriDayo/NekoBoxForAndroid](https://github.com/MatsuriDayo/NekoBoxForAndroid)
- [shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android)
- [SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)

Web Dashboard:

- [MetaCubeX/Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)

## License

GPL-3.0。本仓库是 NekoBoxForAndroid 的修改版本，原始版权归原作者所有，详见 `LICENSE`。
