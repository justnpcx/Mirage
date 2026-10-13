package com.justnpcx.mirage.fmt.snell

import moe.matsuri.nb4a.proxy.snell.SnellBean
import moe.matsuri.nb4a.proxy.snell.buildSingBoxOutboundSnellBean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Snell (1.14). v4 and v6 share the outer shape but take different extra options, and sing-box
 * merges whichever set matches the version inline -- so emitting the wrong set is a config error,
 * not a harmless extra field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SnellOutboundTest {

    private fun bean(version: Int) = SnellBean().apply {
        initializeDefaultValues()
        serverAddress = "example.com"
        serverPort = 443
        this.version = version
        psk = "secret"
    }

    private fun buildMap(bean: SnellBean) = buildSingBoxOutboundSnellBean(bean).asMap()

    @Test
    fun v4EmitsItsObfsOptionsAndNotMode() {
        val map = buildMap(bean(4).apply {
            obfsMode = "tls"
            obfsHost = "a.com"
            mode = "unshaped"
        })

        assertEquals("snell", map["type"])
        assertEquals("example.com", map["server"])
        assertEquals(443, (map["server_port"] as Number).toInt())
        assertEquals(4, (map["version"] as Number).toInt())
        assertEquals("secret", map["psk"])
        assertEquals("tls", map["obfs_mode"])
        assertEquals("a.com", map["obfs_host"])
        // `mode` belongs to v6
        assertFalse(map.containsKey("mode"))
    }

    @Test
    fun v6EmitsItsModeAndNotObfsOptions() {
        val map = buildMap(bean(6).apply {
            mode = "unshaped"
            obfsMode = "tls"
            obfsHost = "a.com"
        })

        assertEquals(6, (map["version"] as Number).toInt())
        assertEquals("unshaped", map["mode"])
        assertFalse(map.containsKey("obfs_mode"))
        assertFalse(map.containsKey("obfs_host"))
    }

    @Test
    fun optionalFieldsAreOmittedWhenUnset() {
        val map = buildMap(bean(4))

        assertFalse(map.containsKey("userkey"))
        assertFalse(map.containsKey("reuse"))
        assertFalse(map.containsKey("network"))
        assertFalse(map.containsKey("obfs_mode"))
        assertFalse(map.containsKey("obfs_host"))
    }

    @Test
    fun reuseAndNetworkAndUserKeyAreEmitted() {
        val map = buildMap(bean(6).apply {
            reuse = true
            network = "tcp"
            userKey = "uk"
        })

        assertEquals(true, map["reuse"])
        assertEquals(listOf("tcp"), map["network"])
        assertEquals("uk", map["userkey"])
    }
}
