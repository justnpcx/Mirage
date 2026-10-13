package com.justnpcx.mirage.fmt.hysteria

import com.justnpcx.mirage.fmt.BuildSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The sing-box 1.14 Hysteria2 additions: gecko obfuscation, the BBR profile, randomised port
 * hopping and the Chrome QUIC fingerprint switch.
 *
 * Every new field has to survive being read off a bean that was saved before it existed (those
 * deserialize as null, and requireBean does not re-apply defaults), which is what the "unset" cases
 * below pin down.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class Hysteria2OutboundTest {

    private class FakeSettings(override val globalAllowInsecure: Boolean) : BuildSettings

    private fun baseBean() = HysteriaBean().apply {
        initializeDefaultValues()
        protocolVersion = 2
        serverAddress = "example.com"
        serverPorts = "443"
        authPayload = "pw"
    }

    private fun buildMap(bean: HysteriaBean) =
        buildSingBoxOutboundHysteriaBean(bean, FakeSettings(false)).asMap()

    @Test
    fun geckoAddsPacketSizesOnTopOfThePassword() {
        val map = buildMap(baseBean().apply {
            obfuscation = "secret"
            obfsType = "gecko"
            geckoMinPacketSize = 100
            geckoMaxPacketSize = 200
        })

        @Suppress("UNCHECKED_CAST")
        val obfs = map["obfs"] as Map<*, *>
        assertEquals("gecko", obfs["type"])
        // both obfs types need a password -- sing-box refuses to start without one
        assertEquals("secret", obfs["password"])
        assertEquals(100, (obfs["min_packet_size"] as Number).toInt())
        assertEquals(200, (obfs["max_packet_size"] as Number).toInt())
    }

    @Test
    fun obfsIsOmittedWithoutAPassword() {
        // gecko with no password would make sing-box fatal with "missing obfs password"
        val map = buildMap(baseBean().apply { obfsType = "gecko" })
        assertFalse(map.containsKey("obfs"))
    }

    @Test
    fun aLegacyLinkStillGetsSalamander() {
        // obfsType is absent -- the link predates gecko -- but a password is set
        val map = buildMap(baseBean().apply { obfuscation = "secret" })

        @Suppress("UNCHECKED_CAST")
        val obfs = map["obfs"] as Map<*, *>
        assertEquals("salamander", obfs["type"])
        assertEquals("secret", obfs["password"])
    }

    @Test
    fun bbrProfileHopIntervalMaxAndChromeParrotAreEmitted() {
        val map = buildMap(baseBean().apply {
            hopInterval = 10
            hopIntervalMax = 30
            bbrProfile = "aggressive"
            disableChromeParrot = true
        })

        assertEquals("30s", map["hop_interval_max"])
        assertEquals("aggressive", map["bbr_profile"])
        assertEquals(true, map["disable_chrome_parrot"])
    }

    @Test
    fun theNewFieldsAreOmittedWhenUnset() {
        val map = buildMap(baseBean())

        assertFalse(map.containsKey("hop_interval_max"))
        assertFalse(map.containsKey("bbr_profile"))
        assertFalse(map.containsKey("disable_chrome_parrot"))
        assertFalse(map.containsKey("obfs"))
    }

    @Test
    fun parsingALinkRecordsTheObfsType() {
        val bean = parseHysteria2("hy2://pw@example.com:443?obfs=gecko&sni=a.com#n")
        assertEquals("gecko", bean.obfsType)
    }

    @Test
    fun aRoundTrippedLinkKeepsGecko() {
        val bean = baseBean().apply { obfsType = "gecko" }
        val uri = bean.toUri()
        assertEquals("gecko", parseHysteria2(uri).obfsType)
    }
}
