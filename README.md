# Mirage for Android

[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)

基于 **sing-box 1.14** 的 Android 通用代理客户端。

A universal proxy client for Android, powered by a self-maintained sing-box 1.14 kernel.

## 下载 / Download

<https://github.com/justnpcx/Mirage/releases>

包名 `com.justnpcx.mirage`，可与官方 NekoBox 共存；手机一般选 `arm64-v8a`。

## 特性 / Features

- **内核走在前面** —— 内核是自维护的 [justnpcx/sing-box](https://github.com/justnpcx/sing-box)
  fork（分支 `nb-next-1.14.x`），基于 sing-box `1.14.x`。上游还没发版、甚至还没实现的能力，
  可以先落到客户端。
- **XHTTP 传输** —— sing-box 官方**没有**这个传输。已在 fork 内核中实现，支持
  `stream-one` / `stream-up` / `downloadSettings`（上下行可分流到不同服务器）。
- **REALITY 后量子** —— 支持 Xray-core v26+ 要求的 `X25519MLKEM768`。缺了它的客户端与新版
  Xray 服务端（例如 3x-ui）握手会直接失败。
- **DNS 健康检查** —— sing-box 本身没有 DNS 故障转移。客户端在连接前探测所配 DNS，选用首个
  可达的（失败开放 + 结果缓存），避免某个 DNS 挂掉就整体连不上。
- **共享核心** —— 订阅与分享链接解析全部下沉到 Kotlin Multiplatform 的 `:core:model` /
  `:core:subscription`，Android 端不再自行解析，同一份逻辑可跨平台测试。
- **界面** —— Jetpack Compose + Material 3，单向数据流（ViewModel + StateFlow），全 App 统一
  主题与配色。

## 支持的协议 / Protocols

SOCKS (4/4a/5) · HTTP(S) · SSH · Shadowsocks · VMess · Trojan · VLESS ·
AnyTLS · ShadowTLS · TUIC · Hysteria 1/2 · WireGuard

Trojan-Go / NaïveProxy / Mieru 需[插件](https://matsuridayo.github.io/nb4a-plugin/)。

## 支持的传输 / Transports

TCP · WebSocket · HTTP/2 · QUIC · gRPC · HTTPUpgrade · **XHTTP**

其中 **XHTTP** 是 sing-box 官方没有的传输，由本项目的 fork 内核实现（见上方「XHTTP 传输」）。

## 构建 / Building

```bash
# 1) 编译内核（需 Go 1.26.x + Android NDK 25.0.8775105）
JAVA_HOME=/path/to/jdk17 \
PATH=$JAVA_HOME/bin:$GOPATH/bin:$PATH \
ANDROID_HOME=/path/to/android-sdk \
ANDROID_NDK_HOME=$ANDROID_HOME/ndk/25.0.8775105 \
./run lib core

# 2) 静态检查 + 单测
./gradlew :app:detekt :core:model:detekt :core:subscription:detekt \
          :core:model:allTests :core:subscription:allTests :app:testOssReleaseUnitTest

# 3) 构建 APK（按 ABI 拆分）
./gradlew app:assembleOssRelease

# 4) 重新生成图标资源（只在改几何或品牌色时需要；需 Pillow + ttf-dejavu）
python3 tools/gen_icon.py
```

推 `v*` tag 会触发 CI 自动构建并发布。`VERSION_NAME` 与 `VERSION_CODE` 在 `nb4a.properties`：
**版本名可以重启（每一轮公开发布从 `1.0.0` 起），但 `VERSION_CODE` 只增不减** —— 否则已装设备
覆盖安装会撞上 `INSTALL_FAILED_VERSION_DOWNGRADE`。

`app/src/main/res` 下的图标资源全部由 `tools/gen_icon.py` 生成，不要手改 PNG：改脚本后重跑，
产物必须是确定性的（干净树上 `git status` 为空）。

## 致谢 / Credits

内核 [SagerNet/sing-box](https://github.com/SagerNet/sing-box) ·
[MatsuriDayo/sing-box](https://github.com/MatsuriDayo/sing-box)；
界面 [MatsuriDayo/NekoBoxForAndroid](https://github.com/MatsuriDayo/NekoBoxForAndroid) ·
[shadowsocks/shadowsocks-android](https://github.com/shadowsocks/shadowsocks-android) ·
[SagerNet/SagerNet](https://github.com/SagerNet/SagerNet)；
Web Dashboard [MetaCubeX/Yacd-meta](https://github.com/MetaCubeX/Yacd-meta)。

## License

GPL-3.0。本项目是 NekoBoxForAndroid 的修改版本，原始版权归原作者所有，详见 `LICENSE`。
