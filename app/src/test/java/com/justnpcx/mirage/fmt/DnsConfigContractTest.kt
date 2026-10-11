package com.justnpcx.mirage.fmt

import moe.matsuri.nb4a.SingBoxOptions.DNSRule_DefaultOptions
import moe.matsuri.nb4a.SingBoxOptions.DNSServerOptions
import moe.matsuri.nb4a.SingBoxOptions.Inbound_TunOptions
import moe.matsuri.nb4a.SingBoxOptions.Rule_DefaultOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Locks down the sing-box 1.12+/1.14 config contract.
 *
 * sing-box 1.14 removed the legacy DNS and inbound forms; a single wrong field name makes the
 * whole config fail to decode at runtime. These tests assert the serialized JSON keys directly,
 * so regressions are caught on the JVM instead of on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class DnsConfigContractTest {

    @Test
    fun dnsServerUsesTypedFormAndHasNoStrategyField() {
        val map = DNSServerOptions().apply {
            type = "udp"
            server = "8.8.8.8"
            tag = "dns-remote"
            domain_resolver = "dns-direct"
        }.asMap()

        assertEquals("udp", map["type"])
        assertEquals("8.8.8.8", map["server"])
        assertEquals("dns-direct", map["domain_resolver"])
        // per-server `strategy` was removed in 1.12+ (it lives on the dns object / rules now)
        assertFalse(map.containsKey("strategy"))
        // the legacy `address` form is gone
        assertFalse(map.containsKey("address"))
    }

    @Test
    fun fakeIpIsATypedServer() {
        val map = DNSServerOptions().apply {
            type = "fakeip"
            tag = "dns-fake"
            inet4_range = "198.18.0.0/15"
            inet6_range = "fc00::/18"
        }.asMap()

        assertEquals("fakeip", map["type"])
        assertEquals("198.18.0.0/15", map["inet4_range"])
        assertFalse(map.containsKey("address"))
    }

    @Test
    fun dnsRuleCarriesActionRcodeAndStrategy() {
        val map = DNSRule_DefaultOptions().apply {
            action = "predefined"
            rcode = "NOERROR"
        }.asMap()
        assertEquals("predefined", map["action"])
        assertEquals("NOERROR", map["rcode"])

        val resolve = DNSRule_DefaultOptions().apply {
            server = "dns-direct"
            strategy = "prefer_ipv4"
        }.asMap()
        assertEquals("prefer_ipv4", resolve["strategy"])
    }

    @Test
    fun routeRuleCarriesResolveAndSniffActions() {
        val resolve = Rule_DefaultOptions().apply {
            inbound = listOf("tun-in")
            action = "resolve"
            strategy = "prefer_ipv4"
        }.asMap()
        assertEquals("resolve", resolve["action"])
        assertEquals("prefer_ipv4", resolve["strategy"])

        val sniff = Rule_DefaultOptions().apply {
            inbound = listOf("tun-in")
            action = "sniff"
        }.asMap()
        assertEquals("sniff", sniff["action"])
    }

    @Test
    fun tunInboundUsesMergedAddressField() {
        val map = Inbound_TunOptions().apply {
            type = "tun"
            tag = "tun-in"
            address = listOf("172.19.0.1/28", "fdfe:dcba:9876::1/126")
        }.asMap()

        assertEquals(listOf("172.19.0.1/28", "fdfe:dcba:9876::1/126"), map["address"])
        // sing-box 1.12 removed the legacy tun address fields
        assertFalse(map.containsKey("inet4_address"))
        assertFalse(map.containsKey("inet6_address"))
    }
}
