package com.justnpcx.mirage.core.subscription

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClashProxyOptionsTest {

    @Test
    fun normalizeCipherMapsDummyToNone() {
        assertEquals("none", ClashProxyOptions.normalizeCipher("dummy"))
        assertEquals("aes-256-gcm", ClashProxyOptions.normalizeCipher("aes-256-gcm"))
    }

    @Test
    fun parsesTlsFields() {
        val o = ClashProxyOptions.from(
            mapOf(
                "tls" to true,
                "sni" to "example.com",
                "alpn" to listOf("h2", "http/1.1"),
                "skip-cert-verify" to true,
                "client-fingerprint" to "chrome",
            )
        )
        assertEquals("tls", o.security)
        assertEquals("example.com", o.sni)
        assertEquals("h2\nhttp/1.1", o.alpn)
        assertEquals(true, o.allowInsecure)
        assertEquals("chrome", o.fingerprint)
    }

    @Test
    fun tlsFalseClearsSecurity() {
        assertEquals("", ClashProxyOptions.from(mapOf("tls" to false)).security)
    }

    @Test
    fun servernameIsAcceptedAsSni() {
        assertEquals("a.com", ClashProxyOptions.from(mapOf("servername" to "a.com")).sni)
    }

    @Test
    fun parsesRealityOptions() {
        val o = ClashProxyOptions.from(
            mapOf("reality-opts" to mapOf("public-key" to "PBK", "short-id" to "abcd"))
        )
        assertEquals("tls", o.security)
        assertEquals("PBK", o.realityPublicKey)
        assertEquals("abcd", o.realityShortId)
    }

    @Test
    fun networkMapping() {
        assertEquals("http", ClashProxyOptions.from(mapOf("network" to "h2")).network)
        assertEquals("http", ClashProxyOptions.from(mapOf("network" to "http")).network)
        assertEquals("ws", ClashProxyOptions.from(mapOf("network" to "ws")).network)
        assertEquals("grpc", ClashProxyOptions.from(mapOf("network" to "grpc")).network)
        assertEquals("", ClashProxyOptions.from(emptyMap()).network)
    }

    @Test
    fun parsesWebsocketOptions() {
        val o = ClashProxyOptions.from(
            mapOf(
                "ws-opts" to mapOf(
                    "headers" to mapOf("Host" to "ws.example.com"),
                    "path" to "/ws",
                    "max-early-data" to 2048L,
                    "early-data-header-name" to "Sec-WebSocket-Protocol",
                )
            )
        )
        assertEquals("ws.example.com", o.host)
        assertEquals("/ws", o.path)
        assertEquals(2048, o.maxEarlyData)
        assertEquals("Sec-WebSocket-Protocol", o.earlyDataHeaderName)
    }

    @Test
    fun v2rayHttpUpgradeSwitchesNetwork() {
        val o = ClashProxyOptions.from(
            mapOf("network" to "ws", "ws-opts" to mapOf("v2ray-http-upgrade" to true))
        )
        assertEquals("httpupgrade", o.network)
    }

    @Test
    fun parsesH2Options() {
        val o = ClashProxyOptions.from(
            mapOf("h2-opts" to mapOf("host" to listOf("a.com", "b.com"), "path" to "/h2"))
        )
        assertEquals("a.com\nb.com", o.host)
        assertEquals("/h2", o.path)
    }

    @Test
    fun parsesHttpOptions() {
        val o = ClashProxyOptions.from(
            mapOf("http-opts" to mapOf("path" to listOf("/p1", "/p2"), "headers" to mapOf("Host" to listOf("h.com"))))
        )
        assertEquals("/p1\n/p2", o.path)
        assertEquals("h.com", o.host)
    }

    @Test
    fun parsesGrpcServiceName() {
        assertEquals("svc", ClashProxyOptions.from(mapOf("grpc-opts" to mapOf("grpc-service-name" to "svc"))).path)
    }

    @Test
    fun parsesSmux() {
        val o = ClashProxyOptions.from(
            mapOf("smux" to mapOf("enabled" to true, "max-streams" to 8L, "padding" to false))
        )
        assertEquals(true, o.enableMux)
        assertEquals(8, o.muxConcurrency)
        assertEquals(false, o.muxPadding)
    }

    @Test
    fun absentMuxFieldsStayNull() {
        val o = ClashProxyOptions.from(emptyMap())
        assertNull(o.enableMux)
        assertNull(o.muxConcurrency)
        assertNull(o.muxPadding)
        assertNull(o.enableEch)
        assertNull(o.maxEarlyData)
        // presence-sensitive fields must stay null so callers can leave the target untouched
        assertNull(o.security)
        assertNull(o.allowInsecure)
    }

    @Test
    fun parsesEch() {
        assertEquals(true, ClashProxyOptions.from(mapOf("ech-opts" to mapOf("enable" to true))).enableEch)
    }

    @Test
    fun buildsObfsPlugin() {
        val plugin = ClashProxyOptions.shadowsocksPlugin(
            mapOf("plugin" to "obfs", "plugin-opts" to mapOf("mode" to "http", "host" to "h.com"))
        )
        assertEquals("obfs-local;obfs=http;obfs-host=h.com", plugin)
    }

    @Test
    fun buildsV2rayPlugin() {
        val plugin = ClashProxyOptions.shadowsocksPlugin(
            mapOf(
                "plugin" to "v2ray-plugin",
                "plugin-opts" to mapOf("mode" to "websocket", "host" to "h.com", "path" to "/p", "mux" to "true"),
            )
        )
        assertEquals("v2ray-plugin;mode=websocket;host=h.com;path=/p;mux=8", plugin)
    }

    @Test
    fun noPluginYieldsEmptyString() {
        assertEquals("", ClashProxyOptions.shadowsocksPlugin(mapOf("plugin" to "unknown")))
        assertEquals("", ClashProxyOptions.shadowsocksPlugin(emptyMap()))
    }
}
