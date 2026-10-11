package io.nekohasekai.sagernet.fmt.wireguard

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the WireGuard outbound -> endpoint migration (sing-box 1.13+).
 *
 * Asserts the exact JSON shape handed to sing-box, since the option classes are hand-maintained.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class WireGuardEndpointTest {

    private fun sampleBean() = WireGuardBean().apply {
        serverAddress = "162.159.192.1"
        serverPort = 2408
        localAddress = "172.16.0.2/32\n2606:4700:110:8a12::2/128"
        privateKey = "cHJpdmF0ZS1rZXk="
        peerPublicKey = "cHVibGljLWtleQ=="
        peerPreSharedKey = ""
        mtu = 1280
        reserved = "1, 2, 3"
        initializeDefaultValues()
    }

    @Test
    fun buildsWireGuardEndpoint() {
        val map = buildSingBoxEndpointWireguardBean(sampleBean()).asMap()

        assertEquals("wireguard", map["type"])
        assertEquals(1280, (map["mtu"] as Number).toInt())
        assertEquals(listOf("172.16.0.2/32", "2606:4700:110:8a12::2/128"), map["address"])
        assertEquals("cHJpdmF0ZS1rZXk=", map["private_key"])
    }

    @Test
    fun buildsSinglePeerWithRequiredAllowedIps() {
        val map = buildSingBoxEndpointWireguardBean(sampleBean()).asMap()

        @Suppress("UNCHECKED_CAST")
        val peers = map["peers"] as List<Map<String, Any?>>
        assertEquals(1, peers.size)

        val peer = peers[0]
        assertEquals("162.159.192.1", peer["address"])
        assertEquals(2408, (peer["port"] as Number).toInt())
        assertEquals("cHVibGljLWtleQ==", peer["public_key"])
        // allowed_ips is required by sing-box; a single-peer client routes everything through it.
        assertEquals(listOf("0.0.0.0/0", "::/0"), peer["allowed_ips"])
    }

    @Test
    fun encodesReservedAsBase64() {
        val map = buildSingBoxEndpointWireguardBean(sampleBean()).asMap()
        @Suppress("UNCHECKED_CAST")
        val peers = map["peers"] as List<Map<String, Any?>>
        // bytes [1, 2, 3] -> base64 "AQID" (sing-box accepts a base64 string for []uint8)
        assertEquals("AQID", peers[0]["reserved"])
    }

    @Test
    fun omitsReservedWhenBlank() {
        val bean = sampleBean().apply { reserved = "" }
        val map = buildSingBoxEndpointWireguardBean(bean).asMap()
        @Suppress("UNCHECKED_CAST")
        val peers = map["peers"] as List<Map<String, Any?>>
        assertEquals(null, peers[0]["reserved"])
    }
}
