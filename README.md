# Mirage for Android

[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-orange.svg)](https://www.gnu.org/licenses/gpl-3.0)

基于 sing-box 的 Android 通用代理客户端。

A universal proxy client for Android, powered by sing-box.

> Mirage 由 [NekoBoxForAndroid](https://github.com/MatsuriDayo/NekoBoxForAndroid) 衍生而来，并持续独立演进。

## 特性 / Features

- **sing-box `1.14.x` 内核**：由上游的 `1.12.19` 升级到 `1.14.x`
  （基于 [MatsuriDayo/sing-box](https://github.com/MatsuriDayo/sing-box) 的 `1.14.x` 分支，见 [justnpcx/sing-box](https://github.com/justnpcx/sing-box)）。
- **支持 Xray-core v26+ 的 REALITY（后量子）**：
  Xray-core 自 `v26.9.8` 起要求 REALITY 的 ClientHello 携带后量子混合密钥交换 `X25519MLKEM768`。
  上游 sing-box 会主动剥离该 key share，导致与运行新版 Xray 内核（如 3x-ui 的 `v26.10.10`）的服务端握手失败
  (`reality verification failed`)。
  Mirage 依据订阅链接中的 `support-x25519mlkem768=true` 参数决定是否启用该 key share，
  并在计算 REALITY 认证密钥时回退到 `MlkemEcdhe`（行为与 Xray-core / mihomo 一致）。
- **独立应用身份**：包名 `com.justnpcx.mirage`，可与官方 NekoBox 共存安装。

### REALITY / X25519MLKEM768

分享链接示例：

```
vless://<uuid>@host:443?type=tcp&encryption=none&security=reality&flow=xtls-rprx-vision\
&sni=intel.com&fp=chrome&pbk=<public-key>&sid=<short-id>&support-x25519mlkem768=true#node
```

当链接带有 `support-x25519mlkem768=true` 时，客户端会在 ClientHello 中保留 `X25519MLKEM768` 混合 key share；
否则维持旧行为（剥离），以兼容旧版 REALITY 服务端。

- **WireGuard 已迁移到 endpoint**：sing-box `1.13` 起移除了 WireGuard **outbound**（改为 endpoint）。
  Mirage 现在生成 `endpoints[]`（单 peer、`allowed_ips: 0.0.0.0/0, ::/0`）。由于 endpoint 在 sing-box 中同样可按 tag 寻址，
  selector 与路由规则无需改动。

## 下载 / Downloads

[GitHub Releases](https://github.com/justnpcx/Mirage/releases)

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

CI 使用 [`.github/workflows/build.yml`](.github/workflows/build.yml)：
先由 Go 编译 `libcore`（sing-box 的 gomobile 绑定），再编译并签名 APK。
签名使用仓库内的 `release.keystore`（自签名，凭据见 `keystore.properties`）。
手动触发 `Build Mirage` workflow 即可产出 APK；推送 `v*` tag 会自动发布 Release。

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
