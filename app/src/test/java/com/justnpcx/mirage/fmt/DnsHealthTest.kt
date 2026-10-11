package com.justnpcx.mirage.fmt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the DNS reachability probe that works around sing-box having no DNS failover
 * (issue #1176).
 */
class DnsHealthTest {

    @Test
    fun mapsHttpsAddressToPort443AndDropsThePath() {
        assertEquals("223.5.5.5" to 443, DnsHealth.probeTarget("https://223.5.5.5/dns-query"))
        assertEquals("dns.google" to 443, DnsHealth.probeTarget("https://dns.google/dns-query"))
    }

    @Test
    fun mapsTlsQuicToPort853() {
        assertEquals("8.8.8.8" to 853, DnsHealth.probeTarget("tls://8.8.8.8"))
        assertEquals("1.1.1.1" to 853, DnsHealth.probeTarget("quic://1.1.1.1"))
    }

    @Test
    fun mapsPlainAndUdpTcpAddressesToPort53() {
        assertEquals("1.1.1.1" to 53, DnsHealth.probeTarget("1.1.1.1"))
        assertEquals("9.9.9.9" to 53, DnsHealth.probeTarget("udp://9.9.9.9"))
        assertEquals("8.8.8.8" to 53, DnsHealth.probeTarget("tcp://8.8.8.8"))
    }

    @Test
    fun honoursAnExplicitPort() {
        assertEquals("1.1.1.1" to 5353, DnsHealth.probeTarget("tls://1.1.1.1:5353"))
        assertEquals("1.1.1.1" to 8443, DnsHealth.probeTarget("https://1.1.1.1:8443/dns-query"))
    }

    @Test
    fun stripsIpv6Brackets() {
        assertEquals("2001:4860:4860::8888" to 443, DnsHealth.probeTarget("https://[2001:4860:4860::8888]/dns-query"))
    }

    @Test
    fun nonNetworkResolversAreNotProbed() {
        assertNull(DnsHealth.probeTarget("local"))
        assertNull(DnsHealth.probeTarget("dhcp://wlan0"))
        assertNull(DnsHealth.probeTarget(""))
    }

    @Test
    fun singleCandidateIsReturnedWithoutProbing() {
        val one = listOf("https://223.5.5.5/dns-query")
        assertEquals(one, DnsHealth.reorder(one))
    }

    @Test
    fun emptyListStaysEmpty() {
        assertEquals(emptyList<String>(), DnsHealth.reorder(emptyList()))
    }

    @Test
    fun unreachableCandidatesAreKeptInTheirOriginalOrder() {
        DnsHealth.invalidate()
        // loopback ports that nothing listens on -> probe fails fast (connection refused)
        val candidates = listOf("https://127.0.0.1:1/dns-query", "https://127.0.0.1:2/dns-query")
        assertEquals(candidates, DnsHealth.reorder(candidates))
    }
}
